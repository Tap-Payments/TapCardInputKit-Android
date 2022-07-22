package company.tap.InlineCardInput

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import company.tap.cardinputwidget.widget.inline.CardInlineForm
import company.tap.cardinputwidget.widget.inline.InlineCardInput
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    lateinit var cardInlineForm:InlineCardInput
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cardInlineForm =findViewById(R.id.cardInlineForm)
        cardInlineForm.holderNameEnabled= true
    }
}
