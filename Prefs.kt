package np.mero.soundbox

import android.content.Context

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("settings", 0)
    fun s(c: Context, k: String, d: String) = sp(c).getString(k, d) ?: d
    fun setS(c: Context, k: String, v: String) = sp(c).edit().putString(k, v).apply()
    fun b(c: Context, k: String, d: Boolean) = sp(c).getBoolean(k, d)
    fun setB(c: Context, k: String, v: Boolean) = sp(c).edit().putBoolean(k, v).apply()
    fun i(c: Context, k: String, d: Int) = sp(c).getInt(k, d)
    fun setI(c: Context, k: String, v: Int) = sp(c).edit().putInt(k, v).apply()

    fun enabled(c: Context) = b(c, "enabled", true)
    fun saving(c: Context) = b(c, "saving", false)
    fun debug(c: Context) = b(c, "debug", true) && !saving(c)
    fun historyOn(c: Context) = b(c, "history", true) && !saving(c)
    fun volume(c: Context) = i(c, "volume", 100)
    fun speed(c: Context) = i(c, "speed", 85)

    /** Exact package name of the eSewa app. Empty = nothing is ever announced. */
    fun pkg(c: Context) = s(c, "pkg", "").trim()
    /** Phrases (comma/newline separated) that mark an INCOMING payment. Empty = nothing announced. */
    fun incomingRaw(c: Context) = s(c, "incoming", "")
    fun rejectRaw(c: Context) = s(c, "reject", DEFAULT_REJECT)
    fun amountRegex(c: Context) = s(c, "amountRegex", DEFAULT_AMOUNT)
    fun txnRegex(c: Context) = s(c, "txnRegex", "")

    fun list(raw: String): List<String> =
        raw.split(',', '\n').map { it.trim().lowercase() }.filter { it.isNotEmpty() }

    const val DEFAULT_REJECT =
        "otp, verification code, failed, unsuccessful, pending, declined, cancelled, canceled, login, logged in, offer, discount, cashback, promo, sent, debited, withdraw, असफल, रद्द"
    // currency marker (NPR / NRP / Rs / रु) followed by an amount; capture group 1 = amount
    const val DEFAULT_AMOUNT = "(?:npr|nrp|rs|रु)\\.?\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)"
}
