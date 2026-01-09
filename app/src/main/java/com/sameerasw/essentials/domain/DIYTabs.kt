package com.sameerasw.essentials.domain

import com.sameerasw.essentials.R

enum class DIYTabs(val title: String, val subtitle: String, val iconRes: Int) {
    ESSENTIALS("Essentials", "└(=^‥^=)┐", R.drawable.ic_stat_name),
    FREEZE("Freeze","Disabled apps",  R.drawable.rounded_mode_cool_24),
    DIY("DIY","Do It Yourself" , R.drawable.rounded_experiment_24)
}