package dora.widget.badge

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.withStyledAttributes
import dora.widget.tabbar.R
import kotlin.math.max

class BadgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(
    context, attrs, defStyleAttr
) {

    private val gradientDrawable = GradientDrawable()
    private var backgroundColor = 0
    private var cornerRadius = 0
    private var strokeWidth = 0
    private var strokeColor = 0
    private var isRadiusHalfHeight = false
    private var isWidthEqualsHeight = false

    init {
        obtainAttributes(context, attrs)
    }

    private fun obtainAttributes(context: Context, attrs: AttributeSet?) {
        context.withStyledAttributes(attrs, R.styleable.BadgeView) {
            backgroundColor = getColor(R.styleable.BadgeView_dview_bv_backgroundColor, Color.TRANSPARENT)
            cornerRadius = getDimensionPixelSize(R.styleable.BadgeView_dview_bv_cornerRadius, 0)
            strokeWidth = getDimensionPixelSize(R.styleable.BadgeView_dview_bv_strokeWidth, 0)
            strokeColor = getColor(R.styleable.BadgeView_dview_bv_strokeColor, Color.TRANSPARENT)
            isRadiusHalfHeight = getBoolean(R.styleable.BadgeView_dview_bv_isRadiusHalfHeight, false)
            isWidthEqualsHeight = getBoolean(R.styleable.BadgeView_dview_bv_isWidthEqualsHeight, false)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (isWidthEqualsHeight() && width > 0 && height > 0) {
            val max = max(width, height)
            val measureSpec = MeasureSpec.makeMeasureSpec(max, MeasureSpec.EXACTLY)
            super.onMeasure(measureSpec, measureSpec)
            return
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (isRadiusHalfHeight()) {
            setCornerRadius(height / 2)
        } else {
            setBgSelector()
        }
    }

    override fun setBackgroundColor(backgroundColor: Int) {
        this.backgroundColor = backgroundColor
        setBgSelector()
    }

    fun Int.dp() : Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            toFloat(), context.resources.displayMetrics).toInt()
    }

    fun setCornerRadius(cornerRadius: Int) {
        this.cornerRadius = cornerRadius
        setBgSelector()
    }

    fun setStrokeWidth(strokeWidth: Int) {
        this.strokeWidth = strokeWidth
        setBgSelector()
    }

    fun setCornerRadiusInDp(cornerRadius: Int) {
        this.cornerRadius = cornerRadius.dp()
        setBgSelector()
    }

    fun setStrokeWidthInDp(strokeWidth: Int) {
        this.strokeWidth = strokeWidth.dp()
        setBgSelector()
    }

    fun setStrokeColor(@ColorInt strokeColor: Int) {
        this.strokeColor = strokeColor
        setBgSelector()
    }

    fun setIsRadiusHalfHeight(isRadiusHalfHeight: Boolean) {
        this.isRadiusHalfHeight = isRadiusHalfHeight
        setBgSelector()
    }

    fun setWidthEqualsHeight(isWidthEqualsHeight: Boolean) {
        this.isWidthEqualsHeight = isWidthEqualsHeight
        setBgSelector()
    }

    fun getBackgroundColor(): Int {
        return backgroundColor
    }

    fun getCornerRadius(): Int {
        return cornerRadius
    }

    fun getStrokeWidth(): Int {
        return strokeWidth
    }

    fun getStrokeColor(): Int {
        return strokeColor
    }

    fun isRadiusHalfHeight(): Boolean {
        return isRadiusHalfHeight
    }

    fun isWidthEqualsHeight(): Boolean {
        return isWidthEqualsHeight
    }

    private fun setDrawable(gd: GradientDrawable, color: Int, strokeColor: Int) {
        gd.setColor(color)
        gd.cornerRadius = cornerRadius.toFloat()
        gd.setStroke(strokeWidth, strokeColor)
    }

    fun setBgSelector() {
        val bg = StateListDrawable()
        setDrawable(gradientDrawable, backgroundColor, strokeColor)
        bg.addState(intArrayOf(-android.R.attr.state_pressed), gradientDrawable)
        background = bg
    }
}