package np.mero.soundbox

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/** Creates a TTS engine only when needed and releases it right after speaking. Never falls back to another language. */
object Speaker {
    fun speak(ctx: Context, text: String, onResult: ((Boolean, String) -> Unit)? = null) {
        val app = ctx.applicationContext
        val released = AtomicBoolean(false)
        var holder: TextToSpeech? = null
        fun release() { if (released.compareAndSet(false, true)) holder?.shutdown() }
        holder = TextToSpeech(app) { status ->
            val t = holder
            if (status != TextToSpeech.SUCCESS || t == null) {
                onResult?.invoke(false, "TTS engine failed to start"); release(); return@TextToSpeech
            }
            var loc = Locale("ne", "NP")
            if (t.isLanguageAvailable(loc) < 0) loc = Locale("ne")
            if (t.isLanguageAvailable(loc) < 0 || t.setLanguage(loc) < 0) {
                onResult?.invoke(false, "Nepali voice NOT available in engine: ${t.defaultEngine}")
                release(); return@TextToSpeech
            }
            t.setSpeechRate(Prefs.speed(app) / 100f)
            t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) { onResult?.invoke(true, "Spoken with ${t.defaultEngine}"); release() }
                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) { onResult?.invoke(false, "Speech error"); release() }
            })
            val p = Bundle()
            p.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, Prefs.volume(app) / 100f)
            if (t.speak(text, TextToSpeech.QUEUE_FLUSH, p, "msb") != TextToSpeech.SUCCESS) {
                onResult?.invoke(false, "speak() failed"); release()
            }
        }
        Handler(Looper.getMainLooper()).postDelayed({ release() }, 30_000)
    }
}
