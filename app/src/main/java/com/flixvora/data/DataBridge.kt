package com.flixvora.data

import com.flixvora.data.model.MovieSubject
import com.flixvora.data.model.TvChannel

object DataBridge {
    fun movieSubjectToMediaTitle(movie: MovieSubject): MediaTitle {
        val tmdb = movie.id.removePrefix("tmdb_").removePrefix("anifam_").toIntOrNull() ?: 1001
        val category = when {
            movie.id.startsWith("anifam_") || movie.genres.any { it.equals("anime", ignoreCase = true) } -> MediaCategory.ANIME
            movie.isTvShow -> MediaCategory.SERIES
            else -> MediaCategory.MOVIE
        }
        val yr = movie.year?.filter { it.isDigit() }?.toIntOrNull() ?: 2024
        val dur = movie.duration.orEmpty().ifBlank { if (movie.isTvShow) "${movie.totalSeasons.coerceAtLeast(1)} Seasons" else "120 min" }
        val rating = movie.rating.orEmpty().ifBlank { "8.5" }
        val age = if (movie.isAdult || movie.restrictKid == 1) "18+" else "PG-13"
        val quality = if (movie.quality.isNotBlank()) movie.quality else "4K UHD"
        val genres = if (movie.genres.isNotEmpty()) movie.genres else listOf("Action", "Drama")
        val ott = if (category == MediaCategory.ANIME) "Crunchyroll" else "MovieBox Hot"
        val poster = if (movie.poster.isNotBlank()) movie.poster else "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600&q=80"
        val backdrop = if (!movie.backdrop.isNullOrBlank()) movie.backdrop!! else poster

        return MediaTitle(
            id = movie.id,
            tmdbId = tmdb,
            title = movie.name.ifBlank { "Untitled" },
            editorialTagline = movie.description.take(120),
            category = category,
            year = yr,
            duration = dur,
            rating = rating,
            ageRating = age,
            quality = quality,
            genres = genres,
            ottProvider = ott,
            synopsis = movie.description.ifBlank { "No synopsis available." },
            posterUrl = poster,
            backdropUrl = backdrop,
            continueProgress = null,
            continueEpisode = null,
            rank = null,
            introEndSeconds = 85,
            isRestricted = movie.isAdult || movie.restrictKid == 1,
            cast = emptyList(),
            seasons = emptyList(),
            audioTracks = if (category == MediaCategory.ANIME) SampleCatalog.defaultAnimeTracks else SampleCatalog.defaultMovieTracks
        )
    }

    fun tvChannelToLiveChannel(tv: TvChannel, index: Int): LiveChannel {
        val chNum = (index + 1).toString().padStart(2, '0')
        val nowTitle = "Live Broadcast · ${tv.title}"
        val nextTitle = "Upcoming Segment · ${tv.countryName.ifBlank { "World" }}"
        val preview = if (tv.countryFlag.isNotBlank()) tv.countryFlag else "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=800&q=80"
        return LiveChannel(
            id = tv.channelId.ifBlank { "ch_$index" },
            number = chNum,
            name = tv.title.ifBlank { "Channel $chNum" },
            category = tv.countryName.ifBlank { "Global" },
            currentProgram = nowTitle,
            nextProgram = nextTitle,
            timeWindow = "Now Live",
            streamFormat = "1080p · 60 FPS HLS",
            progressPercent = 45,
            previewUrl = preview,
            epgSlots = listOf(
                EpgSlot("08:00 - 10:00", "Morning Highlights", isNow = false),
                EpgSlot("10:00 - 14:00", nowTitle, isNow = true),
                EpgSlot("14:00 - 18:00", nextTitle, isNow = false),
                EpgSlot("18:00 - 22:00", "Prime Time Feature", isNow = false)
            )
        )
    }

    fun movieSubjectToComingSoon(movie: MovieSubject, index: Int): ComingSoonRelease {
        val poster = if (movie.poster.isNotBlank()) movie.poster else "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600&q=80"
        val backdrop = if (!movie.backdrop.isNullOrBlank()) movie.backdrop!! else poster
        val daysLater = ((index + 1) * 3).toLong()
        return ComingSoonRelease(
            id = "cs_${movie.id}",
            title = movie.name.ifBlank { "Upcoming Premiere" },
            categoryLabel = if (movie.isTvShow) "SERIES PREMIERE" else "BLOCKBUSTER PREMIERE",
            releaseEpochMs = System.currentTimeMillis() + (daysLater * 24 * 60 * 60 * 1000L),
            quality = if (movie.quality.isNotBlank()) movie.quality else "4K HDR · IMAX",
            tagline = movie.description.take(100).ifBlank { "Exclusive premiere coming soon to Flixvora." },
            backdropUrl = backdrop,
            genres = if (movie.genres.isNotEmpty()) movie.genres else listOf("Action", "Thriller")
        )
    }
}
