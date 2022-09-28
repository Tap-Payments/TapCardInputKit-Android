package company.tap.InlineCardInput

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import company.tap.cardinputwidget.CardBrandSingle
import company.tap.cardinputwidget.widget.inline.InlineCardInput
import company.tap.taplocalizationkit.LocalizationManager
import company.tap.tapuilibrary.themekit.ThemeManager
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var cardInlineForm:InlineCardInput
    lateinit var  paymentInputContainer: LinearLayout
    lateinit var  mainView: LinearLayout
    private var clearView: ImageView? = null
    private var nfcView: ImageView? = null
    private var scannerView: ImageView? = null
    private lateinit var tabLayout: company.tap.tapuilibrary.uikit.views.TapSelectionTabLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //cardInlineForm =findViewById(R.id.cardInlineForm)

        ThemeManager.loadTapTheme(this.resources,R.raw.defaultlighttheme,"lighttheme")
        LocalizationManager.setLocale(this, Locale("en"))
        setContentView(R.layout.activity_main)

        cardInlineForm = InlineCardInput(this)
        paymentInputContainer = findViewById(R.id.payment_input_layout)
        mainView = findViewById(R.id.mainView)
        cardInlineForm.holderNameEnabled= false
        paymentInputContainer.addView(cardInlineForm)

        clearView = findViewById(R.id.clear_text)
        tabLayout = findViewById(R.id.sections_tablayout)
        mainView.setOnTouchListener { v, event ->
            cardInlineForm.onTouchView()
            true
        }

        cardInlineForm.setCardNumberTextWatcher(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
//                cardNumAfterTextChangeListener(s, this)

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(s?.length!=null && s.isNotEmpty()){
                    clearView?.visibility =View.VISIBLE
                }

                if( s !=null && s.length >= 19){

                  //  cardInlineForm.setCardNumber(maskCardNumber(s.toString()))
                    cardInlineForm.setSingleCardInput(CardBrandSingle.fromCode(s.toString()))

                }

            }
        })

        clearView?.setOnClickListener {
            tabLayout.resetBehaviour()
            cardInlineForm.clear()
            clearView?.visibility =View.INVISIBLE
        }
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
}
