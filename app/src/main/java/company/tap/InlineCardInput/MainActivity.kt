package company.tap.InlineCardInput

import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout

import company.tap.cardinputwidget.widget.inline.CardInlineForm
import company.tap.cardinputwidget.widget.inline.InlineCardInput
import company.tap.taplocalizationkit.LocalizationManager
import company.tap.tapuilibrary.themekit.ThemeManager
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var cardInlineForm:InlineCardInput
    lateinit var  paymentInputContainer: LinearLayout
    lateinit var  mainView: LinearLayout

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

        mainView.setOnTouchListener { v, event ->
            cardInlineForm.onTouchView()
            true
        }
    }
}
