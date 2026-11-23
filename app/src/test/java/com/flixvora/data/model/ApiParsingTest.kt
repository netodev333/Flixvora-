package com.flixvora.data.model

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parses fixtures captured verbatim from the live catalogue. The models are the
 * real contract with the API, so a fixture that fails here is a real runtime
 * failure the app would hit — Moshi rejects the whole body on one type mismatch.
 */
class ApiParsingTest {

    // Mirrors MovieBoxRetrofitClient.buildExtendedApi so the test exercises the
    // same adapter chain the app actually parses with.
    private val moshi: Moshi = Moshi.Builder()
        .add(MovieSubjectAdapter())
        .add(CommentItemAdapter())
        .build()

    private fun fixture(name: String): String =
        javaClass.classLoader!!.getResource("fixtures/$name")!!.readText()

    /**
     * Maps a raw play-info body the same way the proxy does: `data.streams` in,
     * `PlayInfo.streamList` out, with the quality label taken from the highest
     * entry in `resolutions`.
     */
    private fun playInfoOf(name: String): PlayInfo {
        val root = moshi.adapter(Map::class.java).fromJson(fixture(name)) as Map<*, *>
        val data = root["data"] as? Map<*, *> ?: return PlayInfo()
        val streams = data["streams"] as? List<*> ?: return PlayInfo()

        val items = streams.mapNotNull { raw ->
            val stream = raw as? Map<*, *> ?: return@mapNotNull null
            val url = stream["url"] as? String ?: return@mapNotNull null
            val quality = (stream["resolutions"] as? String).orEmpty()
                .split(',')
                .mapNotNull { it.trim().toIntOrNull() }
                .maxOrNull()
                ?.let { "${it}p" } ?: "Auto"
            StreamItem(
                quality = quality,
                url = url,
                size = stream["size"] as? String ?: ""
            )
        }
        return PlayInfo(streamList = items)
    }

    /**
     * play-info addresses movies with se=0&ep=0 and series with a real
     * season/episode. Asking with the wrong pair returns an empty stream list,
     * so a caller that trusts one addressing half empties the catalogue. These
     * four fixtures are the live responses that show the asymmetry.
     */
    @Test
    fun seriesStreamsOnlyAnswerAtItsRealSeasonAndEpisode() {
        val hit = playInfoOf("play_info_series_se1ep1.json")
        val miss = playInfoOf("play_info_series_se0ep0.json")

        assertTrue("series should stream at se=1&ep=1", hit.streamList.isNotEmpty())
        assertEquals("series should be empty at se=0&ep=0", 0, miss.streamList.size)
    }

    @Test
    fun movieStreamsOnlyAnswerAtSeasonZeroEpisodeZero() {
        val hit = playInfoOf("play_info_movie_se0ep0.json")
        val miss = playInfoOf("play_info_movie_se1ep1.json")

        assertTrue("movie should stream at se=0&ep=0", hit.streamList.isNotEmpty())
        assertEquals("movie should be empty at se=1&ep=1", 0, miss.streamList.size)
    }

    @Test
    fun aStreamingResponseCarriesAPlayableUrl() {
        val playInfo = playInfoOf("play_info_series_se1ep1.json")
        val stream = playInfo.streamList.first()

        assertTrue("stream url must be absolute", stream.url.startsWith("https://"))
        assertTrue("stream must advertise a quality", stream.quality.isNotBlank())
    }

    @Test
    fun `tv country list parses`() {
        val res = moshi.adapter(TvCountryResponse::class.java).fromJson(fixture("tv_country_list.json"))
        assertNotNull("country list body must parse", res)
        assertEquals(0, res!!.code)
        val items = res.data?.items.orEmpty()
        assertTrue("expected countries, got ${items.size}", items.size > 100)
        assertTrue(items.all { it.countryCode.isNotBlank() })
    }

    @Test
    fun `tv channel list parses with pager and inline streams`() {
        val res = moshi.adapter(TvChannelResponse::class.java)
            .fromJson(fixture("tv_channel_list_IN.json"))
        assertNotNull("channel list body must parse", res)
        val data = res!!.data
        assertNotNull(data)
        val items = data!!.items
        assertEquals("endpoint caps a page at 10", 10, items.size)

        // Every listed channel needs a usable identity, or the grid renders blanks.
        assertTrue(items.all { it.channelId.isNotBlank() })
        assertTrue("titles must survive parsing", items.all { it.title.isNotBlank() })

        // Inline streams are what the grid uses to play without a second call.
        val withStream = items.count { !it.playUrl.isNullOrBlank() }
        assertTrue("expected inline streams, got $withStream", withStream > 0)

        // Pagination drives how many channels a country shows; a null pager
        // collapses every country to a single page of 10.
        val pager = data.pager
        assertNotNull("pager must parse, else pagination silently stops at page 1", pager)
        assertTrue("totalCount must be real, got ${pager!!.totalCount}", pager.totalCount > 10)
        assertTrue("hasMore must parse as a boolean", pager.hasMore)
    }

    @Test
    fun `tv channel detail parses streams`() {
        val res = moshi.adapter(TvChannelDetailResponse::class.java)
            .fromJson(fixture("tv_channel_detail.json"))
        assertNotNull(res)
        val d = res!!.data
        assertNotNull(d)
        assertTrue(d!!.channelId.isNotBlank())
        assertTrue("detail must carry a playable stream", d.streams.any { it.url.isNotBlank() })
        assertTrue(d.streams.first { it.url.isNotBlank() }.url.startsWith("http"))
    }

    @Test
    fun `subject detail parses resourceDetectors`() {
        val res = moshi.adapter(SubjectDetailResponse::class.java)
            .fromJson(fixture("subject_detail.json"))
        assertNotNull("detail body must parse", res)
        val data = res!!.data
        assertNotNull(data)
        assertTrue(data!!.id.isNotBlank())
        assertTrue(data.name.isNotBlank())
        assertTrue(
            "detail must expose a resource id for playback",
            data.resourceDetectors.any { it.resourceId.isNotBlank() }
        )
    }

    @Test
    fun `search parses results`() {
        val res = moshi.adapter(SearchResponse::class.java).fromJson(fixture("search_avatar.json"))
        assertNotNull(res)
        val items = res!!.data?.let { d -> d.items.ifEmpty { d.list } }.orEmpty()
        assertTrue("expected search hits, got ${items.size}", items.isNotEmpty())
    }
}
