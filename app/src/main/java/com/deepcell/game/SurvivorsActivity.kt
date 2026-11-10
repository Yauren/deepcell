package com.deepcell.game

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.deepcell.game.data.GameDatabase
import com.deepcell.game.model.Survivor
import com.deepcell.game.ui.ResourceIcons
import com.deepcell.game.ui.Jobs
import com.deepcell.game.ui.SurvivorAdapter
import com.deepcell.game.ui.Tutorial

/**
 * Survivor list. Tap a survivor to open the job picker (Roof, Scrap Dig, Mushroom Farm,
 * Collector, each with what it does). Recruiting costs food, more for every survivor already
 * in the bunker; the recruit gets a generated name and starts in Scrap Dig.
 */
class SurvivorsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BUNKER_NAME = "bunker_name"
        /** Food for the next recruit: 25 for the first, then x1.9 for every survivor after that. */
        const val RECRUIT_BASE = 25
        private const val RECRUIT_GROWTH = 1.9

        fun recruitCost(survivorCount: Int): Int =
            Math.round(RECRUIT_BASE * Math.pow(RECRUIT_GROWTH, (survivorCount - 1).coerceAtLeast(0).toDouble())).toInt()
    }

    private lateinit var db: GameDatabase
    private lateinit var foodText: TextView
    private lateinit var adapter: SurvivorAdapter
    private lateinit var listView: ListView
    private lateinit var tutorial: Tutorial
    private lateinit var recruitButton: Button
    private var survivors: List<Survivor> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_survivors)
        db = GameDatabase(this)

        // Extra from GameActivity (Module 7).
        val bunkerName = intent.extras?.getString(EXTRA_BUNKER_NAME) ?: getString(R.string.default_bunker_name)
        findViewById<TextView>(R.id.survivorsTitle).text = getString(R.string.survivors_title, bunkerName)
        foodText = findViewById(R.id.survivorsFood)

        adapter = SurvivorAdapter(this, survivors)
        listView = findViewById(R.id.survivorList)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ -> pickJob(survivors[position]) }

        recruitButton = findViewById(R.id.recruitButton)
        ResourceIcons.setPriceIcon(recruitButton, food = true, atEnd = true)

        // Tutorial steps on this screen point at the recruit button, then at the newest survivor.
        tutorial = Tutorial(this, db) { step ->
            when (step) {
                Tutorial.RECRUIT -> recruitButton
                Tutorial.JOBS -> listView.getChildAt(survivors.size - 1 - listView.firstVisiblePosition) ?: listView
                else -> null
            }
        }

        recruitButton.setOnClickListener {
            val food = db.getFood()
            val cost = recruitCost(survivors.size)
            if (food < cost) {
                Toast.makeText(this, R.string.not_enough_food, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val name = Survivor.randomName(survivors.map { it.name })
            db.recruitSurvivor(name, Survivor.JOB_DIG, food - cost)
            Toast.makeText(this, getString(R.string.recruited, name), Toast.LENGTH_SHORT).show()
            tutorial.complete(Tutorial.RECRUIT)
            refresh()
        }

        findViewById<ImageButton>(R.id.survivorsBack).setOnClickListener { finish() }
        refresh()
    }

    /** Job picker: one row per job with its figure and what it does; the current job is marked. */
    private fun pickJob(survivor: Survivor) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_job)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9f).toInt(), WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.findViewById<TextView>(R.id.jobTitle).text = getString(R.string.job_for, survivor.name)
        val rows = dialog.findViewById<LinearLayout>(R.id.jobRows)
        for (job in Survivor.JOBS) {
            val row = layoutInflater.inflate(R.layout.row_job, rows, false)
            row.findViewById<ImageView>(R.id.jobFigure).setImageResource(Jobs.figure(job))
            row.findViewById<TextView>(R.id.jobName).text = job
            row.findViewById<TextView>(R.id.jobEffect).setText(Jobs.effect(job))
            row.isSelected = job == survivor.job
            row.setOnClickListener {
                if (job != survivor.job) {
                    db.updateSurvivorJob(survivor.id, job)
                    Toast.makeText(this, getString(R.string.job_changed, survivor.name, job), Toast.LENGTH_SHORT).show()
                    tutorial.complete(Tutorial.JOBS)
                    refresh()
                }
                dialog.dismiss()
            }
            rows.addView(row)
        }
        dialog.show()
    }

    /** Reloads the list; the tutorial arrow is placed again once the rows are laid out. */
    private fun refresh() {
        survivors = db.getSurvivors()
        foodText.text = db.getFood().toString()
        adapter.update(survivors)
        recruitButton.text = getString(R.string.recruit_cost, recruitCost(survivors.size))
        listView.post { tutorial.show() }
    }

    override fun onDestroy() {
        db.close()
        super.onDestroy()
    }
}
