package com.deepcell.game

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.deepcell.game.data.GameDatabase
import com.deepcell.game.data.JsonLoader
import com.deepcell.game.model.Enemy
import com.deepcell.game.model.Floor
import com.deepcell.game.model.GameState
import com.deepcell.game.model.Survivor
import com.deepcell.game.model.Upgrade
import com.deepcell.game.model.Wave
import com.deepcell.game.ui.FloorAdapter
import com.deepcell.game.ui.Fx
import com.deepcell.game.ui.Music
import com.deepcell.game.ui.Notifier
import com.deepcell.game.ui.Quality
import com.deepcell.game.ui.Sfx
import com.deepcell.game.ui.Tutorial
import com.deepcell.game.ui.UndergroundToggle
import com.deepcell.game.ui.UpgradeAdapter
import kotlin.random.Random

/**
 * The main screen, top to bottom: sky (enemies fall), hatch + health bar,
 * underground floors, upgrade list, bottom tabs.
 *
 * One Handler loop runs every FRAME_MS while the screen is visible. It moves enemies,
 * spawns the wave, and once per second runs the automation machines.
 *
 * Save flow: onResume loads from SQLite and adds offline earnings; onPause stops the
 * loop and saves. Other screens (Survivors, Settings) write straight to SQLite.
 */
class GameActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BUNKER_NAME = "bunker_name"
        const val EXTRA_NEW_GAME = "new_game"

        const val BASE_HATCH_HP = 100
        private const val HATCH_HP_PER_PLATING = 20
        private const val REPAIR_SCRAP_COST = 1
        private const val REPAIR_AMOUNT = 5

        private const val FRAME_MS = 33L                  // about 30 updates per second
        private const val FRAME_MS_LOW = 50L              // 20 updates per second in low detail
        private const val AUTOMATION_TICK_MS = 1000L      // machines act once per second
        private const val WAVE_BREAK_MS = 2000L           // pause before the next wave
        private const val MAX_OFFLINE_SECONDS = 8 * 60 * 60L
        private const val MIN_OFFLINE_SECONDS_FOR_TOAST = 30L

        // Upgrade ids from assets/upgrades.json
        private const val UP_TAP_POWER = "tap_power"
        private const val UP_HATCH_PLATING = "hatch_plating"
        private const val UP_DIG_TOOLS = "dig_tools"
        private const val UP_FARM_TOOLS = "farm_tools"
        private const val AUTO_TURRET = "auto_turret"
        private const val DRILL_RIG = "drill_rig"
        private const val SPRINKLER = "sprinkler"
        private const val REPAIR_ARM = "repair_arm"
        private const val STORAGE = "storage"
        private const val GUARD_TRAINING = "guard_training"
        private const val WORK_SHIFTS = "work_shifts"
        /** Each Guard Training level multiplies the guards' time between shots by this. */
        private const val GUARD_TRAINING_FACTOR = 0.85
        private const val BANNER_MS = 1600L
        // Upkeep: every survivor eats 1 food every EAT_SECONDS, also while the app is closed.
        // With no food left the bunker is hungry: workers stop and guards stop shooting.
        private const val EAT_SECONDS = 40.0

        // Rooms: each worker adds this much progress per second by themselves; storage holds
        // ten full bars, plus STORAGE_PER_LEVEL per Storage Racks level.
        private const val WORKER_RATE = 1
        private const val STORAGE_BARS = 10
        private const val STORAGE_PER_LEVEL = 25
        // Collector walking speed in dp per second: base, plus per extra survivor on the job.
        private const val COLLECTOR_SPEED_DP = 70f
        private const val COLLECTOR_SPEED_PER_LEVEL = 25f
        private const val COLLECTOR_GRAB_MS = 480L

        // Roof guards: spots on the deck as fractions of the bunker image (left of the hatch,
        // right of it, then closer in), the deck height, and the guard frame size.
        private val GUARD_SLOTS = floatArrayOf(0.34f, 0.66f, 0.43f, 0.57f)
        private const val DECK_Y = 0.49f
        private const val GUARD_DP = 47
        private const val FEET_Y = 0.93f          // feet in the guard frame
        private const val GUARD_FIRE_MS = 230L
        // Roof guards also shoot by themselves: together they fire once every
        // GUARD_AUTO_MS / guards, each shot doing GUARD_DAMAGE to the enemy nearest the hatch.
        private const val GUARD_AUTO_MS = 2000L
        private const val GUARD_DAMAGE = 1
        /** Guards only shoot at drones that have come this far down the sky (0 top, 1 hatch). */
        private const val GUARD_RANGE = 0.4f
        // Auto-Turret, mounted on the hatch dome in the middle of the deck: spot (fractions of
        // the bunker image), size, and where the foot is in its frame.
        private const val TURRET_X = 0.5f
        private const val DOME_Y = 0.43f
        private const val TURRET_DP = 50
        private const val TURRET_FOOT_Y = 0.64f
        private const val TRACER_MS = 160L
        private const val HATCH_Y = 0.30f         // hatch on the bunker image, for sparks
        // Enemy flight: side-to-side sway and up-down bob (sine waves, so they ease in and out
        // at each end of the swing).
        private const val SWAY_DP = 10f
        private const val BOB_DP = 3f
        private const val SWAY_SPEED = 2.2f

        private const val BOSS = "boss"

        /** Enemy HP multiplier for each entry of R.array.difficulty_names. */
        private val DIFFICULTY_HP = doubleArrayOf(0.75, 1.0, 1.5)
    }

    // ---- views ----
    private lateinit var sky: FrameLayout
    private lateinit var bunker: FrameLayout
    private lateinit var roofCrew: FrameLayout
    private lateinit var fxLayer: FrameLayout
    private lateinit var floorList: ListView
    private lateinit var undergroundToggle: UndergroundToggle
    /** Low detail (ui/Quality): fewer decorations, 20 updates a second. */
    private var lowDetail = false
    private var frameMs = FRAME_MS
    private lateinit var hatchBar: ProgressBar
    private lateinit var hatchText: TextView
    private lateinit var hudScrap: TextView
    private lateinit var hudFood: TextView
    private lateinit var hudWave: TextView
    private lateinit var hudBossIcon: ImageView
    private lateinit var upgradeSheet: LinearLayout
    private lateinit var upgradeTitle: TextView
    private lateinit var tabButtons: List<ImageButton>

    // ---- data ----
    private lateinit var db: GameDatabase
    private lateinit var sfx: Sfx
    private lateinit var music: Music
    private lateinit var tutorial: Tutorial
    private val guardViews = ArrayList<ImageView>()
    private var guardTimerMs = 0L
    private var nextGuard = 0
    private var turretGun: ImageView? = null
    private var tracer: View? = null
    private lateinit var upgrades: List<Upgrade>
    private lateinit var waveData: JsonLoader.WaveData
    private lateinit var floorAdapter: FloorAdapter
    private lateinit var upgradeAdapter: UpgradeAdapter
    private var state: GameState? = null
    private var survivors: List<Survivor> = emptyList()
    private var difficultyHp = 1.0
    /** Upgrade type shown in the sheet, or null when the sheet is closed. */
    private var currentTab: String? = null

    private lateinit var digFloor: Floor
    private lateinit var farmFloor: Floor

    // ---- wave in progress ----
    private val enemies = ArrayList<Enemy>()
    private val spawnQueue = ArrayList<String>()
    private var currentWave: Wave? = null
    private var waveHpScale = 1.0
    private var waveSpeedScale = 1f
    /** Food owed but not yet eaten (fractions of a meal add up each second). */
    private var foodDebt = 0.0
    /** Out of food: workers and guards stop until there is food again. */
    private var hungry = false
    private var spawnTimerMs = 0L
    private var automationTimerMs = 0L
    /** Goes up every time a wave (re)starts, so the loop can tell a breach happened mid-frame. */
    private var waveGeneration = 0

    // ---- Handler game loop (Module 6) ----
    private val handler = Handler(Looper.getMainLooper())
    private val gameLoop = object : Runnable {
        override fun run() {
            step()
            handler.postDelayed(this, frameMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        volumeControlStream = android.media.AudioManager.STREAM_MUSIC   // volume keys change game sounds

        db = GameDatabase(this)
        upgrades = JsonLoader.loadUpgrades(this)
        waveData = JsonLoader.loadWaves(this)
        Notifier.createChannel(this)

        sky = findViewById(R.id.sky)
        bunker = findViewById(R.id.bunker)
        roofCrew = findViewById(R.id.roofCrew)
        fxLayer = findViewById(R.id.fxLayer)
        sfx = Sfx(this)
        music = Music(this)
        hatchBar = findViewById(R.id.hatchBar)
        hatchText = findViewById(R.id.hatchText)
        hudScrap = findViewById(R.id.hudScrap)
        hudFood = findViewById(R.id.hudFood)
        hudWave = findViewById(R.id.hudWave)
        hudBossIcon = findViewById(R.id.hudBossIcon)
        upgradeSheet = findViewById(R.id.upgradeSheet)
        upgradeTitle = findViewById(R.id.upgradeTitle)

        digFloor = Floor("dig", getString(R.string.floor_dig), "scrap", 10, 5)
        farmFloor = Floor("farm", getString(R.string.floor_farm), "food", 10, 3)
        floorAdapter = FloorAdapter(this, listOf(digFloor, farmFloor), { workersIn(it) },
            { f -> state?.let { productionPerSecond(f, it) } ?: 0 }, { hungry }, { openSurvivors() }) {
            onFloorTapped(it)
        }
        floorList = findViewById(R.id.floorList)
        floorList.addFooterView(layoutInflater.inflate(R.layout.footer_bedrock, floorList, false), null, false)
        floorList.adapter = floorAdapter
        undergroundToggle = UndergroundToggle(
            findViewById(R.id.undergroundPanel), findViewById(R.id.toggleUnderground)
        ) { selectTab(null) }

        upgradeAdapter = UpgradeAdapter(this, { state }) { buyUpgrade(it) }
        findViewById<ListView>(R.id.upgradeList).adapter = upgradeAdapter
        findViewById<ImageButton>(R.id.upgradeClose).setOnClickListener { selectTab(null) }

        bunker.setOnClickListener { repairHatch() }

        // Bottom bar: three upgrade tabs and the Survivors screen.
        val defense = findViewById<ImageButton>(R.id.tabDefense)
        val supplies = findViewById<ImageButton>(R.id.tabSupplies)
        val automation = findViewById<ImageButton>(R.id.tabAutomation)
        tabButtons = listOf(defense, supplies, automation)
        defense.setOnClickListener { pop(it); selectTab(GameState.TYPE_DEFENSE) }
        supplies.setOnClickListener { pop(it); selectTab(GameState.TYPE_SUPPLIES) }
        automation.setOnClickListener { pop(it); selectTab(GameState.TYPE_AUTOMATION) }
        val survivorsTab = findViewById<ImageButton>(R.id.tabSurvivors)
        survivorsTab.setOnClickListener {
            pop(it)
            openSurvivors()
        }
        findViewById<ImageButton>(R.id.settingsButton).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra(SettingsActivity.EXTRA_BUNKER_NAME, state?.bunkerName)
            intent.putExtra(SettingsActivity.EXTRA_WAVE, state?.wave ?: 1)
            startActivity(intent)
        }

        // Extras from MainActivity (Module 7).
        val extras = intent.extras
        if (savedInstanceState == null && extras != null && extras.getBoolean(EXTRA_NEW_GAME, false)) {
            val name = extras.getString(EXTRA_BUNKER_NAME) ?: getString(R.string.default_bunker_name)
            Toast.makeText(this, getString(R.string.welcome, name), Toast.LENGTH_SHORT).show()
        }
        selectTab(null)

        // Tutorial: what each step points at on this screen.
        tutorial = Tutorial(this, db) { step ->
            // A room tutorial step must keep its target visible.
            if (step == Tutorial.DIG || step == Tutorial.STORAGE) undergroundToggle.expand()
            when (step) {
                Tutorial.SHOOT -> sky
                Tutorial.DIG -> roomRow(0)?.findViewById(R.id.room)
                Tutorial.RECRUIT, Tutorial.JOBS -> survivorsTab
                // The storage of the room the new worker went to.
                Tutorial.STORAGE -> roomRow(if (workersIn(farmFloor) > workersIn(digFloor)) 1 else 0)
                    ?.findViewById(R.id.roomStorage)
                Tutorial.UPGRADES -> defense.parent.parent as? View    // the whole tab bar
                else -> null
            }
        }
        // Keep each enemy's progress towards the hatch when the sky grows or shrinks.
        sky.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            val oldHeight = oldBottom - oldTop
            val newHeight = bottom - top
            if (oldHeight > 0 && oldHeight != newHeight) {
                val scale = newHeight.toFloat() / oldHeight
                for (enemy in enemies) {
                    enemy.y = (enemy.y + enemy.view.height) * scale - enemy.view.height
                    enemy.view.translationY = enemy.y
                }
                tutorial.show()
            }
        }
    }

    /** The on-screen row of room [index], or null while it is scrolled away. */
    private fun roomRow(index: Int): View? = floorList.getChildAt(index - floorList.firstVisiblePosition)

    private fun openSurvivors() {
        val intent = Intent(this, SurvivorsActivity::class.java)
        intent.putExtra(SurvivorsActivity.EXTRA_BUNKER_NAME, state?.bunkerName)
        startActivity(intent)
    }

    // ---------------- lifecycle (Module 5) ----------------

    override fun onResume() {
        super.onResume()
        val loaded = db.loadState()
        if (loaded == null) {           // save was reset or never made
            finish()
            return
        }
        state = loaded
        survivors = db.getSurvivors()
        sfx.enabled = db.getSetting(GameDatabase.SETTING_SOUND, "true") == "true"
        music.enabled = db.getSetting(GameDatabase.SETTING_MUSIC, "true") == "true"
        lowDetail = Quality.lowDetail(this, db.getSetting(GameDatabase.SETTING_DETAIL, Quality.AUTO))
        floorAdapter.lowDetail = lowDetail
        frameMs = if (lowDetail) FRAME_MS_LOW else FRAME_MS
        showRoofCrew()
        difficultyHp = DIFFICULTY_HP[db.getSetting(GameDatabase.SETTING_DIFFICULTY, "1").toInt()]

        digFloor.progress = loaded.digProgress
        farmFloor.progress = loaded.farmProgress
        digFloor.stock = loaded.digStock
        farmFloor.stock = loaded.farmStock
        updateCapacity(loaded)
        digFloor.automated = loaded.level(DRILL_RIG) > 0
        farmFloor.automated = loaded.level(SPRINKLER) > 0

        addOfflineEarnings(loaded)

        if (currentWave == null) startWave()
        updateMusic()
        refreshAll()
        handler.post(gameLoop)
        // Recruited in the Survivors screen: that tutorial step is done.
        if (tutorial.step == Tutorial.RECRUIT && survivors.size > 1) tutorial.complete(Tutorial.RECRUIT)
        floorList.post { tutorial.show() }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(gameLoop)
        cancelCollector()
        music.pause()
        saveNow()
    }

    override fun onDestroy() {
        handler.removeCallbacks(gameLoop)
        sfx.release()
        music.release()
        db.close()
        super.onDestroy()
    }

    private fun saveNow() {
        val s = state ?: return
        s.digProgress = digFloor.progress
        s.farmProgress = farmFloor.progress
        s.digStock = digFloor.stock
        s.farmStock = farmFloor.stock
        s.lastPlayed = System.currentTimeMillis()
        db.saveState(s)
    }

    /**
     * While the app was closed the workers and the Drill Rig kept working. Without a Collector
     * their output waits in the rooms (up to the storage capacity); with one, it was carried up
     * as it was made, so storage never filled.
     */
    private fun addOfflineEarnings(s: GameState) {
        val now = System.currentTimeMillis()
        val seconds = ((now - s.lastPlayed) / 1000L).coerceIn(0L, MAX_OFFLINE_SECONDS)
        s.lastPlayed = now
        if (seconds <= 0) return
        val collector = collectors() > 0
        // Meals while away. If the food ran out, workers only worked while it lasted.
        val meals = (survivors.size * seconds / EAT_SECONDS).toInt()
        val fed = if (meals <= 0) 1.0 else Math.min(1.0, s.food.toDouble() / meals)
        s.food = Math.max(0, s.food - meals)
        var scrap = 0
        var food = 0
        for (floor in listOf(digFloor, farmFloor)) {
            val amount = (workerRate(floor, s) * seconds * fed + machineRate(floor, s) * seconds).toInt()
            if (amount <= 0) continue
            val made = if (collector) {
                floor.addProgress(amount) * floor.yieldPerFill
            } else {
                floor.work(amount)
            }
            if (floor === digFloor) scrap += made else food += made
        }
        if (collector) {
            s.scrap += scrap + digFloor.takeStock()
            s.food += food + farmFloor.takeStock()
        }
        if ((scrap > 0 || food > 0) && seconds >= MIN_OFFLINE_SECONDS_FOR_TOAST) {
            val text = if (collector) getString(R.string.collected, scrap, food)
            else getString(R.string.offline_stored, scrap, food)
            Toast.makeText(this, text, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Progress per second a room makes by itself: each worker (more with Work Shifts), unless the
     * bunker is hungry, plus its machine (Drill Rig in Scrap Dig, Sprinkler in Mushroom Farm),
     * which doesn't eat.
     */
    private fun productionPerSecond(floor: Floor, s: GameState): Int =
        (if (hungry) 0 else workerRate(floor, s)) + machineRate(floor, s)

    private fun workerRate(floor: Floor, s: GameState): Int = workersIn(floor) * (WORKER_RATE + s.level(WORK_SHIFTS))

    private fun machineRate(floor: Floor, s: GameState): Int = s.level(if (floor === digFloor) DRILL_RIG else SPRINKLER)

    /** Called once a second: survivors eat; running out (or getting food again) is announced. */
    private fun eat(s: GameState) {
        foodDebt += survivors.size / EAT_SECONDS
        while (foodDebt >= 1.0 && s.food > 0) {
            s.food -= 1
            foodDebt -= 1.0
        }
        val nowHungry = s.food == 0 && foodDebt >= 1.0
        if (nowHungry != hungry) {
            hungry = nowHungry
            if (hungry) {
                sfx.play(Sfx.Sound.ALARM)
                Toast.makeText(this, R.string.out_of_food, Toast.LENGTH_LONG).show()
            }
            floorAdapter.notifyDataSetChanged()
        }
        if (!hungry) foodDebt = Math.min(foodDebt, 1.0)
        refreshHud()
    }

    /** Storage: ten full bars per room, more with Storage Racks. */
    private fun updateCapacity(s: GameState) {
        for (floor in listOf(digFloor, farmFloor)) {
            floor.capacity = floor.yieldPerFill * STORAGE_BARS + STORAGE_PER_LEVEL * s.level(STORAGE)
        }
    }

    // ---------------- game loop ----------------

    private fun step() {
        val s = state ?: return
        if (sky.height == 0) return     // layout not measured yet

        // Spawn the next enemy of the wave.
        val wave = currentWave
        val generation = waveGeneration
        spawnTimerMs += frameMs
        if (wave != null && spawnQueue.isNotEmpty() && spawnTimerMs >= wave.spawnMs) {
            spawnTimerMs = 0
            spawnEnemy(spawnQueue.removeAt(0))
        }

        // Move enemies down. Anything that reaches the bottom of the sky hits the hatch.
        // enemy.y is the real position (it decides when the hatch is hit); the view adds a
        // flight sway on top, which never changes the timing.
        val pxPerDp = resources.displayMetrics.density
        val now = System.currentTimeMillis() / 1000f
        for (enemy in ArrayList(enemies)) {
            enemy.y += enemy.speedDp * pxPerDp * frameMs / 1000f
            val fallen = (enemy.y / sky.height).coerceIn(0f, 1f)
            // Drift towards the bunker with ease-in-out (smoothstep): it turns slowly at the
            // start, crosses fastest in the middle, and lines up gently over the roof.
            val eased = fallen * fallen * (3f - 2f * fallen)
            val sway = Math.sin((now * SWAY_SPEED + enemy.phase).toDouble()).toFloat()
            val bob = Math.sin((now * SWAY_SPEED * 2f + enemy.phase).toDouble()).toFloat()
            // Sway fades out near the hatch so the hit point stays where the drift aims.
            val calm = 1f - eased
            enemy.view.translationX = enemy.startX + (enemy.targetX - enemy.startX) * eased +
                sway * SWAY_DP * pxPerDp * calm
            enemy.view.translationY = enemy.y + bob * BOB_DP * pxPerDp * calm
            if (enemy.y + enemy.view.height >= sky.height) {
                val blast = Math.max(enemy.view.width * 3 / 2, (64 * pxPerDp).toInt())
                Fx.burst(fxLayer, R.drawable.fx_explode, enemy.view.translationX + enemy.view.width / 2f,
                    sky.height.toFloat(), blast)
                sfx.play(Sfx.Sound.IMPACT)
                removeEnemy(enemy)
                damageHatch(enemy.type.damage)
                if (waveGeneration != generation) return   // a breach restarted the wave
            }
        }

        updateCollector()
        guardsAutoFire()

        // Machines work once per second.
        automationTimerMs += frameMs
        if (automationTimerMs >= AUTOMATION_TICK_MS) {
            automationTimerMs = 0
            automationTick(s)
        }

        if (wave != null && spawnQueue.isEmpty() && enemies.isEmpty()) onWaveCleared()
    }

    private fun automationTick(s: GameState) {
        // Auto-Turret: shoots the enemy closest to the hatch. Damage = its level.
        val turret = s.level(AUTO_TURRET)
        if (turret > 0) {
            val nearest = enemies.filter { it.y > 0 }.maxByOrNull { it.y }
            if (nearest != null) {
                sfx.play(Sfx.Sound.TURRET)
                turretShot(nearest)
                hitEnemy(nearest, turret)
            }
        }
        eat(s)
        // Repair Arm: patches the hatch a little every second.
        val arm = s.level(REPAIR_ARM)
        if (arm > 0 && s.hatchHp < maxHatchHp()) {
            s.hatchHp = Math.min(maxHatchHp(), s.hatchHp + arm)
            refreshHud()
        }
        // Rooms work by themselves (workers, machines); the output goes into their storage.
        var changed = false
        for (floor in listOf(digFloor, farmFloor)) {
            val rate = productionPerSecond(floor, s)
            if (rate > 0 && !floor.isFull) {
                floor.work(rate)
                changed = true
            }
        }
        if (changed) floorAdapter.refresh(floorList)
        // Collector: walks to the next room that has something stored.
        if (collectors() > 0 && trip == null) startCollectorTrip()
    }

    // ---------------- collector ----------------

    /** Survivors with the Collector job. One walks the rooms; each extra one makes it faster. */
    private fun collectors(): Int = survivors.count { it.job == Survivor.JOB_COLLECTOR }

    /** One Collector trip through a room: walk in, grab, walk out. Driven by the game loop. */
    private class Trip(val floor: Floor, val index: Int, val walkMs: Long) {
        var phase = 0
        var phaseStart = System.currentTimeMillis()
    }

    private var trip: Trip? = null
    private var collectorNext = 0

    /** Sends the Collector to the next room (in order) with something in storage. */
    private fun startCollectorTrip() {
        val floors = listOf(digFloor, farmFloor)
        val index = (0 until floors.size).map { (collectorNext + it) % floors.size }
            .firstOrNull { floors[it].stock > 0 } ?: return
        collectorNext = index + 1
        val speed = (COLLECTOR_SPEED_DP + COLLECTOR_SPEED_PER_LEVEL * (collectors() - 1)) *
            resources.displayMetrics.density
        val walkMs = (floorAdapter.collectorDistancePx() / speed * 1000).toLong().coerceAtLeast(400L)
        trip = Trip(floors[index], index, walkMs)
    }

    /** Called every frame: moves the trip on and draws the Collector in its room. */
    private fun updateCollector() {
        val t = trip ?: return
        val now = System.currentTimeMillis()
        val elapsed = now - t.phaseStart
        when (t.phase) {
            0 -> if (elapsed >= t.walkMs) { t.phase = 1; t.phaseStart = now }
            1 -> if (elapsed >= COLLECTOR_GRAB_MS) { collectFrom(t.floor); t.phase = 2; t.phaseStart = now }
            2 -> if (elapsed >= t.walkMs) { t.phase = 3 }
        }
        val list = findViewById<ListView>(R.id.floorList)
        val row = list.getChildAt(t.index - list.firstVisiblePosition)
        if (t.phase == 3) {
            row?.let { floorAdapter.drawCollector(it, -1, 0f) }
            trip = null
            return
        }
        val progress = if (t.phase == 1) 1f else (now - t.phaseStart).toFloat() / t.walkMs
        row?.let { floorAdapter.drawCollector(it, t.phase, progress) }
    }

    /** Leaving the screen: the trip is dropped (offline earnings collect for the Collector). */
    private fun cancelCollector() {
        val t = trip ?: return
        val list = findViewById<ListView>(R.id.floorList)
        list.getChildAt(t.index - list.firstVisiblePosition)?.let { floorAdapter.drawCollector(it, -1, 0f) }
        trip = null
    }

    /** Empties a room's storage into the bunker's resources, with the "+N" popup and a sound. */
    private fun collectFrom(floor: Floor): Int {
        val s = state ?: return 0
        val amount = floor.takeStock()
        if (amount <= 0) return 0
        if (floor === digFloor) s.scrap += amount else s.food += amount
        floor.pendingPopup = amount
        sfx.play(Sfx.Sound.COLLECT)
        floorAdapter.refresh(floorList)
        refreshUpgrades()
        refreshHud()
        return amount
    }

    // ---------------- waves and enemies ----------------

    /**
     * Wave n uses entry (n-1) % 10 of waves.json as its mix of enemies, and gets harder as n
     * grows: countPerWave more enemies per wave (wave 11 has 1.8x the drones of wave 1), spawns
     * closer together, and each later 10-wave cycle adds HP and speed and one more boss every
     * second cycle. The order is shuffled (the same way each time for a wave), bosses in the middle.
     */
    private fun startWave() {
        val s = state ?: return
        val index = (s.wave - 1) % waveData.waves.size
        val cycle = (s.wave - 1) / waveData.waves.size
        val n = s.wave - 1
        val template = waveData.waves[index]
        val countScale = 1.0 + n * waveData.countPerWave
        val ids = ArrayList<String>()
        for ((type, group) in template.enemyIds.filter { it != BOSS }.groupBy { it }) {
            repeat(Math.round(group.size * countScale).toInt()) { ids.add(type) }
        }
        ids.shuffle(Random(s.wave))
        val bosses = template.enemyIds.count { it == BOSS }.let { if (it > 0) it + cycle / 2 else 0 }
        for (b in 1..bosses) ids.add(ids.size * b / (bosses + 1), BOSS)
        val spawnScale = Math.max(waveData.minSpawnScale, 1.0 - n * waveData.spawnFasterPerWave)
        currentWave = Wave((template.spawnMs * spawnScale).toLong(), ids)
        waveGeneration++
        waveHpScale = (1.0 + cycle * waveData.hpScalePerCycle + n * waveData.hpPerWave) * difficultyHp
        waveSpeedScale = (1.0 + cycle * waveData.speedPerCycle).toFloat()
        spawnQueue.clear()
        spawnQueue.addAll(ids)
        spawnTimerMs = -WAVE_BREAK_MS
        updateMusic()
        showWaveBanner(s.wave, bosses > 0)
    }

    /** "Wave 7" eases in over the sky, holds, and fades; boss waves add a warning line. */
    private fun showWaveBanner(wave: Int, boss: Boolean) {
        val banner = findViewById<View>(R.id.waveBanner)
        findViewById<TextView>(R.id.waveBannerTitle).text = getString(R.string.wave_number, wave)
        val sub = findViewById<TextView>(R.id.waveBannerSub)
        sub.visibility = if (boss) View.VISIBLE else View.GONE
        banner.animate().cancel()
        banner.visibility = View.VISIBLE
        banner.alpha = 0f
        banner.scaleX = 0.8f
        banner.scaleY = 0.8f
        banner.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(320)
            .setInterpolator(android.view.animation.OvershootInterpolator())
            .withEndAction {
                banner.animate().alpha(0f).setStartDelay(BANNER_MS).setDuration(400)
                    .setInterpolator(android.view.animation.AccelerateInterpolator())
                    .withEndAction { banner.visibility = View.INVISIBLE; banner.animate().setStartDelay(0) }
                    .start()
            }.start()
    }

    /** Boss waves get their own music. */
    private fun updateMusic() {
        val s = state ?: return
        music.play(if (s.wave % 10 == 0) R.raw.music_boss else R.raw.music_game)
    }

    private fun spawnEnemy(typeId: String) {
        val type = waveData.enemyTypes[typeId] ?: return
        val sizePx = (type.sizeDp * resources.displayMetrics.density).toInt()
        val view = ImageView(this)
        // Rotors spin / wings flap: rendered frames in an AnimationDrawable.
        Fx.loop(
            view, when (type.id) {
                "shrike" -> R.drawable.anim_shrike
                "bomber" -> R.drawable.anim_bomber
                "boss" -> R.drawable.anim_boss
                else -> R.drawable.anim_scout
            }
        )
        view.contentDescription = type.name
        view.layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
        val startX = Random.nextInt(0, (sky.width - sizePx).coerceAtLeast(1)).toFloat()
        // Aim somewhere over the bunker roof.
        val spread = bunker.width * 0.35f
        val targetX = sky.width / 2f - sizePx / 2f + Random.nextFloat() * 2 * spread - spread
        val hp = Math.max(1, Math.round(type.hp * waveHpScale).toInt())
        val enemy = Enemy(type, hp, -sizePx.toFloat(), startX, targetX, view, Random.nextFloat() * 6.28f,
            type.speedDp * waveSpeedScale)
        view.translationX = startX
        view.translationY = enemy.y
        view.setOnClickListener {
            guardsFire()
            sfx.play(Sfx.Sound.SHOT)
            hitEnemy(enemy, tapDamage())
            tutorial.complete(Tutorial.SHOOT)
        }
        sky.addView(view)
        view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.enemy_in))
        enemies.add(enemy)
        if (type.id == BOSS) updateBossBar()
    }

    /** Manual shot: 1 + Sharpened Rounds level + one per survivor on the roof. */
    private fun tapDamage(): Int {
        val s = state ?: return 1
        return 1 + s.level(UP_TAP_POWER) + survivors.count { it.job == Survivor.JOB_ROOF }
    }

    /** Damages [enemy]; returns true if this hit destroyed it. */
    private fun hitEnemy(enemy: Enemy, damage: Int): Boolean {
        val s = state ?: return false
        if (!enemies.contains(enemy)) return false
        enemy.hp -= damage
        if (enemy.type.id == BOSS) updateBossBar()
        enemy.view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.hit_flash))
        val cx = enemy.view.translationX + enemy.view.width / 2f
        val cy = enemy.view.translationY + enemy.view.height / 2f
        if (enemy.hp <= 0) {
            Fx.burst(fxLayer, R.drawable.fx_explode, cx, cy, enemy.view.width * 2)
            sfx.play(Sfx.Sound.EXPLODE)
            removeEnemy(enemy)
            s.scrap += enemy.type.reward
            refreshHud()
            refreshUpgrades()
            return true
        }
        Fx.burst(fxLayer, R.drawable.fx_sparks, cx, cy, enemy.view.width)
        sfx.play(Sfx.Sound.HIT)
        return false
    }

    private fun removeEnemy(enemy: Enemy) {
        enemy.view.clearAnimation()
        sky.removeView(enemy.view)
        enemies.remove(enemy)
        if (enemy.type.id == BOSS) updateBossBar()
    }

    /** Boss health bar: the bosses in the sky, added up. Hidden when none is left. */
    private fun updateBossBar() {
        val panel = findViewById<View>(R.id.bossPanel)
        val bosses = enemies.filter { it.type.id == BOSS }
        if (bosses.isEmpty()) {
            panel.visibility = View.GONE
            return
        }
        val hp = bosses.sumOf { Math.max(0, it.hp) }
        val max = bosses.sumOf { it.maxHp }
        val bar = findViewById<ProgressBar>(R.id.bossBar)
        bar.max = max
        bar.progress = hp
        findViewById<TextView>(R.id.bossName).text = getString(R.string.boss_hp, hp, max)
        panel.visibility = View.VISIBLE
    }

    private fun clearSky() {
        for (enemy in ArrayList(enemies)) removeEnemy(enemy)
        spawnQueue.clear()
    }

    private fun onWaveCleared() {
        val s = state ?: return
        val cleared = s.wave
        s.wave += 1
        if (cleared % 10 == 0) {
            Notifier.show(
                this, Notifier.ID_BOSS_CLEARED,
                getString(R.string.notif_boss_title), getString(R.string.notif_boss_text, cleared)
            )
        }
        sfx.play(Sfx.Sound.WAVE)
        startWave()        // the banner for the next wave replaces the old "cleared" toast
        saveNow()
        refreshHud()
    }

    // ---------------- hatch ----------------

    private fun maxHatchHp(): Int = BASE_HATCH_HP + HATCH_HP_PER_PLATING * (state?.level(UP_HATCH_PLATING) ?: 0)

    private fun damageHatch(amount: Int) {
        val s = state ?: return
        s.hatchHp = (s.hatchHp - amount).coerceAtLeast(0)
        bunker.startAnimation(AnimationUtils.loadAnimation(this, R.anim.hatch_shake))
        if (s.hatchHp == 0) onHatchBreached(s)
        refreshHud()
    }

    /**
     * Breach rule: lose 25% of scrap, fall back one wave, and the hatch is patched. Falling back
     * means a bunker that can't hold its wave stops advancing, instead of climbing for free.
     */
    private fun onHatchBreached(s: GameState) {
        sfx.play(Sfx.Sound.ALARM)
        val lost = s.scrap / 4
        val breachedWave = s.wave
        s.scrap -= lost
        s.wave = Math.max(1, s.wave - 1)
        s.hatchHp = maxHatchHp()
        clearSky()
        startWave()
        saveNow()
        Toast.makeText(this, getString(R.string.breach_toast, lost, s.wave), Toast.LENGTH_LONG).show()
        refreshHud()
        Notifier.show(
            this, Notifier.ID_HATCH_BREACHED,
            getString(R.string.notif_breach_title), getString(R.string.notif_breach_text, breachedWave, lost)
        )
        refreshUpgrades()
    }

    /** Manual repair: tap the hatch. Costs scrap; the Repair Arm will automate this later. */
    private fun repairHatch() {
        val s = state ?: return
        when {
            s.hatchHp >= maxHatchHp() -> Toast.makeText(this, R.string.hatch_full, Toast.LENGTH_SHORT).show()
            s.scrap < REPAIR_SCRAP_COST -> Toast.makeText(this, R.string.not_enough_scrap, Toast.LENGTH_SHORT).show()
            else -> {
                s.scrap -= REPAIR_SCRAP_COST
                s.hatchHp = (s.hatchHp + REPAIR_AMOUNT).coerceAtMost(maxHatchHp())
                sfx.play(Sfx.Sound.REPAIR)
                Fx.burst(fxLayer, R.drawable.fx_sparks, bunker.left + bunker.width / 2f,
                    bunker.top + bunker.height * HATCH_Y, (48 * resources.displayMetrics.density).toInt())
                refreshHud()
            }
        }
    }

    // ---------------- floors and upgrades ----------------

    /**
     * Tapping a room: the player lends a hand (1 + tools level of progress, into storage) and
     * collects whatever is in storage. Workers work on their own, so they no longer add to taps.
     */
    private fun onFloorTapped(floor: Floor) {
        val s = state ?: return
        val isDig = floor === digFloor
        val tools = s.level(if (isDig) UP_DIG_TOOLS else UP_FARM_TOOLS)
        sfx.play(if (isDig) Sfx.Sound.DIG else Sfx.Sound.HARVEST, 0.92f + Random.nextFloat() * 0.16f)
        // One tap can finish at most the current bar, however strong the tools are.
        floor.work(Math.min(1 + tools, floor.maxProgress - floor.progress))
        val collected = collectFrom(floor)
        if (collected == 0) floorAdapter.refresh(floorList)
        else if (isDig) tutorial.complete(Tutorial.DIG)
    }

    private fun buyUpgrade(upgrade: Upgrade) {
        val s = state ?: return
        val level = s.levelOf(upgrade)
        if (level >= upgrade.maxLevel) {
            Toast.makeText(this, R.string.already_maxed, Toast.LENGTH_SHORT).show()
            return
        }
        val cost = upgrade.costAt(level)
        val have = if (upgrade.paidInFood) s.food else s.scrap
        if (have < cost) {
            Toast.makeText(this, if (upgrade.paidInFood) R.string.not_enough_food else R.string.not_enough_scrap,
                Toast.LENGTH_SHORT).show()
            return
        }
        if (upgrade.paidInFood) s.food -= cost else s.scrap -= cost
        s.setLevel(upgrade, level + 1)
        sfx.play(Sfx.Sound.BUY)
        when (upgrade.id) {
            UP_HATCH_PLATING -> s.hatchHp += HATCH_HP_PER_PLATING
            DRILL_RIG -> digFloor.automated = true
            SPRINKLER -> farmFloor.automated = true
            AUTO_TURRET -> showRoofCrew()
            STORAGE -> updateCapacity(s)
        }
        saveNow()   // purchases are saved straight away
        Toast.makeText(this, getString(R.string.bought, upgrade.name, level + 1), Toast.LENGTH_SHORT).show()
        refreshAll()
    }

    /** Opens the upgrade sheet for a tab. Tapping the open tab again (or null) closes it. */
    private fun selectTab(type: String?) {
        if (::sfx.isInitialized && type != null) sfx.play(Sfx.Sound.TAP)
        currentTab = if (type == currentTab) null else type
        val types = listOf(GameState.TYPE_DEFENSE, GameState.TYPE_SUPPLIES, GameState.TYPE_AUTOMATION)
        tabButtons.forEachIndexed { i, button -> button.isSelected = types[i] == currentTab }
        val open = currentTab
        if (open == null) {
            if (upgradeSheet.visibility == View.VISIBLE) {
                // Ease out of view, then hide.
                val out = AnimationUtils.loadAnimation(this, R.anim.sheet_out)
                upgradeSheet.startAnimation(out)
            }
            upgradeSheet.visibility = View.GONE
            return
        }
        val wasOpen = upgradeSheet.visibility == View.VISIBLE
        undergroundToggle.expand()
        val titles = mapOf(
            GameState.TYPE_DEFENSE to R.string.tab_defense,
            GameState.TYPE_SUPPLIES to R.string.tab_supplies,
            GameState.TYPE_AUTOMATION to R.string.tab_automation
        )
        upgradeTitle.setText(titles[open] ?: R.string.tab_defense)
        upgradeAdapter.show(upgrades.filter { it.type == open })
        upgradeSheet.visibility = View.VISIBLE
        if (!wasOpen) upgradeSheet.startAnimation(AnimationUtils.loadAnimation(this, R.anim.sheet_in))
    }

    /** Survivors working in a room. They are drawn in the room and fill it by themselves. */
    private fun workersIn(floor: Floor): Int {
        val job = if (floor === digFloor) Survivor.JOB_DIG else Survivor.JOB_FARM
        return survivors.count { it.job == job }
    }

    /**
     * One guard on the bunker roof per survivor with the Roof job, up to 4. They stand on the
     * deck either side of the hatch, feet on the deck, aiming outwards (the frames aim right,
     * so guards left of the hatch are mirrored).
     */
    private fun showRoofCrew() {
        roofCrew.removeAllViews()
        guardViews.clear()
        val size = (GUARD_DP * resources.displayMetrics.density).toInt()
        val bunkerW = resources.getDimensionPixelSize(R.dimen.bunker_width)
        val bunkerH = resources.getDimensionPixelSize(R.dimen.bunker_height)
        val roof = survivors.count { it.job == Survivor.JOB_ROOF }
        val guards = Math.min(roof, GUARD_SLOTS.size)
        repeat(guards) {
            val slot = GUARD_SLOTS[it]
            val figure = ImageView(this)
            figure.layoutParams = FrameLayout.LayoutParams(size, size)
            figure.translationX = bunkerW * slot - size / 2f
            figure.translationY = bunkerH * DECK_Y - size * FEET_Y
            if (slot < 0.5f) figure.scaleX = -1f
            figure.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            roofCrew.addView(figure)
            guardViews.add(figure)
            Fx.loop(figure, R.drawable.anim_guard_idle, it * 300L)
        }
        // More guards than spots on the deck: they all shoot, and a "+2" says how many aren't drawn.
        if (roof > guards) {
            val more = TextView(this)
            more.text = getString(R.string.more_guards, roof - guards)
            more.setTextAppearance(R.style.SceneLabel)
            more.setBackgroundResource(R.drawable.np_label)
            more.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            more.translationX = bunkerW * 0.14f
            more.translationY = bunkerH * DECK_Y - size * 1.1f
            roofCrew.addView(more)
        }
        // The Auto-Turret sits on the hatch dome once bought: a fixed base, and a gun
        // that turns towards each target (both frames share one centre, the gun's axis).
        turretGun = null
        if ((state?.level(AUTO_TURRET) ?: 0) > 0) {
            val tSize = (TURRET_DP * resources.displayMetrics.density).toInt()
            for (res in intArrayOf(R.drawable.turret_base, R.drawable.turret_gun)) {
                val part = ImageView(this)
                part.layoutParams = FrameLayout.LayoutParams(tSize, tSize)
                part.translationX = bunkerW * TURRET_X - tSize / 2f
                part.translationY = bunkerH * DOME_Y - tSize * TURRET_FOOT_Y
                part.setImageResource(res)
                part.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                roofCrew.addView(part)
                if (res == R.drawable.turret_gun) turretGun = part
            }
        }
    }

    /**
     * Guards on the roof shoot by themselves, taking turns: one shot every GUARD_AUTO_MS divided
     * by the number of guards, at the enemy nearest the hatch once it is within GUARD_RANGE.
     */
    private fun guardsAutoFire() {
        if (guardViews.isEmpty() || hungry) return
        guardTimerMs += frameMs
        val interval = GUARD_AUTO_MS * Math.pow(GUARD_TRAINING_FACTOR, (state?.level(GUARD_TRAINING) ?: 0).toDouble())
        if (guardTimerMs < interval / guardViews.size) return
        val target = enemies.filter { it.y > sky.height * GUARD_RANGE }.maxByOrNull { it.y } ?: return
        guardTimerMs = 0
        nextGuard = (nextGuard + 1) % guardViews.size
        guardFire(guardViews[nextGuard])
        sfx.play(Sfx.Sound.SHOT, 1.15f)
        hitEnemy(target, GUARD_DAMAGE)
    }

    /**
     * The turret turns to face [enemy] (eased), fires (flash and recoil frames) and draws a
     * short tracer from the muzzle to the target.
     */
    private fun turretShot(enemy: Enemy) {
        val gun = turretGun ?: return
        val layer = IntArray(2).also { fxLayer.getLocationInWindow(it) }
        val g = IntArray(2).also { gun.getLocationInWindow(it) }
        val e = IntArray(2).also { enemy.view.getLocationInWindow(it) }
        val gx = g[0] - layer[0] + gun.width / 2f
        val gy = g[1] - layer[1] + gun.height / 2f
        val ex = e[0] - layer[0] + enemy.view.width / 2f
        val ey = e[1] - layer[1] + enemy.view.height / 2f
        // 0 degrees = barrels straight up; clockwise is positive, like View.rotation.
        val angle = Math.toDegrees(Math.atan2((ex - gx).toDouble(), (gy - ey).toDouble())).toFloat()
        gun.animate().rotation(angle).setDuration(90).start()
        gun.setImageResource(R.drawable.anim_turret_fire)
        (gun.drawable as? AnimationDrawable)?.start()
        if (lowDetail) return
        // Tracer: a thin bright line from the muzzle to the target, gone in a blink.
        val muzzle = gun.height * 0.42f
        val sx = gx + muzzle * Math.sin(Math.toRadians(angle.toDouble())).toFloat()
        val sy = gy - muzzle * Math.cos(Math.toRadians(angle.toDouble())).toFloat()
        val length = Math.hypot((ex - sx).toDouble(), (ey - sy).toDouble()).toFloat()
        val line = tracer ?: View(this).also {
            it.setBackgroundColor(0xFFFFD98A.toInt())
            it.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            fxLayer.addView(it, FrameLayout.LayoutParams((3f * resources.displayMetrics.density).toInt(), 1))
            tracer = it
        }
        line.layoutParams.height = Math.max(1, length.toInt())
        line.requestLayout()
        line.pivotX = line.layoutParams.width / 2f
        line.pivotY = length
        line.translationX = sx - line.layoutParams.width / 2f
        line.translationY = sy - length
        line.rotation = angle
        line.animate().cancel()
        line.alpha = 1f
        line.visibility = View.VISIBLE
        line.animate().alpha(0f).setDuration(TRACER_MS).start()
    }

    /** Every roof guard fires with the player's tap. */
    private fun guardsFire() {
        for (guard in guardViews) guardFire(guard)
    }

    /** One guard plays one shot (muzzle flash in the frames), then goes back to idle. */
    private fun guardFire(guard: ImageView) {
        (guard.tag as? Runnable)?.let { guard.removeCallbacks(it) }
        guard.setImageResource(R.drawable.anim_guard_fire)
        (guard.drawable as? AnimationDrawable)?.start()
        val back = Runnable { Fx.loop(guard, R.drawable.anim_guard_idle) }
        guard.tag = back
        guard.postDelayed(back, GUARD_FIRE_MS)
    }

    /** Prices in the upgrade sheet follow the scrap count; only redraw them while it is open. */
    private fun refreshUpgrades() {
        if (currentTab != null) upgradeAdapter.notifyDataSetChanged()
    }

    // ---------------- HUD ----------------

    private fun refreshAll() {
        refreshHud()
        floorAdapter.notifyDataSetChanged()
        upgradeAdapter.notifyDataSetChanged()
    }

    /** Pressed buttons squash and spring back. */
    private fun pop(view: View) {
        view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.press_pop))
    }

    /** Sets a HUD number; when it goes up, it pops so the gain is noticed. */
    private fun setCount(view: TextView, value: Int) {
        val old = view.text.toString().toIntOrNull()
        if (old == value) return                     // nothing changed: no relayout, no redraw
        view.text = value.toString()
        if (old != null && value > old && !lowDetail) view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.hud_pop))
    }

    private fun refreshHud() {
        val s = state ?: return
        setCount(hudScrap, s.scrap)
        setCount(hudFood, s.food)
        hudFood.setTextColor(getColor(if (hungry) R.color.alarm_red else R.color.text_main))
        hudWave.text = getString(R.string.wave_number, s.wave)
        hudBossIcon.visibility = if (s.wave % 10 == 0) View.VISIBLE else View.GONE
        val max = maxHatchHp()
        hatchBar.max = max
        hatchBar.progress = s.hatchHp
        hatchText.text = getString(R.string.hatch_hp, s.bunkerName, s.hatchHp, max)
    }
}
