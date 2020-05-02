package company.tap.cardinputwidget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.DrawableCompat
import company.tap.cardinputwidget.databinding.CardBrandViewBinding

internal class CardBrandView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val viewBinding: CardBrandViewBinding = CardBrandViewBinding.inflate(
        LayoutInflater.from(context),
        this
    )
    private val iconView = viewBinding.icon

    @ColorInt
    internal var tintColorInt: Int = 0

    init {
        isFocusable = false
        setScanClickListener()
    }

    internal fun showBrandIcon(brand: CardBrand, shouldShowErrorIcon: Boolean) {
        iconView.setOnClickListener(null)
        if (shouldShowErrorIcon) {
            iconView.setImageResource(brand.errorIcon)
        } else {
            iconView.setImageResource(brand.icon)

            if (brand == CardBrand.Unknown) {
                applyTint(false)
                setScanClickListener()
            }
        }
    }

    private fun setScanClickListener() {
        iconView.setOnClickListener {
            Toast.makeText(iconView.context, "Clicked", Toast.LENGTH_SHORT).show()
        }
    }

    internal fun showCvcIcon(brand: CardBrand) {
        iconView.setImageResource(brand.cvcIcon)
        iconView.setOnClickListener(null)
        applyTint(true)
    }

    internal fun applyTint(apply: Boolean) {
        if (!apply)
            return
        val icon = iconView.drawable
        val compatIcon = DrawableCompat.wrap(icon)
        DrawableCompat.setTint(compatIcon.mutate(), tintColorInt)
        iconView.setImageDrawable(DrawableCompat.unwrap(compatIcon))
    }
}
