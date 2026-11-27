package com.flixvora.data

object SampleCatalog {

    val defaultMovieTracks = listOf(
        AudioTrackMetadata("en-atmos", "English (Original)", "EN", "Dolby Atmos", "7.1.4 Spatial", "768 kbps", isDefault = true),
        AudioTrackMetadata("hi-atmos", "Hindi (Studio Mix)", "HI", "Dolby Digital+", "5.1 Surround", "640 kbps"),
        AudioTrackMetadata("ta-aac", "Tamil", "TA", "AAC", "5.1 Surround", "320 kbps"),
        AudioTrackMetadata("es-aac", "Español (Latino)", "ES", "AAC", "2.0 Stereo", "256 kbps")
    )

    val defaultAnimeTracks = listOf(
        AudioTrackMetadata("ja-orig", "Japanese (Original)", "JA", "FLAC 24-bit", "2.0 Stereo", "1.5 Mbps", isDefault = true),
        AudioTrackMetadata("en-dub", "English (Simulcast Dub)", "EN", "AAC-LC", "5.1 Surround", "384 kbps"),
        AudioTrackMetadata("hi-dub", "Hindi (Simulcast Dub)", "HI", "AAC-LC", "5.1 Surround", "320 kbps")
    )

    val mediaCatalog: List<MediaTitle> = listOf(
        MediaTitle(
            id = "skyfall-citadel",
            tmdbId = 948210,
            title = "Skyfall Citadel",
            editorialTagline = "One rogue orbital beacon stands between Earth and silence.",
            category = MediaCategory.MOVIE,
            year = 2026,
            duration = "2h 19m",
            rating = "9.2",
            ageRating = "PG-13",
            quality = "4K Dolby Vision",
            genres = listOf("Sci-Fi", "Action", "Thriller"),
            ottProvider = "MovieBox Hot",
            continueProgress = 68,
            rank = 1,
            synopsis = "When an orbital relay station spirals out of sync above the Indian Ocean, flight engineer Kiran Rao leads a zero-gravity extraction before the atmospheric shield collapses.",
            posterUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1200&q=80",
            cast = listOf(
                CastMember("Kiran Rao", "Cmdr. Arav"),
                CastMember("Elena Rostova", "Dr. Vega"),
                CastMember("Kenji Sato", "Flight Ops")
            ),
            audioTracks = defaultMovieTracks
        ),
        MediaTitle(
            id = "phantom-shinobi",
            tmdbId = 873104,
            title = "Phantom Shinobi: Tempest",
            editorialTagline = "Steel forged in stormlight answers only to the sworn.",
            category = MediaCategory.ANIME,
            year = 2026,
            duration = "24 Episodes",
            rating = "9.4",
            ageRating = "TV-14",
            quality = "1080p Hi10P",
            genres = listOf("Anime", "Dark Fantasy", "Action"),
            ottProvider = "Crunchyroll",
            continueProgress = 42,
            continueEpisode = "S1 · Ep 3",
            rank = 2,
            synopsis = "Beneath a sky fractured by storm spirits, a disgraced blade smith hunts cursed scrolls across floating citadel districts.",
            posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200&q=80",
            cast = listOf(
                CastMember("Takashi Mori", "Renji"),
                CastMember("Aoi Mizuki", "Kagura")
            ),
            seasons = listOf(
                SeasonItem(
                    seasonNumber = 1,
                    label = "Season 1 · Tempest Arc",
                    episodes = listOf(
                        EpisodeItem(1, "Ashes of the Gate", "24m", "Renji intercepts an imperial courier at midnight.", 380),
                        EpisodeItem(2, "Thunder over Kyoto", "24m", "Two rival clans clash above the lantern canal.", 395),
                        EpisodeItem(3, "Sky Citadel Breach", "25m", "The sealed blade awakens inside the storm core.", 430)
                    )
                ),
                SeasonItem(
                    seasonNumber = 2,
                    label = "Season 2 · Eclipse Arc",
                    episodes = listOf(
                        EpisodeItem(1, "Shadow Covenant", "24m", "New emissaries arrive from the northern peaks.", 410)
                    )
                )
            ),
            audioTracks = defaultAnimeTracks
        ),
        MediaTitle(
            id = "neon-drift-77",
            tmdbId = 762190,
            title = "Neon Drift 77",
            editorialTagline = "Midnight streets have no speed limit—only debt.",
            category = MediaCategory.MOVIE,
            year = 2026,
            duration = "2h 04m",
            rating = "8.8",
            ageRating = "PG-13",
            quality = "4K HDR10+",
            genres = listOf("Action", "Racing", "Crime"),
            ottProvider = "Netflix",
            rank = 3,
            synopsis = "An underground telemetry tuner enters Tokyo's rain-slick expressway circuit to clear her brother's syndicate ledger in one night.",
            posterUrl = "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1511919884226-fd3cad34687c?w=1200&q=80",
            cast = listOf(
                CastMember("Mackenyu", "Kenji"),
                CastMember("Awkwafina", "Mina")
            ),
            audioTracks = defaultMovieTracks
        ),
        MediaTitle(
            id = "odyssey-07",
            tmdbId = 651402,
            title = "Odyssey 07: Dustfall",
            editorialTagline = "Nine months of red dust. Zero margin for error.",
            category = MediaCategory.SERIES,
            year = 2025,
            duration = "2 Seasons",
            rating = "9.0",
            ageRating = "TV-14",
            quality = "4K Dolby Atmos",
            genres = listOf("Sci-Fi", "Drama", "Mystery"),
            ottProvider = "Apple TV+",
            continueProgress = 81,
            continueEpisode = "S2 · Ep 1",
            rank = 4,
            synopsis = "A subterranean Mars research outpost loses satellite link just as an ancient ice cavern reveals an artificial signal.",
            posterUrl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1614728894747-a83421e2b9c9?w=1200&q=80",
            cast = listOf(
                CastMember("Cillian Vance", "Aris Vance"),
                CastMember("Elena Sato", "Dr. Rei")
            ),
            seasons = listOf(
                SeasonItem(
                    1,
                    "Season 1",
                    listOf(
                        EpisodeItem(1, "Red Silence", "52m", "Crew 07 detects seismic echoes beneath Sector 4.", 790),
                        EpisodeItem(2, "Sub-Zero Core", "49m", "Drilling stalls 400 meters below the basin.", 760)
                    )
                ),
                SeasonItem(
                    2,
                    "Season 2",
                    listOf(
                        EpisodeItem(1, "Dustwall", "55m", "Crew 07 modifies the rover for a blind canyon crossing.", 840)
                    )
                )
            ),
            audioTracks = defaultMovieTracks
        ),
        MediaTitle(
            id = "crimson-ledger-18",
            tmdbId = 993108,
            title = "Crimson Ledger",
            editorialTagline = "Uncut neo-noir underworld thriller locked behind Adult PIN access.",
            category = MediaCategory.MOVIE,
            year = 2026,
            duration = "2h 14m",
            rating = "8.8",
            ageRating = "18+",
            quality = "4K HDR",
            genres = listOf("Thriller", "Crime"),
            ottProvider = "MovieBox Hot",
            isRestricted = true,
            synopsis = "An ex-forensic auditor goes off-grid across Macau and Osaka after decrypting a syndicate black-budget ledger.",
            posterUrl = "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=1200&q=80",
            cast = listOf(
                CastMember("Kenji Watanabe", "Haruto Sato"),
                CastMember("Clara Moreau", "Agent Mercer")
            ),
            audioTracks = defaultMovieTracks
        )
    )

    val moods = listOf(
        MoodCollection("mood-chill", "Chill Vibes", "Slow-burn mysteries & nocturnal journeys", listOf("Mystery", "Drama"), "Relaxed"),
        MoodCollection("mood-adrenaline", "Heart-Pounding", "High-speed drifts & relentless action", listOf("Action", "Thriller", "Racing"), "Adrenaline"),
        MoodCollection("mood-mind", "Mind-Bending", "Orbital monoliths & survival sci-fi", listOf("Sci-Fi"), "Cerebral"),
        MoodCollection("mood-anime", "Anime Binge", "Simulcast storm blades & dual audio", listOf("Anime", "Dark Fantasy"), "Sub & Dub")
    )

    val comingSoonReleases: List<ComingSoonRelease> = listOf(
        ComingSoonRelease(
            id = "cs-1",
            title = "Aether Protocol: Genesis",
            categoryLabel = "Movie · Sci-Fi",
            releaseEpochMs = System.currentTimeMillis() + 3L * 24 * 3600 * 1000 + 5 * 3600 * 1000,
            quality = "4K Dolby Vision",
            tagline = "When quantum memory rewrites history, one archivist holds the key.",
            backdropUrl = "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?w=1000&q=80",
            genres = listOf("Sci-Fi", "Thriller")
        ),
        ComingSoonRelease(
            id = "cs-2",
            title = "Kurogane: Eclipse Season 2",
            categoryLabel = "Anime · Simulcast",
            releaseEpochMs = System.currentTimeMillis() + 6L * 24 * 3600 * 1000 + 11 * 3600 * 1000,
            quality = "1080p Hi10P",
            tagline = "Dual audio global premiere simultaneous with Tokyo broadcast.",
            backdropUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=1000&q=80",
            genres = listOf("Anime", "Action")
        )
    )

    val liveChannels: List<LiveChannel> = listOf(
        LiveChannel(
            id = "ch-apex",
            number = "101",
            name = "Apex Motorsports 4K",
            category = "Sports",
            currentProgram = "World Endurance Night Qualifiers — Sector 3",
            nextProgram = "Paddock Technical Breakdown (Live)",
            timeWindow = "21:00 – 23:30",
            streamFormat = "4K HLS · 60fps",
            progressPercent = 68,
            previewUrl = "https://images.unsplash.com/photo-1511919884226-fd3cad34687c?w=900&q=80",
            epgSlots = listOf(
                EpgSlot("20:00 – 21:00", "Grid Warmup & Telemetry"),
                EpgSlot("21:00 – 23:30", "World Endurance Night Qualifiers", isNow = true),
                EpgSlot("23:30 – 00:30", "Paddock Technical Breakdown")
            )
        ),
        LiveChannel(
            id = "ch-cinema",
            number = "104",
            name = "MovieBox Premiere HD",
            category = "Cinema",
            currentProgram = "Skyfall Citadel — IMAX Showcase",
            nextProgram = "Neon Drift 77 (Uncut)",
            timeWindow = "20:30 – 22:55",
            streamFormat = "4K DASH · Atmos",
            progressPercent = 45,
            previewUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=900&q=80",
            epgSlots = listOf(
                EpgSlot("19:00 – 20:30", "Directors Spotlight"),
                EpgSlot("20:30 – 22:55", "Skyfall Citadel — IMAX", isNow = true),
                EpgSlot("22:55 – 01:05", "Neon Drift 77")
            )
        ),
        LiveChannel(
            id = "ch-anifam",
            number = "108",
            name = "AniFam Simulcast HD",
            category = "Anime",
            currentProgram = "Phantom Shinobi: Tempest — Ep 14",
            nextProgram = "Kurogane Double Feature",
            timeWindow = "21:30 – 22:00",
            streamFormat = "1080p HLS · Dual Audio",
            progressPercent = 76,
            previewUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=900&q=80",
            epgSlots = listOf(
                EpgSlot("21:00 – 21:30", "Anime News Roundup"),
                EpgSlot("21:30 – 22:00", "Phantom Shinobi — Ep 14", isNow = true),
                EpgSlot("22:00 – 22:50", "Kurogane Double Feature")
            )
        )
    )

    val streamSources: List<StreamSource> = listOf(
        StreamSource("src-akamai", "Akamai CDN Edge 01 (Mumbai Primary)", "Akamai", "HLS (m3u8)", "4K Ultra HD", 18, 18.5f, isPrimary = true),
        StreamSource("src-cf", "Cloudflare Stream Global Anycast", "Cloudflare", "DASH (mpd)", "1080p 60fps", 34, 12.0f),
        StreamSource("src-fastly", "Fastly High-Capacity Edge Node", "Fastly", "HLS (m3u8)", "1080p 60fps", 46, 10.5f),
        StreamSource("src-clearkey", "ClearKey Multi-DRM Direct Mirror", "ClearKey DRM", "DASH (mpd)", "4K Ultra HD", 78, 16.0f),
        StreamSource("src-p2p", "Decentralized P2P WebTorrent Swarm", "P2P Swarm", "WebTorrent", "1080p 60fps", 112, 8.4f)
    )

    val initialDownloads: List<DownloadTask> = listOf(
        createDownloadTask(mediaCatalog[0], "Feature Film · 4K Dolby Vision", 2640, 74, DownloadStatus.DOWNLOADING),
        createDownloadTask(mediaCatalog[1], "S1 · Ep 3 · Sky Citadel Breach", 430, 100, DownloadStatus.COMPLETED)
    )

    fun createDownloadTask(
        item: MediaTitle,
        subtitle: String,
        sizeMb: Int,
        progress: Int,
        status: DownloadStatus
    ): DownloadTask {
        val chunkCount = 6
        val chunkSize = (sizeMb / chunkCount).coerceAtLeast(1)
        val chunks = (0 until chunkCount).map { idx ->
            val start = idx * chunkSize
            val end = if (idx == chunkCount - 1) sizeMb else (idx + 1) * chunkSize
            val range = (end - start).coerceAtLeast(1)
            val doneMb = when (status) {
                DownloadStatus.COMPLETED -> range
                DownloadStatus.DOWNLOADING -> ((range * (progress - idx * 10).coerceAtLeast(0)) / 100).coerceIn(0, range)
                DownloadStatus.PAUSED -> (range * progress / 120).coerceIn(0, range)
            }
            DownloadChunk(
                chunkIndex = idx,
                startMb = start,
                endMb = end,
                downloadedMb = doneMb,
                completed = doneMb >= range,
                speedMbps = if (doneMb >= range || status != DownloadStatus.DOWNLOADING) 0f else 3.2f + idx * 0.3f
            )
        }
        return DownloadTask(
            id = "dl-${item.id}-${subtitle.hashCode()}",
            titleId = item.id,
            title = item.title,
            subtitle = subtitle,
            posterUrl = item.posterUrl,
            sizeMb = sizeMb,
            progress = progress,
            quality = if (item.quality.contains("4K")) "4K HDR" else "1080p",
            status = status,
            chunks = chunks
        )
    }

    fun filterCatalogBySafety(
        catalog: List<MediaTitle>,
        profile: ProfileType,
        adultUnlocked: Boolean
    ): List<MediaTitle> {
        return catalog.filter { item ->
            when (profile) {
                ProfileType.KIDS -> !item.isRestricted && item.ageRating !in setOf("R", "TV-MA", "18+")
                ProfileType.MAIN -> if (adultUnlocked) true else !item.isRestricted
                ProfileType.ADULT -> true
            }
        }
    }

    fun computePersonalizedRecommendations(
        catalog: List<MediaTitle>,
        watchlist: Set<String>,
        downloads: List<DownloadTask>,
        traktConnected: Boolean
    ): List<PersonalizedRecommendation> {
        val affinityGenres = mutableMapOf<String, Int>()
        catalog.forEach { item ->
            val weight = (if (item.continueProgress != null) 3 else 0) +
                (if (watchlist.contains(item.id)) 2 else 0) +
                (if (downloads.any { it.titleId == item.id }) 2 else 0)
            if (weight > 0) {
                item.genres.forEach { g ->
                    affinityGenres[g] = (affinityGenres[g] ?: 0) + weight
                }
            }
        }

        return catalog.map { item ->
            val topGenre = item.genres.maxByOrNull { affinityGenres[it] ?: 0 } ?: item.genres.firstOrNull().orEmpty()
            val genreScore = item.genres.sumOf { affinityGenres[it] ?: 0 } * 3
            val rawScore = 82 + genreScore.coerceAtMost(14) + if (traktConnected) 3 else 0
            val matchPercent = rawScore.coerceIn(84, 99)

            val reason = when {
                item.continueProgress != null -> "Continue watching · ${item.continueProgress}% watched"
                traktConnected && watchlist.contains(item.id) -> "Trakt Watchlist · High $topGenre match"
                traktConnected -> "Synced from Trakt history · $topGenre"
                watchlist.contains(item.id) -> "Saved in Watchlist · Matches $topGenre"
                else -> "Based on your $topGenre watch history"
            }

            PersonalizedRecommendation(item, matchPercent, reason)
        }.sortedByDescending { it.matchPercent }
    }
}
