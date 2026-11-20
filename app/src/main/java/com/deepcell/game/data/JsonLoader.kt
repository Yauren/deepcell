package com.deepcell.game.data

import android.content.Context
import com.deepcell.game.model.Floor
import com.deepcell.game.model.EnemyType
import com.deepcell.game.model.Upgrade
import com.deepcell.game.model.Wave
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reads the balance files in assets/ with JSONObject and JSONArray (Module 10).
 * To rebalance the game, change the JSON files, not this code.
 */
object JsonLoader {

    private fun readAsset(context: Context, fileName: String): String =
        context.assets.open(fileName).bufferedReader().use { it.readText() }

    /** Room payouts and bar lengths live in JSON, so changing the economy needs no code edit. */
    fun loadFloor(context: Context, id: String, name: String, resource: String): Floor {
        val balance = JSONObject(readAsset(context, "floors.json")).getJSONObject(id)
        return Floor(id, name, resource, balance.getInt("maxProgress"), balance.getInt("yieldPerFill"))
    }

    fun loadUpgrades(context: Context): List<Upgrade> {
        val root = JSONObject(readAsset(context, "upgrades.json"))
        val array: JSONArray = root.getJSONArray("upgrades")
        val result = ArrayList<Upgrade>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            result.add(
                Upgrade(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    type = o.getString("type"),
                    description = o.getString("description"),
                    baseCost = o.getInt("baseCost"),
                    costGrowth = o.getDouble("costGrowth"),
                    maxLevel = o.getInt("maxLevel"),
                    currency = o.optString("currency", Upgrade.CURRENCY_SCRAP)
                )
            )
        }
        return result
    }

    /**
     * Parsed waves.json: enemy kinds, the 10-wave cycle, and how waves grow: more enemies per
     * wave, shorter gaps between spawns (down to minSpawnScale), more HP every wave and more HP
     * and speed every cycle.
     */
    class WaveData(
        val enemyTypes: Map<String, EnemyType>,
        val waves: List<Wave>,
        val hpScalePerCycle: Double,
        val hpPerWave: Double,
        val countPerWave: Double,
        val spawnFasterPerWave: Double,
        val minSpawnScale: Double,
        val speedPerCycle: Double
    )

    fun loadWaves(context: Context): WaveData {
        val root = JSONObject(readAsset(context, "waves.json"))

        val types = HashMap<String, EnemyType>()
        val typeArray = root.getJSONArray("enemyTypes")
        for (i in 0 until typeArray.length()) {
            val o = typeArray.getJSONObject(i)
            val type = EnemyType(
                id = o.getString("id"),
                name = o.getString("name"),
                hp = o.getInt("hp"),
                speedDp = o.getDouble("speedDp").toFloat(),
                damage = o.getInt("damage"),
                reward = o.getInt("reward"),
                sizeDp = o.getInt("sizeDp")
            )
            types[type.id] = type
        }

        val waves = ArrayList<Wave>()
        val waveArray = root.getJSONArray("waves")
        for (i in 0 until waveArray.length()) {
            val o = waveArray.getJSONObject(i)
            val ids = ArrayList<String>()
            val groups = o.getJSONArray("enemies")
            for (g in 0 until groups.length()) {
                val group = groups.getJSONObject(g)
                repeat(group.getInt("count")) { ids.add(group.getString("type")) }
            }
            waves.add(Wave(o.getLong("spawnMs"), ids))
        }
        return WaveData(
            types, waves, root.getDouble("hpScalePerCycle"), root.optDouble("hpPerWave", 0.0),
            root.optDouble("countPerWave", 0.0), root.optDouble("spawnFasterPerWave", 0.0),
            root.optDouble("minSpawnScale", 1.0), root.optDouble("speedPerCycle", 0.0)
        )
    }
}
