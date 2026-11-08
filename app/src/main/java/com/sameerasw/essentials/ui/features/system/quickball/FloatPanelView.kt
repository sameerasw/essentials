/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Quick Ball Radial Menus
 * File: FloatPanelView.kt
 * Description: Full-screen overlay displaying the radial shortcut items and connectors fanning out from the ball.
 */

package com.sameerasw.essentials.ui.features.system.quickball

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.island.ui.IslandHaptics
import com.sameerasw.essentials.utils.HapticUtil
import kotlin.math.hypot
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor")
class FloatPanelView(
    context: Context,
    fabX: Int,
    fabY: Int,
    fabSize: Int,
    private val actions: List<Action>,
    private val isOnRight: Boolean,
    private val panelHeight: Int,
    private val onDismiss: () -> Unit,
    private val onDismissFinished: () -> Unit,
    private val onActionClicked: (Action) -> Unit,
) : ViewGroup(context) {

    companion object {
        private const val SCREEN_MARGIN_DP = 12f
        private const val ANIMATION_DURATION = 260L
        private const val STAGGER_DELAY = 18L
        private val OVERSHOOT = OvershootInterpolator(0.9f)
        private val DECELERATE = DecelerateInterpolator(1.0f)
    }

    private val settings = SettingsRepository(context)
    private val density = context.resources.displayMetrics.density

    private val buttonSize = (settings.getQuickBallMenuSize() * density).roundToInt()
    private val iconSize = ((settings.getQuickBallMenuSize() * 0.52f) * density).roundToInt()
    private val radius = (settings.getQuickBallMenuRadius() * density).roundToInt()

    private val halfFab = fabSize / 2
    private val fabCenterX = fabX + halfFab
    private val fabCenterY = fabY + halfFab

    private val colors = QuickBallTheme.resolveColors(context, settings)

    private val connectors =
        ConnectorRenderer(
            style = MenuConnectorStyle.fromName(settings.getQuickBallConnectorStyle()),
            color = colors.accentColor,
            strokeWidth = 2.2f * density,
        )
    private val connectorStart = halfFab + (ConnectorRenderer.INSET_DP * density)

    private var arcRadius = 0f
    private var arcEdgeAngle = 0f
    private var fanShiftY = 0f
    private var ringStart = 0.0
    private var ringSweep = 0.0
    private var curveBends = emptyList<Float>()

    private val itemViews = ArrayList<FrameLayout>(actions.size)
    private val itemOffsets: List<Pair<Float, Float>>
    private var isCollapsing = false
    private var hasDismissed = false

    init {
        clipChildren = false
        clipToPadding = false

        itemOffsets = computeFanningOffsets()
        FanLayout.angleRange(itemOffsets).let { (start, sweep) ->
            ringStart = start
            ringSweep = sweep
        }
        curveBends = FanLayout.curveBends(itemOffsets)

        setupTouchHandling()
        createChildViews()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchHandling() {
        setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_OUTSIDE,
                MotionEvent.ACTION_DOWN -> {
                    onDismiss()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    view.performClick()
                    onDismiss()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    onDismiss()
                    true
                }
                else -> false
            }
        }
    }

    private fun createChildViews() {
        val buttonBg = colors.surfaceColor
        val iconColor = colors.accentColor
        val borderColor = colors.borderColor

        val shortcutStyle = QuickBallShortcutStyle.fromName(settings.getQuickBallShortcutStyle())
        val iconPack = QuickBallIconPack.fromName(settings.getQuickBallIconPack())

        actions.forEachIndexed { index, action ->
            val isApp = action is Action.OpenApp && action.packageName.isNotBlank()
            val itemView =
                FrameLayout(context).apply {
                    elevation = 4f * density
                    outlineProvider = ViewOutlineProvider.BACKGROUND
                    clipToOutline = true

                    val itemBg = if (isApp && shortcutStyle == QuickBallShortcutStyle.FULL_BLEED) {
                        GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Color.TRANSPARENT)
                        }
                    } else {
                        GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(buttonBg)
                            setStroke((1.5f * density).roundToInt(), borderColor)
                        }
                    }
                    background = itemBg

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        foreground =
                            RippleDrawable(
                                ColorStateList.valueOf(Color.argb(50, 255, 255, 255)),
                                null,
                                itemBg,
                            )
                    }

                    val imageView =
                        ImageView(context).apply {
                            if (isApp) {
                                val pkg = (action as Action.OpenApp).packageName
                                try {
                                    setImageDrawable(context.packageManager.getApplicationIcon(pkg))
                                    if (shortcutStyle == QuickBallShortcutStyle.MONOCHROME) {
                                        setColorFilter(iconColor)
                                    }
                                } catch (_: Exception) {
                                    val iconRes = QuickBallIconResolver.resolveIcon(action, iconPack)
                                    setImageResource(iconRes)
                                    setColorFilter(iconColor)
                                }
                            } else {
                                val iconRes = QuickBallIconResolver.resolveIcon(action, iconPack)
                                setImageResource(iconRes)
                                setColorFilter(iconColor)
                            }
                        }

                    val imageParams = if (isApp && shortcutStyle == QuickBallShortcutStyle.FULL_BLEED) {
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            Gravity.CENTER,
                        ).apply {
                            val pad = (2 * density).roundToInt()
                            setMargins(pad, pad, pad, pad)
                        }
                    } else {
                        FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER)
                    }

                    addView(imageView, imageParams)

                    setOnClickListener {
                        if (!isCollapsing) {
                            if (settings.isQuickBallHaptic()) {
                                IslandHaptics.commit(context)
                            }
                            onActionClicked(action)
                        }
                    }
                }

            val (offsetX, offsetY) = itemOffsets[index]
            itemView.translationX = -offsetX
            itemView.translationY = -offsetY
            itemView.scaleX = 0f
            itemView.scaleY = 0f
            itemView.alpha = 0f

            addView(itemView, LayoutParams(buttonSize, buttonSize))
            itemViews.add(itemView)
        }
    }

    private fun computeFanningOffsets(): List<Pair<Float, Float>> {
        val margin = SCREEN_MARGIN_DP * density

        val layout =
            FanLayout.compute(
                count = actions.size,
                buttonSize = buttonSize.toFloat(),
                radius = radius.toFloat(),
                halfFab = halfFab.toFloat(),
                density = density,
                maxHalfSpan = if (panelHeight > 0) (panelHeight - buttonSize) / 2f - margin else Float.MAX_VALUE,
            )

        val offsets =
            layout.offsets.map { (offsetX, offsetY) ->
                Pair(if (isOnRight) offsetX else -offsetX, offsetY)
            }

        val minOffsetY = offsets.minOfOrNull { it.second } ?: 0f
        val maxOffsetY = offsets.maxOfOrNull { it.second } ?: 0f
        fanShiftY = getScreenFitShift(minOffsetY, maxOffsetY)

        arcEdgeAngle = layout.arcEdgeAngle
        arcRadius = layout.arcRadius

        return offsets.map { (offsetX, offsetY) -> Pair(offsetX, offsetY + fanShiftY) }
    }

    private fun getScreenFitShift(
        minOffsetY: Float,
        maxOffsetY: Float,
    ): Float {
        if (panelHeight <= 0) return 0f
        val margin = 8f * density
        val halfButton = buttonSize / 2f
        val top = fabCenterY + minOffsetY - halfButton
        val bottom = fabCenterY + maxOffsetY + halfButton

        return when {
            top < margin -> margin - top
            bottom > panelHeight - margin -> panelHeight - margin - bottom
            else -> 0f
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        when (connectors.style) {
            MenuConnectorStyle.NONE -> Unit
            MenuConnectorStyle.ARC -> drawArcConnectors(canvas)
            else -> drawRadialConnectors(canvas)
        }
        super.dispatchDraw(canvas)
    }

    private fun drawRadialConnectors(canvas: Canvas) {
        val centerX = fabCenterX.toFloat()
        val centerY = fabCenterY.toFloat()

        if (connectors.hasRing) {
            connectors.drawRing(
                canvas,
                centerX,
                centerY,
                connectorStart,
                ringStart,
                ringSweep,
                groupOpacity(),
            )
        }

        itemViews.forEachIndexed { index, itemView ->
            val (offsetX, offsetY) = itemOffsets[index]
            val deltaX = offsetX + itemView.translationX
            val deltaY = offsetY + itemView.translationY
            val distance = hypot(deltaX, deltaY)
            if (distance <= 0f) return@forEachIndexed

            connectors.drawConnector(
                canvas = canvas,
                centerX = centerX,
                centerY = centerY,
                directionX = deltaX / distance,
                directionY = deltaY / distance,
                start = connectorStart,
                end = distance - buttonSize / 2f * itemView.scaleX,
                opacity = itemView.alpha,
                bend = curveBends.getOrElse(index) { 0f },
            )
        }
    }

    private fun drawArcConnectors(canvas: Canvas) {
        val centerX = fabCenterX.toFloat()
        val centerY = fabCenterY + fanShiftY

        val startAngle = if (isOnRight) 180f - arcEdgeAngle else 270f
        connectors.drawRing(
            canvas,
            centerX,
            centerY,
            arcRadius,
            Math.toRadians(startAngle.toDouble()),
            Math.toRadians(90.0 + arcEdgeAngle),
            groupOpacity(),
        )

        itemViews.forEachIndexed { index, itemView ->
            val (offsetX, offsetY) = itemOffsets[index]
            val restX = offsetX
            val restY = offsetY - fanShiftY
            val restDistance = hypot(restX, restY)
            if (restDistance <= 0f) return@forEachIndexed

            val distance = hypot(restX + itemView.translationX, restY + itemView.translationY)

            connectors.drawConnector(
                canvas = canvas,
                centerX = centerX,
                centerY = centerY,
                directionX = restX / restDistance,
                directionY = restY / restDistance,
                start = arcRadius,
                end = distance - buttonSize / 2f * itemView.scaleX,
                opacity = itemView.alpha,
            )
        }
    }

    private fun groupOpacity(): Float {
        val minAlpha = itemViews.minOfOrNull { it.alpha } ?: 1f
        return (if (isCollapsing) minAlpha * 2f - 1f else minAlpha).coerceIn(0f, 1f)
    }

    fun animateExpand() {
        isCollapsing = false
        hasDismissed = false

        val lastIndex = itemViews.size - 1
        itemViews.forEachIndexed { index, itemView ->
            itemView.animate()
                .translationX(0f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(ANIMATION_DURATION)
                .setStartDelay((lastIndex - index) * STAGGER_DELAY)
                .setInterpolator(OVERSHOOT)
                .setUpdateListener { invalidate() }
                .start()
        }
    }

    fun animateCollapse() {
        if (isCollapsing) return
        isCollapsing = true

        if (itemViews.isEmpty()) {
            if (!hasDismissed) {
                hasDismissed = true
                onDismissFinished()
            }
            return
        }

        val lastIndex = itemViews.size - 1
        itemViews.forEachIndexed { index, itemView ->
            val (offsetX, offsetY) = itemOffsets[index]
            val animator =
                itemView.animate()
                    .translationX(-offsetX)
                    .translationY(-offsetY)
                    .scaleX(0f)
                    .scaleY(0f)
                    .alpha(0f)
                    .setDuration(ANIMATION_DURATION)
                    .setStartDelay((lastIndex - index) * STAGGER_DELAY)
                    .setInterpolator(DECELERATE)
                    .setUpdateListener { invalidate() }

            if (index == 0) {
                animator.withEndAction {
                    if (!hasDismissed) {
                        hasDismissed = true
                        onDismissFinished()
                    }
                }
            }
            animator.start()
        }
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        itemViews.forEachIndexed { index, child ->
            val (offsetX, offsetY) = itemOffsets[index]
            val centerX = (fabCenterX + offsetX).toInt()
            val centerY = (fabCenterY + offsetY).toInt()
            val halfWidth = child.measuredWidth / 2
            val halfHeight = child.measuredHeight / 2
            child.layout(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        itemViews.forEach { it.animate().cancel() }
    }
}
