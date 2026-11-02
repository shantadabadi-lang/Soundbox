package np.mero.soundbox

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.text.format.DateFormat
import android.widget.*
import org.json.JSONObject
import java.math.BigDecimal

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private lateinit var last: TextView
    private lateinit var result: TextView
    private lateinit var hist: LinearLayout
    private lateinit var caps: LinearLayout
    private lateinit var ePkg: EditText
    private lateinit var eIn: EditText
    private lateinit var eRej: EditText
    private lateinit var eAmt: EditText
    private lateinit var eTxn: EditText
    private lateinit var eTest: EditText
    private var testSpeech: String? = null

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun tv(t: String, size: Float = 14f) = TextView(this).apply { text = t; textSize = size; setPadding(0, dp(8), 0, dp(2)) }
    private fun btn(t: String, f: () -> Unit) = Button(this).apply { text = t; setOnClickListener { f() } }
    private fun edit(v: String, multi: Boolean = false) = EditText(this).apply {
        setText(v); textSize = 13f
        if (multi) { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE; minLines = 2 }
    }
    private fun toast(m: String) = runOnUiThread { Toast.makeText(this, m, Toast.LENGTH_LONG).show() }

    private fun seek(title: String, init: Int, min: Int, max: Int, save: (Int) -> Unit) {
        val label = tv("$title: $init%")
        root.addView(label)
        root.addView(SeekBar(this).apply {
            this.max = max - min; progress = init - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) { label.text = "$title: ${p + min}%" }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) { save((s?.progress ?: 0) + min) }
            })
        })
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(24), dp(14), dp(24)) }
        setContentView(ScrollView(this).apply { addView(root) })
        val c = this

        root.addView(tv("मेरो साउन्डबक्स", 26f))
        status = tv("", 15f); root.addView(status)
        last = tv("", 15f); root.addView(last)

        root.addView(Switch(this).apply {
            text = "Soundbox ON/OFF"; isChecked = Prefs.enabled(c)
            setOnCheckedChangeListener { _, v -> Prefs.setB(c, "enabled", v) }
        })
        root.addView(btn("1. Open Notification Access settings") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
        root.addView(btn("2. Test Nepali voice (255)") {
            Speaker.speak(c, NepaliNumber.speech(BigDecimal(255))!!) { ok, m -> toast((if (ok) "OK: " else "FAILED: ") + m) }
        })
        root.addView(btn("Open Android Text-to-speech settings") {
            try { startActivity(Intent("com.android.settings.TTS_SETTINGS")) }
            catch (_: Exception) { startActivity(Intent(Settings.ACTION_SETTINGS)) }
        })
        seek("Volume", Prefs.volume(c), 10, 100) { Prefs.setI(c, "volume", it) }
        seek("Speed", Prefs.speed(c), 50, 150) { Prefs.setI(c, "speed", it) }
        root.addView(CheckBox(this).apply {
            text = "Battery-saving mode (no history, no debug capture)"; isChecked = Prefs.saving(c)
            setOnCheckedChangeListener { _, v -> Prefs.setB(c, "saving", v) }
        })
        root.addView(CheckBox(this).apply {
            text = "Debug capture of eSewa notifications (turn off after setup)"; isChecked = Prefs.b(c, "debug", true)
            setOnCheckedChangeListener { _, v -> Prefs.setB(c, "debug", v) }
        })
        root.addView(CheckBox(this).apply {
            text = "Keep local history"; isChecked = Prefs.b(c, "history", true)
            setOnCheckedChangeListener { _, v -> Prefs.setB(c, "history", v) }
        })

        root.addView(tv("PARSER SETTINGS", 18f))
        root.addView(tv("eSewa package name (exact). Empty = nothing is announced."))
        ePkg = edit(Prefs.pkg(c)); root.addView(ePkg)
        root.addView(btn("Use package from last captured eSewa notification") {
            val p = CaptureStore.all(c).firstOrNull { it.optString("pkg").contains("esewa", true) }?.optString("pkg")
            if (p == null) toast("No eSewa-like notification captured yet") else { ePkg.setText(p); toast("Check it, then Save") }
        })
        root.addView(tv("Incoming-payment phrase(s), comma separated, copied from a REAL notification (e.g. a word that appears only in received-money messages). Empty = nothing announced."))
        eIn = edit(Prefs.incomingRaw(c), true); root.addView(eIn)
        root.addView(tv("Reject words (if any appears, never announce)"))
        eRej = edit(Prefs.rejectRaw(c), true); root.addView(eRej)
        root.addView(tv("Amount regex (group 1 = number). Searched only AFTER the incoming phrase."))
        eAmt = edit(Prefs.amountRegex(c), true); root.addView(eAmt)
        root.addView(tv("Transaction-ID regex (optional, group 1 = id)"))
        eTxn = edit(Prefs.txnRegex(c)); root.addView(eTxn)
        root.addView(btn("Save parser settings") {
            Prefs.setS(c, "pkg", ePkg.text.toString().trim())
            Prefs.setS(c, "incoming", eIn.text.toString())
            Prefs.setS(c, "reject", eRej.text.toString())
            Prefs.setS(c, "amountRegex", eAmt.text.toString())
            Prefs.setS(c, "txnRegex", eTxn.text.toString())
            toast("Saved"); refresh()
        })

        root.addView(tv("PARSER TEST (paste a notification text)", 18f))
        eTest = edit("", true); root.addView(eTest)
        root.addView(btn("Run parser test (uses saved settings)") {
            val r = PaymentParser.parse(eTest.text.toString(), Prefs.list(Prefs.incomingRaw(c)),
                Prefs.list(Prefs.rejectRaw(c)), Prefs.amountRegex(c), Prefs.txnRegex(c))
            when (r) {
                is PaymentParser.Result.Accept -> {
                    testSpeech = NepaliNumber.speech(r.amount)
                    result.text = "ACCEPT amount=${r.amount.toPlainString()} txn=${r.txnId}\n$testSpeech"
                }
                is PaymentParser.Result.Reject -> { testSpeech = null; result.text = "REJECT: ${r.reason}" }
            }
        })
        result = tv(""); root.addView(result)
        root.addView(btn("Speak test result") {
            val t = testSpeech
            if (t == null) toast("Nothing accepted") else Speaker.speak(c, t) { ok, m -> toast((if (ok) "OK: " else "FAILED: ") + m) }
        })

        root.addView(tv("HISTORY", 18f)); hist = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; root.addView(hist)
        root.addView(btn("Clear history") { History.clear(c); refresh() })

        root.addView(tv("CAPTURED NOTIFICATIONS (debug)", 18f))
        root.addView(CheckBox(this).apply {
            text = "Temporary: capture ALL apps (to discover eSewa package)"; isChecked = CaptureStore.captureAll(c)
            setOnCheckedChangeListener { _, v -> CaptureStore.setCaptureAll(c, v) }
        })
        root.addView(btn("Refresh") { refresh() })
        root.addView(btn("Copy sanitized (digits -> X)") {
            val t = CaptureStore.all(c).joinToString("\n\n") { fmt(it, true) }
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("caps", t))
            toast("Copied")
        })
        root.addView(btn("Clear captures") { CaptureStore.clear(c); refresh() })
        caps = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; root.addView(caps)
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun fmt(o: JSONObject, mask: Boolean): String {
        val s = "pkg: ${o.optString("pkg")}\ntime: ${DateFormat.format("HH:mm:ss", o.optLong("time"))}\n" +
            "category: ${o.optString("cat")}\ntitle: ${o.optString("title")}\ntext: ${o.optString("text")}\n" +
            "bigText: ${o.optString("big")}\nsubText: ${o.optString("sub")}"
        return if (mask) s.replace(Regex("[0-9]"), "X") else s
    }

    private fun refresh() {
        val c = this
        val on = (Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: "").contains(packageName)
        status.text = (if (on) "Notification access: ON ✅" else "Notification access: OFF ❌ (button 1)") + "\n" +
            (if (Prefs.pkg(c).isEmpty()) "eSewa package: NOT SET ❌" else "eSewa package: ${Prefs.pkg(c)} ✅") + "\n" +
            (if (Prefs.list(Prefs.incomingRaw(c)).isEmpty()) "Parser: NOT CONFIGURED ❌ (nothing will be announced)" else "Parser: configured ✅")
        val la = History.lastAmount(c)
        last.text = if (la == null) "Last payment: none yet" else
            "Last payment: Rs. $la at ${DateFormat.format("yyyy-MM-dd HH:mm:ss", History.lastTime(c))}"
        hist.removeAllViews()
        History.all(c).forEach { hist.addView(tv("${DateFormat.format("MM-dd HH:mm", it.second)}   Rs. ${it.first}")) }
        caps.removeAllViews()
        CaptureStore.all(c).forEach { caps.addView(tv(fmt(it, false)).apply { setTextIsSelectable(true) }) }
    }
}
