package com.flixvora.data

import com.flixvora.data.model.AniFamBrowseResponse
import com.flixvora.data.model.AniFamHomeResponse
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AniFamParsingTest {

    private val moshi = Moshi.Builder().build()

    private fun resource(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "missing $name" }
            .bufferedReader()
            .use { it.readText() }

    @Test
    fun homePayloadMapsEverySectionToUiTitles() {
        val home = moshi.adapter(AniFamHomeResponse::class.java)
            .fromJson(resource("anifam_home.json"))!!

        assertTrue("spotlight should parse", home.spotlight.isNotEmpty())
        assertTrue("trending should parse", home.trending.isNotEmpty())
        assertTrue("top_airing should parse", home.top_airing.isNotEmpty())
        assertTrue("completed should parse", home.completed.isNotEmpty())
        assertTrue("top10 should parse", home.top10.today.isNotEmpty())

        val titles = home.trending.map { DataBridge.movieSubjectToMediaTitle(it.toMovieSubject()) }
        assertTrue(titles.isNotEmpty())
        assertTrue(titles.all { it.title.isNotBlank() })
        assertTrue(titles.all { it.posterUrl.startsWith("https://") })
        assertTrue(titles.all { it.category == MediaCategory.ANIME })
        assertTrue(titles.all { it.ottProvider == "AniFam" })
        assertEquals(titles.size, titles.distinctBy { it.id }.size)
    }

    @Test
    fun browsePayloadMapsToUiTitles() {
        val browse = moshi.adapter(AniFamBrowseResponse::class.java)
            .fromJson(resource("anifam_browse.json"))!!

        assertTrue(browse.data.isNotEmpty())
        val titles = browse.data.map { DataBridge.movieSubjectToMediaTitle(it.toMovieSubject()) }
        assertTrue(titles.isNotEmpty())
        assertTrue(titles.all { it.id.startsWith("anifam_") })
        assertTrue(titles.all { it.posterUrl.startsWith("https://") })
    }
}
