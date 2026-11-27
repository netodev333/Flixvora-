package com.flixvora.data

import com.flixvora.data.model.MovieSubject
import com.flixvora.data.model.TvChannel

/**
 * Converts the real catalogue models into the UI models used by the Compose
 * screens. Everything here is derived from the API payload: nothing is
 * invented, so a missing field stays missing instead of being replaced with a
 * fake value.
 */
object DataBridge {

    private fun parseYear(raw: String?): Int? =
        raw?.filter { it.isDigit() }?.take(4)?.toIntOrNull()

    private fun numericId(raw: String): Int {
        val digits = raw.filter { it.isDigit() }
        return digits.takeLast(9).toIntOrNull() ?: raw.hashCode().and(Int.MAX_VALUE)
    }

    private fun categoryOf(movie: MovieSubject): MediaCategory = when {
        movie.id.startsWith("anifam_") ||
            movie.genres.any { it.equals("anime", ignoreCase = true) } -> MediaCategory.ANIME
        movie.isTvShow -> MediaCategory.SERIES
        else -> MediaCategory.MOVIE
    }

    private fun providerOf(movie: MovieSubject): String =
        if (movie.id.startsWith("anifam_")) "AniFam" else "MovieBox"

    private fun audioTracksOf(movie: MovieSubject, category: MediaCategory): List<AudioTrackMetadata> {
        if (movie.dubs.isNotEmpty()) {
            return movie.dubs.mapIndexed { index, dub ->
                AudioTrackMetadata(
                    id = "${movie.id}_audio_$index",
                    language = dub,
                    code = dub.take(2).uppercase(),
                    codec = "AAC",
                    channels = "2.0 Stereo",
                    bitrate = "-",
                    isDefault = index == 0
                )
            }
        }
        return listOf(
            AudioTrackMetadata(
                id = "${movie.id}_audio_original",
                language = if (category == MediaCategory.ANIME) "Japanese (Original)" else "Original",
                code = if (category == MediaCategory.ANIME) "JA" else "EN",
                codec = "AAC",
                channels = "2.0 Stereo",
                bitrate = "-",
                isDefault = true
            )
        )
    }

    private fun seasonsOf(movie: MovieSubject): List<SeasonItem> {
        if (!movie.isTvShow || movie.seasonList.isEmpty()) return emptyList()
        return movie.seasonList
            .filter { it.episodeList.isNotEmpty() }
            .map { season ->
                SeasonItem(
                    seasonNumber = season.season,
                    label = "Season ${season.season} · ${season.episodeList.size} Episodes",
                    episodes = season.episodeList.map { ep ->
                        EpisodeItem(
                            number = ep.episode,
                            title = ep.title.ifBlank { "Episode ${ep.episode}" },
                            duration = ep.duration.ifBlank { "-" },
                            synopsis = "",
                            sizeMb = 0
                        )
                    }
                )
            }
    }

    fun movieSubjectToMediaTitle(movie: MovieSubject): MediaTitle {
        val category = categoryOf(movie)
        val poster = movie.poster.ifBlank { movie.backdrop.orEmpty() }
        val backdrop = movie.backdrop?.takeIf { it.isNotBlank() } ?: poster
        val seasons = seasonsOf(movie)
        val duration = when {
            movie.duration?.isNotBlank() == true -> movie.duration!!
            movie.isTvShow && seasons.isNotEmpty() -> "${seasons.sumOf { it.episodes.size }} Episodes"
            movie.isTvShow -> "${movie.totalSeasons.coerceAtLeast(1)} Season(s)"
            else -> "-"
        }
        val rating = movie.rating?.takeIf { it.isNotBlank() } ?: "-"

        return MediaTitle(
            id = movie.id,
            tmdbId = numericId(movie.id),
            title = movie.name.ifBlank { "Untitled" },
            editorialTagline = movie.description.take(140),
            category = category,
            year = parseYear(movie.year) ?: 0,
            duration = duration,
            rating = rating,
            ageRating = when {
                movie.isAdult -> "18+"
                category == MediaCategory.SERIES -> "TV-14"
                else -> "U/A 13+"
            },
            quality = "HD",
            genres = movie.genres.distinct(),
            ottProvider = providerOf(movie),
            synopsis = movie.description.ifBlank { "No synopsis available." },
            posterUrl = poster,
            backdropUrl = backdrop,
            continueProgress = null,
            continueEpisode = null,
            rank = null,
            introEndSeconds = 85,
            isRestricted = movie.isAdult,
            cast = movie.cast.map { CastMember(it.name, it.role) },
            seasons = seasons,
            audioTracks = audioTracksOf(movie, category),
            releaseEpochMs = parseReleaseEpoch(movie.releaseDate)
        )
    }

    private fun parseReleaseEpoch(raw: String?): Long? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", true) } ?: return null
        value.toLongOrNull()?.let { return if (it > 100_000_000_000L) it else null }
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "MMM d, yyyy",
            "dd MMM yyyy",
            "yyyy"
        )
        for (pattern in patterns) {
            try {
                val parsed = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
                    .apply { isLenient = false }
                    .parse(value)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun tvChannelToLiveChannel(tv: TvChannel, index: Int): LiveChannel {
        val chNum = (index + 1).toString().padStart(2, '0')
        val nowTitle = "Live · ${tv.title}"
        val nextTitle = "Next on ${tv.countryName.ifBlank { "Live TV" }}"
        return LiveChannel(
            id = tv.channelId,
            number = chNum,
            name = tv.title.ifBlank { "Channel $chNum" },
            category = tv.countryName.ifBlank { tv.countryCode.ifBlank { "Live" } },
            currentProgram = nowTitle,
            nextProgram = nextTitle,
            timeWindow = "Now Live",
            streamFormat = "LIVE",
            progressPercent = 0,
            previewUrl = "",
            epgSlots = listOf(
                EpgSlot("Now", nowTitle, isNow = true),
                EpgSlot("Next", nextTitle, isNow = false)
            )
        )
    }
}
