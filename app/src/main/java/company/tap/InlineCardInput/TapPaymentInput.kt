package company.tap.InlineCardInput

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import company.tap.tapuilibrary.R
import company.tap.tapuilibrary.themekit.ThemeManager
import company.tap.tapuilibrary.themekit.theme.SeparatorViewTheme
import company.tap.tapuilibrary.themekit.theme.TabSelectTheme
import company.tap.tapuilibrary.uikit.atoms.TapSeparatorView
import company.tap.tapuilibrary.uikit.interfaces.TapPaymentShowHideClearImage
import company.tap.tapuilibrary.uikit.interfaces.TapView
import company.tap.tapuilibrary.uikit.models.TabSection
import company.tap.tapuilibrary.uikit.views.TapMobilePaymentView
import company.tap.tapuilibrary.uikit.views.TapSelectionTabLayout

/**
 *
 * Copyright © 2020 Tap Payments. All rights reserved.
 *
 */
class TapPaymentInput(context: Context?, attrs: AttributeSet?) :
    LinearLayout(context, attrs), TapPaymentShowHideClearImage,
    TapView<TabSelectTheme> {

    val tabLayout by lazy { findViewById<TapSelectionTabLayout>(R.id.sections_tablayout) }
    val paymentInputContainer by lazy { findViewById<LinearLayout>(R.id.payment_input_layout) }
    val tabLinear by lazy { findViewById<RelativeLayout>(R.id.tabLinear) }
    val clearView by lazy { findViewById<ImageView>(R.id.clear_text) }
    val separator by lazy { findViewById<TapSeparatorView>(R.id.separator) }
    private  var tapMobileInputView: TapMobilePaymentView
    private var displayMetrics: Int? = null

    init {
        inflate(context, R.layout.tap_payment_input, this)
        applyTheme()
        clearView.setOnClickListener {
            rootView.invalidate()
        }
        tapMobileInputView = TapMobilePaymentView(context, null)



    }
    fun setDisplayMetrics(displayMetrics: Int) {
        this.displayMetrics = displayMetrics
    }


    @RequiresApi(Build.VERSION_CODES.N)
    fun addTabLayoutSection(vararg sections: TabSection) {
        sections.forEach {
            tabLayout.addSection(it.items)
        }
    }

    fun clearCardNumber() {
        paymentInputContainer.clearFocus()
    }

    override fun setTheme(theme: TabSelectTheme) {
        theme.backgroundColor?.let { setBackgroundColor(it) }
        theme.selectedBackgroundColor?.let { setBackgroundColor(it) }
        theme.unselectedBackgroundColor?.let { setBackgroundColor(it) }
    }

    private fun applyTheme() {
        tabLinear.setBackgroundColor(Color.parseColor(ThemeManager.getValue("inlineCard.commonAttributes.backgroundColor")))
        clearView.setBackgroundColor(Color.parseColor(ThemeManager.getValue("inlineCard.commonAttributes.backgroundColor")))
        setSeparatorTheme()
    }

    private fun setSeparatorTheme() {
        val separatorViewTheme = SeparatorViewTheme()
        separatorViewTheme.strokeColor =
            Color.parseColor(ThemeManager.getValue("tapSeparationLine.backgroundColor"))
        separatorViewTheme.strokeHeight = ThemeManager.getValue("tapSeparationLine.height")
        separator.setTheme(separatorViewTheme)
        separator.setBackgroundColor(Color.parseColor(ThemeManager.getValue("tapSeparationLine.backgroundColor")))
    }

    override fun showHideClearImage(show: Boolean) {
        if (show) {
            clearView.visibility = VISIBLE
        } else {
            clearView.visibility = GONE
        }
    }

}