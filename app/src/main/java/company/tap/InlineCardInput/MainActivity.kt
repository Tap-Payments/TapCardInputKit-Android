package company.tap.InlineCardInput

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.core.view.marginTop
import company.tap.cardinputwidget.Card
import company.tap.cardinputwidget.CardBrand
import company.tap.cardinputwidget.CardBrandSingle
import company.tap.cardinputwidget.CardInputUIStatus
import company.tap.cardinputwidget.views.CardBrandView
import company.tap.cardinputwidget.widget.inline.CardInlineForm
import company.tap.cardinputwidget.widget.inline.InlineCardInput
import company.tap.cardinputwidget.widget.inline.InlineCardInput2
import company.tap.tapcardvalidator_android.CardValidator
import company.tap.taplocalizationkit.LocalizationManager
import company.tap.tapuilibrary.themekit.ThemeManager
import company.tap.tapuilibrary.uikit.atoms.TapImageView
import company.tap.tapuilibrary.uikit.atoms.TapSwitch
import company.tap.tapuilibrary.uikit.views.TapAlertView
import company.tap.tapuilibrary.uikit.views.TapCardSwitch
import company.tap.tapuilibrary.uikit.datasource.TapSwitchDataSource
import company.tap.tapuilibrary.uikit.ktx.setBorderedView
import company.tap.tapuilibrary.uikit.views.TapInlineCardSwitch

import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var cardInlineForm:InlineCardInput
    lateinit var cardInlineForm2: InlineCardInput2
    lateinit var tap_payment_input:TapPaymentInput
    lateinit var tap_payment_input2:TapPaymentInput
    lateinit var  paymentInputContainer: LinearLayout
    lateinit var  mainView: LinearLayout
    private var clearView: ImageView? = null
    private var tabLinear: RelativeLayout? = null
    private var nfcButton: ImageView? = null
    private var cardScannerBtn: ImageView? = null
    private lateinit var tabLayout: company.tap.tapuilibrary.uikit.views.TapSelectionTabLayout
    var tapAlertView: TapAlertView? = null
    var backArrow: TapImageView? = null
    var switchLL: TapInlineCardSwitch? = null
    var switchSaveCard: TapSwitch? = null
    var cardBrna: CardBrandView? = null
    var cardNumber:String?=null
    var cardInputChipView:CardView ?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //cardInlineForm =findViewById(R.id.cardInlineForm)
        initTheme()

       // ThemeManager.loadTapTheme(this.resources, R.raw.defaultlighttheme, "lighttheme")
       //  ThemeManager.loadTapTheme(this.resources,R.raw.defaultdarktheme,"darktheme")
        LocalizationManager.loadTapLocale(this.resources, R.raw.lang)
        LocalizationManager.setLocale(this, Locale("en"))
        setContentView(R.layout.activity_main)

        cardInlineForm = InlineCardInput(this)
        cardInlineForm2 = InlineCardInput2(this)
        paymentInputContainer = findViewById(R.id.payment_input_layout)
        mainView = findViewById(R.id.mainView)
        tap_payment_input = findViewById(R.id.tap_payment_input)
        tap_payment_input2 = findViewById(R.id.tap_payment_input2)
        cardInlineForm.holderNameEnabled= true
        cardInlineForm.setVisibilityOfHolderField(true)
        //switchLL = cardInlineForm.findViewById(R.id.mainSwitchInline)
        switchLL = findViewById(R.id.switch_Inline_card)
        cardInlineForm.holderNameEnabled = false
        tabLinear = findViewById(R.id.tabLinear)
        tapAlertView = findViewById(R.id.alertView)
        clearView = cardInlineForm.findViewById(R.id.clear_text)
        backArrow = cardInlineForm.findViewById(R.id.backView)
        tabLayout = findViewById(R.id.sections_tablayout)
        cardScannerBtn = tap_payment_input.findViewById(R.id.card_scanner_button)
        nfcButton = tap_payment_input.findViewById(R.id.nfc_button)
        cardBrna = cardInlineForm.findViewById(R.id.card_brand_view)
       // cardBrna?.iconView?.setImageResource(R.drawable.bahrain)
        tapAlertView?.alertMessage?.text = "Card number is missing"
        tapAlertView?.visibility = View.GONE
        nfcButton?.visibility = View.VISIBLE
        cardScannerBtn?.visibility = View.VISIBLE
        paymentInputContainer.addView(cardInlineForm)
        //paymentInputContainer.addView(cardInlineForm2)

        backArrow?.setOnClickListener {
            tabLayout.resetBehaviour()
            cardInlineForm.clear()
            clearView?.visibility = View.GONE
            controlScannerOptions()
        }

        clearView?.setOnClickListener {
            println("clearView lcicc")
            tabLayout.resetBehaviour()
            cardInlineForm.clear()
            clearView?.visibility = View.GONE
            controlScannerOptions()
        }

        switchSaveCard = switchLL?.findViewById(R.id.switchSaveCard)
        //   switchLL?.setSwitchDataSource(TapSwitchDataSource("Sasa","Save For later","sa","asa","asa"))
        cardInlineForm.switchCardEnabled = true
        cardInputChipView = tap_payment_input.findViewById(R.id.inline_CardView)
       /* if (ThemeManager.currentTheme.isNotEmpty() && ThemeManager.currentTheme.contains("dark")) {
            //   tapPaymentInput?.cardInputChipView?.setBackgroundResource(R.drawable.border_unclick_black)
        } else {
            cardInputChipView?.setBackgroundResource(R.drawable.border_unclick_cardinput)
        }
        cardInputChipView?.let {
            setBorderedView(
                it,
                15.0f,// corner raduis
                0.2f,
                Color.parseColor(ThemeManager.getValue("inlineCard.commonAttributes.shadow.color")),
                Color.parseColor(ThemeManager.getValue("inlineCard.commonAttributes.shadow.color")),
                Color.parseColor(ThemeManager.getValue("inlineCard.commonAttributes.shadow.color"))

            )
        }*/

      //  tap_payment_input?.cardInputChipView?.cardElevation= 0.2f

   /* cardInlineForm.setSavedCardDetails(Card("5123 4500 0000 0008",null,7,23,
            "dsd",null,null,null,
            null,null,null,null,null,
            "0008",CardBrand.MasterCard,"sdsds",null,null,null,null,null),CardInputUIStatus.SavedCard)

        cardInlineForm.setSingleCardInput(
            CardBrandSingle.fromCode(
                company.tap.cardinputwidget.CardBrand.fromCardNumber("512345")
                    .toString()
            ), "https://back-end.b-cdn.net/payment_methods/visa.svg"
        )*/
        mainView.setOnTouchListener { v, event ->
            //cardInlineForm.onTouchView()
            true
        }

        nfcButton?.visibility = View.GONE
        cardScannerBtn?.visibility = View.GONE
        cardInlineForm.setCardNumberTextWatcher(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
//                cardNumAfterTextChangeListener(s, this)
                println("cardInlineForm.card.number"+cardInlineForm?.card?.number)
                println("isDeleting"+cardInlineForm.isDeleting)

                if (s != null && s.length >= 19) {

                    if(cardInlineForm.isDeleting == true){


                    }else {
                        println("full no" + cardInlineForm.fullCardNumber)
                        cardInlineForm.setCardNumberMasked(cardInlineForm.fullCardNumber?.let {
                            maskCardNumber(
                                it
                            )
                        })
                    }
                    cardInlineForm.removeCardNumberTextWatcher(this)
                    cardInlineForm.setCardNumberTextWatcher(this)

                    // cardInlineForm.setCardNumber(maskCardNumber(s.toString()))
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                if (s?.length != null && s.isNotEmpty()) {
                    clearView?.visibility = View.VISIBLE
                    nfcButton?.visibility = View.GONE
                    cardScannerBtn?.visibility = View.GONE
                }
                val card = CardValidator.validate(s.toString())
                if(card.cardBrand!=null && s?.toString()?.length!! <= 6)
                logicTosetValues(card.cardBrand,s.toString())
                if (s != null && s.length >= 19) {
                    //  cardInlineForm.setCardNumber(maskCardNumber(s.toString()))
                        //Dynamically set value from API
                  if(s.toString().startsWith("5")){
                      cardInlineForm.setSingleCardInput(CardBrandSingle.fromCode(s.toString()),"https://back-end.b-cdn.net/payment_methods/mastercard.svg")

                  }else {
                      cardInlineForm.setSingleCardInput(CardBrandSingle.fromCode(s.toString()),"https://back-end.b-cdn.net/payment_methods/visa.svg")

                  }
                   // alertView.visibility =View.VISIBLE
                   // alertView.alertMessage.text ="vwrongggg"
                    cardNumber = s.toString()
                   // cardInlineForm.setCardNumberMasked(cardInlineForm.fullCardNumber)

                }

            }
        })
        cardInlineForm.iconUrl ="https://back-end.b-cdn.net/payment_methods/visa.svg"
        //cardInlineForm.setIconUrl("https://back-end.b-cdn.net/payment_methods/visa.svg")

        clearView?.setOnClickListener {
            tabLayout.resetBehaviour()
            cardInlineForm.clear()
            clearView?.visibility = View.GONE
            nfcButton?.visibility = View.VISIBLE
            cardScannerBtn?.visibility = View.VISIBLE
        }
        cardInlineForm.setExpiryDateTextWatcher(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
               // cardInlineForm.setCardNumberText(cardNumber?.let { mask(it) })
            }
        })

        cardInlineForm.setHolderNameTextWatcher(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                println("switchLL>>>" + switchLL)
                println("s val >>>" + s.toString())
                switchLL?.visibility = View.VISIBLE

            }

        })

        cardInlineForm.setCvcNumberTextWatcher(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //   checkoutFragment.scrollView?.scrollTo(0,height)
                // tapCardInputView.requestFocus()
                //alertView.visibility =View.VISIBLE
               // alertView.alertMessage.text = "Enter the 3-digit CVV number (usually at the back of the card)"
                switchLL?.switchSaveCard?.visibility =View.GONE
                switchLL?.saveForOtherCheckBox?.visibility =View.GONE
                switchLL?.toolsTipImageView?.visibility =View.GONE
                switchLL?.visibility =View.GONE
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                switchLL?.switchSaveCard?.text="Save for later"

                cardInlineForm.setVisibilityOfHolderField(true)
                cardInlineForm.separator_1.visibility =View.VISIBLE

                switchLL?.switchSaveCard?.visibility =View.VISIBLE
                switchLL?.saveForOtherCheckBox?.visibility =View.VISIBLE
                switchLL?.toolsTipImageView?.visibility =View.VISIBLE
                switchLL?.brandingLayout?.visibility =View.GONE
                switchLL?.visibility =View.VISIBLE
                if(cardInlineForm.holderNameEnabled){
                    switchLL?.setPaddingRelative(0,100,0,0)


                }
            }
        })
    }

    private fun initTheme() {
        if (ThemeManager.currentTheme.isNotEmpty() && ThemeManager.currentTheme.contains("dark"))
            ThemeManager.loadTapTheme(resources, R.raw.defaultdarktheme, "darktheme")
        else if (ThemeManager.currentTheme.isNotEmpty() && !ThemeManager.currentTheme.contains("dark"))
            ThemeManager.loadTapTheme(resources, R.raw.defaultlighttheme, "lighttheme")
        else ThemeManager.loadTapTheme(resources, R.raw.defaultlighttheme, "lighttheme")

    }


    private fun  maskCardNumber(cardInput: String): String {
        val maskLen: Int = cardInput.length - 4
        println("maskLen"+maskLen)
        println("cardInput"+cardInput.length)
        if (maskLen <= 0) return cardInput // Nothing to mask
        return (cardInput).replaceRange(0, maskLen, "•••• ")
    }

    fun mask(input: String): String? {
        return input.replace(".(?=.{4})".toRegex(), "•")
    }

    fun addME(view: View) {
        println("clickckc")
        cardInlineForm.setSavedCardDetails(Card("5123 4500 0000 0008",null,7,23,
            "dsd",null,null,null,
            null,null,null,null,null,
            "0008",CardBrand.fromCardNumber("512345"),"sdsds",null,null,null,null,null),CardInputUIStatus.SavedCard)

        cardInlineForm.setSingleCardInput(
            CardBrandSingle.fromCode(
                company.tap.cardinputwidget.CardBrand.fromCardNumber("512345")
                    .toString()
            ), "https://back-end.b-cdn.net/payment_methods/visa.svg"
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menumain, menu)
        return true
    }
    // actions on click menu items
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_dark -> {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            ThemeManager.loadTapTheme(resources, R.raw.defaultdarktheme, "darktheme")

            recreate()
            true
        }
        R.id.action_light -> {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            ThemeManager.loadTapTheme(resources, R.raw.defaultlighttheme, "lighttheme")
            recreate()
            true
        }
        R.id.action_arabic -> {
            if (ThemeManager.currentTheme.isNotEmpty() && LocalizationManager.currentLocalized.toString()
                    .isNotEmpty()) {

                LocalizationManager.setLocale(this, Locale("ar"))

            }
            recreate()
            true
        }
        R.id.action_english -> {
            if (ThemeManager.currentTheme.isNotEmpty() && LocalizationManager.currentLocalized.toString()
                    .isNotEmpty()
            ) {

                LocalizationManager.setLocale(this, Locale("en"))

            }
            recreate()
            true
        }


        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }
    private fun controlScannerOptions() {
        if (true) {
            nfcButton?.visibility = View.GONE

            cardScannerBtn?.visibility = View.VISIBLE
        } else {

            nfcButton?.visibility = View.GONE
            cardScannerBtn?.visibility = View.VISIBLE
        }
    }
    fun logicTosetValues(card: company.tap.tapcardvalidator_android.CardBrand,s:String){

        if(s.toString().startsWith("5")){
            cardInlineForm.setSingleCardInput(CardBrandSingle.fromCode(s.toString()),"https://back-end.b-cdn.net/payment_methods/mastercard.svg")

        }else {
            cardInlineForm.setSingleCardInput(CardBrandSingle.fromCode(s.toString()),"https://back-end.b-cdn.net/payment_methods/visa.svg")

        }

        }
    }


