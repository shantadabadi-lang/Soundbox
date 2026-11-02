package np.mero.soundbox

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Stage 2 debug store: last 20 captured notifications, local only. */
object CaptureStore {
    private const val PREFS = "capture"
    private const val MAX = 20

    fun captureAll(c: Context) = c.getSharedPreferences(PREFS, 0).getBoolean("all", false)
    fun setCaptureAll(c: Context, v: Boolean) =
        c.getSharedPreferences(PREFS, 0).edit().putBoolean("all", v).apply()

    @Synchronized
    fun add(c: Context, o: JSONObject) {
        val sp = c.getSharedPreferences(PREFS, 0)
        val old = JSONArray(sp.getString("items", "[]"))
        val n = JSONArray().put(o)
        for (i in 0 until minOf(old.length(), MAX - 1)) n.put(old.get(i))
        sp.edit().putString("items", n.toString()).apply()
    }

    fun all(c: Context): List<JSONObject> {
        val a = JSONArray(c.getSharedPreferences(PREFS, 0).getString("items", "[]"))
        return (0 until a.length()).map { a.getJSONObject(it) }
    }

    fun clear(c: Context) = c.getSharedPreferences(PREFS, 0).edit().remove("items").apply()
}
