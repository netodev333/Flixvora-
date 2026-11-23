package com.flixvora.data.network


/**
 * Pure rules for the media URLs the MovieBox catalogue returns.
 *
 * Kept free of Android context so the decoy detection and the signed-manifest
 * reconstruction can be unit tested against captured responses.
 */
object StreamUrlPolicy {

    private const val AUTHORIZED_MEDIA_HOST = "sbcdn5.hakunaymatata.com"
    private val DASH_PATH = Regex("^/dash/[A-Za-z0-9_]+/$")

    /**
     * True for the placeholder media served in place of a real stream:
     * deprecation notices and the shared anti-piracy trailer clips.
     */
    fun isDecoyUrl(url: String): Boolean {
        if (url.isEmpty()) return true
        val lower = url.lowercase()
        return lower.contains("macdn.aoneroom.com") ||
            (lower.contains("/other/") && lower.contains("notice.mp4")) ||
            lower.contains("please_update") ||
            lower.contains("update_app") ||
            lower.contains("b164fbfb43") ||
            lower.contains("1c7de0bd3393702d9191801f15f88f8d") ||
            lower.contains("9a0461bc39da389663bf3dbb17091d3f") ||
            lower.contains("80da52722ee7de7a4303290af6783424")
    }

    /**
     * Rebuilds the playable DASH manifest from the CloudFront policy carried in
     * `signCookie`.
     *
     * The catalogue points at a placeholder host but hands out a signed policy
     * whose `Resource` names the real object prefix. The policy is only trusted
     * when it is a single statement scoped to one wildcard under
     * [AUTHORIZED_MEDIA_HOST] and that prefix matches this subject's season and
     * episode, so a policy for another title cannot be replayed here.
     *
     * Returns null when no usable policy is present, which is the normal case
     * for a guest session.
     */
    fun authorizedManifestUrl(signCookie: String, subjectId: String, season: Int, episode: Int): String? {
        val policy = signCookie.split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("CloudFront-Policy=") }
            ?.substringAfter("CloudFront-Policy=")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        val decoded = try {
            String(decodeBase64(policy), Charsets.UTF_8)
        } catch (_: Exception) {
            return null
        }

        if (!decoded.contains("Statement")) return null

        val resources = RESOURCE_FIELD.findAll(decoded).map { it.groupValues[1] }.toList()
        if (resources.size != 1) return null
        val resource = resources.single()

        if (!resource.endsWith("/*")) return null
        if (resource.indexOf('*') != resource.lastIndexOf('*')) return null

        val prefix = resource.dropLast(1)
        val url = parseHttpsUrl(prefix) ?: return null

        if (url.host != AUTHORIZED_MEDIA_HOST) return null
        if (url.port != 443) return null
        if (url.userInfo.isNotEmpty()) return null
        if (url.query != null || url.fragment != null) return null

        val path = url.path
        if (!DASH_PATH.matches(path)) return null
        if (!path.startsWith("/dash/${subjectId}_${season}_${episode}_")) return null

        return "${prefix}index_web.mpd"
    }

    private data class ParsedUrl(
        val host: String,
        val port: Int,
        val path: String,
        val userInfo: String,
        val query: String?,
        val fragment: String?
    )

    private fun parseHttpsUrl(raw: String): ParsedUrl? {
        val rest = raw.removePrefix("https://").takeIf { it != raw } ?: return null
        if (rest.isEmpty()) return null

        val fragment = rest.substringAfterLast('#', "").takeIf { '#' in rest }
        val withoutFragment = rest.substringBefore('#')
        val query = withoutFragment.substringAfterLast('?', "").takeIf { '?' in withoutFragment }
        val withoutQuery = withoutFragment.substringBefore('?')

        val slash = withoutQuery.indexOf('/')
        val authority = if (slash >= 0) withoutQuery.take(slash) else withoutQuery
        val path = if (slash >= 0) withoutQuery.drop(slash) else "/"

        val userInfo = authority.substringBefore('@', "").takeIf { '@' in authority } ?: ""
        val hostAndPort = authority.substringAfterLast('@')

        val host: String
        val port: Int
        if (hostAndPort.startsWith("[")) {
            val close = hostAndPort.indexOf(']')
            if (close < 0) return null
            host = hostAndPort.substring(1, close)
            val tail = hostAndPort.drop(close + 1)
            port = when {
                tail.isEmpty() -> 443
                tail.startsWith(":") -> tail.drop(1).toIntOrNull() ?: return null
                else -> return null
            }
        } else {
            val colon = hostAndPort.lastIndexOf(':')
            if (colon >= 0) {
                host = hostAndPort.take(colon)
                port = hostAndPort.drop(colon + 1).toIntOrNull() ?: return null
            } else {
                host = hostAndPort
                port = 443
            }
        }

        if (host.isEmpty()) return null
        if (path.isEmpty() || !path.startsWith("/")) return null

        return ParsedUrl(host, port, path, userInfo, query, fragment)
    }

    /** Standard base64 with the CloudFront URL-safe substitutions. */
    private fun decodeBase64(value: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val normalized = buildString {
            for (ch in value.trim()) {
                when (ch) {
                    '-', '~' -> append('+')
                    '_' -> append('/')
                    '=' -> {}
                    else -> append(ch)
                }
            }
        }
        if (normalized.length % 4 == 1) throw IllegalArgumentException("bad length")

        val out = java.io.ByteArrayOutputStream()
        var buffer = 0
        var bits = 0
        for (ch in normalized) {
            val index = alphabet.indexOf(ch)
            if (index < 0) throw IllegalArgumentException("bad char")
            buffer = (buffer shl 6) or index
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out.write((buffer shr bits) and 0xFF)
            }
        }
        return out.toByteArray()
    }

    private val RESOURCE_FIELD = Regex("\"Resource\"\\s*:\\s*\"([^\"]*)\"")

    private val ERROR_CODE = Regex("\"code\"\\s*:\\s*(-?\\d+)")

    /**
     * Whether a proxy response body is real data. A body that carries an error
     * code (406 "find no content", 400 params error, expired token) is a
     * transient upstream failure and must not be cached, or the failure is
     * replayed for the whole cache window after the upstream recovers.
     */
    fun isCacheableBody(body: String): Boolean {
        if (body.isEmpty()) return false
        val code = ERROR_CODE.find(body)?.groupValues?.get(1)?.toIntOrNull() ?: return true
        return code == 0 || code == 200
    }
}
