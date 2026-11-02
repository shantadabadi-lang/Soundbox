package np.mero.soundbox

import java.math.BigDecimal

object PaymentParser {
    sealed class Result {
        data class Accept(val amount: BigDecimal, val txnId: String?) : Result()
        data class Reject(val reason: String) : Result()
    }

    private fun digits(s: String): String {
        val sb = StringBuilder()
        for (ch in s) sb.append(if (ch in '०'..'९') ('0' + (ch - '०')) else ch)
        return sb.toString()
    }

    fun parse(
        raw: String, incoming: List<String>, reject: List<String>,
        amountRegex: String, txnRegex: String
    ): Result {
        val text = digits(raw).replace(Regex("\\s+"), " ").trim()
        if (text.isEmpty()) return Result.Reject("empty text")
        val lower = text.lowercase()
        if (incoming.isEmpty()) return Result.Reject("parser not configured (no incoming phrase set)")
        reject.firstOrNull { lower.contains(it) }?.let { return Result.Reject("reject word: $it") }
        val idx = incoming.map { lower.indexOf(it) }.filter { it >= 0 }.minOrNull()
            ?: return Result.Reject("no incoming-payment phrase found")
        val re = try { Regex(amountRegex, RegexOption.IGNORE_CASE) }
        catch (e: Exception) { return Result.Reject("bad amount regex") }
        val m = re.find(text, idx) ?: return Result.Reject("no currency-marked amount after incoming phrase")
        val numStr = m.groups[1]?.value?.replace(",", "") ?: return Result.Reject("amount regex has no group 1")
        val amt = numStr.toBigDecimalOrNull() ?: return Result.Reject("amount not a number")
        if (amt.signum() <= 0) return Result.Reject("amount not positive")
        if (amt.stripTrailingZeros().scale() > 2) return Result.Reject("too many decimals")
        if (amt > BigDecimal(NepaliNumber.MAX_RUPEES)) return Result.Reject("amount above limit")
        var txn: String? = null
        if (txnRegex.isNotBlank()) {
            txn = try { Regex(txnRegex, RegexOption.IGNORE_CASE).find(text)?.groups?.get(1)?.value }
            catch (e: Exception) { null }
        }
        return Result.Accept(amt, txn)
    }
}
