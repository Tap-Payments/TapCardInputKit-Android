package company.tap.cardinputwidget

import androidx.annotation.DrawableRes
import company.tap.cardinputwidget.utils.TapTextUtils
import company.tap.tapuilibrary.themekit.ThemeManager
import java.util.regex.Pattern

/**
 * A representation of supported card brands and related data
 */
enum class CardBrandSingle(
    val code: String,
    val displayName: String,
    @DrawableRes val icon: Int,
    @DrawableRes val cvcIconDark: Int =R.drawable.dark_cvv,
    @DrawableRes val cvcIconLight: Int =R.drawable.light_cvv,
    @DrawableRes val errorIconDark: Int = R.drawable.card_icon_dark,
    @DrawableRes val errorIconLight: Int = R.drawable.card_icon_light,

    /**
     * Accepted CVC lengths
     */
    val cvcLength: Set<Int> = setOf(3),

    /**
     * The default max length when the card number is formatted without spaces (e.g. "4242424242424242")
     *
     * Note that [CardBrandSingle.DinersClub]'s max length depends on the BIN (e.g. card number prefix).
     * In the case of a [CardBrandSingle.DinersClub] card, use [getMaxLengthForCardNumber].
     */
    val defaultMaxLength: Int = 16,

    /**
     * Based on [Issuer identification number table](http://en.wikipedia.org/wiki/Bank_card_number#Issuer_identification_number_.28IIN.29)
     */
    val pattern: Pattern? = null,

    /**
     * Patterns for discrete lengths
     */
    private val partialPatterns: Map<Int, Pattern> = emptyMap(),

    /**
     * The position of spaces in a formatted card number. For example, "4242424242424242" is
     * formatted to "4242 4242 4242 4242".
     */
    val defaultSpacePositions: Set<Int> = setOf(4, 9, 14),

    /**
     * By default, a [CardBrandSingle] does not have variants.
     */
    private val variantMaxLength: Map<Pattern, Int> = emptyMap(),

    private val variantSpacePositions: Map<Pattern, Set<Int>> = emptyMap()
) {
   /* AmericanExpress(
        "amex",
        "American Express",
        if (ThemeManager.currentTheme.isNotEmpty() && ThemeManager.currentTheme.contains("dark")) R.drawable.ic_americanexpress else R.drawable.ic_americanexpress,
        cvcLength = setOf(3, 4),
        defaultMaxLength = 15,
        pattern = Pattern.compile("^(34|37)[0-9]*$"),
        defaultSpacePositions = setOf(4, 11)
    ),*/

    Discover(
        "discover",
        "Discover",
        if (ThemeManager.currentTheme.isNotEmpty() && ThemeManager.currentTheme.contains("dark")) R.drawable.ic_discover else R.drawable.ic_discover,
        pattern = Pattern.compile("^(60|64|65)[0-9]*$")
    ),

    /**
     * JCB
     *
     * BIN range: 352800 to 358999
     */
    JCB(
        "jcb",
        "JCB",
        if (ThemeManager.currentTheme.isNotEmpty() && ThemeManager.currentTheme.contains("dark")) R.drawable.ic_jcb else R.drawable.ic_jcb,
        pattern = Pattern.compile("^(352[89]|35[3-8][0-9])[0-9]*$"),
        partialPatterns = mapOf(
            2 to Pattern.compile("^(35)$"),
            3 to Pattern.compile("^(35[2-8])$")
        )
    ),

    /**
     * Diners Club
     *
     * 14-digits: BINs starting with 36
     * 16-digits: BINs starting with 30, 38, 39
     */
    DinersClub(
        "diners",
        "Diners Club",
        if (ThemeManager.currentTheme.isNotEmpty() && ThemeManager.currentTheme.contains("dark")) R.drawable.ic_diners else R.drawable.ic_diners,
        defaultMaxLength = 16,
        pattern = Pattern.compile("^(36|30|38|39)[0-9]*$"),
        variantMaxLength = mapOf(
            Pattern.compile("^(36)[0-9]*$") to 14
        ),
        variantSpacePositions = mapOf(
            Pattern.compile("^(36)[0-9]*$") to setOf(4, 11)
        )
    ),

  Visa(
        "visa",
        "Visa",
       // if (ThemeManager.currentTheme.isNotEmpty() && ThemeManager.currentTheme.contains("dark")) R.drawable.ic_visa else R.drawable.ic_visa,
      if (ThemeManager.currentTheme.isNotEmpty() && ThemeManager.currentTheme.contains("dark")) R.drawable.card_icon_dark else R.drawable.card_icon_light,

      pattern = Pattern.compile("^(4)[0-9]*$")
    ),

    MasterCard(
        "mastercard",
        "Mastercard",
        if (ThemeManager.currentTheme.isNotEmpty() && ThemeManager.currentTheme.contains("dark")) R.drawable.card_icon_dark else R.drawable.card_icon_light,

        // if (ThemeManager.currentTheme.isNotEmpty() && ThemeManager.currentTheme.contains("dark")) R.drawable.ic_mastercard else R.drawable.ic_mastercard,
        pattern = Pattern.compile("^(2221|2222|2223|2224|2225|2226|2227|2228|2229|223|224|225|226|227|228|229|23|24|25|26|270|271|2720|50|51|52|53|54|55|67)[0-9]*$")
    ),

    UnionPay(
        "unionpay",
        "UnionPay",
        if (ThemeManager.currentTheme.isNotEmpty() && ThemeManager.currentTheme.contains("dark")) R.drawable.ic_unionpay else R.drawable.ic_unionpay,
        pattern = Pattern.compile("^(62|81)[0-9]*$")
    ),

    Unknown(
        "unknown",
        "Unknown",
        if (ThemeManager.currentTheme.isNotEmpty() && ThemeManager.currentTheme.contains("dark")) R.drawable.dark_card else R.drawable.card_icon_light,
        cvcLength = setOf(3, 4)
    );

    val defaultMaxLengthWithSpaces: Int = defaultMaxLength + defaultSpacePositions.size

    val maxCvcLength: Int
        get() {
            return cvcLength.maxOrNull() ?: CVC_COMMON_LENGTH
        }

    /**
     * Checks to see whether the input number is of the correct length, given the assumed brand of
     * the card. This function does not perform a Luhn check.
     *
     * @param cardNumber the card number with no spaces or dashes
     * @return `true` if the card number is the correct length for the assumed brand
     */
    fun isValidCardNumberLength(cardNumber: String?): Boolean {
        return cardNumber != null && Unknown != this &&
            cardNumber.length == getMaxLengthForCardNumber(cardNumber)
    }

    fun isValidCvc(cvc: String): Boolean {
        return cvcLength.contains(cvc.length)
    }

    fun isMaxCvc(cvcText: String?): Boolean {
        val cvcLength = cvcText?.trim()?.length ?: 0
        return maxCvcLength == cvcLength
    }

    /**
     * If the [CardBrandSingle] has variants, and the [cardNumber] starts with one of the variant
     * prefixes, return the length for that variant. Otherwise, return [defaultMaxLength].
     *
     * Note: currently only [CardBrandSingle.DinersClub] has variants
     */
    fun getMaxLengthForCardNumber(cardNumber: String): Int {
        val normalizedCardNumber =
            TapTextUtils.removeSpacesAndHyphens(cardNumber).orEmpty()
        return variantMaxLength.entries.firstOrNull { (pattern, _) ->
            pattern.matcher(normalizedCardNumber).matches()
        }?.value ?: defaultMaxLength
    }

    fun getMaxLengthWithSpacesForCardNumber(cardNumber: String): Int {
        return getMaxLengthForCardNumber(cardNumber) +
            getSpacePositionsForCardNumber(cardNumber).size
    }

    /**
     * If the [CardBrandSingle] has variants, and the [cardNumber] starts with one of the variant
     * prefixes, return the length for that variant. Otherwise, return [defaultMaxLength].
     *
     * Note: currently only [CardBrandSingle.DinersClub] has variants
     */
    fun getSpacePositionsForCardNumber(cardNumber: String): Set<Int> {
        val normalizedCardNumber =
            TapTextUtils.removeSpacesAndHyphens(cardNumber).orEmpty()
        return variantSpacePositions.entries.firstOrNull { (pattern, _) ->
            pattern.matcher(normalizedCardNumber).matches()
        }?.value ?: defaultSpacePositions
    }

    /**
     * Format a number according to brand requirements.
     *
     * e.g. `"4242424242424242"` will return `"4242 4242 4242 4242"`
     */
    fun formatNumber(cardNumber: String): String {
        return groupNumber(cardNumber)
            .takeWhile { it != null }
            .joinToString(" ")
    }

    /**
     * Separates a card number according to the brand requirements, including prefixes of card
     * numbers, so that the groups can be easily displayed if the user is typing them in.
     * Note that this does not verify that the card number is valid, or even that it is a number.
     *
     * e.g. `"4242424242424242"` will return `["4242", "4242", "4242", "4242"]`
     *
     * @param cardNumber the raw card number
     *
     * @return an array of strings with the number groups, in order. If the number is not complete,
     * some of the array entries may be `null`.
     */
    fun groupNumber(cardNumber: String): Array<String?> {
        val spacelessCardNumber = cardNumber.take(getMaxLengthForCardNumber(cardNumber))
        val spacePositions = getSpacePositionsForCardNumber(cardNumber)
        val groups = arrayOfNulls<String?>(spacePositions.size + 1)

        val length = spacelessCardNumber.length
        var lastUsedIndex = 0

        spacePositions
            .toList().sorted().forEachIndexed { idx, spacePosition ->
                val adjustedSpacePosition = spacePosition - idx
                if (length > adjustedSpacePosition) {
                    groups[idx] = spacelessCardNumber.substring(
                        lastUsedIndex,
                        adjustedSpacePosition
                    )
                    lastUsedIndex = adjustedSpacePosition
                }
            }

        // populate any remaining digits in the first index with a null value
        groups
            .indexOfFirst { it == null }
            .takeIf {
                it != -1
            }?.let {
                groups[it] = spacelessCardNumber.substring(lastUsedIndex)
            }

        return groups
    }

    private fun getPatternForLength(cardNumber: String): Pattern? {
        return partialPatterns[cardNumber.length] ?: pattern
    }

    companion object {
        /**
         * @param cardNumber a card number
         * @return the [CardBrandSingle] that matches the [cardNumber]'s prefix, if one is found;
         * otherwise, [CardBrandSingle.Unknown]
         */
        fun fromCardNumber(cardNumber: String?): CardBrandSingle {
            if (cardNumber.isNullOrBlank()) {
                return Unknown
            }

            return values()
                .firstOrNull { cardBrand ->
                    cardBrand.getPatternForLength(cardNumber)?.matcher(cardNumber)?.matches() == true
                } ?: Unknown
        }

        fun getUnKnown():Int{
            if (ThemeManager.currentTheme.isNotEmpty() && ThemeManager.currentTheme.contains("dark"))
             return   R.drawable.dark_card else return R.drawable.card_icon_light


        }

        /**
         * @param code a brand code, such as `Visa` or `American Express`.
         * See [PaymentMethod.Card.brand].
         */
        fun fromCode(code: String?): CardBrandSingle {
            return values().firstOrNull { it.code.equals(code) } ?: Unknown
        }

        private const val CVC_COMMON_LENGTH: Int = 3
    }
}
