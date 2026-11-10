package com.deepcell.game.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.deepcell.game.R
import com.deepcell.game.model.GameState
import com.deepcell.game.model.Upgrade

/**
 * The upgrade list inside the upgrade sheet. [items] is the list for the selected bottom tab
 * (Defense, Supplies or Automation); call [show] to switch tabs.
 */
class UpgradeAdapter(
    private val context: Context,
    private val stateProvider: () -> GameState?,
    private val onBuy: (Upgrade) -> Unit
) : BaseAdapter() {

    private var items: List<Upgrade> = emptyList()

    fun show(newItems: List<Upgrade>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): Upgrade = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    /** Icon for each upgrade id in assets/upgrades.json. Add a line when you add an upgrade. */
    private fun iconFor(id: String): Int = when (id) {
        "tap_power" -> R.drawable.icon_up_tap_power
        "hatch_plating" -> R.drawable.icon_up_hatch_plating
        "dig_tools" -> R.drawable.icon_up_dig_tools
        "farm_tools" -> R.drawable.icon_up_farm_tools
        "auto_turret" -> R.drawable.icon_up_auto_turret
        "drill_rig" -> R.drawable.icon_up_drill_rig
        "storage" -> R.drawable.icon_up_storage
        "guard_training" -> R.drawable.icon_up_guard_training
        "work_shifts" -> R.drawable.icon_up_work_shifts
        "sprinkler" -> R.drawable.icon_up_sprinkler
        "repair_arm" -> R.drawable.icon_up_repair_arm
        else -> R.drawable.icon_scrap
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = convertView ?: LayoutInflater.from(context).inflate(R.layout.row_upgrade, parent, false)
        val upgrade = items[position]
        val state = stateProvider()
        val level = state?.levelOf(upgrade) ?: 0

        row.findViewById<ImageView>(R.id.upgradeIcon).setImageResource(iconFor(upgrade.id))
        row.findViewById<TextView>(R.id.upgradeName).text = upgrade.name
        row.findViewById<TextView>(R.id.upgradeDescription).text = upgrade.description
        val levelBar = row.findViewById<ProgressBar>(R.id.upgradeLevel)
        levelBar.max = upgrade.maxLevel
        levelBar.progress = level

        val button = row.findViewById<Button>(R.id.upgradeBuy)
        if (level >= upgrade.maxLevel) {
            button.text = context.getString(R.string.maxed)
            button.isEnabled = false
            button.alpha = 1f
        } else {
            val cost = upgrade.costAt(level)
            button.text = cost.toString()
            button.isEnabled = true
            // Dim the button when the player can't afford it; tapping still explains why.
            val have = if (upgrade.paidInFood) state?.food else state?.scrap
            button.alpha = if (have != null && have >= cost) 1f else 0.45f
        }
        // Price icon: scrap for most upgrades, a mushroom for the ones paid in food.
        ResourceIcons.setPriceIcon(button, upgrade.paidInFood)
        button.setOnClickListener { onBuy(upgrade) }
        return row
    }
}
