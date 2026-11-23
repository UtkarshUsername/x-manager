package app.xmanager.scrolltimer

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.roundToInt

internal class TimerOverlay(
  private val context: Context,
  private val settings: ScrollTimerSettings,
  private val lock: () -> Unit,
  private val unlock: () -> Unit,
) {
  private data class Placement(val params: WindowManager.LayoutParams, val area: Rect)

  private val manager = context.getSystemService(WindowManager::class.java)!!
  private var view: View? = null
  private var text: TextView? = null
  private var bounds: Rect? = null
  private var blocking = false

  fun show(target: Rect, locked: Boolean, message: String): Boolean {
    if (view == null || bounds != target || blocking != locked) {
      hide()
      val placement = placement(target, locked) ?: return false
      val label = TextView(context).apply {
        setTextColor(Color.WHITE)
        textSize = if (locked) 25f else 14f
        setTypeface(typeface, Typeface.BOLD)
        gravity = if (locked) Gravity.CENTER else Gravity.CENTER_VERTICAL
      }
      val panel = if (locked) blocker(label) else chip(label)
      label.text = message
      if (!locked) makeDraggable(panel, placement)
      try {
        manager.addView(panel, placement.params)
      } catch (_: WindowManager.BadTokenException) {
        return false
      } catch (_: WindowManager.InvalidDisplayException) {
        return false
      } catch (_: SecurityException) {
        return false
      }
      view = panel
      text = label
      bounds = Rect(target)
      blocking = locked
    }
    text?.let { if (it.text != message) it.text = message }
    return true
  }

  fun hide() {
    view?.let {
      try {
        manager.removeViewImmediate(it)
      } catch (_: IllegalArgumentException) {
        // Android may already have removed the window when the service disconnects.
      }
    }
    view = null
    text = null
    bounds = null
  }

  @Suppress("RtlHardcoded")
  private fun placement(target: Rect, locked: Boolean): Placement? {
    val params = WindowManager.LayoutParams(
      if (locked) MATCH_PARENT else dp(238),
      if (locked) MATCH_PARENT else WRAP_CONTENT,
      WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
      PixelFormat.TRANSLUCENT,
    )
    params.gravity = Gravity.TOP or Gravity.RIGHT
    params.title = "X Manager scroll timer"
    val area = Rect(target)
    if (Build.VERSION.SDK_INT >= 30) {
      val metrics = manager.currentWindowMetrics
      val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
        WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
      )
      val safe = Rect(metrics.bounds).apply { inset(insets.left, insets.top, insets.right, insets.bottom) }
      if (!area.intersect(safe)) return null

      params.flags = params.flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
      params.setFitInsetsTypes(0)
      params.y = area.top - metrics.bounds.top
      if (locked) {
        params.gravity = Gravity.TOP or Gravity.LEFT
        params.width = area.width()
        params.height = area.height()
        params.x = area.left - metrics.bounds.left
      } else {
        params.x = metrics.bounds.right - area.right
      }
    }

    if (!locked) {
      val margin = dp(8)
      if (area.width() < dp(150) || area.height() < dp(70)) return null
      params.width = minOf(params.width, area.width() - 2 * margin)
      params.x += margin
      params.y += margin
    }
    return Placement(params, area)
  }

  @Suppress("ClickableViewAccessibility")
  private fun makeDraggable(panel: View, placement: Placement) {
    val (params, area) = placement
    val originX = params.x
    val originY = params.y
    val margins = 2 * dp(8)
    val maxX = (area.width() - params.width - margins).coerceAtLeast(0)
    val preferred = settings.overlayOffset

    fun move(offsetX: Int, offsetY: Int, height: Int) {
      val maxY = (area.height() - height - margins).coerceAtLeast(0)
      val x = originX + offsetX.coerceIn(0, maxX)
      val y = originY + offsetY.coerceIn(0, maxY)
      if (x == params.x && y == params.y) return
      params.x = x
      params.y = y
      if (view === panel) {
        try {
          manager.updateViewLayout(panel, params)
        } catch (_: IllegalArgumentException) {
          // Android may already have removed the window.
        }
      }
    }

    panel.measure(
      View.MeasureSpec.makeMeasureSpec(params.width, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
    )
    move(preferred.x, preferred.y, panel.measuredHeight)
    panel.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
      move(preferred.x, preferred.y, panel.height)
    }

    val slop = ViewConfiguration.get(context).scaledTouchSlop
    val down = PointF()
    val start = Point()
    var tracking = false
    var dragging = false
    // The Lock button handles its own taps. The surrounding chip handles drags.
    panel.setOnTouchListener { _, event ->
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          down.set(event.rawX, event.rawY)
          start.set(params.x - originX, params.y - originY)
          tracking = true
        }
        MotionEvent.ACTION_MOVE -> if (tracking) {
          // Screen coordinates stay stable as the window moves under the finger.
          val dx = event.rawX - down.x
          val dy = event.rawY - down.y
          if (dragging || abs(dx) > slop || abs(dy) > slop) {
            dragging = true
            // With right gravity, increasing x moves the window left.
            move(start.x - dx.roundToInt(), start.y + dy.roundToInt(), panel.height)
            preferred.set(params.x - originX, params.y - originY)
          }
        }
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> {
          if (dragging) settings.overlayOffset = preferred
          dragging = false
          tracking = false
        }
      }
      true
    }
  }

  private fun chip(label: TextView) = LinearLayout(context).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    background = rounded(0xff184832.toInt(), 12)
    elevation = dp(5).toFloat()
    setPadding(dp(12), dp(4), dp(4), dp(4))
    addView(label, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
    addView(action("Lock") { lock() }.apply { contentDescription = "Lock X" })
  }

  private fun blocker(label: TextView) = FrameLayout(context).apply {
    setBackgroundColor(0xeb14251e.toInt())
    // Consume gestures over X while keeping key focus and system navigation with Android.
    setOnTouchListener { _, _ -> true }
    val panel = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      gravity = Gravity.CENTER_HORIZONTAL
    }
    val question = TextView(context).apply {
      text = "Are you sure you want to unlock X?"
      textSize = 16f
      setTextColor(Color.WHITE)
      gravity = Gravity.CENTER
      accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
      visibility = View.GONE
    }
    val choices = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      gravity = Gravity.CENTER_HORIZONTAL
      visibility = View.GONE
    }
    val start = action("Unlock") { pressed ->
      pressed.visibility = View.GONE
      question.visibility = View.VISIBLE
      choices.visibility = View.VISIBLE
    }
    choices.addView(action("Keep locked") {
      question.visibility = View.GONE
      choices.visibility = View.GONE
      start.visibility = View.VISIBLE
    }, spaced())
    choices.addView(action("Confirm unlock") { unlock() }, spaced())
    panel.addView(label)
    panel.addView(start, spaced())
    panel.addView(question, spaced())
    panel.addView(choices)
    addView(panel, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.CENTER).apply {
      marginStart = dp(20)
      marginEnd = dp(20)
    })
  }

  private fun action(title: String, onPress: (View) -> Unit) = Button(context).apply {
    text = title
    setAllCaps(false)
    textSize = 14f
    setTextColor(0xff14251e.toInt())
    minWidth = dp(56)
    minHeight = dp(48)
    setPadding(dp(12), 0, dp(12), 0)
    background = rounded(0xffe0eee0.toInt(), 9)
    setOnClickListener { onPress(it) }
  }

  private fun spaced() = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
    topMargin = dp(12)
  }

  private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
    setColor(color)
    cornerRadius = dp(radius).toFloat()
  }

  private fun dp(value: Int) = (value * context.resources.displayMetrics.density).roundToInt()
}
