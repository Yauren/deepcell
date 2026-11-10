package com.deepcell.game.ui

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.deepcell.game.R
import com.deepcell.game.data.GameDatabase

/**
 * The first-game tutorial. Each step shows a card (step number, short title, one or two plain
 * sentences), a glowing amber border around the thing the step is about, and an arrow pointing
 * at it. Offered when a new game starts (MainActivity). The step is saved in settings, so it
 * carries on across the Game and Survivors screens and after the app is closed.
 *
 * Steps that wait for the player to do something (shoot, dig, recruit) move on when the game
 * calls [complete] with that step; explanation steps have a "Got it" button.
 *
 * Each screen creates one with the overlay from view_tutorial.xml and a function that says
 * which view each step points at on that screen (null: not this screen).
 */
class Tutorial(
    private val activity: Activity,
    private val db: GameDatabase,
    private val onStepChanged: (Int) -> Unit = {},
    private val target: (Int) -> View?
) {

    companion object {
        const val OFF = 0
        const val SHOOT = 1        // tap drones
        const val DIG = 2          // tap Scrap Dig until the bar fills
        const val RECRUIT = 3      // open Survivors, recruit
        const val JOBS = 4         // (Survivors screen) choose a job
        const val STORAGE = 5      // workers fill storage; tap to collect
        const val UPGRADES = 6     // spend scrap; repair the hatch
        private const val LAST = UPGRADES

        /** Steps that are explained with a "Got it" button instead of waiting for an action. */
        private val EXPLAIN = setOf(JOBS, STORAGE, UPGRADES)
        /** Steps about the whole sky: the glow outlines it, no arrow. */
        private val NO_ARROW = setOf(SHOOT)

        private val TITLE = mapOf(
            SHOOT to R.string.tutorial_shoot_title,
            DIG to R.string.tutorial_dig_title,
            RECRUIT to R.string.tutorial_recruit_title,
            JOBS to R.string.tutorial_jobs_title,
            STORAGE to R.string.tutorial_storage_title,
            UPGRADES to R.string.tutorial_upgrades_title
        )
        private val TEXT = mapOf(
            SHOOT to R.string.tutorial_shoot,
            DIG to R.string.tutorial_dig,
            RECRUIT to R.string.tutorial_recruit,
            JOBS to R.string.tutorial_jobs,
            STORAGE to R.string.tutorial_storage,
            UPGRADES to R.string.tutorial_upgrades
        )

        /** How far the glowing border sits outside the target, in dp. */
        private const val GLOW_PAD_DP = 6
    }

    private val layer: FrameLayout = activity.findViewById(R.id.tutorialLayer)
    private val glow: View = activity.findViewById(R.id.tutorialGlow)
    private val arrow: View = activity.findViewById(R.id.tutorialArrow)
    private val card: View = activity.findViewById(R.id.tutorialCard)
    private val stepText: TextView = activity.findViewById(R.id.tutorialStep)
    private val title: TextView = activity.findViewById(R.id.tutorialTitle)
    private val text: TextView = activity.findViewById(R.id.tutorialText)
    private val next: Button = activity.findViewById(R.id.tutorialNext)
    private val density = activity.resources.displayMetrics.density

    val step: Int get() = db.getSetting(GameDatabase.SETTING_TUTORIAL, "0").toIntOrNull() ?: OFF
    val active: Boolean get() = step != OFF

    init {
        activity.findViewById<Button>(R.id.tutorialSkip).setOnClickListener { finish() }
        next.setOnClickListener { complete(step) }
    }

    /** Shows the current step if it belongs to this screen; hides the overlay otherwise. */
    fun show() {
        val s = step
        onStepChanged(s)
        val view = if (s == OFF) null else target(s)
        if (view == null) {
            hide()
            return
        }
        stepText.text = activity.getString(R.string.tutorial_step, s, LAST)
        title.setText(TITLE[s] ?: return)
        text.setText(TEXT[s] ?: return)
        next.visibility = if (s in EXPLAIN) View.VISIBLE else View.GONE
        layer.visibility = View.VISIBLE
        // Wait for layout so the target and the card have their sizes.
        layer.post { point(view) }
    }

    /** The player did what [doneStep] asked: go to the next step. Ignored for other steps. */
    fun complete(doneStep: Int) {
        if (step != doneStep || doneStep == OFF) return
        val nextStep = if (doneStep >= LAST) OFF else doneStep + 1
        db.setSetting(GameDatabase.SETTING_TUTORIAL, nextStep.toString())
        show()
    }

    fun finish() {
        db.setSetting(GameDatabase.SETTING_TUTORIAL, OFF.toString())
        onStepChanged(OFF)
        hide()
    }

    private fun hide() {
        arrow.clearAnimation()
        glow.clearAnimation()
        layer.visibility = View.GONE
    }

    /**
     * Frames [view] with the glowing border and points the arrow at it. The card sits low on the
     * screen, over the bedrock under the rooms (above the tab bar), so the sky stays clear during
     * the waves; if that would cover the target, it moves up above the arrow.
     */
    private fun point(view: View) {
        if (layer.visibility != View.VISIBLE) return
        val origin = IntArray(2).also { layer.getLocationInWindow(it) }
        val at = IntArray(2).also { view.getLocationInWindow(it) }
        val left = (at[0] - origin[0]).toFloat()
        val top = (at[1] - origin[1]).toFloat()
        val bottom = top + view.height
        val x = left + view.width / 2f

        // Glowing border: a pulsing amber frame just outside the target.
        // Kept on screen: a full-width target (the sky) gets its frame just inside the edges.
        val pad = (GLOW_PAD_DP * density).toInt()
        val gl = Math.max(0f, left - pad)
        val gt = Math.max(0f, top - pad)
        val gr = Math.min(layer.width.toFloat(), left + view.width + pad)
        val gb = Math.min(layer.height.toFloat(), bottom + pad)
        val gp = glow.layoutParams
        gp.width = (gr - gl).toInt()
        gp.height = (gb - gt).toInt()
        glow.layoutParams = gp
        glow.translationX = gl
        glow.translationY = gt
        if (glow.animation == null) glow.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.tutorial_glow))

        val inUpperQuarter = (top + bottom) / 2 < layer.height * 0.25f
        arrow.translationX = (x - arrow.width / 2f).coerceIn(0f, (layer.width - arrow.width).toFloat())
        if (inUpperQuarter) {
            arrow.rotation = 180f                       // below the target, pointing up
            arrow.translationY = bottom + pad + 2
        } else {
            arrow.rotation = 0f                         // above the target, pointing down
            arrow.translationY = top - arrow.height - pad - 2
        }
        val params = card.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.BOTTOM
        val tabBar = (density * 84).toInt()
        val cardTop = layer.height - tabBar - card.height
        val covered = bottom + pad > cardTop - 8
        params.bottomMargin = if (covered) (layer.height - top).toInt() + pad + arrow.height + 8 else tabBar
        card.layoutParams = params
        if (step in NO_ARROW) {
            arrow.clearAnimation()
            arrow.visibility = View.INVISIBLE
        } else {
            arrow.visibility = View.VISIBLE
            if (arrow.animation == null) arrow.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.tutorial_bob))
        }
    }
}
