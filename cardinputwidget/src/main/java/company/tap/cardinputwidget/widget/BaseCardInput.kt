package company.tap.cardinputwidget.widget

import android.text.TextWatcher
import androidx.annotation.IntRange
import company.tap.cardinputwidget.Card
import company.tap.cardinputwidget.CardBrand
import company.tap.cardinputwidget.CardBrandSingle
import company.tap.cardinputwidget.utils.TextValidator

internal interface BaseCardInput {
    val card: Card?

    val cardBuilder: Card.Builder?

    fun setCardValidCallback(callback: CardValidCallback?)

    fun setCardInputListener(listener: CardInputListener?)

    fun setSingleCardInput(cardBrand: CardBrandSingle)

    /**
     * Set a `TextWatcher` to receive card number changes.
     */
    fun setCardNumberApiTextWatcher(cardApiNumberTextWatcher: TextValidator)
    /**
     * Set a `TextWatcher` to receive card number changes.
     */
    fun setCardNumberTextWatcher(cardNumberTextWatcher: TextWatcher?)
    /**
     * Remove a `TextWatcher` to receive card number changes.
     */
    fun removeCardNumberTextWatcher(cardNumberTextWatcher: TextWatcher?)

    /**
     * Set a `TextWatcher` to receive expiration date changes.
     */
    fun setExpiryDateTextWatcher(expiryDateTextWatcher: TextWatcher?)

    /**
     * Set a `TextWatcher` to receive CVC value changes.
     */
    fun setCvcNumberTextWatcher(cvcNumberTextWatcher: TextWatcher?)

    /**
     * Set a `TextWatcher` to receive postal code changes.
     */
    fun setHolderNameTextWatcher(holderNameTextWatcher: TextWatcher?)

    fun setCardHint(cardHint: String)
    fun setCardHolderHint(cardHolderHint: String)
    fun setCVVHint(cvvHint: String)
    fun setExpiryHint(expiryHint: String)

    fun clear()

    fun setCardNumber(cardNumber: String?)

    fun setCardHolderName(cardHolderName: String?)

    fun setExpiryDate(
        @IntRange(from = 1, to = 12) month: Int,
        @IntRange(from = 0, to = 9999) year: Int
    )

    fun setCvcCode(cvcCode: String?)

    companion object {
        internal const val DEFAULT_HOLDER_NAME_ENABLED = false
        internal const val DEFAULT_HOLDER_NAME_REQUIRED = false
    }
}
