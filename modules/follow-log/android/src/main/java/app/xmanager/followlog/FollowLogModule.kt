package app.xmanager.followlog

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class FollowLogModule : Module() {
  private val postsChanged: () -> Unit = { sendEvent("onPostsChanged") }

  override fun definition() = ModuleDefinition {
    Name("XManagerFollowLog")
    Events("onPostsChanged")

    AsyncFunction("listPosts") {
      FollowLogStore.list(requireNotNull(appContext.reactContext).applicationContext)
    }

    OnStartObserving("onPostsChanged") { FollowLogStore.listeners.add(postsChanged) }
    OnStopObserving("onPostsChanged") { FollowLogStore.listeners.remove(postsChanged) }
    OnDestroy { FollowLogStore.listeners.remove(postsChanged) }
  }
}
