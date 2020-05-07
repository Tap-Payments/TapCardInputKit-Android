package company.tap.cardinputwidget.widget.block

import android.text.TextWatcher
import company.tap.cardinputwidget.Card
import company.tap.cardinputwidget.widget.CardInputListener
import company.tap.cardinputwidget.widget.CardValidCallback
import company.tap.cardinputwidget.widget.BaseCardInput

/**
 *
 * Created by Mario Gamal on 5/7/20
 * Copyright © 2020 Tap Payments. All rights reserved.
 *
 */
class BlockCardInput(override val card: Card?, override val cardBuilder: Card.Builder?) : BaseCardInput {
    override fun setCardValidCallback(callback: CardValidCallback?) {
        TODO("Not yet implemented")
    }

    override fun setCardInputListener(listener: CardInputListener?) {
        TODO("Not yet implemented")
    }

    override fun setCardNumberTextWatcher(cardNumberTextWatcher: TextWatcher?) {
        TODO("Not yet implemented")
    }

    override fun setExpiryDateTextWatcher(expiryDateTextWatcher: TextWatcher?) {
        TODO("Not yet implemented")
    }

    override fun setCvcNumberTextWatcher(cvcNumberTextWatcher: TextWatcher?) {
        TODO("Not yet implemented")
    }

    override fun setHolderNameTextWatcher(holderNameTextWatcher: TextWatcher?) {
        TODO("Not yet implemented")
    }

    override fun setCardHint(cardHint: String) {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun setCardNumber(cardNumber: String?) {
        TODO("Not yet implemented")
    }

    override fun setExpiryDate(month: Int, year: Int) {
        TODO("Not yet implemented")
    }

    override fun setCvcCode(cvcCode: String?) {
        TODO("Not yet implemented")
    }
}