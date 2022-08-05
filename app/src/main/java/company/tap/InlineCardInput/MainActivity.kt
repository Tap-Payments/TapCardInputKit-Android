package company.tap.InlineCardInput

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout

import company.tap.cardinputwidget.widget.inline.CardInlineForm
import company.tap.cardinputwidget.widget.inline.InlineCardInput
import company.tap.tapuilibrary.themekit.ThemeManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    lateinit var cardInlineForm:InlineCardInput
    lateinit var  paymentInputContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //cardInlineForm =findViewById(R.id.cardInlineForm)
        ThemeManager.loadTapTheme(this.resources,R.raw.defaulttheme,"ligththeme")
        setContentView(R.layout.activity_main)

        cardInlineForm = InlineCardInput(this)
        paymentInputContainer = findViewById(R.id.payment_input_layout)
        cardInlineForm.holderNameEnabled= true
        paymentInputContainer.addView(cardInlineForm)
    }
}
