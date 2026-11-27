package com.flixvora.data

import com.flixvora.data.model.EpisodeInfo
import com.flixvora.data.model.MovieSubject
import com.flixvora.data.model.SeasonInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataBridgeTest {

    @Test
    fun `maps a movie with real metadata`() {
        val movie = MovieSubject(
            id = "123456",
            name = "Test Movie",
            poster = "https://cdn/poster.jpg",
            description = "A real synopsis",
            rating = "8.2",
            year = "2024",
            genres = listOf("Action", "Drama"),
            isTvShow = false,
            duration = "2h 10m",
            dubs = listOf("Hindi", "Tamil"),
            cast = listOf(com.flixvora.data.model.CastMember("Lead Actor", "Hero"))
        )

        val title = DataBridge.movieSubjectToMediaTitle(movie)

        assertEquals(MediaCategory.MOVIE, title.category)
        assertEquals(2024, title.year)
        assertEquals("8.2", title.rating)
        assertEquals("2h 10m", title.duration)
        assertEquals("MovieBox", title.ottProvider)
        assertEquals(listOf("Action", "Drama"), title.genres)
        assertEquals(1, title.cast.size)
        assertEquals("Lead Actor", title.cast.first().name)
        assertEquals(2, title.audioTracks.size)
        assertEquals("Hindi", title.audioTracks.first().language)
        assertTrue(title.seasons.isEmpty())
    }

    @Test
    fun `maps a series with real seasons and episodes`() {
        val series = MovieSubject(
            id = "777",
            name = "Test Series",
            poster = "https://cdn/series.jpg",
            isTvShow = true,
            totalSeasons = 1,
            seasonList = listOf(
                SeasonInfo(
                    season = 1,
                    episodeList = listOf(
                        EpisodeInfo(episode = 1, title = "Pilot", duration = "45m"),
                        EpisodeInfo(episode = 2, title = "Second", duration = "42m")
                    )
                )
            )
        )

        val title = DataBridge.movieSubjectToMediaTitle(series)

        assertEquals(MediaCategory.SERIES, title.category)
        assertEquals(1, title.seasons.size)
        assertEquals(2, title.seasons.first().episodes.size)
        assertEquals("Pilot", title.seasons.first().episodes.first().title)
        assertEquals("2 Episodes", title.duration)
    }

    @Test
    fun `marks anifam ids as anime provider`() {
        val anime = MovieSubject(
            id = "anifam_99",
            name = "Test Anime",
            poster = "https://cdn/anime.jpg",
            genres = listOf("Anime")
        )

        val title = DataBridge.movieSubjectToMediaTitle(anime)

        assertEquals(MediaCategory.ANIME, title.category)
        assertEquals("AniFam", title.ottProvider)
    }
}
