package com.flixvora.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamUrlPolicyTest {

    private fun policyCookie(resource: String, extraStatements: Int = 0): String {
        val statements = buildString {
            append("""{"Resource":"$resource","Condition":{"DateLessThan":{"AWS:EpochTime":1900000000}}}""")
            repeat(extraStatements) {
                append(""","{"Resource":"https://evil.example.com/*","Condition":{}}""")
            }
        }
        val json = """{"Statement":[$statements]}"""
        return "CloudFront-Policy=${base64Url(json)};CloudFront-Signature=abc;CloudFront-Key-Pair-Id=K1"
    }

    private fun base64Url(value: String): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val bytes = value.toByteArray(Charsets.UTF_8)
        val out = StringBuilder()
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i].toInt() and 0xFF
            val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xFF else 0
            val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xFF else 0
            out.append(alphabet[b0 shr 2])
            out.append(alphabet[((b0 and 0x03) shl 4) or (b1 shr 4)])
            if (i + 1 < bytes.size) out.append(alphabet[((b1 and 0x0F) shl 2) or (b2 shr 6)]) else out.append('=')
            if (i + 2 < bytes.size) out.append(alphabet[b2 and 0x3F]) else out.append('=')
            i += 3
        }
        return out.toString().replace('+', '-').replace('/', '~').replace('=', '_')
    }

    private val subject = "297214768056779112"

    @Test
    fun `decoy urls are recognised`() {
        assertTrue(StreamUrlPolicy.isDecoyUrl("https://macdn.aoneroom.com/x/index.m3u8"))
        assertTrue(StreamUrlPolicy.isDecoyUrl("https://cdn.example.com/other/notice.mp4"))
        assertTrue(StreamUrlPolicy.isDecoyUrl("https://cdn.example.com/b164fbfb4347792950bdfbfb563d39d9.mp4"))
        assertTrue(StreamUrlPolicy.isDecoyUrl(""))
    }

    @Test
    fun `real urls are not decoys`() {
        assertFalse(StreamUrlPolicy.isDecoyUrl("https://sbcdn5.hakunaymatata.com/dash/x/index_web.mpd"))
        assertFalse(StreamUrlPolicy.isDecoyUrl("https://cdn.example.com/movie/index.m3u8"))
    }

    @Test
    fun `signed manifest is rebuilt from a matching policy`() {
        val cookie = policyCookie("https://sbcdn5.hakunaymatata.com/dash/${subject}_1_1_abc/*")
        assertEquals(
            "https://sbcdn5.hakunaymatata.com/dash/${subject}_1_1_abc/index_web.mpd",
            StreamUrlPolicy.authorizedManifestUrl(cookie, subject, 1, 1)
        )
    }

    @Test
    fun `policy for another episode is rejected`() {
        val cookie = policyCookie("https://sbcdn5.hakunaymatata.com/dash/${subject}_1_2_abc/*")
        assertNull(StreamUrlPolicy.authorizedManifestUrl(cookie, subject, 1, 1))
    }

    @Test
    fun `policy for another subject is rejected`() {
        val cookie = policyCookie("https://sbcdn5.hakunaymatata.com/dash/999_1_1_abc/*")
        assertNull(StreamUrlPolicy.authorizedManifestUrl(cookie, subject, 1, 1))
    }

    @Test
    fun `policy on an unauthorised host is rejected`() {
        val cookie = policyCookie("https://evil.example.com/dash/${subject}_1_1_abc/*")
        assertNull(StreamUrlPolicy.authorizedManifestUrl(cookie, subject, 1, 1))
    }

    @Test
    fun `policy over plain http is rejected`() {
        val cookie = policyCookie("http://sbcdn5.hakunaymatata.com/dash/${subject}_1_1_abc/*")
        assertNull(StreamUrlPolicy.authorizedManifestUrl(cookie, subject, 1, 1))
    }

    @Test
    fun `policy with credentials is rejected`() {
        val cookie = policyCookie("https://user:pw@sbcdn5.hakunaymatata.com/dash/${subject}_1_1_abc/*")
        assertNull(StreamUrlPolicy.authorizedManifestUrl(cookie, subject, 1, 1))
    }

    @Test
    fun `policy with multiple statements is rejected`() {
        val cookie = policyCookie("https://sbcdn5.hakunaymatata.com/dash/${subject}_1_1_abc/*", extraStatements = 1)
        assertNull(StreamUrlPolicy.authorizedManifestUrl(cookie, subject, 1, 1))
    }

    @Test
    fun `policy without a wildcard is rejected`() {
        val cookie = policyCookie("https://sbcdn5.hakunaymatata.com/dash/${subject}_1_1_abc/index_web.mpd")
        assertNull(StreamUrlPolicy.authorizedManifestUrl(cookie, subject, 1, 1))
    }

    @Test
    fun `missing policy yields null`() {
        assertNull(StreamUrlPolicy.authorizedManifestUrl("", subject, 1, 1))
        assertNull(StreamUrlPolicy.authorizedManifestUrl("CloudFront-Signature=abc", subject, 1, 1))
    }

    @Test
    fun `malformed policy yields null`() {
        assertNull(StreamUrlPolicy.authorizedManifestUrl("CloudFront-Policy=!!!not-base64!!!", subject, 1, 1))
    }

    @Test
    fun `error bodies are not cacheable`() {
        assertFalse(StreamUrlPolicy.isCacheableBody("""{"code":406,"reason":"Not Acceptable","message":"find no content"}"""))
        assertFalse(StreamUrlPolicy.isCacheableBody("""{"code":400,"reason":"PARAMS_ERROR","message":"empty subjectId"}"""))
        assertFalse(StreamUrlPolicy.isCacheableBody("""{"code": 401,"message":"JWT token has expired"}"""))
        assertFalse(StreamUrlPolicy.isCacheableBody(""))
    }

    @Test
    fun `successful bodies are cacheable`() {
        assertTrue(StreamUrlPolicy.isCacheableBody("""{"code":0,"message":"ok","data":{"list":[]}}"""))
        assertTrue(StreamUrlPolicy.isCacheableBody("""{"code":200,"data":{}}"""))
        assertTrue(StreamUrlPolicy.isCacheableBody("""{"subjectId":"1","title":"x"}"""))
    }
}
