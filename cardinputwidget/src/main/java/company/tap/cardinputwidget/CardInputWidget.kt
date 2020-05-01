package company.tap.cardinputwidget

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.Layout
import android.text.TextPaint
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.Transformation
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.IntRange
import androidx.annotation.VisibleForTesting
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import company.tap.cardinputwidget.CardInputListener.FocusField.Companion.FOCUS_CARD
import company.tap.cardinputwidget.CardInputListener.FocusField.Companion.FOCUS_CVC
import company.tap.cardinputwidget.CardInputListener.FocusField.Companion.FOCUS_EXPIRY
import company.tap.cardinputwidget.databinding.CardInputWidgetBinding
import kotlin.properties.Delegates

/**
 * A card input widget that handles all animation on its own.
 *
 * The individual `EditText` views of this widget can be styled by defining a style
 * `Tap.CardInputWidget.EditText` that extends `Tap.Base.CardInputWidget.EditText`.
 */
class CardInputWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), CardWidget {
    private val viewBinding = CardInputWidgetBinding.inflate(
            LayoutInflater.from(context),
            this
    )

    private val containerLayout = viewBinding.container

    @JvmSynthetic
    internal val cardBrandView = viewBinding.cardBrandView

    private val cardNumberTextInputLayout = viewBinding.cardNumberTextInputLayout
    private val expiryDateTextInputLayout = viewBinding.expiryDateTextInputLayout
    private val cvcNumberTextInputLayout = viewBinding.cvcTextInputLayout
    internal val postalCodeTextInputLayout = viewBinding.postalCodeTextInputLayout

    @JvmSynthetic
    internal val cardNumberEditText = viewBinding.cardNumberEditText

    @JvmSynthetic
    internal val expiryDateEditText = viewBinding.expiryDateEditText

    @JvmSynthetic
    internal val cvcNumberEditText = viewBinding.cvcEditText

    @JvmSynthetic
    internal val postalCodeEditText = viewBinding.postalCodeEditText

    private var cardInputListener: CardInputListener? = null
    private var cardValidCallback: CardValidCallback? = null
    private val cardValidTextWatcher = object : TapTextWatcher() {
        override fun afterTextChanged(s: Editable?) {
            super.afterTextChanged(s)
            cardValidCallback?.onInputChanged(invalidFields.isEmpty(), invalidFields)
        }
    }
    private val inputChangeTextWatcher = object : TapTextWatcher() {
        override fun afterTextChanged(s: Editable?) {
            super.afterTextChanged(s)
            shouldShowErrorIcon = false
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
    internal var layoutWidthCalculator: LayoutWidthCalculator = DefaultLayoutWidthCalculator()

    internal val placementParameters: PlacementParameters = PlacementParameters()

    private val postalCodeValue: String?
        get() {
            return if (postalCodeEnabled) {
                postalCodeEditText.postalCode
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

    @VisibleForTesting
    @JvmSynthetic
    internal val requiredFields: List<TapEditText>
    private val allFields: List<TapEditText>

    /**
     * The [TapEditText] fields that are currently enabled and active in the UI.
     */
    @VisibleForTesting
    internal val currentFields: List<TapEditText>
        @JvmSynthetic
        get() {
            return requiredFields
                    .plus(postalCodeEditText.takeIf { postalCodeEnabled })
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
            postalCodeEditText.shouldShowError =
                    postalCodeRequired &&
                            postalCodeEditText.postalCode.isNullOrBlank()

            // Announce error messages for accessibility
            currentFields
                    .filter { it.shouldShowError }
                    .forEach { editText ->
                        editText.errorMessage?.let { errorMessage ->
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
                }
                postalCodeEditText.shouldShowError -> {
                    postalCodeEditText.requestFocus()
                }
                else -> {
                    shouldShowErrorIcon = false
                    return Card.Builder(cardNumber, cardDate.first, cardDate.second, cvcValue)
                            .addressZip(postalCodeValue)
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
    var postalCodeEnabled: Boolean by Delegates.observable(
        CardWidget.DEFAULT_POSTAL_CODE_ENABLED
    ) { _, _, isEnabled ->
        if (isEnabled) {
            postalCodeEditText.isEnabled = true
            postalCodeTextInputLayout.visibility = View.VISIBLE

            cvcNumberEditText.imeOptions = EditorInfo.IME_ACTION_NEXT
        } else {
            postalCodeEditText.isEnabled = false
            postalCodeTextInputLayout.visibility = View.GONE

            cvcNumberEditText.imeOptions = EditorInfo.IME_ACTION_DONE
        }
    }

    /**
     * If [postalCodeEnabled] is true and [postalCodeRequired] is true, then postal code is a
     * required field.
     *
     * If [postalCodeEnabled] is false, this value is ignored.
     *
     * Note that some countries do not have postal codes, so requiring postal code will prevent
     * those users from submitting this form successfully.
     */
    var postalCodeRequired: Boolean = CardWidget.DEFAULT_POSTAL_CODE_REQUIRED


    init {
        // This ensures that onRestoreInstanceState is called
        // during rotations.
        if (id == View.NO_ID) {
            id = DEFAULT_READER_ID
        }

        orientation = HORIZONTAL
        minimumWidth = resources.getDimensionPixelSize(R.dimen.card_widget_min_width)

        frameWidthSupplier = { containerLayout.width }

        requiredFields = listOf(
                cardNumberEditText, cvcNumberEditText, expiryDateEditText
        )
        allFields = requiredFields.plus(postalCodeEditText)

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
        cvcNumberEditText.setText(cvcCode)
    }

    @JvmSynthetic
    internal fun setPostalCode(postalCode: String?) {
        postalCodeEditText.setText(postalCode)
    }

    /**
     * Clear all text fields in the CardInputWidget.
     */
    override fun clear() {
        if (currentFields.any { it.hasFocus() } || this.hasFocus()) {
            cardNumberEditText.requestFocus()
        }

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
     * Set a `TextWatcher` to receive expiration date changes.
     */
    override fun setExpiryDateTextWatcher(expiryDateTextWatcher: TextWatcher?) {
        expiryDateEditText.addTextChangedListener(expiryDateTextWatcher)
    }

    /**
     * Set a `TextWatcher` to receive CVC value changes.
     */
    override fun setCvcNumberTextWatcher(cvcNumberTextWatcher: TextWatcher?) {
        cvcNumberEditText.addTextChangedListener(cvcNumberTextWatcher)
    }

    /**
     * Set a `TextWatcher` to receive postal code changes.
     */
    override fun setPostalCodeTextWatcher(postalCodeTextWatcher: TextWatcher?) {
        postalCodeEditText.addTextChangedListener(postalCodeTextWatcher)
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
            putBoolean(STATE_POSTAL_CODE_ENABLED, postalCodeEnabled)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            postalCodeEnabled = state.getBoolean(STATE_POSTAL_CODE_ENABLED, true)
            cardNumberIsViewed = state.getBoolean(STATE_CARD_VIEWED, true)
            updateSpaceSizes(cardNumberIsViewed)
            placementParameters.totalLengthInPixels = frameWidth
            val cardLeftMargin: Int
            val dateLeftMargin: Int
            val cvcLeftMargin: Int
            val postalCodeLeftMargin: Int
            if (cardNumberIsViewed) {
                cardLeftMargin = 0
                dateLeftMargin = placementParameters.getDateLeftMargin(isFullCard = true)
                cvcLeftMargin = placementParameters.getCvcLeftMargin(isFullCard = true)
                postalCodeLeftMargin = placementParameters.getPostalCodeLeftMargin(isFullCard = true)
            } else {
                cardLeftMargin = -1 * placementParameters.hiddenCardWidth
                dateLeftMargin = placementParameters.getDateLeftMargin(isFullCard = false)
                cvcLeftMargin = placementParameters.getCvcLeftMargin(isFullCard = false)
                postalCodeLeftMargin = if (postalCodeEnabled) {
                    placementParameters.getPostalCodeLeftMargin(isFullCard = false)
                } else {
                    placementParameters.totalLengthInPixels
                }
            }

            updateFieldLayout(
                    view = cardNumberTextInputLayout,
                    width = placementParameters.cardWidth,
                    leftMargin = cardLeftMargin
            )
            updateFieldLayout(
                    view = expiryDateTextInputLayout,
                    width = placementParameters.dateWidth,
                    leftMargin = dateLeftMargin
            )
            updateFieldLayout(
                    view = cvcNumberTextInputLayout,
                    width = placementParameters.cvcWidth,
                    leftMargin = cvcLeftMargin
            )
            updateFieldLayout(
                    view = postalCodeTextInputLayout,
                    width = placementParameters.postalCodeWidth,
                    leftMargin = postalCodeLeftMargin
            )

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
     * @param touchX distance in pixels from the left side of this control
     * @return a [TapEditText] that needs to request focus, or `null`
     * if no such request is necessary.
     */
    @VisibleForTesting
    internal fun getFocusRequestOnTouch(touchX: Int): View? {
        val frameStart = containerLayout.left

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
            postalCodeEnabled -> {
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
                    touchX < placementParameters.dateRightTouchBufferLimit -> // We need to act like this was a touch on the date editor
                        expiryDateEditText
                    touchX < placementParameters.cvcStartPosition -> // We need to act like this was a touch on the cvc editor.
                        cvcNumberEditText
                    touchX < placementParameters.cvcStartPosition + placementParameters.cvcWidth -> // Just a regular touch on the cvc editor.
                        null
                    touchX < placementParameters.cvcRightTouchBufferLimit -> // We need to act like this was a touch on the cvc editor.
                        cvcNumberEditText
                    touchX < placementParameters.postalCodeStartPosition -> // We need to act like this was a touch on the postal code editor.
                        postalCodeEditText
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
                    touchX < placementParameters.dateRightTouchBufferLimit -> // We need to act like this was a touch on the date editor
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
        val frameStart = containerLayout.left
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

        placementParameters.postalCodeWidth = getDesiredWidthInPixels(
                FULL_SIZING_HOLDER_NAME_TEXT, postalCodeEditText
        )

        placementParameters.peekCardWidth = getDesiredWidthInPixels(
                peekCardText, cardNumberEditText
        )

        placementParameters.updateSpacing(isCardViewed, postalCodeEnabled, frameStart, frameWidth)
    }

    private fun updateFieldLayout(view: View, width: Int, leftMargin: Int) {
        view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
            this.width = width
            this.leftMargin = leftMargin
        }
    }

    private fun getDesiredWidthInPixels(text: String, editText: TapEditText): Int {
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
                scrollLeft()
                cardInputListener?.onFocusChange(FOCUS_CARD)
            }
        }

        expiryDateEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollRight()
                cardInputListener?.onFocusChange(FOCUS_EXPIRY)
            }
        }

        expiryDateEditText.setDeleteEmptyListener(BackUpFieldDeleteListener(cardNumberEditText))
        cvcNumberEditText.setDeleteEmptyListener(BackUpFieldDeleteListener(expiryDateEditText))
        postalCodeEditText.setDeleteEmptyListener(BackUpFieldDeleteListener(cvcNumberEditText))

        cvcNumberEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollRight()
                cardInputListener?.onFocusChange(FOCUS_CVC)
            }
            updateIconCvc(hasFocus, cvcValue)
        }

        cvcNumberEditText.setAfterTextChangedListener(
                object : TapEditText.AfterTextChangedListener {
                    override fun onTextChanged(text: String) {
                        if (brand.isMaxCvc(text)) {
                            cardInputListener?.onCvcComplete()
                        }
                        updateIconCvc(cvcNumberEditText.hasFocus(), text)
                    }
                }
        )

        cardNumberEditText.completionCallback = {
            scrollRight()
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
            if (postalCodeEnabled) {
                postalCodeEditText.requestFocus()
            }
        }

        allFields.forEach { it.addTextChangedListener(inputChangeTextWatcher) }

        cardNumberEditText.requestFocus()
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

        try {
            postalCodeEnabled = typedArray.getBoolean(
                    R.styleable.CardElement_shouldShowPostalCode,
                CardWidget.DEFAULT_POSTAL_CODE_ENABLED
            )
            postalCodeRequired = typedArray.getBoolean(
                    R.styleable.CardElement_shouldRequirePostalCode,
                CardWidget.DEFAULT_POSTAL_CODE_REQUIRED
            )
        } finally {
            typedArray.recycle()
        }
    }

    // reveal the full card number field
    private fun scrollLeft() {
        if (cardNumberIsViewed || !initFlag) {
            return
        }

        val dateStartPosition = placementParameters.getDateLeftMargin(isFullCard = false)
        val cvcStartPosition = placementParameters.getCvcLeftMargin(isFullCard = false)
        val postalCodeStartPosition = placementParameters.getPostalCodeLeftMargin(isFullCard = false)

        updateSpaceSizes(isCardViewed = true)

        val slideCardLeftAnimation = CardNumberSlideLeftAnimation(
                view = cardNumberTextInputLayout
        )

        val dateDestination = placementParameters.getDateLeftMargin(isFullCard = true)
        val slideDateLeftAnimation = ExpiryDateSlideLeftAnimation(
                view = expiryDateTextInputLayout,
                startPosition = dateStartPosition,
                destination = dateDestination
        )

        val cvcDestination = cvcStartPosition + (dateDestination - dateStartPosition)
        val slideCvcLeftAnimation = CvcSlideLeftAnimation(
                view = cvcNumberTextInputLayout,
                startPosition = cvcStartPosition,
                destination = cvcDestination,
                newWidth = placementParameters.cvcWidth
        )

        val postalCodeDestination = postalCodeStartPosition + (cvcDestination - cvcStartPosition)
        val slidePostalCodeLeftAnimation = if (postalCodeEnabled) {
            PostalCodeSlideLeftAnimation(
                    view = postalCodeTextInputLayout,
                    startPosition = postalCodeStartPosition,
                    destination = postalCodeDestination,
                    newWidth = placementParameters.postalCodeWidth
            )
        } else {
            null
        }

        startSlideAnimation(listOfNotNull(
                slideCardLeftAnimation,
                slideDateLeftAnimation,
                slideCvcLeftAnimation,
                slidePostalCodeLeftAnimation
        ))

        cardNumberIsViewed = true
    }

    // reveal the secondary fields
    private fun scrollRight() {
        if (!cardNumberIsViewed || !initFlag) {
            return
        }

        val dateStartMargin = placementParameters.getDateLeftMargin(isFullCard = true)

        updateSpaceSizes(isCardViewed = false)

        val slideCardRightAnimation = CardNumberSlideRightAnimation(
                view = cardNumberTextInputLayout,
                hiddenCardWidth = placementParameters.hiddenCardWidth,
                focusOnEndView = expiryDateEditText
        )

        val dateDestination = placementParameters.getDateLeftMargin(isFullCard = false)
        val slideDateRightAnimation = ExpiryDateSlideRightAnimation(
                view = expiryDateTextInputLayout,
                startMargin = dateStartMargin,
                destination = dateDestination
        )

        val cvcDestination = placementParameters.getCvcLeftMargin(isFullCard = false)
        val cvcStartMargin = cvcDestination + (dateStartMargin - dateDestination)
        val slideCvcRightAnimation = CvcSlideRightAnimation(
                view = cvcNumberTextInputLayout,
                startMargin = cvcStartMargin,
                destination = cvcDestination,
                newWidth = placementParameters.cvcWidth
        )

        val postalCodeDestination = placementParameters.getPostalCodeLeftMargin(isFullCard = false)
        val postalCodeStartMargin = postalCodeDestination + (cvcStartMargin - cvcDestination)
        val slidePostalCodeRightAnimation = if (postalCodeEnabled) {
            PostalCodeSlideRightAnimation(
                    view = postalCodeTextInputLayout,
                    startMargin = postalCodeStartMargin,
                    destination = postalCodeDestination,
                    newWidth = placementParameters.postalCodeWidth
            )
        } else {
            null
        }

        startSlideAnimation(listOfNotNull(
                slideCardRightAnimation,
                slideDateRightAnimation,
                slideCvcRightAnimation,
                slidePostalCodeRightAnimation
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
            cardBrandView.applyTint()
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
                    leftMargin = if (cardNumberIsViewed) {
                        0
                    } else {
                        -1 * placementParameters.hiddenCardWidth
                    }
            )

            updateFieldLayout(
                    view = expiryDateTextInputLayout,
                    width = placementParameters.dateWidth,
                    leftMargin = placementParameters.getDateLeftMargin(cardNumberIsViewed)
            )

            updateFieldLayout(
                    view = cvcNumberTextInputLayout,
                    width = placementParameters.cvcWidth,
                    leftMargin = placementParameters.getCvcLeftMargin(cardNumberIsViewed)
            )

            updateFieldLayout(
                    view = postalCodeTextInputLayout,
                    width = placementParameters.postalCodeWidth,
                    leftMargin = placementParameters.getPostalCodeLeftMargin(cardNumberIsViewed)
            )
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
            shouldIconShowBrand(brand, hasFocus, cvcText) -> {
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
        internal var cvcPostalCodeSeparation: Int = 0
        internal var postalCodeWidth: Int = 0

        internal var cardTouchBufferLimit: Int = 0
        internal var dateStartPosition: Int = 0
        internal var dateRightTouchBufferLimit: Int = 0
        internal var cvcStartPosition: Int = 0
        internal var cvcRightTouchBufferLimit: Int = 0
        internal var postalCodeStartPosition: Int = 0

        private val cardPeekDateLeftMargin: Int
            @JvmSynthetic
            get() {
                return peekCardWidth + cardDateSeparation
            }

        private val cardPeekCvcLeftMargin: Int
            @JvmSynthetic
            get() {
                return cardPeekDateLeftMargin + dateWidth + dateCvcSeparation
            }

        internal val cardPeekPostalCodeLeftMargin: Int
            @JvmSynthetic
            get() {
                return cardPeekCvcLeftMargin + postalCodeWidth + cvcPostalCodeSeparation
            }

        @JvmSynthetic
        internal fun getDateLeftMargin(isFullCard: Boolean): Int {
            return if (isFullCard) {
                cardWidth + cardDateSeparation
            } else {
                cardPeekDateLeftMargin
            }
        }

        @JvmSynthetic
        internal fun getCvcLeftMargin(isFullCard: Boolean): Int {
            return if (isFullCard) {
                totalLengthInPixels
            } else {
                cardPeekCvcLeftMargin
            }
        }

        @JvmSynthetic
        internal fun getPostalCodeLeftMargin(isFullCard: Boolean): Int {
            return if (isFullCard) {
                totalLengthInPixels
            } else {
                cardPeekPostalCodeLeftMargin
            }
        }

        @JvmSynthetic
        internal fun updateSpacing(
                isCardViewed: Boolean,
                postalCodeEnabled: Boolean,
                frameStart: Int,
                frameWidth: Int
        ) {
            when {
                isCardViewed -> {
                    cardDateSeparation = frameWidth - cardWidth - dateWidth
                    cardTouchBufferLimit = frameStart + cardWidth + cardDateSeparation / 2
                    dateStartPosition = frameStart + cardWidth + cardDateSeparation
                }
                postalCodeEnabled -> {
                    this.cardDateSeparation = (frameWidth * 4 / 16) - peekCardWidth - dateWidth / 4
                    this.dateCvcSeparation = (frameWidth * 5 / 9) - peekCardWidth - cardDateSeparation -
                            dateWidth - cvcWidth
                    this.cvcPostalCodeSeparation = (frameWidth * 5 / 7) - peekCardWidth - cardDateSeparation -
                            dateWidth - cvcWidth - dateCvcSeparation - postalCodeWidth

                    val dateStartPosition = frameStart + peekCardWidth + cardDateSeparation
                    this.cardTouchBufferLimit = dateStartPosition / 3
                    this.dateStartPosition = dateStartPosition

                    val cvcStartPosition = dateStartPosition + dateWidth + dateCvcSeparation
                    this.dateRightTouchBufferLimit = cvcStartPosition / 3
                    this.cvcStartPosition = cvcStartPosition

                    val postalCodeStartPosition = cvcStartPosition + cvcWidth + cvcPostalCodeSeparation
                    this.cvcRightTouchBufferLimit = postalCodeStartPosition / 3
                    this.postalCodeStartPosition = postalCodeStartPosition
                }
                else -> {
                    this.cardDateSeparation = frameWidth / 2 - peekCardWidth - dateWidth / 2
                    this.dateCvcSeparation = frameWidth - peekCardWidth - cardDateSeparation -
                            dateWidth - cvcWidth

                    this.cardTouchBufferLimit = frameStart + peekCardWidth + cardDateSeparation / 2
                    this.dateStartPosition = frameStart + peekCardWidth + cardDateSeparation

                    this.dateRightTouchBufferLimit = dateStartPosition + dateWidth + dateCvcSeparation / 2
                    this.cvcStartPosition = dateStartPosition + dateWidth + dateCvcSeparation
                }
            }
        }

        override fun toString(): String {
            val touchBufferData = """
                Touch Buffer Data:
                CardTouchBufferLimit = $cardTouchBufferLimit
                DateStartPosition = $dateStartPosition
                DateRightTouchBufferLimit = $dateRightTouchBufferLimit
                CvcStartPosition = $cvcStartPosition
                CvcRightTouchBufferLimit = $cvcRightTouchBufferLimit
                PostalCodeStartPosition = $postalCodeStartPosition
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
                CvcPostalCodeSeparation = $cvcPostalCodeSeparation
                PostalCodeWidth: $postalCodeWidth
                """

            return elementSizeData + touchBufferData
        }
    }

    private abstract class CardFieldAnimation : Animation() {
        init {
            duration = ANIMATION_LENGTH
        }

        private companion object {
            private const val ANIMATION_LENGTH = 150L
        }
    }

    private class CardNumberSlideLeftAnimation(
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
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin = (leftMargin * (1 - interpolatedTime)).toInt()
            }
        }
    }

    private class ExpiryDateSlideLeftAnimation(
            private val view: View,
            private val startPosition: Int,
            private val destination: Int
    ) : CardFieldAnimation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin =
                        (interpolatedTime * destination + (1 - interpolatedTime) * startPosition).toInt()
            }
        }
    }

    private class CvcSlideLeftAnimation(
            private val view: View,
            private val startPosition: Int,
            private val destination: Int,
            private val newWidth: Int
    ) : CardFieldAnimation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                this.leftMargin = (interpolatedTime * destination + (1 - interpolatedTime) * startPosition).toInt()
                this.rightMargin = 0
                this.width = newWidth
            }
        }
    }

    private class PostalCodeSlideLeftAnimation(
            private val view: View,
            private val startPosition: Int,
            private val destination: Int,
            private val newWidth: Int
    ) : CardFieldAnimation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                this.leftMargin =
                        (interpolatedTime * destination + (1 - interpolatedTime) * startPosition).toInt()
                this.rightMargin = 0
                this.width = newWidth
            }
        }
    }

    private class CardNumberSlideRightAnimation(
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
                leftMargin = (-1f * hiddenCardWidth.toFloat() * interpolatedTime).toInt()
            }
        }
    }

    private class ExpiryDateSlideRightAnimation(
            private val view: View,
            private val startMargin: Int,
            private val destination: Int
    ) : CardFieldAnimation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin =
                        (interpolatedTime * destination + (1 - interpolatedTime) * startMargin).toInt()
            }
        }
    }

    private class CvcSlideRightAnimation(
            private val view: View,
            private val startMargin: Int,
            private val destination: Int,
            private val newWidth: Int
    ) : CardFieldAnimation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin =
                        (interpolatedTime * destination + (1 - interpolatedTime) * startMargin).toInt()
                rightMargin = 0
                width = newWidth
            }
        }
    }

    private class PostalCodeSlideRightAnimation(
            private val view: View,
            private val startMargin: Int,
            private val destination: Int,
            private val newWidth: Int
    ) : CardFieldAnimation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                this.leftMargin =
                        (interpolatedTime * destination + (1 - interpolatedTime) * startMargin).toInt()
                this.rightMargin = 0
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

    internal class DefaultLayoutWidthCalculator : LayoutWidthCalculator {
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
        private val DEFAULT_READER_ID = R.id.default_reader_id

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
