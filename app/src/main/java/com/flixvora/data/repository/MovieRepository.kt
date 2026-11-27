package com.flixvora.data.repository

import com.flixvora.data.local.FavoriteEntity
import com.flixvora.data.local.HistoryEntity
import com.flixvora.data.local.MovieDao
import com.flixvora.data.model.*
import com.flixvora.data.network.AniFamApi
import com.flixvora.data.network.AniFamRetrofitClient
import com.flixvora.data.network.ExtendedApi
import com.flixvora.data.network.GetListRequest
import com.flixvora.data.network.MovieBoxApi
import com.flixvora.data.network.MovieBoxRetrofitClient
import com.flixvora.data.preferences.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class MovieRepository(
    private val api: MovieBoxApi,
    private val dao: MovieDao,
    private val prefs: PreferencesManager? = null
) {
    // Room Databases operations
    val favorites: Flow<List<FavoriteEntity>> = dao.getAllFavorites()
    val history: Flow<List<HistoryEntity>> = dao.getAllHistory()

    suspend fun insertFavorite(favorite: FavoriteEntity) = withContext(Dispatchers.IO) {
        dao.insertFavorite(favorite)
    }

    suspend fun deleteFavorite(id: String) = withContext(Dispatchers.IO) {
        dao.deleteFavoriteById(id)
    }

    suspend fun clearFavorites() = withContext(Dispatchers.IO) {
        dao.deleteAllFavorites()
    }

    suspend fun syncFavorites(favorites: List<FavoriteEntity>) = withContext(Dispatchers.IO) {
        dao.syncFavorites(favorites)
    }

    fun isFavorite(id: String): Flow<Boolean> = dao.isFavorite(id)
    suspend fun isFavoriteDirect(id: String): Boolean = withContext(Dispatchers.IO) {
        dao.isFavoriteDirect(id)
    }

    suspend fun insertHistory(history: HistoryEntity) = withContext(Dispatchers.IO) {
        dao.insertHistory(history)
    }

    suspend fun deleteHistory(id: String) = withContext(Dispatchers.IO) {
        dao.deleteHistoryById(id)
    }

    suspend fun getHistoryById(id: String): HistoryEntity? = withContext(Dispatchers.IO) {
        dao.getHistoryById(id)
    }

    // AniFam API & Helper
    private val aniFamApi: AniFamApi
        get() = AniFamRetrofitClient.getApi(prefs)

    val isAniFam: Boolean
        get() = prefs?.isAniFam == true

    /**
     * Whether adult (18+) rows may be returned. Read live on every call so a
     * toggle in Settings takes effect without rebuilding the repository.
     */
    val adultContentEnabled: Boolean
        get() = prefs?.adultContentEnabled == true

    /**
     * Single gate for adult rows. Every list-returning read funnels through
     * here, so a new call path cannot forget the check.
     */
    private fun List<MovieSubject>.filterAdultMovies(): List<MovieSubject> =
        if (adultContentEnabled) this else this.filterNot { it.isAdult }

    private fun List<CategorySection>.filterAdultSections(): List<CategorySection> =
        if (adultContentEnabled) this else this.mapNotNull { section ->
            val kept = section.items.filterNot { it.isAdult }
            if (kept.isEmpty()) null else section.copy(items = kept)
        }

    private val detailCache = ConcurrentHashMap<String, MovieSubject>()
    private val aniFamDetailCache = ConcurrentHashMap<Int, AniFamAnimeDetail>()
    private val aniFamEpisodeMap = ConcurrentHashMap<String, Int>() // "subjectId_epNumber" -> episodeId
    private val aniFamEpisodeItemMap = ConcurrentHashMap<Int, AniFamEpisodeItem>()
    private val aniFamGenresCache = CopyOnWriteArrayList<AniFamGenreItem>()
    private val memoryAnimeRails = CopyOnWriteArrayList<CategorySection>()
    private val memoryTrendingCache = CopyOnWriteArrayList<MovieSubject>()
    private val memoryCategoryCache = ConcurrentHashMap<Int, List<MovieSubject>>()
    private val memorySectionsCache = ConcurrentHashMap<Int, List<CategorySection>>()

    private val moshi = com.squareup.moshi.Moshi.Builder()
        .add(MovieSubjectAdapter())
        .build()
    private val sectionsListAdapter: com.squareup.moshi.JsonAdapter<List<CategorySection>> = moshi.adapter(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, CategorySection::class.java)
    )
    private val moviesListAdapter: com.squareup.moshi.JsonAdapter<List<MovieSubject>> = moshi.adapter(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, MovieSubject::class.java)
    )

    private fun getCacheFile(filename: String): java.io.File? {
        val dir = prefs?.context?.cacheDir ?: return null
        return java.io.File(dir, filename)
    }

    private fun saveSectionsToDisk(key: String, sections: List<CategorySection>) {
        try {
            val file = getCacheFile("feed_sec_${key}.json") ?: return
            file.writeText(sectionsListAdapter.toJson(sections))
        } catch (_: Exception) {}
    }

    private fun loadSectionsFromDisk(key: String): List<CategorySection>? {
        try {
            val file = getCacheFile("feed_sec_${key}.json") ?: return null
            if (!file.exists()) return null
            val json = file.readText()
            if (json.isBlank()) return null
            return sectionsListAdapter.fromJson(json)
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Detail rows are cached on disk too: feeds are served from disk on a cold
     * start, so without this the episode list had nothing to read until the
     * network detail call came back and the season view sat on a spinner.
     */
    private fun saveDetailToDisk(subjectId: String, subject: MovieSubject) {
        try {
            val file = getCacheFile("detail_${subjectId.replace('/', '_')}.json") ?: return
            file.writeText(moviesListAdapter.toJson(listOf(subject)))
        } catch (_: Exception) {}
    }

    private fun loadDetailFromDisk(subjectId: String): MovieSubject? {
        try {
            val file = getCacheFile("detail_${subjectId.replace('/', '_')}.json") ?: return null
            if (!file.exists()) return null
            val json = file.readText()
            if (json.isBlank()) return null
            return moviesListAdapter.fromJson(json)?.firstOrNull()
        } catch (_: Exception) {
            return null
        }
    }

    private fun saveMoviesToDisk(key: String, items: List<MovieSubject>) {
        try {
            val file = getCacheFile("feed_mov_${key}.json") ?: return
            file.writeText(moviesListAdapter.toJson(items))
        } catch (_: Exception) {}
    }

    private fun loadMoviesFromDisk(key: String): List<MovieSubject>? {
        try {
            val file = getCacheFile("feed_mov_${key}.json") ?: return null
            if (!file.exists()) return null
            val json = file.readText()
            if (json.isBlank()) return null
            return moviesListAdapter.fromJson(json)
        } catch (_: Exception) {
            return null
        }
    }

    fun getCachedCategorySections(categoryId: Int): List<CategorySection> {
        val mem = memorySectionsCache[categoryId]
        if (!mem.isNullOrEmpty()) return mem.filterAdultSections()
        val disk = loadSectionsFromDisk("${if (isAniFam) "anifam" else "moviebox"}_$categoryId")
        if (!disk.isNullOrEmpty()) {
            memorySectionsCache[categoryId] = disk
            return disk.filterAdultSections()
        }
        return emptyList()
    }

    fun getCachedCategoryList(categoryId: Int): List<MovieSubject> {
        val mem = memoryCategoryCache[categoryId]
        if (!mem.isNullOrEmpty()) return mem.filterAdultMovies()
        val disk = loadMoviesFromDisk("${if (isAniFam) "anifam" else "moviebox"}_$categoryId")
        if (!disk.isNullOrEmpty()) {
            memoryCategoryCache[categoryId] = disk
            return disk.filterAdultMovies()
        }
        return emptyList()
    }

    fun getCachedTrending(): List<MovieSubject> {
        if (memoryTrendingCache.isNotEmpty()) return memoryTrendingCache.toList().filterAdultMovies()
        val disk = loadMoviesFromDisk("trending_${if (isAniFam) "anifam" else "moviebox"}")
        if (!disk.isNullOrEmpty()) {
            memoryTrendingCache.clear()
            memoryTrendingCache.addAll(disk)
            return disk.filterAdultMovies()
        }
        return emptyList()
    }

    private suspend fun <T> retryWithBackoff(
        retries: Int = 2,
        initialDelayMs: Long = 400,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        var lastException: Exception? = null
        repeat(retries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (attempt < retries - 1) {
                    kotlinx.coroutines.delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong()
                }
            }
        }
        throw lastException ?: java.io.IOException("Request failed after retries")
    }

    val defaultAniFamGenres = listOf(
        AniFamGenreItem(1, "action", "Action"),
        AniFamGenreItem(22, "action-adventure", "Action & Adventure"),
        AniFamGenreItem(2, "adventure", "Adventure"),
        AniFamGenreItem(23, "animation", "Animation"),
        AniFamGenreItem(5, "comedy", "Comedy"),
        AniFamGenreItem(12, "drama", "Drama"),
        AniFamGenreItem(3, "fantasy", "Fantasy"),
        AniFamGenreItem(19, "horror", "Horror"),
        AniFamGenreItem(40, "isekai", "Isekai"),
        AniFamGenreItem(15, "romance", "Romance"),
        AniFamGenreItem(8, "sci-fi", "Sci-Fi"),
        AniFamGenreItem(13, "sports", "Sports"),
        AniFamGenreItem(9, "supernatural", "Supernatural"),
        AniFamGenreItem(20, "suspense", "Suspense"),
        AniFamGenreItem(29, "thriller", "Thriller"),
        AniFamGenreItem(17, "slice-of-life", "Slice of Life")
    )

    suspend fun getAniFamGenres(): List<AniFamGenreItem> = withContext(Dispatchers.IO) {
        if (aniFamGenresCache.isNotEmpty()) return@withContext aniFamGenresCache.toList()
        try {
            val list = retryWithBackoff { aniFamApi.getGenres() }
            if (list.isNotEmpty()) {
                aniFamGenresCache.clear()
                aniFamGenresCache.addAll(list)
                return@withContext list
            }
        } catch (_: Exception) {}
        defaultAniFamGenres
    }

    /**
     * Anime shelves for the single merged feed.
     *
     * Both catalogues are always on, so this deliberately ignores the
     * provider flag: it reads AniFam directly and labels each rail with its
     * own heading. Cached separately from the MovieBox category caches
     * because it is keyed by nothing but "the anime home page".
     */
    suspend fun getAnimeRails(): List<CategorySection> = withContext(Dispatchers.IO) {
        val cached = memoryAnimeRails
        if (cached.isNotEmpty()) return@withContext cached.filterAdultSections()
        val home = runCatching { retryWithBackoff { aniFamApi.getHome() } }.getOrNull()
        val rails = mutableListOf<CategorySection>()
        if (home != null) {
            if (home.trending.isNotEmpty()) {
                rails.add(CategorySection("Trending Anime", "TRENDING", home.trending.map { it.toMovieSubject() }))
            }
            if (home.top_airing.isNotEmpty()) {
                rails.add(CategorySection("Top Airing Anime", "AIRING", home.top_airing.map { it.toMovieSubject() }))
            }
            if (home.latest.isNotEmpty()) {
                rails.add(CategorySection("Latest Episodes", "LATEST", home.latest.map { it.toMovieSubject() }))
            }
            if (home.most_popular.isNotEmpty()) {
                rails.add(CategorySection("Most Popular Anime", "POPULAR", home.most_popular.map { it.toMovieSubject() }))
            }
            if (home.top_movies.isNotEmpty()) {
                rails.add(CategorySection("Anime Movies", "MOVIES", home.top_movies.map { it.toMovieSubject() }))
            }
            if (home.new_releases.isNotEmpty()) {
                rails.add(CategorySection("New Anime Releases", "NEW", home.new_releases.map { it.toMovieSubject() }))
            }
        }
        if (rails.isEmpty()) {
            rails.addAll(aniFamBrowseRails())
        }
        if (rails.isNotEmpty()) {
            memoryAnimeRails.clear()
            memoryAnimeRails.addAll(rails)
        }
        rails.filterAdultSections()
    }

    /** Real AniFam rows for when /app/home is unavailable or empty. */
    private suspend fun aniFamBrowseRails(): List<CategorySection> {
        val sections = mutableListOf<CategorySection>()
        val popular = runCatching { aniFamApi.browse(sort = "popular", page = 1) }.getOrNull()
            ?.data?.map { it.toMovieSubject() }.orEmpty()
        if (popular.isNotEmpty()) {
            sections.add(CategorySection("Popular Anime", "ANIFAM_POPULAR", popular))
        }
        val latest = runCatching { aniFamApi.browse(sort = "latest", page = 1) }.getOrNull()
            ?.data?.map { it.toMovieSubject() }.orEmpty()
        val fresh = latest.filter { item -> popular.none { it.id == item.id } }
        if (fresh.isNotEmpty()) {
            sections.add(CategorySection("New Anime", "ANIFAM_NEW", fresh))
        }
        return sections
    }

    suspend fun getTrending(page: Int = 1, pageSize: Int = 20): List<MovieSubject> = withContext(Dispatchers.IO) {
        val providerKey = if (isAniFam) "anifam" else "moviebox"
        val cached = if (page == 1) getCachedTrending() else emptyList()
        if (isAniFam) {
            try {
                val home = retryWithBackoff { aniFamApi.getHome() }
                val list = (if (home.trending.isNotEmpty()) home.trending else home.spotlight).map { it.toMovieSubject() }
                if (list.isNotEmpty()) {
                    memoryTrendingCache.clear()
                    memoryTrendingCache.addAll(list)
                    if (page == 1) saveMoviesToDisk("trending_anifam", list)
                }
                (if (list.isNotEmpty()) list else cached).filterAdultMovies()
            } catch (e: Exception) {
                cached.ifEmpty { memoryTrendingCache.toList() }.filterAdultMovies()
            }
        } else {
            try {
                val response = retryWithBackoff { api.getTrending(page, pageSize) }
                val list = response.data?.let { data ->
                    if (data.list.isNotEmpty()) data.list else data.items
                } ?: emptyList()
                if ((response.code == 200 || response.code == 0) && list.isNotEmpty()) {
                    if (page == 1) {
                        memoryTrendingCache.clear()
                        memoryTrendingCache.addAll(list)
                        saveMoviesToDisk("trending_moviebox", list)
                    }
                    list.filterAdultMovies()
                } else {
                    cached.ifEmpty { memoryTrendingCache.toList() }.filterAdultMovies()
                }
            } catch (e: Exception) {
                cached.ifEmpty { memoryTrendingCache.toList() }.filterAdultMovies()
            }
        }
    }

    suspend fun getDetailRec(subjectId: String): List<MovieSubject> = withContext(Dispatchers.IO) {
        if (isAniFamSubject(subjectId)) {
            val numId = parseAniFamId(subjectId)
            val cached = if (numId != null) aniFamDetailCache[numId] else null
            if (cached != null && cached.recommended.isNotEmpty()) {
                return@withContext cached.recommended.map { it.toMovieSubject() }.filterAdultMovies()
            }
            emptyList()
        } else {
            try {
                val req = mapOf(
                    "subjectId" to subjectId,
                    "page" to 1,
                    "pageSize" to 15
                )
                val response = api.getDetailRec(req)
                val list = response.data?.let { data ->
                    if (data.list.isNotEmpty()) data.list else data.items
                } ?: emptyList()
                if (response.code == 0 || response.code == 200) list.filterAdultMovies() else emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun getCategorySections(categoryId: Int, page: Int = 1, pageSize: Int = 50): List<CategorySection> = withContext(Dispatchers.IO) {
        val providerKey = if (isAniFam) "anifam" else "moviebox"
        val cached = if (page == 1) getCachedCategorySections(categoryId) else (memorySectionsCache[categoryId] ?: emptyList())
        if (isAniFam) {
            try {
                if (categoryId == 0 || categoryId == 1 || categoryId == 2 || categoryId == 5 || categoryId == 8 || categoryId == 18) {
                    val home = runCatching { retryWithBackoff { aniFamApi.getHome() } }
                        .getOrDefault(AniFamHomeResponse())
                    val sections = mutableListOf<CategorySection>()
                    if (home.spotlight.isNotEmpty()) {
                        sections.add(CategorySection("Spotlight", "SPOTLIGHT", home.spotlight.map { it.toMovieSubject() }))
                    }
                    if (home.trending.isNotEmpty()) {
                        sections.add(CategorySection("Trending Anime", "TRENDING", home.trending.map { it.toMovieSubject() }))
                    }
                    if (home.latest.isNotEmpty()) {
                        sections.add(CategorySection("Latest Episodes", "LATEST", home.latest.map { it.toMovieSubject() }))
                    }
                    if (home.new_releases.isNotEmpty()) {
                        sections.add(CategorySection("New Releases", "NEW", home.new_releases.map { it.toMovieSubject() }))
                    }
                    if (home.top_airing.isNotEmpty()) {
                        sections.add(CategorySection("Top Airing", "AIRING", home.top_airing.map { it.toMovieSubject() }))
                    }
                    if (home.most_popular.isNotEmpty()) {
                        sections.add(CategorySection("Most Popular", "POPULAR", home.most_popular.map { it.toMovieSubject() }))
                    }
                    if (home.top_movies.isNotEmpty()) {
                        sections.add(CategorySection("Top Anime Movies", "MOVIES", home.top_movies.map { it.toMovieSubject() }))
                    }
                    if (sections.isEmpty()) {
                        sections.addAll(aniFamBrowseRails())
                    }
                    if (sections.isNotEmpty()) {
                        memorySectionsCache[categoryId] = sections
                        if (page == 1) saveSectionsToDisk("${providerKey}_$categoryId", sections)
                    }
                    (if (sections.isNotEmpty()) sections else cached).filterAdultSections()
                } else {
                    val genreItem = getAniFamGenres().find { it.id == categoryId }
                    val slug = genreItem?.slug
                    val browse = retryWithBackoff { aniFamApi.browse(genre = slug, page = page) }
                    val items = browse.data.map { it.toMovieSubject() }
                    val sec = listOf(CategorySection(genreItem?.name ?: "Anime", "GENRE", items))
                    if (items.isNotEmpty()) {
                        memorySectionsCache[categoryId] = sec
                        if (page == 1) saveSectionsToDisk("${providerKey}_$categoryId", sec)
                    }
                    (if (items.isNotEmpty()) sec else cached).filterAdultSections()
                }
            } catch (e: Exception) {
                cached.filterAdultSections()
            }
        } else {
            try {
                val response = retryWithBackoff { api.getList(GetListRequest(categoryId, page, pageSize)) }
                val sections = response.data?.sections ?: emptyList()
                if ((response.code == 200 || response.code == 0) && sections.isNotEmpty()) {
                    if (page == 1) {
                        memorySectionsCache[categoryId] = sections
                        saveSectionsToDisk("${providerKey}_$categoryId", sections)
                    }
                    sections.filterAdultSections()
                } else {
                    cached.filterAdultSections()
                }
            } catch (e: Exception) {
                cached.filterAdultSections()
            }
        }
    }

    suspend fun getCategoryList(categoryId: Int, page: Int = 1, pageSize: Int = 50): List<MovieSubject> = withContext(Dispatchers.IO) {
        val providerKey = if (isAniFam) "anifam" else "moviebox"
        val cached = if (page == 1) getCachedCategoryList(categoryId) else (memoryCategoryCache[categoryId] ?: emptyList())
        if (isAniFam) {
            val sections = getCategorySections(categoryId, page, pageSize)
            var allItems = sections.flatMap { it.items }
            if (allItems.isEmpty()) {
                allItems = getAnimeRails().flatMap { it.items }
            }
            if (allItems.isNotEmpty()) {
                memoryCategoryCache[categoryId] = allItems
                if (page == 1) saveMoviesToDisk("${providerKey}_$categoryId", allItems)
                allItems.filterAdultMovies()
            } else {
                cached.filterAdultMovies()
            }
        } else {
            try {
                val response = retryWithBackoff { api.getList(GetListRequest(categoryId, page, pageSize)) }
                val sections = response.data?.sections ?: emptyList()
                if (sections.isNotEmpty() && page == 1) {
                    memorySectionsCache[categoryId] = sections
                    saveSectionsToDisk("${providerKey}_$categoryId", sections)
                }
                val list = response.data?.let { data ->
                    if (data.list.isNotEmpty()) data.list else data.items
                } ?: emptyList()
                if ((response.code == 200 || response.code == 0) && list.isNotEmpty()) {
                    if (page == 1) {
                        memoryCategoryCache[categoryId] = list
                        saveMoviesToDisk("${providerKey}_$categoryId", list)
                    }
                    list.filterAdultMovies()
                } else {
                    cached.filterAdultMovies()
                }
            } catch (e: Exception) {
                cached.filterAdultMovies()
            }
        }
    }

    fun getCachedDetail(subjectId: String): MovieSubject? {
        detailCache[subjectId]?.let { return it }
        return loadDetailFromDisk(subjectId)?.also { detailCache[subjectId] = it }
    }

    fun getCachedEpisodes(subjectId: String, season: Int): List<Episode> {
        val detail = getCachedDetail(subjectId) ?: return emptyList()
        val seasonInfo = detail.seasonList.find { it.season == season } ?: return emptyList()
        return seasonInfo.episodeList.map { epInfo ->
            Episode(
                episodeId = "${subjectId}_S${season}_E${epInfo.episode}",
                title = epInfo.title.ifEmpty { "Episode ${epInfo.episode}" },
                episodeNumber = epInfo.episode,
                seasonNumber = season,
                stillPath = epInfo.stillPath
            )
        }
    }

    private fun isAniFamSubject(subjectId: String): Boolean {
        return subjectId.startsWith("anifam_")
    }

    private fun parseAniFamId(subjectId: String): Int? {
        return subjectId.removePrefix("anifam_").toIntOrNull()
    }

    /**
     * Fetches ALL episodes for an AniFam anime by walking through every page.
     * The API may return paginated results (last_page > 1); this collects them all.
     */
    private suspend fun fetchAllAniFamEpisodes(numId: Int): List<com.flixvora.data.model.AniFamEpisodeItem> {
        val allEpisodes = mutableListOf<com.flixvora.data.model.AniFamEpisodeItem>()
        try {
            var page = 1
            while (true) {
                val response = aniFamApi.getEpisodes(numId, page)
                allEpisodes.addAll(response.data)
                // If the API signals pagination via last_page/current_page, use that.
                // Otherwise fall back: if the page had data and we got a full page (~100),
                // try fetching the next page until we get an empty response.
                val hasNext = when {
                    response.last_page != null -> page < (response.last_page ?: 1)
                    response.data.size >= 100 -> true  // assume paginated at 100
                    else -> false
                }
                if (!hasNext) break
                page++
                if (page > 50) break  // safety cap: never fetch more than 50 pages
            }
        } catch (e: Exception) {
            android.util.Log.w("MovieRepository", "fetchAllAniFamEpisodes failed for $numId: ${e.message}")
        }
        return allEpisodes
    }

    suspend fun getDetail(subjectId: String): MovieSubject? = withContext(Dispatchers.IO) {
        val cached = detailCache[subjectId]
        if (cached != null) return@withContext cached

        if (isAniFamSubject(subjectId)) {
            val numId = parseAniFamId(subjectId) ?: return@withContext null
            try {
                val detail = aniFamApi.getAnimeDetail(numId)
                aniFamDetailCache[numId] = detail

                // Pre-fetch ALL episodes (paginated) so seasonList is fully populated
                val epList = fetchAllAniFamEpisodes(numId)

                epList.forEach { ep ->
                    aniFamEpisodeMap["${numId}_${ep.episodeNumber}"] = ep.id
                    aniFamEpisodeItemMap[ep.id] = ep
                }

                // Determine sub/dub options from episodes
                val hasAnySub = epList.any { it.isSubAvailable }
                val hasAnyDub = epList.any { it.isDubAvailable }
                val dubOptions = mutableListOf<String>()
                if (hasAnySub) dubOptions.add("Sub")
                if (hasAnyDub) dubOptions.add("Dub")
                if (dubOptions.isEmpty()) dubOptions.add("Sub")

                val resourceDetectors = mutableListOf<ResourceDetector>()
                if (hasAnySub) {
                    resourceDetectors.add(ResourceDetector("sub", "Sub"))
                    resourceDetectors.add(ResourceDetector("sub_server_2", "Sub 1"))
                    resourceDetectors.add(ResourceDetector("sub_server_3", "Sub 2"))
                    resourceDetectors.add(ResourceDetector("sub_server_4", "Sub 3"))
                    resourceDetectors.add(ResourceDetector("sub_server_5", "Sub 4"))
                }
                if (hasAnyDub) {
                    resourceDetectors.add(ResourceDetector("dub", "Dub"))
                    resourceDetectors.add(ResourceDetector("dub_server_2", "Dub 1"))
                    resourceDetectors.add(ResourceDetector("dub_server_3", "Dub 2"))
                    resourceDetectors.add(ResourceDetector("dub_server_4", "Dub 3"))
                    resourceDetectors.add(ResourceDetector("dub_server_5", "Dub 4"))
                }
                if (resourceDetectors.isEmpty()) {
                    resourceDetectors.add(ResourceDetector("sub", "Sub"))
                }

                val episodeInfoList = epList.map { ep ->
                    EpisodeInfo(
                        episode = ep.episodeNumber,
                        title = ep.title ?: "Episode ${ep.episodeNumber}",
                        duration = detail.duration ?: "24 min",
                        stillPath = detail.fullPosterUrl
                    )
                }

                val movieSubject = MovieSubject(
                    id = if (subjectId.startsWith("anifam_")) subjectId else "anifam_$subjectId",
                    name = detail.displayName,
                    poster = detail.fullPosterUrl,
                    backdrop = detail.fullBannerUrl,
                    description = detail.synopsis ?: "",
                    rating = detail.score?.let { String.format("%.1f", it) },
                    year = detail.type ?: "Anime",
                    genres = detail.genres.map { it.name },
                    isTvShow = detail.type?.equals("Movie", ignoreCase = true) != true,
                    totalSeasons = 1,
                    duration = detail.duration,
                    dubs = dubOptions,
                    resourceDetectors = resourceDetectors,
                    seasonList = listOf(SeasonInfo(season = 1, episodeList = episodeInfoList))
                )

                detailCache[subjectId] = movieSubject
                detailCache[movieSubject.id] = movieSubject
                saveDetailToDisk(subjectId, movieSubject)
                movieSubject
            } catch (e: Exception) {
                null
            }
        } else {
            try {
                val response = api.getDetail(subjectId)
                if ((response.code == 200 || response.code == 0) && response.data != null) {
                    detailCache[subjectId] = response.data
                    saveDetailToDisk(subjectId, response.data)
                    response.data
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getEpisodes(subjectId: String, season: Int): List<Episode> = withContext(Dispatchers.IO) {
        if (isAniFamSubject(subjectId)) {
            val numId = parseAniFamId(subjectId) ?: return@withContext emptyList()
            try {
                val epList = fetchAllAniFamEpisodes(numId)
                val detail = detailCache[subjectId] ?: detailCache["anifam_$numId"] ?: detailCache[numId.toString()]
                val still = detail?.poster ?: ""
                epList.map { ep ->
                    aniFamEpisodeMap["${numId}_${ep.episodeNumber}"] = ep.id
                    aniFamEpisodeItemMap[ep.id] = ep
                    Episode(
                        episodeId = "anifam_${numId}_S1_E${ep.episodeNumber}",
                        title = ep.title ?: "Episode ${ep.episodeNumber}",
                        episodeNumber = ep.episodeNumber,
                        seasonNumber = 1,
                        duration = detail?.duration ?: "24 min",
                        stillPath = still
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            val detail = getDetail(subjectId)
            if (detail != null && detail.seasonList.isNotEmpty()) {
                val seasonInfo = detail.seasonList.find { it.season == season }
                if (seasonInfo != null) {
                    return@withContext seasonInfo.episodeList.map { epInfo ->
                        Episode(
                            episodeId = "${subjectId}_S${season}_E${epInfo.episode}",
                            title = epInfo.title.ifEmpty { "Episode ${epInfo.episode}" },
                            episodeNumber = epInfo.episode,
                            seasonNumber = season,
                            duration = epInfo.duration.ifEmpty { "45 min" },
                            stillPath = epInfo.stillPath
                        )
                    }
                }
            }
            emptyList()
        }
    }

    suspend fun getPlayInfo(
        subjectId: String,
        season: Int,
        episode: Int,
        quality: String = "1080p",
        resourceId: String? = null
    ): PlayInfo = withContext(Dispatchers.IO) {
        if (isAniFamSubject(subjectId)) {
            val numId = parseAniFamId(subjectId) ?: return@withContext PlayInfo()
            val episodeNumber = if (episode > 0) episode else 1
            val epId = aniFamEpisodeMap["${numId}_$episodeNumber"]
                ?: try {
                    val allEps = fetchAllAniFamEpisodes(numId)
                    allEps.forEach { ep ->
                        aniFamEpisodeMap["${numId}_${ep.episodeNumber}"] = ep.id
                        aniFamEpisodeItemMap[ep.id] = ep
                    }
                    allEps.find { it.episodeNumber == episodeNumber }?.id ?: allEps.firstOrNull()?.id
                } catch (e: Exception) { null }

            if (epId == null) return@withContext PlayInfo()

            val track = if (resourceId?.lowercase()?.contains("dub") == true) "dub" else "sub"
            val targetServerId = Regex("_server_(\\d+)").find(resourceId ?: "")?.groupValues?.get(1)?.toIntOrNull()
            try {
                val playResp = aniFamApi.playEpisode(epId, mapOf("track" to track))
                val streams = mutableListOf<StreamItem>()
                val subtitles = mutableListOf<SubtitleItem>()

                var directSource: AniFamPlaySource? = null

                if (targetServerId != null) {
                    try {
                        val resolved = aniFamApi.resolveSource(epId, mapOf("server_id" to targetServerId, "track" to track))
                        if (!resolved.url.isNullOrEmpty()) {
                            directSource = resolved
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("MovieRepository", "Failed to resolve AniFam server $targetServerId: ${e.message}")
                    }
                }

                if (directSource == null || directSource.url.isNullOrEmpty()) {
                    directSource = playResp.sources.find { it.kind == "hls" && !it.url.isNullOrEmpty() }
                }
                if (directSource == null || directSource.url.isNullOrEmpty()) {
                    val sId = playResp.sources.firstOrNull { it.server_id != null }?.server_id ?: 2
                    try {
                        val resolved = aniFamApi.resolveSource(epId, mapOf("server_id" to sId, "track" to track))
                        if (!resolved.url.isNullOrEmpty()) {
                            directSource = resolved
                        }
                    } catch (_: Exception) {}
                }
                if (directSource == null) {
                    directSource = playResp.sources.firstOrNull { !it.url.isNullOrEmpty() }
                }

                if (directSource?.url != null) {
                    val mainUrl = directSource.url
                    val cookies = directSource.media_headers?.entries?.joinToString("; ") { "${it.key}=${it.value}" } ?: ""

                    if (directSource.qualities.isNotEmpty()) {
                        directSource.qualities.forEach { q ->
                            val height = q.height ?: 1080
                            val qLabel = "${height}p"
                            streams.add(StreamItem(quality = qLabel, url = mainUrl, cookie = cookies))
                        }
                    } else {
                        streams.add(StreamItem(quality = "1080p", url = mainUrl, cookie = cookies))
                        streams.add(StreamItem(quality = "720p", url = mainUrl, cookie = cookies))
                    }

                    directSource.subtitles.forEach { sub ->
                        if (!sub.url.isNullOrEmpty()) {
                            val label = sub.label?.trim()
                            val cleanLang = when {
                                !label.isNullOrBlank() -> {
                                    // Remove long uploader tags like "(Subtitled by @Shridhuu)" but keep language name
                                    if (label.contains("English", ignoreCase = true) && label.contains("subtitled by", ignoreCase = true)) {
                                        "English"
                                    } else {
                                        label
                                    }
                                }
                                sub.lang?.equals("en", ignoreCase = true) == true -> "English"
                                !sub.lang.isNullOrBlank() -> sub.lang.uppercase()
                                else -> "English"
                            }
                            subtitles.add(SubtitleItem(language = cleanLang, url = sub.url))
                        }
                    }
                }

                if (streams.isEmpty() && track == "sub") {
                    return@withContext getPlayInfo(subjectId, season, episode, quality, "dub")
                }
                PlayInfo(streamList = streams, subTitleList = subtitles)
            } catch (e: Exception) {
                PlayInfo()
            }
        } else {
            try {
                val primary = api.playInfo(subjectId, season, episode, quality, resourceId)
                if (primary.streamList.isNotEmpty()) return@withContext primary

                // Movies answer at se=0&ep=0 and series at their real
                // season/episode, but a subject's isTvShow flag can disagree with
                // how the catalogue addresses it. Try the equivalent first
                // episode before giving up, and never invent deeper seasons.
                val alternatives = when {
                    season <= 0 && episode <= 0 -> listOf(1 to 1, 0 to 1, 1 to 0)
                    season <= 0 -> listOf(1 to episode)
                    episode <= 0 -> listOf(season to 1)
                    else -> emptyList()
                }
                for ((altSeason, altEpisode) in alternatives) {
                    try {
                        val alt = api.playInfo(subjectId, altSeason, altEpisode, quality, resourceId)
                        if (alt.streamList.isNotEmpty()) return@withContext alt
                    } catch (_: Exception) {
                    }
                }
                primary
            } catch (e: Exception) {
                PlayInfo(emptyList(), emptyList())
            }
        }
    }

    /**
     * Raw (pre-filter) item count of the most recent [search] page. Search
     * pagination must be decided on this: the merged, de-duplicated and
     * adult-filtered list can be shorter than the page the API returned, which
     * used to disable "load more" while later pages still existed.
     */
    @Volatile
    var lastSearchPageRawCount: Int = 0
        private set

    private companion object {
        /** Page size asked of both search backends. */
        const val SEARCH_PAGE_SIZE = 50
    }

    suspend fun search(query: String, page: Int = 1): List<MovieSubject> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()
        val movieBox = try {
            val response = retryWithBackoff { api.search(cleanQuery, page, SEARCH_PAGE_SIZE) }
            val items = response.data?.let { data ->
                if (data.items.isNotEmpty()) data.items else data.list
            } ?: emptyList()
            if ((response.code == 200 || response.code == 0) && items.isNotEmpty()) items else emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        // One search box covers both catalogues; a failure on either side
        // still returns the other side's hits.
        val anime = try {
            retryWithBackoff { aniFamApi.search(cleanQuery, page) }.data.map { it.toMovieSubject() }
        } catch (e: Exception) {
            emptyList()
        }

        lastSearchPageRawCount = maxOf(movieBox.size, anime.size)
        // The local proxy fans one page out to several upstream sub-pages, so a
        // "short" page does not mean the end. Only a page with nothing at all does.
        lastSearchExhausted = movieBox.isEmpty() && anime.isEmpty()
        (movieBox + anime).distinctBy { it.id }.filterAdultMovies()
    }

    /**
     * True when the last [search] page came back short on both catalogues, so
     * there is nothing left to ask for. The two backends paginate independently,
     * hence "both" rather than either.
     */
    @Volatile
    var lastSearchExhausted: Boolean = false
        private set

    suspend fun getSearchSuggestions(query: String? = null): List<String> = withContext(Dispatchers.IO) {
        val movieBox = run {
            try {
                val response = api.getSearchSuggestions(query)
                if ((response.code == 0 || response.code == 200) && response.data.isNotEmpty()) {
                    response.data
                } else if (query.isNullOrBlank()) {
                    emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                if (query.isNullOrBlank()) {
                    emptyList()
                } else {
                    emptyList()
                }
            }
        }

        val anime = if (query.isNullOrBlank()) emptyList() else try {
            aniFamApi.searchSuggest(query).map { it.displayName }
        } catch (e: Exception) {
            emptyList()
        }

        (movieBox + anime).distinct().take(20)
    }

    // ---- Extended official-API features ----
    // Served through the on-device proxy's /api-proxy passthrough, which signs
    // each request and forwards it to the real MovieBox backend.

    private val extApi: ExtendedApi?
        get() = prefs?.let { MovieBoxRetrofitClient.getExtendedApi(it) }

    private suspend fun <T> safeExt(block: suspend (ExtendedApi) -> T): T? = withContext(Dispatchers.IO) {
        val api = extApi ?: return@withContext null
        if (isAniFam) return@withContext null
        try {
            block(api)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getRankingCategories(): List<RankingCategory> =
        safeExt { it.getRankingList(null, 1, 1).data?.categoryList } ?: emptyList()

    suspend fun getRankingList(type: String?, page: Int = 1): List<MovieSubject> =
        safeExt { it.getRankingList(type, page, 30).data?.subjects?.filterAdultMovies() } ?: emptyList()

    suspend fun getGenreTop(genre: String, page: Int = 1): List<MovieSubject> = withContext(Dispatchers.IO) {
        if (isAniFam) {
            val slug = runCatching {
                aniFamApi.getGenres().firstOrNull { it.name.equals(genre, true) }?.slug
            }.getOrNull() ?: genre
            runCatching {
                aniFamApi.browse(genre = slug, sort = "popular", page = page)
                    .data.map { it.toMovieSubject() }.filterAdultMovies()
            }.getOrDefault(emptyList())
        } else {
            safeExt {
                it.getGenreTop(mapOf("type" to "0", "genre" to genre, "page" to page, "pageSize" to 30))
                    .data?.list?.filterAdultMovies()
            } ?: emptyList()
        }
    }

    suspend fun getFilterOptions(categoryId: Int = 2): List<FilterOption> =
        safeExt { it.getFilterOptions(categoryId).data?.typeList?.firstOrNull()?.items } ?: emptyList()

    suspend fun getDailyMovieRec(page: Int = 1): List<MovieSubject> =
        safeExt {
            it.getDailyMovieRec(mapOf("page" to page, "pageSize" to 20)).data?.items?.filterAdultMovies()
        } ?: emptyList()

    suspend fun getTabOperatingSubjects(tabId: Int, page: Int = 1): List<MovieSubject> =
        safeExt {
            it.getTabOperating(tabId, page, 30).data?.items
                ?.flatMap { module ->
                    module.subjects + (module.customData?.items ?: emptyList()).mapNotNull { c -> c.subject }
                }
                ?.filterAdultMovies()
        } ?: emptyList()

    /**
     * Raw, titled modules of a catalogue tab (e.g. "Trending Now", "Top Series").
     *
     * The home UI renders real rails, so it needs the module titles the
     * catalogue sends instead of one flattened list. Promo rows are dropped by
     * exact title; matching substrings removed real content rails.
     */
    suspend fun getTabOperatingModules(tabId: Int, page: Int = 1): List<CategorySection> =
        safeExt {
            it.getTabOperating(tabId, page, 30).data?.items
                ?.mapNotNull { module ->
                    val items = module.subjects +
                        (module.customData?.items ?: emptyList()).mapNotNull { c -> c.subject }
                    val filtered = items.distinctBy { s -> s.id }.filterAdultMovies()
                    if (filtered.isEmpty()) null
                    else CategorySection(
                        title = module.title.ifBlank { "Tab $tabId" },
                        type = module.type.ifBlank { "SUBJECTS_MOVIE" },
                        items = filtered
                    )
                }
        } ?: emptyList()

    suspend fun getShortsFeed(page: Int = 1): List<PostItem> =
        safeExt { it.getShortsFeed(page, 20).data?.items } ?: emptyList()

    suspend fun getSportMatches(page: Int = 1): List<SportMatch> =
        safeExt { it.getSportMatchList(page, 20).data?.list } ?: emptyList()

    suspend fun getSportNews(page: Int = 1): List<SportNewsItem> =
        safeExt { it.getSportNewsTrending(page, 20).data?.items } ?: emptyList()

    suspend fun getTvCountries(): List<TvCountry> =
        safeExt { it.getTvCountries().data?.items } ?: emptyList()

    suspend fun getTvChannels(countryCode: String, page: Int = 1, pageSize: Int = 30): List<TvChannel> =
        safeExt { it.getTvChannelsByCountry(countryCode, page, pageSize).data?.items } ?: emptyList()

    /**
     * Every channel a country publishes.
     *
     * The endpoint serves 10 items per page no matter what pageSize is asked for
     * (India: 289 channels, the US: 588), so a single request showed a fraction
     * of each country. The first response carries the real total, which lets the
     * remaining pages be fetched concurrently instead of in a long serial walk.
     */
    suspend fun getAllTvChannels(countryCode: String, maxPages: Int = 100): List<TvChannel> =
        withContext(Dispatchers.IO) {
            if (isAniFam) return@withContext emptyList()
            val first = try {
                retryWithBackoff { extApi?.getTvChannelsByCountry(countryCode, 1, 30) }?.data
            } catch (e: Exception) {
                null
            } ?: return@withContext emptyList()

            val all = LinkedHashMap<String, TvChannel>()
            first.items.forEach { ch -> if (ch.channelId.isNotEmpty()) all[ch.channelId] = ch }

            val pager = first.pager
            val totalPages = when {
                pager == null -> 1
                pager.totalCount > 0 && pager.perPage > 0 ->
                    ((pager.totalCount + pager.perPage - 1) / pager.perPage).coerceAtMost(maxPages)
                pager.hasMore -> 2
                else -> 1
            }

            if (totalPages > 1) {
                val rest = coroutineScope {
                    (2..totalPages).map { page ->
                        async {
                            try {
                                retryWithBackoff { extApi?.getTvChannelsByCountry(countryCode, page, 30) }?.data?.items
                            } catch (e: Exception) {
                                null
                            }.orEmpty()
                        }
                    }.awaitAll()
                }
                rest.flatten().forEach { ch -> if (ch.channelId.isNotEmpty()) all[ch.channelId] = ch }
            }
            all.values.toList()
        }

    suspend fun getTvChannelStream(channelId: String): String? =
        safeExt { it.getTvChannel(channelId).data?.streams?.firstOrNull { s -> s.url.isNotBlank() }?.url }

    /**
     * Public iptv-org playlist used when the catalogue's live-TV endpoints are
     * unavailable (guest sessions, region blocks, or AniFam mode).
     */
    suspend fun getFallbackTvChannels(countryCode: String = "in"): List<TvChannel> =
        withContext(Dispatchers.IO) {
            val url = "https://iptv-org.github.io/iptv/countries/${countryCode.lowercase()}.m3u"
            val body = try {
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", "Flixvora-Android")
                    .build()
                okhttp3.OkHttpClient.Builder()
                    .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(25, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                    .newCall(request)
                    .execute()
                    .use { if (it.isSuccessful) it.body?.string() else null }
            } catch (_: Exception) {
                null
            } ?: return@withContext emptyList()

            val channels = mutableListOf<TvChannel>()
            var pendingTitle: String? = null
            var pendingGroup: String? = null
            for (line in body.lineSequence()) {
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("#EXTINF") -> {
                        pendingTitle = trimmed.substringAfterLast(',', "").trim().ifBlank { null }
                        pendingGroup = Regex("""group-title="([^"]*)"""")
                            .find(trimmed)?.groupValues?.get(1)?.ifBlank { null }
                    }
                    trimmed.isNotEmpty() && !trimmed.startsWith("#") && pendingTitle != null -> {
                        val channelId = "iptv_${trimmed.hashCode().toUInt().toString(16)}"
                        channels.add(
                            TvChannel(
                                channelId = channelId,
                                title = pendingTitle.orEmpty(),
                                countryCode = countryCode.uppercase(),
                                countryName = pendingGroup ?: "Live",
                                streams = listOf(TvStream(streamId = channelId, url = trimmed, urlType = "hls"))
                            )
                        )
                        pendingTitle = null
                        pendingGroup = null
                    }
                }
            }
            channels.distinctBy { it.channelId }
        }

    suspend fun getComments(topicId: String, page: Int = 1): List<CommentItem> =
        safeExt { it.getComments(topicId, 1, page, 20).data?.all } ?: emptyList()

    suspend fun postComment(topicId: String, content: String): Boolean =
        safeExt {
            it.postComment(
                mapOf("topicId" to topicId, "topicType" to 1, "content" to content)
            ).code == 0
        } ?: false

    suspend fun likeSubject(subjectId: String): Boolean =
        safeExt {
            it.likeSubject(
                mapOf("subjectId" to subjectId, "subjectType" to 1, "action" to 1)
            ).code == 0
        } ?: false

    suspend fun addToWatchlist(subjectId: String, subjectType: Int = 1, action: Int = 1): Boolean =
        safeExt { it.wantToSee(WantToSeeRequest(subjectId, action, subjectType)).code == 0 } ?: false

    suspend fun getUnreadCounts(): UnreadCounts? =
        safeExt { it.getUnreadCounts().data }

    fun clearCaches() {
        detailCache.clear()
        aniFamDetailCache.clear()
        aniFamEpisodeMap.clear()
        aniFamEpisodeItemMap.clear()
        memoryTrendingCache.clear()
        memoryCategoryCache.clear()
        memorySectionsCache.clear()
    }

    /**
     * Drops feed caches only. Used when the adult-content preference changes:
     * cached feeds written under the old setting would otherwise keep serving
     * adult rows after the toggle is switched off (or hide them after it is on)
     * until the on-disk TTL expires.
     */
    fun clearFeedCaches() {
        memoryTrendingCache.clear()
        memoryCategoryCache.clear()
        memorySectionsCache.clear()
        memoryAnimeRails.clear()
        val dir = prefs?.context?.cacheDir ?: return
        dir.listFiles()
            ?.filter { it.name.startsWith("feed_sec_") || it.name.startsWith("feed_mov_") }
            ?.forEach { it.delete() }
    }
}
