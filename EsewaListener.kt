package np.mero.soundbox

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.json.JSONObject
import java.math.BigDecimal

class EsewaListener : NotificationListenerService() {

    override fun onListenerDisconnected() {
        // Ask the system to reconnect us (no polling, system-driven).
        try { requestRebind(ComponentName(this, EsewaListener::class.java)) } catch (_: Exception) {}
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        val c = this
        val expected = Prefs.pkg(c)
        val esewaLike = pkg.contains("esewa", ignoreCase = true)
        val debug = Prefs.debug(c)
        val capture = debug && (esewaLike || pkg == expected || CaptureStore.captureAll(c))
        val announce = expected.isNotEmpty() && pkg == expected && Prefs.enabled(c)
        if (!capture && !announce) return   // every other package rejected immediately

        val ex = sbn.notification.extras
        val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val big = ex.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val sub = ex.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        if (capture) {
            CaptureStore.add(c, JSONObject().put("pkg", pkg).put("time", sbn.postTime)
                .put("title", title).put("text", text).put("big", big).put("sub", sub)
                .put("cat", sbn.notification.category ?: ""))
        }
        if (!announce) return

        val full = listOf(title, text, big).filter { it.isNotBlank() }.distinct().joinToString(" ")
        val r = PaymentParser.parse(full, Prefs.list(Prefs.incomingRaw(c)), Prefs.list(Prefs.rejectRaw(c)),
            Prefs.amountRegex(c), Prefs.txnRegex(c))
        if (r !is PaymentParser.Result.Accept) return
        val now = System.currentTimeMillis()
        if (Dedup.isDuplicate(c, r.txnId, r.amount.toPlainString(), full, now)) return
        val speech = NepaliNumber.speech(r.amount) ?: return
        History.setLast(c, r.amount.toPlainString(), now)
        if (Prefs.historyOn(c)) History.add(c, r.amount.toPlainString(), now)
        Speaker.speak(c, speech)
    }
}
