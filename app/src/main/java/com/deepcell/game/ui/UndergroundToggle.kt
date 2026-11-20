package com.deepcell.game.ui

import android.view.View
import android.widget.ImageButton
import com.deepcell.game.R

/** Only hides the room panel; its views, workers and game data stay alive. */
class UndergroundToggle(
    private val panel: View,
    private val button: ImageButton,
    private val onCollapse: () -> Unit
) {
    private val collapsed: Boolean get() = panel.visibility == View.GONE

    init {
        button.setOnClickListener {
            val hide = !collapsed
            if (hide) onCollapse()
            setCollapsed(hide)
        }
    }

    fun expand() {
        if (collapsed) setCollapsed(false)
    }

    private fun setCollapsed(hide: Boolean) {
        panel.visibility = if (hide) View.GONE else View.VISIBLE
        button.setImageResource(if (hide) android.R.drawable.arrow_down_float else android.R.drawable.arrow_up_float)
        button.contentDescription = button.context.getString(
            if (hide) R.string.expand_underground else R.string.collapse_underground
        )
    }
}
