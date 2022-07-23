package company.tap.cardinputwidget.widget.inline

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.*
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.AUTOFILL_HINT_PASSWORD
import android.view.View.OnFocusChangeListener
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.Transformation
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.IntRange
import androidx.annotation.VisibleForTesting
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import company.tap.cardinputwidget.*
import company.tap.cardinputwidget.databinding.CardInputWidgetBinding
import company.tap.cardinputwidget.widget.CardInputListener.FocusField.Companion.FOCUS_CARD
import company.tap.cardinputwidget.widget.CardInputListener.FocusField.Companion.FOCUS_CVC
import company.tap.cardinputwidget.widget.CardInputListener.FocusField.Companion.FOCUS_EXPIRY
import company.tap.cardinputwidget.utils.DateUtils
import company.tap.cardinputwidget.utils.TextValidator
import company.tap.cardinputwidget.views.CardNumberEditText
import company.tap.cardinputwidget.widget.BaseCardInput
import company.tap.cardinputwidget.widget.CardInputListener
import company.tap.cardinputwidget.widget.CardValidCallback
import company.tap.tapuilibrary.themekit.ThemeManager
import company.tap.tapuilibrary.uikit.atoms.TapTextInput
import company.tap.tapuilibrary.uikit.utils.TapTextWatcher
import kotlinx.android.synthetic.main.card_input_widget.view.*

import kotlin.properties.Delegates

/**
 * A card input widget that handles all animation on its own.
 *
 * The individual `EditText` views of this widget can be styled by defining a style
 * `Tap.CardInputWidget.EditText` that extends `Tap.Base.CardInputWidget.EditText`.
 */
class InlineCardInput @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr),
    BaseCardInput {
    private val viewBinding = CardInputWidgetBinding.inflate(
            LayoutInflater.from(context),
            this
    )

  //  private val containerLayout1 = viewBinding.container1
    private val containerLayout = viewBinding.container

    @JvmSynthetic
    internal val cardBrandView = viewBinding.cardBrandView

    private val cardNumberTextInputLayout = viewBinding.cardNumberTextInputLayout
    private val expiryDateTextInputLayout = viewBinding.expiryDateTextInputLayout
    private val cvcNumberTextInputLayout = viewBinding.cvcTextInputLayout
    internal val holderNameTextInputLayout = viewBinding.holderNameTextInputLayout

    @JvmSynthetic
    internal val cardNumberEditText = viewBinding.cardNumberEditText

    @JvmSynthetic
    internal val expiryDateEditText = viewBinding.expiryDateEditText

    @JvmSynthetic
    internal val cvcNumberEditText = viewBinding.cvcEditText

    @JvmSynthetic
    internal val holderNameEditText = viewBinding.holderNameEditText

    private var cardInputListener: CardInputListener? = null
    private var cardValidCallback: CardValidCallback? = null

    private val frameStart: Int
        get() {
            val isLtr = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_LTR
            return if (isLtr) {
                containerLayout.left
            } else {
                containerLayout.right
            }
        }
    private val cardValidTextWatcher = object : TapTextWatcher() {
        override fun afterTextChanged(s: Editable?) {
            super.afterTextChanged(s)
            cardValidCallback?.onInputChanged(invalidFields.isEmpty(), invalidFields)
        }
    }

    private val invalidFields: Set<CardValidCallback.Fields>
        get() {
            return listOfNotNull(
                    CardValidCallback.Fields.Number.takeIf {
                        cardNumberEditText.cardNumber == null
                    },
                    CardValidCallback.Fields.Expiry.takeIf {
                        expiryDateEditText.validDateFields == null
                    },
                    CardValidCallback.Fields.Cvc.takeIf {
                        this.cvcValue == null
                    }
            ).toSet()
        }

    @VisibleForTesting
    internal var shouldShowErrorIcon = false
        private set(value) {
            val isValueChange = field != value
            field = value

            if (isValueChange) {
                updateIcon()
            }
        }


    @JvmSynthetic
    internal var cardNumberIsViewed = true

    private var initFlag: Boolean = false

    @JvmSynthetic
    internal var layoutWidthCalculator: LayoutWidthCalculator =
        DefaultLayoutWidthCalculator()

    internal val placementParameters: PlacementParameters =
        PlacementParameters()

    private val holderNameValue: String?
        get() {
            return if (holderNameEnabled) {
                holderNameEditText.holderName
            } else {
                null
            }
        }

    private val cvcValue: String?
        get() {
            return cvcNumberEditText.cvcValue
        }

    private val brand: CardBrand
        get() {
            return cardNumberEditText.cardBrand
        }

    var shouldChangeIcon = true

    @VisibleForTesting
    @JvmSynthetic
    internal val requiredFields: List<TapTextInput>
    private val allFields: List<TapTextInput>

    /**
     * The [TapEditText] fields that are currently enabled and active in the UI.
     */
    @VisibleForTesting
    internal val currentFields: List<TapTextInput>
        @JvmSynthetic
        get() {
            return requiredFields
                   .plus(holderNameEditText.takeIf { holderNameEnabled })
                    .filterNotNull()
        }

    /**
     * Gets a [Card] object from the user input, if all fields are valid. If not, returns
     * `null`.
     *
     * @return a valid [Card] object based on user input, or `null` if any field is
     * invalid
     */
    override val card: Card?
        get() {
            return cardBuilder?.build()
        }

    override val cardBuilder: Card.Builder?
        get() {
            val cardNumber = cardNumberEditText.cardNumber
            val cardDate = expiryDateEditText.validDateFields
            val cvcValue = this.cvcValue

            cardNumberEditText.shouldShowError = cardNumber == null
            expiryDateEditText.shouldShowError = cardDate == null
            cvcNumberEditText.shouldShowError = cvcValue == null
          //  cvcNumberEditText.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
            holderNameEditText.shouldShowError =
                   // holderNameRequired &&
                            holderNameEditText.holderName.isNullOrBlank()

            // Announce error messages for accessibility
            currentFields
                    .filter { it.shouldShowError }
                    .forEach { editText ->
                        editText.fieldErrorMessage?.let { errorMessage ->
                            editText.announceForAccessibility(errorMessage)
                        }
                    }

            when {
                cardNumber == null -> {
                    cardNumberEditText.requestFocus()
                }
                cardDate == null -> {
                    expiryDateEditText.requestFocus()
                }
                cvcValue == null -> {
                    cvcNumberEditText.requestFocus()
                  //  cvcNumberEditText.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD

                }
                holderNameEditText.shouldShowError -> {
                    holderNameEditText.requestFocus()
                }
                else -> {
                    shouldShowErrorIcon = false
                    return Card.Builder(
                        cardNumber,
                        cardDate.first,
                        cardDate.second,
                        cvcValue
                    )
                            .addressZip(holderNameValue)
                            .loggingTokens(setOf(LOGGING_TOKEN))
                }
            }

            shouldShowErrorIcon = true

            return null
        }

    private val frameWidth: Int
        get() = frameWidthSupplier()

    @JvmSynthetic
    internal var frameWidthSupplier: () -> Int

    /**
     * The postal code field is enabled by default. Disabling the postal code field may impact
     * auth success rates, so it is discouraged to disable it unless you are collecting the postal
     * code outside of this form.
     */
    var holderNameEnabled: Boolean by Delegates.observable(
        BaseCardInput.DEFAULT_HOLDER_NAME_ENABLED
    ) { _, _, isEnabled ->
        if (isEnabled) {
            holderNameEditText.isEnabled = true
            holderNameTextInputLayout.visibility = View.VISIBLE

            cvcNumberEditText.imeOptions = EditorInfo.IME_ACTION_NEXT
        } else {
            holderNameEditText.isEnabled = false
            holderNameTextInputLayout.visibility = View.GONE

            cvcNumberEditText.imeOptions = EditorInfo.IME_ACTION_DONE
        }
    }

  /*  *//**
     * If [holderNameEnabled] is true and [holderNameRequired] is true, then postal code is a
     * required field.
     *
     * If [holderNameEnabled] is false, this value is ignored.
     *
     * Note that some countries do not have postal codes, so requiring postal code will prevent
     * those users from submitting this form successfully.
     *//*
    var holderNameRequired: Boolean =
        BaseCardInput.DEFAULT_HOLDER_NAME_REQUIRED*/


    init {
        // This ensures that onRestoreInstanceState is called
        // during rotations.
        if (id == View.NO_ID) {
            id =
                DEFAULT_READER_ID
        }

        orientation = VERTICAL
        minimumWidth = resources.getDimensionPixelSize(R.dimen.card_widget_min_width)

        frameWidthSupplier = { containerLayout.width }

        requiredFields = listOf(
                cardNumberEditText, cvcNumberEditText, expiryDateEditText
        )

        allFields = requiredFields.plus(holderNameEditText)
       // allFields = requiredFields
        initView(attrs)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
    }

    override fun setCardValidCallback(callback: CardValidCallback?) {
        this.cardValidCallback = callback
        requiredFields.forEach { it.removeTextChangedListener(cardValidTextWatcher) }

        // only add the TextWatcher if it will be used
        if (callback != null) {
            requiredFields.forEach { it.addTextChangedListener(cardValidTextWatcher) }
        }

        // call immediately after setting
        cardValidCallback?.onInputChanged(invalidFields.isEmpty(), invalidFields)
    }

    /**
     * Set a [CardInputListener] to be notified of card input events.
     *
     * @param listener the listener
     */
    override fun setCardInputListener(listener: CardInputListener?) {
        cardInputListener = listener
    }

    override fun setSingleCardInput(cardBrand: CardBrandSingle) {
        shouldChangeIcon = false
        cardBrandView.showBrandIconSingle(cardBrand, shouldShowErrorIcon)
    }

    override fun setCardNumberApiTextWatcher(cardApiNumberTextWatcher: TextValidator) {
       cardNumberEditText.addTextChangedListener(object :TextValidator(cardNumberEditText){
           override fun validate(cardNumberEditText: CardNumberEditText?, text: String?) {
           }

       })
    }

    /**
     * Set the card number. Method does not change text field focus.
     *
     * @param cardNumber card number to be set
     */
    override fun setCardNumber(cardNumber: String?) {
        cardNumberEditText.setText(cardNumber)
        this.cardNumberIsViewed = !cardNumberEditText.isCardNumberValid
    }

    override fun setCardHint(cardHint: String) {
        cardNumberEditText.hint = cardHint
    }

    override fun setCardHolderHint(cardHolderHint: String) {
        holderNameEditText.hint = cardHolderHint
    }

    override fun setCVVHint(cvvHint: String) {
        cvcNumberEditText.hint =cvvHint
    }

    override fun setExpiryHint(expiryHint: String) {
        expiryDateEditText.hint =expiryHint
    }

    /**
     * Set the expiration date. Method invokes completion listener and changes focus
     * to the CVC field if a valid date is entered.
     *
     * Note that while a four-digit and two-digit year will both work, information
     * beyond the tens digit of a year will be truncated. Logic elsewhere in the SDK
     * makes assumptions about what century is implied by various two-digit years, and
     * will override any information provided here.
     *
     * @param month a month of the year, represented as a number between 1 and 12
     * @param year a year number, either in two-digit form or four-digit form
     */
    override fun setExpiryDate(
            @IntRange(from = 1, to = 12) month: Int,
            @IntRange(from = 0, to = 9999) year: Int
    ) {
        expiryDateEditText.setText(DateUtils.createDateStringFromIntegerInput(month, year))
    }

    /**
     * Set the CVC value for the card. Note that the maximum length is assumed to
     * be 3, unless the brand of the card has already been set (by setting the card number).
     *
     * @param cvcCode the CVC value to be set
     */
    override fun setCvcCode(cvcCode: String?) {
      //  cvcNumberEditText.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
        cvcNumberEditText.setText(cvcCode)
    }

    @JvmSynthetic
    internal fun setHolderName(holderName: String?) {
        holderNameEditText.setText(holderName)
    }

    /**
     * Clear all text fields in the CardInputWidget.
     */
    override fun clear() {
//        if (currentFields.any { it.hasFocus() } || this.hasFocus()) {
//        }
        cardNumberEditText.requestFocus()
        currentFields.forEach { it.setText("") }
    }

    /**
     * Enable or disable text fields
     *
     * @param isEnabled boolean indicating whether fields should be enabled
     */
    override fun setEnabled(isEnabled: Boolean) {
        currentFields.forEach { it.isEnabled = isEnabled }
    }

    /**
     * Set a `TextWatcher` to receive card number changes.
     */
    override fun setCardNumberTextWatcher(cardNumberTextWatcher: TextWatcher?) {
        cardNumberEditText.addTextChangedListener(cardNumberTextWatcher)
    }
    /**
     * Remove a `TextWatcher` to receive card number changes.
     */
    override fun removeCardNumberTextWatcher(cardNumberTextWatcher: TextWatcher?) {
        cardNumberEditText.removeTextChangedListener(cardNumberTextWatcher)
    }

    /**
     * Set a `TextWatcher` to receive expiration date changes.
     */
    override fun setExpiryDateTextWatcher(expiryDateTextWatcher: TextWatcher?) {
        expiryDateEditText.addTextChangedListener(expiryDateTextWatcher)
    }

    /**
     * Set a `TextWatcher` to receive CVC value changes.
     */
    override fun setCvcNumberTextWatcher(cvcNumberTextWatcher: TextWatcher?) {
      //  cvcNumberEditText.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
        cvcNumberEditText.addTextChangedListener(cvcNumberTextWatcher)
    }

    /**
     * Set a `TextWatcher` to receive postal code changes.
     */
    override fun setHolderNameTextWatcher(holderNameTextWatcher: TextWatcher?) {
        holderNameEditText.addTextChangedListener(holderNameTextWatcher)
    }

    /**
     * Override of [View.isEnabled] that returns `true` only
     * if all three sub-controls are enabled.
     *
     * @return `true` if the card number field, expiry field, and cvc field are enabled,
     * `false` otherwise
     */
    override fun isEnabled(): Boolean {
        return requiredFields.all { it.isEnabled }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action != MotionEvent.ACTION_DOWN) {
            return super.onInterceptTouchEvent(ev)
        }

        return getFocusRequestOnTouch(ev.x.toInt())?.let {
            it.requestFocus()
            true
        } ?: super.onInterceptTouchEvent(ev)
    }

    override fun onSaveInstanceState(): Parcelable {
        return Bundle().apply {
            putParcelable(STATE_SUPER_STATE, super.onSaveInstanceState())
            putBoolean(STATE_CARD_VIEWED, cardNumberIsViewed)
            putBoolean(STATE_POSTAL_CODE_ENABLED, holderNameEnabled)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            holderNameEnabled = state.getBoolean(STATE_POSTAL_CODE_ENABLED, true)
            cardNumberIsViewed = state.getBoolean(STATE_CARD_VIEWED, true)
            updateSpaceSizes(cardNumberIsViewed)
            placementParameters.totalLengthInPixels = frameWidth
            val cardStartMargin: Int
            val dateStartMargin: Int
            val cvcStartMargin: Int
            val holderNameStartMargin: Int
            if (cardNumberIsViewed) {
                cardStartMargin = 0
                dateStartMargin = placementParameters.getDateStartMargin(isFullCard = true)
                cvcStartMargin = placementParameters.getCvcStartMargin(isFullCard = true)
                holderNameStartMargin = placementParameters.getHolderNameStartMargin(isFullCard = true)
            } else {
                cardStartMargin = -1 * placementParameters.hiddenCardWidth
                dateStartMargin = placementParameters.getDateStartMargin(isFullCard = false)
                cvcStartMargin = placementParameters.getCvcStartMargin(isFullCard = false)
               /* holderNameStartMargin = if (holderNameEnabled) {
                    placementParameters.getHolderNameStartMargin(isFullCard = false)
                } else {
                    placementParameters.totalLengthInPixels
                }*/
            }

            updateFieldLayout(
                    view = cardNumberTextInputLayout,
                    width = placementParameters.cardWidth,
                marginStart = cardStartMargin
            )
            updateFieldLayout(
                    view = expiryDateTextInputLayout,
                    width = placementParameters.dateWidth,
                marginStart = dateStartMargin
            )
            updateFieldLayout(
                    view = cvcNumberTextInputLayout,
                    width = placementParameters.cvcWidth,
                marginStart = cvcStartMargin
            )
           /* updateFieldLayout(
                    view = holderNameTextInputLayout,
                    width = placementParameters.holderNameWidth,
                marginStart = holderNameStartMargin
            )*/

            super.onRestoreInstanceState(state.getParcelable(STATE_SUPER_STATE))
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    /**
     * Checks on the horizontal position of a touch event to see if
     * that event needs to be associated with one of the controls even
     * without having actually touched it. This essentially gives a larger
     * touch surface to the controls. We return `null` if the user touches
     * actually inside the widget because no interception is necessary - the touch will
     * naturally give focus to that control, and we don't want to interfere with what
     * Android will naturally do in response to that touch.
     *
     * @param touchX distance in pixels from the start side of this control
     * @return a [TapEditText] that needs to request focus, or `null`
     * if no such request is necessary.
     */
    @VisibleForTesting
    internal fun getFocusRequestOnTouch(touchX: Int): View? {
        //check this as it was constraint.left
        val frameStart :Int= this.frameStart

        return when {
            cardNumberIsViewed -> {
                // Then our view is
                // |CARDVIEW||space||DATEVIEW|

                when {
                    touchX < frameStart + placementParameters.cardWidth -> // Then the card edit view will already handle this touch.
                        null
                    touchX < placementParameters.cardTouchBufferLimit -> // Then we want to act like this was a touch on the card view
                        cardNumberEditText
                    touchX < placementParameters.dateStartPosition -> // Then we act like this was a touch on the date editor.
                        expiryDateEditText
                    else -> // Then the date editor will already handle this touch.
                        null
                }
            }
           holderNameEnabled -> {
                // Our view is
                // |PEEK||space||DATE||space||CVC||space||POSTAL|
                when {
                    touchX < frameStart + placementParameters.peekCardWidth -> // This was a touch on the card number editor, so we don't need to handle it.
                        null
                    touchX < placementParameters.cardTouchBufferLimit -> // Then we need to act like the user touched the card editor
                        cardNumberEditText
                    touchX < placementParameters.dateStartPosition -> // Then we need to act like this was a touch on the date editor
                        expiryDateEditText
                    touchX < placementParameters.dateStartPosition + placementParameters.dateWidth -> // Just a regular touch on the date editor.
                        null
                    touchX < placementParameters.dateEndTouchBufferLimit -> // We need to act like this was a touch on the date editor
                        expiryDateEditText
                    touchX < placementParameters.cvcStartPosition -> // We need to act like this was a touch on the cvc editor.
                        cvcNumberEditText
                    touchX < placementParameters.cvcStartPosition + placementParameters.cvcWidth -> // Just a regular touch on the cvc editor.
                        null
                    touchX < placementParameters.cvcEndTouchBufferLimit -> // We need to act like this was a touch on the cvc editor.
                        cvcNumberEditText
                    touchX < placementParameters.holderNameStartPosition -> // We need to act like this was a touch on the postal code editor.
                        holderNameEditText
                    else -> null
                }
            }
            else -> {
                // Our view is
                // |PEEK||space||DATE||space||CVC|
                when {
                    touchX < frameStart + placementParameters.peekCardWidth -> // This was a touch on the card number editor, so we don't need to handle it.
                        null
                    touchX < placementParameters.cardTouchBufferLimit -> // Then we need to act like the user touched the card editor
                        cardNumberEditText
                    touchX < placementParameters.dateStartPosition -> // Then we need to act like this was a touch on the date editor
                        expiryDateEditText
                    touchX < placementParameters.dateStartPosition + placementParameters.dateWidth -> // Just a regular touch on the date editor.
                       null
                   touchX < placementParameters.dateEndTouchBufferLimit -> // We need to act like this was a touch on the date editor
                       expiryDateEditText
                    touchX < placementParameters.cvcStartPosition -> // We need to act like this was a touch on the cvc editor.
                        cvcNumberEditText
                    else -> null
                }
            }
        }
    }

    @VisibleForTesting
    internal fun updateSpaceSizes(isCardViewed: Boolean) {
        val frameWidth = frameWidth
        val frameStart = this.frameStart
        if (frameWidth == 0) {
            // This is an invalid view state.
            return
        }

        placementParameters.cardWidth = getDesiredWidthInPixels(
            FULL_SIZING_CARD_TEXT, cardNumberEditText
        )

        placementParameters.dateWidth = getDesiredWidthInPixels(
            FULL_SIZING_DATE_TEXT, expiryDateEditText
        )

        placementParameters.hiddenCardWidth = getDesiredWidthInPixels(
                hiddenCardText, cardNumberEditText
        )

        placementParameters.cvcWidth = getDesiredWidthInPixels(
                cvcPlaceHolder, cvcNumberEditText
        )

        placementParameters.holderNameWidth = getDesiredWidthInPixels(
            FULL_SIZING_HOLDER_NAME_TEXT, holderNameEditText
        )

        placementParameters.peekCardWidth = getDesiredWidthInPixels(
                peekCardText, cardNumberEditText
        )

        placementParameters.updateSpacing(isCardViewed, holderNameEnabled, frameStart, frameWidth)
    }

    private fun updateFieldLayout(view: View, width: Int, marginStart: Int) {
        view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
            this.width = width
            this.marginStart = marginStart
        }
    }

    private fun getDesiredWidthInPixels(text: String, editText: TapTextInput): Int {
        return layoutWidthCalculator.calculate(text, editText.paint)
    }

    private fun initView(attrs: AttributeSet?) {
        attrs?.let { applyAttributes(it) }

        ViewCompat.setAccessibilityDelegate(
                cardNumberEditText,
                object : AccessibilityDelegateCompat() {
                    override fun onInitializeAccessibilityNodeInfo(
                            host: View,
                            info: AccessibilityNodeInfoCompat
                    ) {
                        super.onInitializeAccessibilityNodeInfo(host, info)
                        // Avoid reading out "1234 1234 1234 1234"
                        info.hintText = null
                    }
                })

        cardNumberIsViewed = true

        @ColorInt var errorColorInt = cardNumberEditText.defaultErrorColorInt
        cardBrandView.tintColorInt = cardNumberEditText.hintTextColors.defaultColor
        var cardHintText: String? = null
        if (attrs != null) {
            val a = context.theme.obtainStyledAttributes(
                    attrs,
                R.styleable.CardInputView,
                    0, 0)

            try {
                cardBrandView.tintColorInt = a.getColor(
                    R.styleable.CardInputView_cardTint,
                        cardBrandView.tintColorInt
                )
                errorColorInt = a.getColor(R.styleable.CardInputView_cardTextErrorColor, errorColorInt)
                cardHintText = a.getString(R.styleable.CardInputView_cardHintText)
            } finally {
                a.recycle()
            }
        }

        cardHintText?.let {
            cardNumberEditText.hint = it
        }

        currentFields.forEach { it.setErrorColor(errorColorInt) }

        cardNumberEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollStart()
                cardInputListener?.onFocusChange(FOCUS_CARD)
                expiryDateEditText.visibility = View.GONE
                cvcNumberEditText.visibility = View.GONE
            }
        }

        expiryDateEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollEnd()
                cardInputListener?.onFocusChange(FOCUS_EXPIRY)
            }
        }

        expiryDateEditText.setDeleteEmptyListener(
            BackUpFieldDeleteListener(
                cardNumberEditText
            )
        )
        cvcNumberEditText.setDeleteEmptyListener(
            BackUpFieldDeleteListener(
                expiryDateEditText
            )
        )
        holderNameEditText.setDeleteEmptyListener(
            BackUpFieldDeleteListener(
                cvcNumberEditText
            )
        )

        cvcNumberEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollEnd()
                cardInputListener?.onFocusChange(FOCUS_CVC)
            }
            updateIconCvc(hasFocus, cvcValue)
        }

        cvcNumberEditText.setAfterTextChangedListener(
                object : TapTextInput.AfterTextChangedListener {
                    override fun onTextChanged(text: String) {
                        if (brand.isMaxCvc(text)) {
                            cardInputListener?.onCvcComplete()
                        }
                        updateIconCvc(cvcNumberEditText.hasFocus(), text)
                    }
                }
        )

        cardBrandView.onScanClicked = {
            Toast.makeText(context, "Clicked", Toast.LENGTH_SHORT).show()
        }

        cardNumberEditText.displayErrorCallback = {
            shouldShowErrorIcon = it
        }

        cardNumberEditText.completionCallback = {
            expiryDateEditText.visibility = View.VISIBLE
            cvcNumberEditText.visibility = View.VISIBLE
            scrollEnd()
            cardInputListener?.onCardComplete()
        }

        cardNumberEditText.brandChangeCallback = { brand ->
            hiddenCardText = createHiddenCardText(brand)
            updateIcon()
            cvcNumberEditText.updateBrand(brand)
        }

        expiryDateEditText.completionCallback = {
            cvcNumberEditText.requestFocus()
            cardInputListener?.onExpirationComplete()
        }

        cvcNumberEditText.completionCallback = {
            if (holderNameEnabled) {
                holderNameEditText.requestFocus()
            }
        }

//        allFields.forEach { it.addTextChangedListener(inputChangeTextWatcher) }

        cardNumberEditText.requestFocus()
        ThemeManager.loadTapTheme(context.resources,R.raw.defaulttheme,"lighttheme")
        cardNumberEditText.setHintTextColor(Color.parseColor(ThemeManager.getValue("emailCard.textFields.placeHolderColor")))
        cardNumberEditText.setTextColor(Color.parseColor(ThemeManager.getValue("emailCard.textFields.textColor")))
        expiryDateEditText.setHintTextColor(Color.parseColor(ThemeManager.getValue("emailCard.textFields.placeHolderColor")))
        expiryDateEditText.setTextColor(Color.parseColor(ThemeManager.getValue("emailCard.textFields.textColor")))
        cvcNumberEditText.setHintTextColor(Color.parseColor(ThemeManager.getValue("emailCard.textFields.placeHolderColor")))
        cvcNumberEditText.setTextColor(Color.parseColor(ThemeManager.getValue("emailCard.textFields.textColor")))
        holderNameEditText.setHintTextColor(Color.parseColor(ThemeManager.getValue("emailCard.textFields.placeHolderColor")))
        holderNameEditText.setTextColor(Color.parseColor(ThemeManager.getValue("emailCard.textFields.textColor")))

    }

    /**
     * @return a [String] that is the length of a full card number for the current [brand],
     * without the last group of digits when the card is formatted with spaces. This is used for
     * measuring the rendered width of the hidden portion (i.e. when the card number is "peeking")
     * and does not have to be a valid card number.
     *
     * e.g. if [brand] is [CardBrand.Visa], this will generate `"0000 0000 0000 "` (including the
     * trailing space).
     *
     * This should only be called when [brand] changes.
     */
    @VisibleForTesting
    internal fun createHiddenCardText(
        brand: CardBrand,
        cardNumber: String = cardNumberEditText.fieldText
    ): String {
        var lastIndex = 0
        val digits: MutableList<String> = mutableListOf()

        brand.getSpacePositionsForCardNumber(cardNumber)
                .toList()
                .sorted()
                .forEach {
                    repeat(it - lastIndex) {
                        digits.add("0")
                    }
                    digits.add(" ")
                    lastIndex = it + 1
                }

        return digits.joinToString(separator = "")
    }

    private fun applyAttributes(attrs: AttributeSet) {
        val typedArray = context.theme.obtainStyledAttributes(
                attrs, R.styleable.CardElement, 0, 0
        )

    /*    try {
            holderNameEnabled = typedArray.getBoolean(
                R.styleable.CardElement_shouldShowHolderName,
                BaseCardInput.DEFAULT_HOLDER_NAME_ENABLED
            )
            holderNameRequired = typedArray.getBoolean(
                R.styleable.CardElement_shouldRequireHolderName,
                BaseCardInput.DEFAULT_HOLDER_NAME_REQUIRED
            )
        } finally {
            typedArray.recycle()
        }*/
    }

    // reveal the full card number field
    private fun scrollStart() {
        if (cardNumberIsViewed || !initFlag) {
            return
        }

        val dateStartPosition = placementParameters.getCvcStartMargin(isFullCard = false)
        val cvcStartPosition = placementParameters.getCvcStartMargin(isFullCard = false)
        val holderNameStartPosition = placementParameters.getHolderNameStartMargin(isFullCard = false)

        updateSpaceSizes(isCardViewed = true)

        val slideCardStartAnimation =
            CardNumberSlideStartAnimation(
                view = cardNumberTextInputLayout
            )

        val dateDestination = placementParameters.getDateStartMargin(isFullCard = true)
        val slideDateStartAnimation =
            ExpiryDateSlideStartAnimation(
                view = expiryDateTextInputLayout,
                startPosition = dateStartPosition,
                destination = dateDestination
            )

        val cvcDestination = cvcStartPosition + (dateDestination - dateStartPosition)
        val slideCvcStartAnimation =
            CvcSlideStartAnimation(
                view = cvcNumberTextInputLayout,
                startPosition = cvcStartPosition,
                destination = cvcDestination,
                newWidth = placementParameters.cvcWidth
            )

        val holderNameDestination = holderNameStartPosition + (cvcDestination - cvcStartPosition)
       /* val slideHolderNameStartAnimation = if (holderNameEnabled) {
            HolderNameSlideStartAnimation(
                view = holderNameTextInputLayout,
                startPosition = holderNameStartPosition,
                destination = holderNameDestination,
                newWidth = placementParameters.holderNameWidth
            )
        } else {
            null
        }
*/
        startSlideAnimation(listOfNotNull(
                slideCardStartAnimation,
                slideDateStartAnimation,
                slideCvcStartAnimation
              //  slideHolderNameStartAnimation
        ))

        cardNumberIsViewed = true
    }

    // reveal the secondary fields
    private fun scrollEnd() {
        if (!cardNumberIsViewed || !initFlag) {
            return
        }

        val dateStartMargin = placementParameters.getDateStartMargin(isFullCard = true)

        updateSpaceSizes(isCardViewed = false)

        val slideCardEndAnimation =
            CardNumberSlideEndAnimation(
                view = cardNumberTextInputLayout,
                hiddenCardWidth = placementParameters.hiddenCardWidth,
                focusOnEndView = expiryDateEditText
            )

        val dateDestination = placementParameters.getDateStartMargin(isFullCard = false)
        val slideDateEndAnimation =
            ExpiryDateSlideEndAnimation(
                view = expiryDateTextInputLayout,
                startMargin = dateStartMargin,
                destination = dateDestination
            )

        val cvcDestination = placementParameters.getCvcStartMargin(isFullCard = false)
        val cvcStartMargin = cvcDestination + (dateStartMargin - dateDestination)
        val slideCvcEndAnimation =
            CvcSlideEndAnimation(
                view = cvcNumberTextInputLayout,
                startMargin = cvcStartMargin,
                destination = cvcDestination,
                newWidth = placementParameters.cvcWidth
            )

        val holderNameDestination = placementParameters.getHolderNameStartMargin(isFullCard = false)
        val holderNameStartMargin = holderNameDestination

        val slideHolderNameEndAnimation = if (holderNameEnabled) {
            HolderNameSlideEndAnimation(
                view = holderNameTextInputLayout,
                startMargin = holderNameStartMargin,
                destination = holderNameDestination,
                newWidth = placementParameters.holderNameWidth
            )
        } else {
            null
        }


        startSlideAnimation(listOfNotNull(
                slideCardEndAnimation,
                slideDateEndAnimation,
                slideCvcEndAnimation
               // slideHolderNameEndAnimation
        ))

        cardNumberIsViewed = false
    }

    private fun startSlideAnimation(animations: List<Animation>) {
        val animationSet = AnimationSet(true).apply {
            animations.forEach { addAnimation(it) }
        }
        containerLayout.startAnimation(animationSet)
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus && CardBrand.Unknown == brand) {
            cardBrandView.applyTint(false)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (!initFlag && width != 0) {
            initFlag = true
            placementParameters.totalLengthInPixels = frameWidth

            updateSpaceSizes(cardNumberIsViewed)

            updateFieldLayout(
                    view = cardNumberTextInputLayout,
                    width = placementParameters.cardWidth,
                    marginStart  = if (cardNumberIsViewed) {
                        0
                    } else {
                        -1 * placementParameters.hiddenCardWidth
                    }
            )

            updateFieldLayout(
                    view = expiryDateTextInputLayout,
                    width = placementParameters.dateWidth,
                    marginStart = placementParameters.getDateStartMargin(cardNumberIsViewed)
            )

            updateFieldLayout(
                    view = cvcNumberTextInputLayout,
                    width = placementParameters.cvcWidth,
                    marginStart = placementParameters.getCvcStartMargin(cardNumberIsViewed)
            )

          /*  updateFieldLayout(
                    view = holderNameTextInputLayout,
                    width = placementParameters.holderNameWidth,
                    marginStart = placementParameters.getHolderNameStartMargin(cardNumberIsViewed)
            )*/
        }
    }

    private var hiddenCardText: String = createHiddenCardText(brand)

    private val cvcPlaceHolder: String
        get() {
            return if (CardBrand.AmericanExpress == brand) {
                CVC_PLACEHOLDER_AMEX
            } else {
                CVC_PLACEHOLDER_COMMON
            }
        }

    private val peekCardText: String
        get() {
            return when (brand) {
                CardBrand.AmericanExpress -> PEEK_TEXT_AMEX
                CardBrand.DinersClub -> PEEK_TEXT_DINERS_14
                else -> PEEK_TEXT_COMMON
            }
        }

    private fun updateIcon() {
        if (!shouldChangeIcon) return
        cardBrandView.showBrandIcon(brand, shouldShowErrorIcon)
    }

    private fun updateIconCvc(
            hasFocus: Boolean,
            cvcText: String?
    ) {
        when {
            shouldShowErrorIcon -> {
                updateIcon()
            }
            shouldIconShowBrand(
                brand,
                hasFocus,
                cvcText
            ) -> {
                updateIcon()
            }
            else -> {
                updateIconForCvcEntry()
            }
        }
    }

    private fun updateIconForCvcEntry() {
        cardBrandView.showCvcIcon(brand)
    }

    /**
     * A class for tracking the placement and layout of fields
     */
    internal class PlacementParameters {
        internal var totalLengthInPixels: Int = 0

        internal var cardWidth: Int = 0
        internal var hiddenCardWidth: Int = 0
        internal var peekCardWidth: Int = 0
        internal var cardDateSeparation: Int = 0
        internal var dateWidth: Int = 0
        internal var dateCvcSeparation: Int = 0
        internal var cvcWidth: Int = 0
        internal var cvcHolderNameSeparation: Int = 0
        internal var holderNameWidth: Int = 0

        internal var cardTouchBufferLimit: Int = 0
        internal var dateStartPosition: Int = 0
        internal var dateEndTouchBufferLimit: Int = 0
        internal var cvcStartPosition: Int = 0
        internal var cvcEndTouchBufferLimit: Int = 0
        internal var holderNameStartPosition: Int = 0

        private val cardPeekDateStartMargin: Int
            @JvmSynthetic
            get() {
                return peekCardWidth + cardDateSeparation
            }

        private val cardPeekCvcStartMargin: Int
            @JvmSynthetic
            get() {
                return cardPeekDateStartMargin + dateWidth + dateCvcSeparation
            }

        internal val cardPeekHolderNameStartMargin: Int
            @JvmSynthetic
            get() {
                return cardPeekCvcStartMargin + holderNameWidth + cvcHolderNameSeparation
            }

        @JvmSynthetic
        internal fun getDateStartMargin(isFullCard: Boolean): Int {
            return if (isFullCard) {
                cardWidth + cardDateSeparation
            } else {
                cardPeekDateStartMargin
            }
        }

        @JvmSynthetic
        internal fun getCvcStartMargin(isFullCard: Boolean): Int {
            return if (isFullCard) {
                totalLengthInPixels
            } else {
                cardPeekCvcStartMargin
            }
        }

        @JvmSynthetic
        internal fun getHolderNameStartMargin(isFullCard: Boolean): Int {
            return if (isFullCard) {
                totalLengthInPixels
            } else {
                cardPeekHolderNameStartMargin
            }
        }

        @JvmSynthetic
        internal fun updateSpacing(
            isCardViewed: Boolean,
            holderNameEnabled: Boolean,
            frameStart: Int,
            frameWidth: Int
        ) {
            when {
                isCardViewed -> {
                    cardDateSeparation = frameWidth - cardWidth - dateWidth
                    cardTouchBufferLimit = frameStart + cardWidth + cardDateSeparation / 2
                    dateStartPosition = frameStart + cardWidth + cardDateSeparation
                }
           /*     holderNameEnabled -> {
                    this.cardDateSeparation = (frameWidth * 4 / 16) - peekCardWidth - dateWidth / 4
                    this.dateCvcSeparation = (frameWidth * 5 / 9) - peekCardWidth - cardDateSeparation -
                            dateWidth - cvcWidth
                    this.cvcHolderNameSeparation = (frameWidth * 5 / 7) - peekCardWidth - cardDateSeparation -
                            dateWidth - cvcWidth - dateCvcSeparation - holderNameWidth

                    val dateStartPosition = frameStart + peekCardWidth + cardDateSeparation
                    this.cardTouchBufferLimit = dateStartPosition / 3
                    this.dateStartPosition = dateStartPosition

                    val cvcStartPosition = dateStartPosition + dateWidth + dateCvcSeparation
                    this.dateEndTouchBufferLimit = cvcStartPosition / 3
                    this.cvcStartPosition = cvcStartPosition

                    val holderNameStartPosition = cvcStartPosition + cvcWidth + cvcHolderNameSeparation
                    this.cvcEndTouchBufferLimit = holderNameStartPosition / 3
                    this.holderNameStartPosition = holderNameStartPosition
                }*/
                else -> {
                    this.cardDateSeparation = frameWidth / 2 - peekCardWidth - dateWidth / 2
                    this.dateCvcSeparation = frameWidth - peekCardWidth - cardDateSeparation -
                            dateWidth - cvcWidth - 60

                    this.cardTouchBufferLimit = frameStart + peekCardWidth + cardDateSeparation / 2
                    this.dateStartPosition = frameStart + peekCardWidth + cardDateSeparation

                    this.dateEndTouchBufferLimit = dateStartPosition + dateWidth + dateCvcSeparation / 2
                    this.cvcStartPosition = dateStartPosition + dateWidth + dateCvcSeparation
                }
            }
        }

        override fun toString(): String {
            val touchBufferData = """
                Touch Buffer Data:
                CardTouchBufferLimit = $cardTouchBufferLimit
                DateStartPosition = $dateStartPosition
                DateEndTouchBufferLimit = $dateEndTouchBufferLimit
                CvcStartPosition = $cvcStartPosition
                CvcEndTouchBufferLimit = $cvcEndTouchBufferLimit
                HolderNameStartPosition = $holderNameStartPosition
                """

            val elementSizeData = """
                TotalLengthInPixels = $totalLengthInPixels
                CardWidth = $cardWidth
                HiddenCardWidth = $hiddenCardWidth
                PeekCardWidth = $peekCardWidth
                CardDateSeparation = $cardDateSeparation
                DateWidth = $dateWidth
                DateCvcSeparation = $dateCvcSeparation
                CvcWidth = $cvcWidth
                CvcHolderNameSeparation = $cvcHolderNameSeparation
                HolderNameWidth: $holderNameWidth
                """

            return elementSizeData + touchBufferData
        }
    }

    private abstract class CardFieldAnimation : Animation() {
        init {
            duration =
                ANIMATION_LENGTH
        }

        private companion object {
            private const val ANIMATION_LENGTH = 150L
        }
    }

    private class CardNumberSlideStartAnimation(
            private val view: View
    ) : CardFieldAnimation() {
        init {
            setAnimationListener(object : AnimationEndListener() {
                override fun onAnimationEnd(animation: Animation) {
                    view.requestFocus()
                }
            })
        }

        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            println("view latout"+view.layoutParams)
            if(view.layoutParams is FrameLayout.LayoutParams){
                view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                    marginStart = (marginStart * (1 - interpolatedTime)).toInt()
                }
            }else
            view.layoutParams = (view.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = (marginStart * (1 - interpolatedTime)).toInt()
            }
        }
    }

    private class ExpiryDateSlideStartAnimation(
            private val view: View,
            private val startPosition: Int,
            private val destination: Int
    ) : CardFieldAnimation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                marginStart =
                        (interpolatedTime * destination + (1 - interpolatedTime) * startPosition).toInt()
            }
        }
    }

    private class CvcSlideStartAnimation(
            private val view: View,
            private val startPosition: Int,
            private val destination: Int,
            private val newWidth: Int
    ) : CardFieldAnimation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                this.marginStart = (interpolatedTime * destination + (1 - interpolatedTime) * startPosition).toInt()
                this.marginEnd = 0
                this.width = newWidth
            }
        }
    }

    private class HolderNameSlideStartAnimation(
            private val view: View,
            private val startPosition: Int,
            private val destination: Int,
            private val newWidth: Int
    ) : CardFieldAnimation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                this.marginStart =
                        (interpolatedTime * destination + (1 - interpolatedTime) * startPosition).toInt()
                this.marginEnd = 0
                this.width = newWidth
            }
        }
    }

    private class CardNumberSlideEndAnimation(
            private val view: View,
            private val hiddenCardWidth: Int,
            private val focusOnEndView: View
    ) : CardFieldAnimation() {
        init {
            setAnimationListener(object : AnimationEndListener() {
                override fun onAnimationEnd(animation: Animation) {
                    focusOnEndView.requestFocus()
                }
            })
        }

        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                marginStart = (-1f * hiddenCardWidth.toFloat() * interpolatedTime).toInt()
            }
        }
    }

    private class ExpiryDateSlideEndAnimation(
            private val view: View,
            private val startMargin: Int,
            private val destination: Int
    ) : CardFieldAnimation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                marginStart =
                        (interpolatedTime * destination + (1 - interpolatedTime) * startMargin).toInt()
            }
        }
    }

    private class CvcSlideEndAnimation(
            private val view: View,
            private val startMargin: Int,
            private val destination: Int,
            private val newWidth: Int
    ) : CardFieldAnimation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                marginStart =
                        (interpolatedTime * destination + (1 - interpolatedTime) * startMargin).toInt()
                marginEnd = 0
                width = newWidth
            }
        }
    }

    private class HolderNameSlideEndAnimation(
            private val view: View,
            private val startMargin: Int,
            private val destination: Int,
            private val newWidth: Int
    ) : CardFieldAnimation() {

        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as LinearLayout.LayoutParams).apply {
                this.marginStart =
                        (interpolatedTime * destination + (1 - interpolatedTime) * startMargin).toInt()
                this.marginEnd = 0
                this.width = newWidth
            }
        }
    }

    /**
     * A convenience class for when we only want to listen for when an animation ends.
     */
    private abstract class AnimationEndListener : Animation.AnimationListener {
        override fun onAnimationStart(animation: Animation) {
            // Intentional No-op
        }

        override fun onAnimationRepeat(animation: Animation) {
            // Intentional No-op
        }
    }

    internal interface LayoutWidthCalculator {
        fun calculate(text: String, paint: TextPaint): Int
    }

    internal class DefaultLayoutWidthCalculator :
        LayoutWidthCalculator {
        override fun calculate(text: String, paint: TextPaint): Int {
            return Layout.getDesiredWidth(text, paint).toInt()
        }
    }

    internal companion object {
        internal const val LOGGING_TOKEN = "CardInputView"

        private const val PEEK_TEXT_COMMON = "4242"
        private const val PEEK_TEXT_DINERS_14 = "88"
        private const val PEEK_TEXT_AMEX = "34343"

        private const val CVC_PLACEHOLDER_COMMON = "CVC"
        private const val CVC_PLACEHOLDER_AMEX = "2345"

        private const val FULL_SIZING_CARD_TEXT = "4242 4242 4242 4242"
        private const val FULL_SIZING_DATE_TEXT = "MM/MM"
        private const val FULL_SIZING_HOLDER_NAME_TEXT = "Holder Name..."

        private const val STATE_CARD_VIEWED = "state_card_viewed"
        private const val STATE_SUPER_STATE = "state_super_state"
        private const val STATE_POSTAL_CODE_ENABLED = "state_postal_code_enabled"

        // This value is used to ensure that onSaveInstanceState is called
        // in the event that the user doesn't give this control an ID.
        @IdRes
        private val DEFAULT_READER_ID =
            R.id.default_reader_id

        /**
         * Determines whether or not the icon should show the card brand instead of the
         * CVC helper icon.
         *
         * @param brand the [CardBrand] of the card number
         * @param cvcHasFocus `true` if the CVC entry field has focus, `false` otherwise
         * @param cvcText the current content of [cvcNumberEditText]
         *
         * @return `true` if we should show the brand of the card, or `false` if we
         * should show the CVC helper icon instead
         */
        @VisibleForTesting
        internal fun shouldIconShowBrand(
            brand: CardBrand,
            cvcHasFocus: Boolean,
            cvcText: String?
        ): Boolean {
            return !cvcHasFocus || brand.isMaxCvc(cvcText)
        }
    }
}
