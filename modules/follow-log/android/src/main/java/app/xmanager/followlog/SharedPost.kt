package app.xmanager.followlog

internal sealed interface ShareResult {
  data class Post(val id: String, val handle: String) : ShareResult {
    val url: String get() = "https://x.com/$handle/status/$id"
  }

  enum class Rejected : ShareResult { MISSING_HANDLE, UNSUPPORTED, AMBIGUOUS }
}

private val urlPattern = Regex("""(?i)\bhttps?://[^\s\p{Z}\uFEFF<>"'…]+""")
private val postPattern = Regex(
  """(?i)https?://(?:www\.|mobile\.|m\.)?(?:x\.com|twitter\.com)/(?:i/web|([a-z0-9_]{1,15}))/status/([1-9]\d{0,19})(?:/(?:photo|video)/\d+)?/?(?:[?#].*)?""",
)

internal fun parseShare(text: String): ShareResult {
  var post: ShareResult.Post? = null
  var statusId: String? = null

  for (url in urlPattern.findAll(text)) {
    val link = url.value.trimEnd { it in ")]}.,;:!?" }
    val (handle, id) = postPattern.matchEntire(link)?.destructured ?: continue
    if (statusId != null && statusId != id) return ShareResult.Rejected.AMBIGUOUS
    statusId = id
    if (handle.isEmpty() || handle.equals("i", ignoreCase = true)) continue

    if (post == null) {
      post = ShareResult.Post(id, handle)
    } else if (!post.handle.equals(handle, ignoreCase = true)) {
      return ShareResult.Rejected.AMBIGUOUS
    }
  }

  return post ?: if (statusId == null) ShareResult.Rejected.UNSUPPORTED else ShareResult.Rejected.MISSING_HANDLE
}
