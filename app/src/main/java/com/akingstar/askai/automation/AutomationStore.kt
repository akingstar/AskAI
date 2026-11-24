package com.akingstar.askai.automation

import android.content.Context
import com.akingstar.askai.PlanValidator
import org.json.JSONArray
import org.json.JSONObject

data class SavedRoutine(
    val id: String,
    val name: String,
    val planJson: String
)

class AutomationStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("askai_automation", Context.MODE_PRIVATE)

    @Synchronized
    fun save(name: String, planJson: String): Boolean {
        val cleanName = name.trim()
        if (cleanName.isBlank() || cleanName.length > 40) return false
        if (planJson.length > 8192 || PlanValidator.parse(planJson) == null) return false

        return runCatching {
            val routines = readAll().toMutableList()
            val existing = routines.indexOfFirst { it.name.equals(cleanName, true) }
            val routine = SavedRoutine(
                id = if (existing >= 0) routines[existing].id
                     else java.util.UUID.randomUUID().toString(),
                name = cleanName,
                planJson = planJson
            )

            if (existing >= 0) routines[existing] = routine
            else {
                if (routines.size >= 50) return false
                routines.add(routine)
            }

            val array = JSONArray()
            routines.forEach {
                array.put(JSONObject().put("id", it.id)
                    .put("name", it.name)
                    .put("plan", it.planJson))
            }
            prefs.edit().putString("routines", array.toString()).commit()
        }.getOrDefault(false)
    }

    @Synchronized
    fun list(): List<SavedRoutine> = readAll()

    @Synchronized
    fun get(id: String): SavedRoutine? =
        readAll().firstOrNull { it.id == id }

    @Synchronized
    fun rename(oldName: String, newName: String): Boolean {
        val oldClean = oldName.trim()
        val newClean = newName.trim()
        if (oldClean.isBlank() || newClean.isBlank() ||
            newClean.length > 40 || oldClean.equals(newClean, true)) return false

        return runCatching {
            val routines = readAll().toMutableList()
            val index = routines.indexOfFirst { it.name.equals(oldClean, true) }
            if (index < 0 || routines.any {
                    it.name.equals(newClean, true) && it.id != routines[index].id
                }) {
                false
            } else {
                routines[index] = routines[index].copy(name = newClean)
                val array = JSONArray()
                routines.forEach {
                    array.put(JSONObject().put("id", it.id)
                        .put("name", it.name)
                        .put("plan", it.planJson))
                }
                prefs.edit().putString("routines", array.toString()).commit()
            }
        }.getOrDefault(false)
    }

    @Synchronized
    fun delete(id: String): Boolean = runCatching {
        val remaining = readAll().filterNot { it.id == id }
        val array = JSONArray()
        remaining.forEach {
            array.put(JSONObject().put("id", it.id)
                .put("name", it.name)
                .put("plan", it.planJson))
        }
        prefs.edit().putString("routines", array.toString()).commit()
    }.getOrDefault(false)

    private fun readAll(): List<SavedRoutine> = runCatching {
        val array = JSONArray(prefs.getString("routines", "[]") ?: "[]")
        buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val id = item.optString("id")
                val name = item.optString("name")
                val plan = item.optString("plan")
                if (id.isNotBlank() && name.isNotBlank() &&
                    PlanValidator.parse(plan) != null) {
                    add(SavedRoutine(id, name, plan))
                }
            }
        }
    }.getOrDefault(emptyList())
}
