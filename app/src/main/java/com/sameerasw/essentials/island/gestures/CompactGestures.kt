package com.sameerasw.essentials.island.gestures

enum class SlideMode { None, Volume, Brightness, SoundMode, Track }

interface CompactGestures {
    val hasLongPress: Boolean
    val longPressOpensBrief: Boolean get() = false
    val slideMode: SlideMode

    fun longPress()

    fun slideStep(forward: Boolean)

    fun slideCommit(dx: Float)

    fun levelPercent(): Int
    fun soundMode(): RingMode
    fun soundModeAfter(dx: Float): RingMode
    fun trackForward(dx: Float): Boolean

    val hasAny: Boolean get() = hasLongPress || slideMode != SlideMode.None

    object None : CompactGestures {
        override val hasLongPress = false
        override val slideMode = SlideMode.None
        override fun longPress() {}
        override fun slideStep(forward: Boolean) {}
        override fun slideCommit(dx: Float) {}
        override fun levelPercent() = 0
        override fun soundMode() = RingMode.Normal
        override fun soundModeAfter(dx: Float) = RingMode.Normal
        override fun trackForward(dx: Float) = true
    }
}
