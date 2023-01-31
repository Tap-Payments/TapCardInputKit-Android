package company.tap.InlineCardInput

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.marginTop
import company.tap.cardinputwidget.Card
import company.tap.cardinputwidget.CardBrand
import company.tap.cardinputwidget.CardBrandSingle
import company.tap.cardinputwidget.CardInputUIStatus
import company.tap.cardinputwidget.widget.inline.InlineCardInput
import company.tap.taplocalizationkit.LocalizationManager
import company.tap.tapuilibrary.themekit.ThemeManager
import company.tap.tapuilibrary.uikit.atoms.TapImageView
import company.tap.tapuilibrary.uikit.atoms.TapSwitch
import company.tap.tapuilibrary.uikit.views.TapAlertView
import company.tap.tapuilibrary.uikit.views.TapCardSwitch
import company.tap.tapuilibrary.uikit.datasource.TapSwitchDataSource
import company.tap.tapuilibrary.uikit.views.TapInlineCardSwitch
import kotlinx.android.synthetic.main.tap_payment_input.*
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var cardInlineForm:InlineCardInput
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //cardInlineForm =findViewById(R.id.cardInlineForm)

        ThemeManager.loadTapTheme(this.resources, R.raw.defaultlighttheme, "lighttheme")
        // ThemeManager.loadTapTheme(this.resources,R.raw.defaultdarktheme,"darktheme")
        LocalizationManager.loadTapLocale(this.resources, R.raw.lang)
        LocalizationManager.setLocale(this, Locale("en"))
        setContentView(R.layout.activity_main)

        cardInlineForm = InlineCardInput(this)
        paymentInputContainer = findViewById(R.id.payment_input_layout)
        mainView = findViewById(R.id.mainView)
        cardInlineForm.holderNameEnabled= true
        cardInlineForm.setVisibilityOfHolderField(true)
        //switchLL = cardInlineForm.findViewById(R.id.mainSwitchInline)
        switchLL = findViewById(R.id.switch_Inline_card)
        cardInlineForm.holderNameEnabled = false
        tabLinear = findViewById(R.id.tabLinear)
        tapAlertView = findViewById(R.id.alertView)
        clearView = findViewById(R.id.clear_text)
        backArrow = findViewById(R.id.backView)
        tabLayout = findViewById(R.id.sections_tablayout)
        cardScannerBtn = findViewById(R.id.card_scanner_button)
        nfcButton = findViewById(R.id.nfc_button)
        tapAlertView?.alertMessage?.text = "Card number is missing"
        tapAlertView?.visibility = View.GONE
        nfcButton?.visibility = View.VISIBLE
        cardScannerBtn?.visibility = View.VISIBLE
        paymentInputContainer.addView(cardInlineForm)
        backArrow?.setOnClickListener {
            tabLayout.resetBehaviour()
            cardInlineForm.clear()
            clearView?.visibility = View.GONE
            nfcButton?.visibility = View.VISIBLE
            cardScannerBtn?.visibility = View.VISIBLE
        }
        switchSaveCard = switchLL?.findViewById(R.id.switchSaveCard)
        //   switchLL?.setSwitchDataSource(TapSwitchDataSource("Sasa","Save For later","sa","asa","asa"))
        cardInlineForm.switchCardEnabled = true


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
                if (s != null && s.length >= 19) {

                    //cardInlineForm.setCardNumber(maskCardNumber(s.toString()))
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length != null && s.isNotEmpty()) {
                    clearView?.visibility = View.VISIBLE
                    nfcButton?.visibility = View.GONE
                    cardScannerBtn?.visibility = View.GONE
                }

                if (s != null && s.length >= 19) {

                    //  cardInlineForm.setCardNumber(maskCardNumber(s.toString()))
                    cardInlineForm.setSingleCardInput(CardBrandSingle.fromCode(s.toString()))
                    alertView.visibility =View.VISIBLE
                    alertView.alertMessage.text ="vwrongggg"
                }

            }
        })

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
                alertView.visibility =View.VISIBLE
                alertView.alertMessage.text = "Enter the 3-digit CVV number (usually at the back of the card)"
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
            "0008",CardBrand.MasterCard,"sdsds",null,null,null,null,null),CardInputUIStatus.SavedCard)

        cardInlineForm.setSingleCardInput(
            CardBrandSingle.fromCode(
                company.tap.cardinputwidget.CardBrand.fromCardNumber("512345")
                    .toString()
            ), "https://back-end.b-cdn.net/payment_methods/visa.svg"
        )
    }


}
