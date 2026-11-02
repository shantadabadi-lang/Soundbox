package np.mero.soundbox

import java.math.BigDecimal
import java.math.RoundingMode

/** Fully local Nepali number-to-words (lakh/crore system). */
object NepaliNumber {
    const val MAX_RUPEES = 9_99_99_99_999L

    // index = number 0..99
    private val W = arrayOf(
        "शून्य","एक","दुई","तीन","चार","पाँच","छ","सात","आठ","नौ",
        "दस","एघार","बाह्र","तेह्र","चौध","पन्ध्र","सोह्र","सत्र","अठार","उन्नाइस",
        "बीस","एक्काइस","बाइस","तेइस","चौबीस","पच्चीस","छब्बीस","सत्ताइस","अठ्ठाइस","उनन्तीस",
        "तीस","एकतीस","बत्तीस","तेत्तीस","चौँतीस","पैँतीस","छत्तीस","सैँतीस","अठतीस","उनन्चालीस",
        "चालीस","एकचालीस","बयालीस","तेचालीस","चौवालीस","पैँतालीस","छयालीस","सतचालीस","अठचालीस","उनन्चास",
        "पचास","एकाउन्न","बाउन्न","त्रिपन्न","चवन्न","पचपन्न","छपन्न","सन्ताउन्न","अन्ठाउन्न","उनान्साठी",
        "साठी","एकसट्ठी","बैसट्ठी","त्रिसट्ठी","चौँसट्ठी","पैँसट्ठी","छयसट्ठी","सतसट्ठी","अठसट्ठी","उनन्सत्तरी",
        "सत्तरी","एकहत्तर","बहत्तर","त्रिहत्तर","चौहत्तर","पचहत्तर","छयहत्तर","सतहत्तर","अठहत्तर","उनासी",
        "असी","एकासी","बयासी","त्रियासी","चौरासी","पचासी","छयासी","सतासी","अठासी","उनान्नब्बे",
        "नब्बे","एकान्नब्बे","बयान्नब्बे","त्रियान्नब्बे","चौरान्नब्बे","पन्चान्नब्बे","छयान्नब्बे","सन्तान्नब्बे","अन्ठान्नब्बे","उनान्सय"
    )

    private fun below1000(n: Int): String {
        val h = n / 100
        val r = n % 100
        val parts = ArrayList<String>()
        if (h > 0) parts.add("${W[h]} सय")
        if (r > 0) parts.add(W[r])
        return parts.joinToString(" ")
    }

    fun words(n: Long): String {
        require(n in 0..MAX_RUPEES)
        if (n == 0L) return W[0]
        var x = n
        val rest = (x % 1000).toInt(); x /= 1000
        val hajar = (x % 100).toInt(); x /= 100
        val lakh = (x % 100).toInt(); x /= 100
        val karod = (x % 100).toInt(); x /= 100
        val arab = x.toInt()
        val p = ArrayList<String>()
        if (arab > 0) p.add("${W[arab]} अर्ब")
        if (karod > 0) p.add("${W[karod]} करोड")
        if (lakh > 0) p.add("${W[lakh]} लाख")
        if (hajar > 0) p.add("${W[hajar]} हजार")
        if (rest > 0) p.add(below1000(rest))
        return p.joinToString(" ")
    }

    /** Returns the full sentence, or null for invalid/negative/zero/too-large amounts. */
    fun speech(amount: BigDecimal): String? {
        if (amount.signum() <= 0) return null
        val a = amount.setScale(2, RoundingMode.HALF_UP)
        val rupees = a.toBigInteger()
        if (rupees > java.math.BigInteger.valueOf(MAX_RUPEES)) return null
        val r = rupees.toLong()
        val paisa = a.remainder(BigDecimal.ONE).movePointRight(2).toInt()
        val parts = ArrayList<String>()
        if (r > 0) parts.add("${words(r)} रुपैयाँ")
        if (paisa > 0) parts.add("${words(paisa.toLong())} पैसा")
        if (parts.isEmpty()) return null
        return parts.joinToString(" ") + " प्राप्त भयो।"
    }
}
