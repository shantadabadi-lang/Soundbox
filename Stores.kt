package np.mero.soundbox

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Duplicate prevention. Transaction IDs (if configured) are remembered; otherwise a short time window. */
object Dedup {
    private const val WINDOW_MS = 60_000L
    private val recent = HashMap<String, Long>()

    @Synchronized
    fun isDuplicate(c: Context, txn: String?, amount: String, text: String, now: Long): Boolean {
        if (txn != null) {
            val sp = c.getSharedPreferences("txn", 0)
            val arr = JSONArray(sp.getString("ids", "[]"))
            for (i in 0 until arr.length()) if (arr.getString(i) == txn) return true
            val n = JSONArray().put(txn)
            for (i in 0 until minOf(arr.length(), 199)) n.put(arr.get(i))
            sp.edit().putString("ids", n.toString()).apply()
            return false
        }
        recent.entries.removeAll { now - it.value > WINDOW_MS }
        val key = amount + "|" + text.hashCode()
        if (recent.containsKey(key)) return true
        recent[key] = now
        return false
    }
}

/** Local history: amount + time only (no names, no text). */
object History {
    fun add(c: Context, amount: String, time: Long) {
        val sp = c.getSharedPreferences("history", 0)
        val old = JSONArray(sp.getString("items", "[]"))
        val n = JSONArray().put(JSONObject().put("a", amount).put("t", time))
        for (i in 0 until minOf(old.length(), 49)) n.put(old.get(i))
        sp.edit().putString("items", n.toString()).apply()
    }
    fun all(c: Context): List<Pair<String, Long>> {
        val a = JSONArray(c.getSharedPreferences("history", 0).getString("items", "[]"))
        return (0 until a.length()).map { a.getJSONObject(it).let { o -> o.getString("a") to o.getLong("t") } }
    }
    fun clear(c: Context) = c.getSharedPreferences("history", 0).edit().clear().apply()
    fun setLast(c: Context, amount: String, time: Long) =
        c.getSharedPreferences("history", 0).edit().putString("lastA", amount).putLong("lastT", time).apply()
    fun lastAmount(c: Context) = c.getSharedPreferences("history", 0).getString("lastA", null)
    fun lastTime(c: Context) = c.getSharedPreferences("history", 0).getLong("lastT", 0L)
}
