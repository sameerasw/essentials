package com.sameerasw.essentials.utils

import android.content.Context
import com.sameerasw.essentials.R

object StatusBarInsetsUtils {
    private const val HIDDEN_MARGIN = 100
    private const val SHOWN_MARGIN = -1

    fun apply(
        context: Context,
        hidden: Boolean,
    ) {
        val margin = if (hidden) HIDDEN_MARGIN else SHOWN_MARGIN
        // The density bump forces SystemUI to relayout so the inset applies without a rotation
        val command =
            "o=\$(wm density); " +
                "b=\$(echo \"\$o\" | grep Override | grep -o '[0-9]*'); " +
                "p=\$(echo \"\$o\" | grep Physical | grep -o '[0-9]*'); " +
                "d=\${b:-\$p}; " +
                "cmd statusbar status-bar-insets bottom-margin -r 0 -m $margin; " +
                "wm density \$((d+5)); " +
                "if [ -n \"\$b\" ]; then wm density \$b; else wm density reset; fi"
        ShellUtils.runCommand(context, command, context.getString(R.string.island_hide_status_bar_title))
    }
}
