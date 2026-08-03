package dora.widget

import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseArray
import android.util.TypedValue
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import java.util.Locale
import androidx.core.content.withStyledAttributes
import androidx.core.view.isNotEmpty
import androidx.core.graphics.toColorInt
import dora.widget.badge.BadgeUtils
import dora.widget.badge.BadgeView
import dora.widget.fragement.FragmentChangeManager
import dora.widget.tabbar.R

/**
 * 简单的静态Tab Bar，仅支持文字，最多只能支持一屏，且只支持fragment，不支持viewpager2。
 */
class DoraStaticTabLayout
    @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), ValueAnimator.AnimatorUpdateListener {

    private var titles: Array<String> = arrayOf()
    private val tabContainer: LinearLayout
    private var currentTab = 0
    private var lastTab = 0
    var tabCount: Int = 0
        private set
    private val indicatorRect = Rect()
    private val indicatorDrawable: GradientDrawable = GradientDrawable()
    private val rectDrawable: GradientDrawable = GradientDrawable()
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var tabPadding = 0f
    private var tabIsDivided = false
    private var tabWidth = 0f
    private var indicatorColor = 0
    private var indicatorHeight = 0f
    private var indicatorCornerRadius = 0f
    var indicatorMarginLeft: Float = 0f
        private set
    var indicatorMarginTop: Float = 0f
        private set
    var indicatorMarginRight: Float = 0f
        private set
    var indicatorMarginBottom: Float = 0f
        private set
    var indicatorAnimationDuration: Long = 0
    var isIndicatorAnimationEnable: Boolean = false
    var isIndicatorBounceEnable: Boolean = false
    private var dividerColor = 0
    private var dividerWidth = 0f
    private var dividerPadding = 0f

    private var tabTextSize = 0f
    private var tabTextSelectedColor = 0
    private var tabTextUnselectedColor = 0
    private var textBold = 0
    private var textAllCaps = false
    private var barColor = 0
    private var barStrokeColor = 0
    private var barStrokeWidth = 0f
    private var isFirstDraw = true
    private var viewHeight = 0
    private val valueAnimator: ValueAnimator
    private val interpolator: OvershootInterpolator = OvershootInterpolator(0.8f)
    private var fragmentChangeManager: FragmentChangeManager? = null
    private val radiusArray = FloatArray(8)
    private var onTabSelectListener: OnTabSelectListener? = null
    private val currentPoint = IndicatorPoint()
    private val lastPoint = IndicatorPoint()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val initMap = SparseArray<Boolean>()

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
        tabContainer = LinearLayout(context)
        addView(tabContainer)
        obtainAttributes(context, attrs)
        val height = attrs?.getAttributeValue("http://schemas.android.com/apk/res/android", "layout_height")
        if (height == LayoutParams.MATCH_PARENT.toString() + "") {
        } else if (height == LayoutParams.WRAP_CONTENT.toString() + "") {
        } else {
            val systemAttrs = intArrayOf(android.R.attr.layout_height)
            context.withStyledAttributes(attrs, systemAttrs) {
                viewHeight = getDimensionPixelSize(0, LayoutParams.WRAP_CONTENT)
            }
        }
        valueAnimator = ValueAnimator.ofObject(PointEvaluator(), lastPoint, currentPoint)
        valueAnimator.addUpdateListener(this)
    }

    private fun obtainAttributes(context: Context, attrs: AttributeSet?) {
        context.withStyledAttributes(attrs, R.styleable.DoraStaticTabBar) {
            indicatorColor = getColor(
                R.styleable.DoraStaticTabBar_dview_tb_indicatorColor,
                "#4CAF50".toColorInt()
            )
            indicatorHeight = getDimension(R.styleable.DoraStaticTabBar_dview_tb_indicatorHeight, -1f)
            indicatorCornerRadius =
                getDimension(R.styleable.DoraStaticTabBar_dview_tb_indicatorCornerRadius, -1f)
            indicatorMarginLeft = getDimension(
                R.styleable.DoraStaticTabBar_dview_tb_indicatorMarginLeft,
                0f
            )
            indicatorMarginTop =
                getDimension(R.styleable.DoraStaticTabBar_dview_tb_indicatorMarginTop, 0f)
            indicatorMarginRight = getDimension(
                R.styleable.DoraStaticTabBar_dview_tb_indicatorMarginRight,
                0f
            )
            indicatorMarginBottom =
                getDimension(R.styleable.DoraStaticTabBar_dview_tb_indicatorMarginBottom, 0f)
            isIndicatorAnimationEnable =
                getBoolean(R.styleable.DoraStaticTabBar_dview_tb_indicatorAnimationEnable, false)
            isIndicatorBounceEnable =
                getBoolean(R.styleable.DoraStaticTabBar_dview_tb_indicatorBounceEnable, true)
            indicatorAnimationDuration =
                getInt(R.styleable.DoraStaticTabBar_dview_tb_indicatorAnimationDuration, -1).toLong()
            dividerColor =
                getColor(R.styleable.DoraStaticTabBar_dview_tb_dividerColor, indicatorColor)
            dividerWidth =
                getDimension(
                    R.styleable.DoraStaticTabBar_dview_tb_dividerWidth,
                    dp2px(1f).toFloat()
                )
            dividerPadding = getDimension(R.styleable.DoraStaticTabBar_dview_tb_dividerPadding, 0f)
            tabTextSize = getDimension(
                R.styleable.DoraStaticTabBar_dview_tb_tabTextSize,
                sp2px(13f).toFloat()
            )
            tabTextSelectedColor = getColor(
                R.styleable.DoraStaticTabBar_dview_tb_tabSelectedTextColor,
                "#FFFFFF".toColorInt()
            )
            tabTextUnselectedColor =
                getColor(R.styleable.DoraStaticTabBar_dview_tb_tabUnselectedTextColor, indicatorColor)
            textBold = getInt(R.styleable.DoraStaticTabBar_dview_tb_tabTextBold, TEXT_BOLD_NONE)
            textAllCaps = getBoolean(R.styleable.DoraStaticTabBar_dview_tb_tabTextAllCaps, false)
            tabIsDivided = getBoolean(R.styleable.DoraStaticTabBar_dview_tb_tabIsDivided, true)
            tabWidth = getDimension(
                R.styleable.DoraStaticTabBar_dview_tb_tabWidth,
                -1f
            )
            tabPadding = getDimension(
                R.styleable.DoraStaticTabBar_dview_tb_tabPadding,
                (if (tabIsDivided || tabWidth > 0) dp2px(0f) else dp2px(
                    10f
                )).toFloat()
            )
            barColor = getColor(R.styleable.DoraStaticTabBar_dview_tb_barColor, Color.TRANSPARENT)
            barStrokeColor =
                getColor(R.styleable.DoraStaticTabBar_dview_tb_barStrokeColor, indicatorColor)
            barStrokeWidth =
                getDimension(
                    R.styleable.DoraStaticTabBar_dview_tb_barStrokeWidth,
                    dp2px(1f).toFloat()
                )
        }
    }

    fun setTextTabs(titles: Array<String>) {
        check(titles.isNotEmpty()) { "Titles can not be NULL or EMPTY !" }
        this.titles = titles
        notifyDataSetChanged()
    }

    fun setTextTabs(
        titles: Array<String>,
        fa: FragmentActivity,
        containerViewId: Int,
        fragments: ArrayList<Fragment>
    ) {
        fragmentChangeManager =
            FragmentChangeManager(fa.supportFragmentManager, containerViewId, fragments)
        setTextTabs(titles)
    }

    fun notifyDataSetChanged() {
        tabContainer.removeAllViews()
        this.tabCount = titles.size
        var tabView: View
        for (i in 0..<this.tabCount) {
            tabView = inflate(context, R.layout.layout_tab_static, null)
            tabView.tag = i
            addTab(i, tabView)
        }
        updateTabStyles()
    }

    private fun addTab(position: Int, tabView: View) {
        val tvTabTitle = tabView.findViewById<View>(R.id.tv_tab_title) as TextView
        tvTabTitle.text = titles[position]
        tabView.setOnClickListener { v ->
            val position = v.tag as Int
            if (currentTab != position) {
                currentTab = position
                onTabSelectListener?.onTabSelected(position)
            } else {
                onTabSelectListener?.onTabReselected(position)
            }
        }
        var lp: LinearLayout.LayoutParams = if (tabIsDivided) LinearLayout.LayoutParams(
            0,
            LayoutParams.MATCH_PARENT,
            1.0f
        ) else LinearLayout.LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.MATCH_PARENT
        )
        if (tabWidth > 0) {
            lp = LinearLayout.LayoutParams(tabWidth.toInt(), LayoutParams.MATCH_PARENT)
        }
        tabContainer.addView(tabView, position, lp)
    }

    private fun updateTabStyles() {
        for (i in 0..<this.tabCount) {
            val tabView = tabContainer.getChildAt(i)
            tabView.setPadding(tabPadding.toInt(), 0, tabPadding.toInt(), 0)
            val tvTabTitle = tabView.findViewById<View>(R.id.tv_tab_title) as TextView
            tvTabTitle.setTextColor(if (i == currentTab) tabTextSelectedColor else tabTextUnselectedColor)
            tvTabTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, tabTextSize)
            if (textAllCaps) {
                tvTabTitle.text = tvTabTitle.text.toString().uppercase(Locale.getDefault())
            }
            if (textBold == TEXT_BOLD_BOTH) {
                tvTabTitle.paint.isFakeBoldText = true
            } else if (textBold == TEXT_BOLD_NONE) {
                tvTabTitle.paint.isFakeBoldText = false
            }
        }
    }

    private fun updateTabSelection(position: Int) {
        for (i in 0..<this.tabCount) {
            val tabView = tabContainer.getChildAt(i)
            val isSelect = i == position
            val tvTabTitle = tabView.findViewById<View>(R.id.tv_tab_title) as TextView
            tvTabTitle.setTextColor(if (isSelect) tabTextSelectedColor else tabTextUnselectedColor)
            if (textBold == TEXT_BOLD_WHEN_SELECT) {
                tvTabTitle.paint.isFakeBoldText = isSelect
            }
        }
    }

    private fun calcOffset() {
        val currentTabView = tabContainer.getChildAt(this.currentTab)
        currentPoint.left = currentTabView.left.toFloat()
        currentPoint.right = currentTabView.right.toFloat()
        val lastTabView = tabContainer.getChildAt(this.lastTab)
        lastPoint.left = lastTabView.left.toFloat()
        lastPoint.right = lastTabView.right.toFloat()
        if (lastPoint.left == currentPoint.left && lastPoint.right == currentPoint.right) {
            invalidate()
        } else {
            valueAnimator.setObjectValues(lastPoint, currentPoint)
            if (this.isIndicatorBounceEnable) {
                valueAnimator.interpolator = interpolator
            }
            if (this.indicatorAnimationDuration < 0) {
                this.indicatorAnimationDuration =
                    (if (this.isIndicatorBounceEnable) 500 else 250).toLong()
            }
            valueAnimator.duration = this.indicatorAnimationDuration
            valueAnimator.start()
        }
    }

    private fun calcIndicatorRect() {
        val currentTabView = tabContainer.getChildAt(this.currentTab)
        val left = currentTabView.left.toFloat()
        val right = currentTabView.right.toFloat()
        indicatorRect.left = left.toInt()
        indicatorRect.right = right.toInt()
        if (!this.isIndicatorAnimationEnable) {
            when (currentTab) {
                0 -> {
                    radiusArray[0] = indicatorCornerRadius
                    radiusArray[1] = indicatorCornerRadius
                    radiusArray[2] = 0f
                    radiusArray[3] = 0f
                    radiusArray[4] = 0f
                    radiusArray[5] = 0f
                    radiusArray[6] = indicatorCornerRadius
                    radiusArray[7] = indicatorCornerRadius
                }
                this.tabCount - 1 -> {
                    radiusArray[0] = 0f
                    radiusArray[1] = 0f
                    radiusArray[2] = indicatorCornerRadius
                    radiusArray[3] = indicatorCornerRadius
                    radiusArray[4] = indicatorCornerRadius
                    radiusArray[5] = indicatorCornerRadius
                    radiusArray[6] = 0f
                    radiusArray[7] = 0f
                }
                else -> {
                    radiusArray[0] = 0f
                    radiusArray[1] = 0f
                    radiusArray[2] = 0f
                    radiusArray[3] = 0f
                    radiusArray[4] = 0f
                    radiusArray[5] = 0f
                    radiusArray[6] = 0f
                    radiusArray[7] = 0f
                }
            }
        } else {
            radiusArray[0] = indicatorCornerRadius
            radiusArray[1] = indicatorCornerRadius
            radiusArray[2] = indicatorCornerRadius
            radiusArray[3] = indicatorCornerRadius
            radiusArray[4] = indicatorCornerRadius
            radiusArray[5] = indicatorCornerRadius
            radiusArray[6] = indicatorCornerRadius
            radiusArray[7] = indicatorCornerRadius
        }
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        val p = animation.animatedValue as IndicatorPoint
        indicatorRect.left = p.left.toInt()
        indicatorRect.right = p.right.toInt()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isInEditMode || this.tabCount <= 0) {
            return
        }
        val height = getHeight()
        val paddingLeft = getPaddingLeft()
        if (indicatorHeight < 0) {
            indicatorHeight = height - this.indicatorMarginTop - this.indicatorMarginBottom
        }
        if (indicatorCornerRadius < 0 || indicatorCornerRadius > indicatorHeight / 2) {
            indicatorCornerRadius = indicatorHeight / 2
        }
        rectDrawable.setColor(barColor)
        rectDrawable.setStroke(barStrokeWidth.toInt(), barStrokeColor)
        rectDrawable.cornerRadius = indicatorCornerRadius
        rectDrawable.setBounds(
            getPaddingLeft(),
            paddingTop,
            width - paddingRight,
            getHeight() - paddingBottom
        )
        rectDrawable.draw(canvas)
        if (!this.isIndicatorAnimationEnable && dividerWidth > 0) {
            dividerPaint.strokeWidth = dividerWidth
            dividerPaint.color = dividerColor
            for (i in 0..<this.tabCount - 1) {
                val tab = tabContainer.getChildAt(i)
                canvas.drawLine(
                    (paddingLeft + tab.right).toFloat(),
                    dividerPadding,
                    (paddingLeft + tab.right).toFloat(),
                    height - dividerPadding,
                    dividerPaint
                )
            }
        }
        if (this.isIndicatorAnimationEnable) {
            if (isFirstDraw) {
                isFirstDraw = false
                calcIndicatorRect()
            }
        } else {
            calcIndicatorRect()
        }
        indicatorDrawable.setColor(indicatorColor)
        indicatorDrawable.setBounds(
            paddingLeft + indicatorMarginLeft.toInt() + indicatorRect.left,
            indicatorMarginTop.toInt(),
            (paddingLeft + indicatorRect.right - this.indicatorMarginRight).toInt(),
            (this.indicatorMarginTop + indicatorHeight).toInt()
        )
        indicatorDrawable.cornerRadii = radiusArray
        indicatorDrawable.draw(canvas)
    }

    fun setIndicatorMargin(
        indicatorMarginLeft: Float, indicatorMarginTop: Float,
        indicatorMarginRight: Float, indicatorMarginBottom: Float
    ) {
        this.indicatorMarginLeft = dp2px(indicatorMarginLeft).toFloat()
        this.indicatorMarginTop = dp2px(indicatorMarginTop).toFloat()
        this.indicatorMarginRight = dp2px(indicatorMarginRight).toFloat()
        this.indicatorMarginBottom = dp2px(indicatorMarginBottom).toFloat()
        invalidate()
    }

    fun setCurrentTab(position: Int) {
        lastTab = this.currentTab
        this.currentTab = position
        updateTabSelection(position)
        fragmentChangeManager?.setFragments(position)
        if (this.isIndicatorAnimationEnable) {
            calcOffset()
        } else {
            invalidate()
        }
    }

    fun setTabPadding(padding: Float) {
        this.tabPadding = dp2px(padding).toFloat()
        updateTabStyles()
    }

    fun setTabIsDivided(isTabDivided: Boolean) {
        this.tabIsDivided = isTabDivided
        updateTabStyles()
    }

    fun setTabWidth(width: Float) {
        this.tabWidth = dp2px(width).toFloat()
        updateTabStyles()
    }

    fun setIndicatorColor(color: Int) {
        this.indicatorColor = color
        invalidate()
    }

    fun setIndicatorHeight(height: Float) {
        this.indicatorHeight = dp2px(height).toFloat()
        invalidate()
    }

    fun setIndicatorCornerRadius(radius: Float) {
        this.indicatorCornerRadius = dp2px(radius).toFloat()
        invalidate()
    }

    fun setDividerColor(color: Int) {
        this.dividerColor = color
        invalidate()
    }

    fun setDividerWidth(width: Float) {
        this.dividerWidth = dp2px(width).toFloat()
        invalidate()
    }

    fun setDividerPadding(padding: Float) {
        this.dividerPadding = dp2px(padding).toFloat()
        invalidate()
    }

    fun setTabTextSize(textSize: Float) {
        this.tabTextSize = sp2px(textSize).toFloat()
        updateTabStyles()
    }

    fun setTabTextSelectedColor(color: Int) {
        this.tabTextSelectedColor = color
        updateTabStyles()
    }

    fun setTabTextUnselectedColor(color: Int) {
        this.tabTextUnselectedColor = color
        updateTabStyles()
    }

    fun setTextBold(textBold: Int) {
        this.textBold = textBold
        updateTabStyles()
    }

    fun setTextAllCaps(isTextAllCaps: Boolean) {
        this.textAllCaps = isTextAllCaps
        updateTabStyles()
    }

    fun getTitleView(tab: Int): TextView? {
        val tabView = tabContainer.getChildAt(tab)
        return tabView.findViewById<View>(R.id.tv_tab_title) as TextView?
    }

    fun showBadge(position: Int, num: Int) {
        var position = position
        if (position >= this.tabCount) {
            position = this.tabCount - 1
        }
        val tabView = tabContainer.getChildAt(position)
        val badgeView: BadgeView? = tabView.findViewById<View>(R.id.bv_num) as BadgeView?
        if (badgeView != null) {
            BadgeUtils.showBadge(badgeView, num)
            if (initMap.get(position) != null && initMap.get(position)) {
                return
            }
            setBadgeMargin(position, 2f, 2f)
            initMap.put(position, true)
        }
    }

    fun showBadge(position: Int) {
        var position = position
        if (position >= this.tabCount) {
            position = this.tabCount - 1
        }
        showBadge(position, 0)
    }

    fun hideBadge(position: Int) {
        var position = position
        if (position >= this.tabCount) {
            position = this.tabCount - 1
        }
        val tabView = tabContainer.getChildAt(position)
        val tipView: BadgeView? = tabView.findViewById<View?>(R.id.bv_num) as BadgeView?
        if (tipView != null) {
            tipView.visibility = GONE
        }
    }

    fun setBadgeMargin(position: Int, leftPadding: Float, bottomPadding: Float) {
        var position = position
        if (position >= this.tabCount) {
            position = this.tabCount - 1
        }
        val tabView = tabContainer.getChildAt(position)
        val badgeView: BadgeView? = tabView.findViewById<View>(R.id.bv_num) as BadgeView?
        if (badgeView != null) {
            textPaint.textSize = tabTextSize
            val textHeight = textPaint.descent() - textPaint.ascent()
            val lp: MarginLayoutParams = badgeView.layoutParams as MarginLayoutParams
            lp.leftMargin = dp2px(leftPadding)
            lp.topMargin =
                if (viewHeight > 0) (viewHeight - textHeight).toInt() / 2 -
                        dp2px(bottomPadding) else dp2px(
                    bottomPadding
                )
            badgeView.layoutParams = lp
        }
    }

    fun getBadgeView(position: Int): BadgeView? {
        var position = position
        if (position >= this.tabCount) {
            position = this.tabCount - 1
        }
        val tabView = tabContainer.getChildAt(position)
        return tabView.findViewById<View?>(R.id.bv_num) as BadgeView?
    }

    fun setOnTabSelectListener(listener: OnTabSelectListener) {
        this.onTabSelectListener = listener
    }

    private fun dp2px(dpVal: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dpVal,
            context.resources.displayMetrics
        ).toInt()
    }

    private fun sp2px(spVal: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            spVal,
            context.resources.displayMetrics
        ).toInt()
    }

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable("instanceState", super.onSaveInstanceState())
        bundle.putInt("currentTab", currentTab)
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        var state: Parcelable? = state
        if (state is Bundle) {
            val bundle = state
            currentTab = bundle.getInt("currentTab")
            state = bundle.getParcelable("instanceState")
            if (currentTab != 0 && tabContainer.isNotEmpty()) {
                updateTabSelection(currentTab)
            }
        }
        super.onRestoreInstanceState(state)
    }

    interface OnTabSelectListener {

        fun onTabSelected(
            position: Int
        )

        fun onTabReselected(
            position: Int
        )
    }

    internal class IndicatorPoint {
        var left: Float = 0f
        var right: Float = 0f
    }

    internal class PointEvaluator : TypeEvaluator<IndicatorPoint> {

        override fun evaluate(
            fraction: Float,
            startValue: IndicatorPoint,
            endValue: IndicatorPoint
        ): IndicatorPoint {
            val left = startValue.left + fraction * (endValue.left - startValue.left)
            val right = startValue.right + fraction * (endValue.right - startValue.right)
            val point = IndicatorPoint()
            point.left = left
            point.right = right
            return point
        }
    }

    companion object {
        private const val TEXT_BOLD_NONE = 0
        private const val TEXT_BOLD_WHEN_SELECT = 1
        private const val TEXT_BOLD_BOTH = 2
    }
}