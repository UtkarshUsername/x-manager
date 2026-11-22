package app.xmanager.followlog

import org.junit.Assert.assertEquals
import org.junit.Test

class SharedPostTest {
  private val id = "9007199254740993"
  private val post = ShareResult.Post(id, "Some_Account")

  @Test
  fun findsPostInTextAndRemovesTracking() {
    val result = parseShare("Interesting post (https://x.com/Some_Account/status/$id?s=20&t=token).")
    assertEquals(post, result)
    assertEquals("https://x.com/Some_Account/status/$id", post.url)
  }

  @Test
  fun normalizesLegacyTwitterMediaLinks() {
    assertEquals(post, parseShare("https://mobile.twitter.com/Some_Account/status/$id/photo/1?ref_src=share"))
  }

  @Test
  fun acceptsRepeatedCopiesOfTheSameLink() {
    assertEquals(post, parseShare("${post.url}\nhttps://twitter.com/some_account/status/$id"))
  }

  @Test
  fun requiresAHandleInTheLink() {
    for (path in listOf("i/status", "i/web/status")) {
      assertEquals(ShareResult.Rejected.MISSING_HANDLE, parseShare("@Some_Account shared https://x.com/$path/$id"))
    }
  }

  @Test
  fun rejectsConflictingPostIdsAndHandles() {
    for (url in listOf("https://x.com/i/status/42", "https://x.com/Different/status/$id")) {
      assertEquals(ShareResult.Rejected.AMBIGUOUS, parseShare("${post.url}\n$url"))
    }
  }

  @Test
  fun rejectsUnsupportedLinksAndText() {
    for (text in listOf(
      "https://x.com/Some_Account",
      "https://x.com/Some_Account/status/abc",
      "https://x.com.example.org/Some_Account/status/$id",
      "https://example.org/?url=${post.url}",
      "https://t.co/shortlink",
      "Plain text with no link",
      "",
    )) {
      assertEquals(ShareResult.Rejected.UNSUPPORTED, parseShare(text))
    }
  }
}
