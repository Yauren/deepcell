package com.deepcell.game.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.deepcell.game.model.GameState
import com.deepcell.game.model.Survivor

/** Contract class: every table and column name in one place (Module 9). */
object DbContract {
    const val DB_NAME = "deepcell.db"
    const val DB_VERSION = 3   // 2: room storage (dig_stock, farm_stock); 3: Collector is a survivor job

    object State {               // always exactly one row, id = 1
        const val TABLE = "game_state"
        const val ID = "id"
        const val BUNKER_NAME = "bunker_name"
        const val SCRAP = "scrap"
        const val FOOD = "food"
        const val WAVE = "wave"
        const val HATCH_HP = "hatch_hp"
        const val DIG_PROGRESS = "dig_progress"
        const val FARM_PROGRESS = "farm_progress"
        const val DIG_STOCK = "dig_stock"        // finished scrap waiting in Scrap Dig (v2)
        const val FARM_STOCK = "farm_stock"      // finished food waiting in Mushroom Farm (v2)
        const val LAST_PLAYED = "last_played"
    }

    object Upgrades {            // defense and supplies upgrades
        const val TABLE = "upgrades"
        const val ID = "id"
        const val LEVEL = "level"
    }

    object Automation {          // machines: Auto-Turret, Drill Rig, ...
        const val TABLE = "automation"
        const val ID = "id"
        const val OWNED = "owned"
        const val LEVEL = "level"
    }

    object Survivors {
        const val TABLE = "survivors"
        const val ID = "_id"
        const val NAME = "name"
        const val JOB = "job"
    }

    object Settings {            // kept when the player resets the save
        const val TABLE = "settings"
        const val KEY = "key"
        const val VALUE = "value"
    }
}

/**
 * The player's save file. Each Activity opens one and closes it in onDestroy().
 *
 * RULE: never drop tables in onUpgrade. Player saves must survive app updates.
 * When you add a column, bump DB_VERSION and add an ALTER TABLE step in onUpgrade.
 */
class GameDatabase(context: Context) :
    SQLiteOpenHelper(context, DbContract.DB_NAME, null, DbContract.DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val s = DbContract.State
        db.execSQL(
            "CREATE TABLE ${s.TABLE} (${s.ID} INTEGER PRIMARY KEY, ${s.BUNKER_NAME} TEXT NOT NULL, " +
                "${s.SCRAP} INTEGER NOT NULL, ${s.FOOD} INTEGER NOT NULL, ${s.WAVE} INTEGER NOT NULL, " +
                "${s.HATCH_HP} INTEGER NOT NULL, ${s.DIG_PROGRESS} INTEGER NOT NULL, " +
                "${s.FARM_PROGRESS} INTEGER NOT NULL, ${s.LAST_PLAYED} INTEGER NOT NULL, " +
                "${s.DIG_STOCK} INTEGER NOT NULL DEFAULT 0, ${s.FARM_STOCK} INTEGER NOT NULL DEFAULT 0)"
        )
        val u = DbContract.Upgrades
        db.execSQL("CREATE TABLE ${u.TABLE} (${u.ID} TEXT PRIMARY KEY, ${u.LEVEL} INTEGER NOT NULL)")
        val a = DbContract.Automation
        db.execSQL(
            "CREATE TABLE ${a.TABLE} (${a.ID} TEXT PRIMARY KEY, ${a.OWNED} INTEGER NOT NULL, " +
                "${a.LEVEL} INTEGER NOT NULL)"
        )
        val sv = DbContract.Survivors
        db.execSQL(
            "CREATE TABLE ${sv.TABLE} (${sv.ID} INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "${sv.NAME} TEXT NOT NULL, ${sv.JOB} TEXT NOT NULL)"
        )
        val st = DbContract.Settings
        db.execSQL("CREATE TABLE ${st.TABLE} (${st.KEY} TEXT PRIMARY KEY, ${st.VALUE} TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // One non-destructive step per version: saves from older versions keep everything.
        val s = DbContract.State
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE ${s.TABLE} ADD COLUMN ${s.DIG_STOCK} INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE ${s.TABLE} ADD COLUMN ${s.FARM_STOCK} INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 3) {
            // The Collector used to be a machine bought in the Automation tab; now it is a
            // survivor job. A save that owned the machine gets a survivor on that job instead.
            val a = DbContract.Automation
            val c = db.query(a.TABLE, arrayOf(a.LEVEL), "${a.ID} = ?", arrayOf("collector"), null, null, null)
            val owned = c.moveToFirst() && c.getInt(0) > 0
            c.close()
            if (owned) {
                val names = ArrayList<String>()
                val n = db.query(DbContract.Survivors.TABLE, arrayOf(DbContract.Survivors.NAME), null, null, null, null, null)
                while (n.moveToNext()) names.add(n.getString(0))
                n.close()
                insertSurvivor(db, Survivor.randomName(names), Survivor.JOB_COLLECTOR)
            }
            db.delete(a.TABLE, "${a.ID} = ?", arrayOf("collector"))
        }
    }

    // ---------- game state ----------

    fun hasSave(): Boolean {
        val c = readableDatabase.query(DbContract.State.TABLE, null, null, null, null, null, null)
        val exists = c.count > 0
        c.close()
        return exists
    }

    /** Replaces the save (settings are kept) with a fresh bunker and one survivor on the roof. */
    fun newGame(bunkerName: String, startHatchHp: Int) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            clearSave(db)
            val v = ContentValues().apply {
                put(DbContract.State.ID, 1)
                put(DbContract.State.BUNKER_NAME, bunkerName)
                put(DbContract.State.SCRAP, 0)
                put(DbContract.State.FOOD, START_FOOD)
                put(DbContract.State.WAVE, 1)
                put(DbContract.State.HATCH_HP, startHatchHp)
                put(DbContract.State.DIG_PROGRESS, 0)
                put(DbContract.State.FARM_PROGRESS, 0)
                put(DbContract.State.DIG_STOCK, 0)
                put(DbContract.State.FARM_STOCK, 0)
                put(DbContract.State.LAST_PLAYED, System.currentTimeMillis())
            }
            db.insert(DbContract.State.TABLE, null, v)
            insertSurvivor(db, Survivor.randomName(emptyList()), Survivor.JOB_ROOF)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** Deletes the player's save. Only called when the player confirms Reset save. */
    fun resetSave() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            clearSave(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun clearSave(db: SQLiteDatabase) {
        db.delete(DbContract.State.TABLE, null, null)
        db.delete(DbContract.Upgrades.TABLE, null, null)
        db.delete(DbContract.Automation.TABLE, null, null)
        db.delete(DbContract.Survivors.TABLE, null, null)
    }

    /** Returns null when there is no save yet. */
    fun loadState(): GameState? {
        val db = readableDatabase
        val c = db.query(DbContract.State.TABLE, null, "${DbContract.State.ID} = 1", null, null, null, null)
        if (!c.moveToFirst()) {
            c.close()
            return null
        }
        val state = GameState(
            bunkerName = c.getString(c.getColumnIndexOrThrow(DbContract.State.BUNKER_NAME)),
            scrap = c.getInt(c.getColumnIndexOrThrow(DbContract.State.SCRAP)),
            food = c.getInt(c.getColumnIndexOrThrow(DbContract.State.FOOD)),
            wave = c.getInt(c.getColumnIndexOrThrow(DbContract.State.WAVE)),
            hatchHp = c.getInt(c.getColumnIndexOrThrow(DbContract.State.HATCH_HP)),
            digProgress = c.getInt(c.getColumnIndexOrThrow(DbContract.State.DIG_PROGRESS)),
            farmProgress = c.getInt(c.getColumnIndexOrThrow(DbContract.State.FARM_PROGRESS)),
            digStock = c.getInt(c.getColumnIndexOrThrow(DbContract.State.DIG_STOCK)),
            farmStock = c.getInt(c.getColumnIndexOrThrow(DbContract.State.FARM_STOCK)),
            lastPlayed = c.getLong(c.getColumnIndexOrThrow(DbContract.State.LAST_PLAYED))
        )
        c.close()

        val u = db.query(DbContract.Upgrades.TABLE, null, null, null, null, null, null)
        while (u.moveToNext()) {
            state.upgradeLevels[u.getString(u.getColumnIndexOrThrow(DbContract.Upgrades.ID))] =
                u.getInt(u.getColumnIndexOrThrow(DbContract.Upgrades.LEVEL))
        }
        u.close()

        val a = db.query(DbContract.Automation.TABLE, null, null, null, null, null, null)
        while (a.moveToNext()) {
            state.automationLevels[a.getString(a.getColumnIndexOrThrow(DbContract.Automation.ID))] =
                a.getInt(a.getColumnIndexOrThrow(DbContract.Automation.LEVEL))
        }
        a.close()
        return state
    }

    /** Saves everything in one transaction so a crash cannot leave half a save. */
    fun saveState(state: GameState) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val v = ContentValues().apply {
                put(DbContract.State.BUNKER_NAME, state.bunkerName)
                put(DbContract.State.SCRAP, state.scrap)
                put(DbContract.State.FOOD, state.food)
                put(DbContract.State.WAVE, state.wave)
                put(DbContract.State.HATCH_HP, state.hatchHp)
                put(DbContract.State.DIG_PROGRESS, state.digProgress)
                put(DbContract.State.FARM_PROGRESS, state.farmProgress)
                put(DbContract.State.DIG_STOCK, state.digStock)
                put(DbContract.State.FARM_STOCK, state.farmStock)
                put(DbContract.State.LAST_PLAYED, state.lastPlayed)
            }
            db.update(DbContract.State.TABLE, v, "${DbContract.State.ID} = 1", null)

            for ((id, level) in state.upgradeLevels) {
                val row = ContentValues().apply {
                    put(DbContract.Upgrades.ID, id)
                    put(DbContract.Upgrades.LEVEL, level)
                }
                db.insertWithOnConflict(DbContract.Upgrades.TABLE, null, row, SQLiteDatabase.CONFLICT_REPLACE)
            }
            for ((id, level) in state.automationLevels) {
                val row = ContentValues().apply {
                    put(DbContract.Automation.ID, id)
                    put(DbContract.Automation.OWNED, if (level > 0) 1 else 0)
                    put(DbContract.Automation.LEVEL, level)
                }
                db.insertWithOnConflict(DbContract.Automation.TABLE, null, row, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** Survivor count, for the title screen's save summary. */
    fun survivorCount(): Int {
        val c = readableDatabase.rawQuery("SELECT COUNT(*) FROM ${DbContract.Survivors.TABLE}", null)
        val n = if (c.moveToFirst()) c.getInt(0) else 0
        c.close()
        return n
    }

    fun getFood(): Int {
        val c = readableDatabase.query(
            DbContract.State.TABLE, arrayOf(DbContract.State.FOOD),
            "${DbContract.State.ID} = 1", null, null, null, null
        )
        val food = if (c.moveToFirst()) c.getInt(0) else 0
        c.close()
        return food
    }

    // ---------- survivors ----------

    fun getSurvivors(): List<Survivor> {
        val list = ArrayList<Survivor>()
        val c = readableDatabase.query(
            DbContract.Survivors.TABLE, null, null, null, null, null, DbContract.Survivors.ID
        )
        while (c.moveToNext()) {
            list.add(
                Survivor(
                    id = c.getLong(c.getColumnIndexOrThrow(DbContract.Survivors.ID)),
                    name = c.getString(c.getColumnIndexOrThrow(DbContract.Survivors.NAME)),
                    job = c.getString(c.getColumnIndexOrThrow(DbContract.Survivors.JOB))
                )
            )
        }
        c.close()
        return list
    }

    /** Recruiting costs food, so the new survivor and the food change are saved together. */
    fun recruitSurvivor(name: String, job: String, newFood: Int) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            insertSurvivor(db, name, job)
            val v = ContentValues().apply { put(DbContract.State.FOOD, newFood) }
            db.update(DbContract.State.TABLE, v, "${DbContract.State.ID} = 1", null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun insertSurvivor(db: SQLiteDatabase, name: String, job: String) {
        val v = ContentValues().apply {
            put(DbContract.Survivors.NAME, name)
            put(DbContract.Survivors.JOB, job)
        }
        db.insert(DbContract.Survivors.TABLE, null, v)
    }

    fun updateSurvivorJob(id: Long, job: String) {
        val v = ContentValues().apply { put(DbContract.Survivors.JOB, job) }
        writableDatabase.update(
            DbContract.Survivors.TABLE, v, "${DbContract.Survivors.ID} = ?", arrayOf(id.toString())
        )
    }

    // ---------- settings ----------

    fun getSetting(key: String, default: String): String {
        val c = readableDatabase.query(
            DbContract.Settings.TABLE, arrayOf(DbContract.Settings.VALUE),
            "${DbContract.Settings.KEY} = ?", arrayOf(key), null, null, null
        )
        val value = if (c.moveToFirst()) c.getString(0) else default
        c.close()
        return value
    }

    fun setSetting(key: String, value: String) {
        val v = ContentValues().apply {
            put(DbContract.Settings.KEY, key)
            put(DbContract.Settings.VALUE, value)
        }
        writableDatabase.insertWithOnConflict(DbContract.Settings.TABLE, null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    companion object {
        const val SETTING_SOUND = "sound_on"          // "true" / "false"
        const val SETTING_DIFFICULTY = "difficulty"   // index into R.array.difficulty_names
        const val SETTING_DETAIL = "detail"           // ui.Quality.AUTO / HIGH / LOW
        const val SETTING_MUSIC = "music_on"          // "true" / "false"
        const val SETTING_TUTORIAL = "tutorial_step"  // ui.Tutorial step number, 0 = off or finished
        const val SETTING_TUTORIAL_PAUSED = "tutorial_paused" // skip elapsed tutorial time on resume
        /** The first recruit (SurvivorsActivity.RECRUIT_BASE, 25) plus a few minutes of meals. */
        const val START_FOOD = 40
    }
}
