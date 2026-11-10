package com.deepcell.game.ui

import android.widget.TextView
import com.deepcell.game.R

/** Use the HUD resource artwork for prices, sized to fit buttons instead of its render size. */
object ResourceIcons {
    fun setPriceIcon(view: TextView, food: Boolean, atEnd: Boolean = false) {
        val icon = view.context.getDrawable(if (food) R.drawable.icon_food else R.drawable.icon_scrap) ?: return
        val size = view.resources.getDimensionPixelSize(R.dimen.resource_price_icon_size)
        icon.setBounds(0, 0, size, size)
        if (atEnd) view.setCompoundDrawablesRelative(null, null, icon, null)
        else view.setCompoundDrawablesRelative(icon, null, null, null)
    }
}
