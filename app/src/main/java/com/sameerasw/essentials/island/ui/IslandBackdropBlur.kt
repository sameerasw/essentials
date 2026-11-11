package com.sameerasw.essentials.island.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt

internal object IslandBackdropBlurSupport {
    val available: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

private class BackdropBlurView(context: Context) : View(context) {
    private var blur: Drawable? = null
    private var failed = false
    private val setRadius by lazy { blur?.javaClass?.getMethod("setBlurRadius", Int::class.javaPrimitiveType) }
    private val setCorner by lazy { blur?.javaClass?.getMethod("setCornerRadius", Float::class.javaPrimitiveType) }

    init {
        setWillNotDraw(false)
    }

    private fun ensureBlur(): Drawable? {
        if (blur != null || failed) return blur
        blur = try {
            val root = View::class.java.getMethod("getViewRootImpl").invoke(this) ?: return null
            root.javaClass.getMethod("createBackgroundBlurDrawable").invoke(root) as? Drawable
        } catch (_: Throwable) {
            failed = true
            null
        }
        return blur
    }

    fun update(frame: IslandVisualFrame, maxRadius: Float) {
        val d = ensureBlur() ?: return
        val b = frame.bounds
        val visible = frame.alpha > 0f && b.width > 0f && b.height > 0f
        try {
            setRadius?.invoke(d, if (visible) maxRadius.roundToInt() else 0)
            setCorner?.invoke(d, frame.cornerRadius)
        } catch (_: Throwable) {
        }
        d.alpha = if (visible) 255 else 0
        d.setBounds(b.left.roundToInt(), b.top.roundToInt(), b.right.roundToInt(), b.bottom.roundToInt())
        invalidate()
    }

    override fun onDetachedFromWindow() {
        blur?.let {
            try {
                setRadius?.invoke(it, 0)
            } catch (_: Throwable) {
            }
        }
        blur = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        blur?.draw(canvas)
    }
}

@Composable
internal fun IslandBackdropBlur(frame: () -> IslandVisualFrame, radiusPx: Float, modifier: Modifier = Modifier) {
    if (!IslandBackdropBlurSupport.available) return
    val holder = remember { arrayOfNulls<BackdropBlurView>(1) }
    Box(modifier.drawBehind { holder[0]?.update(frame(), radiusPx) }) {
        AndroidView(
            factory = { BackdropBlurView(it).also { v -> holder[0] = v } },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
