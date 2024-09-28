package dora.widget

import android.animation.Animator
import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import dora.widget.tabbar.R

class DoraTabBar @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {

    private var position = 0
    private var toPosition = 0
    private var positionOffset = 0f
    private lateinit var wrappedTabLayoutParams: LinearLayout.LayoutParams
    private lateinit var divideTabLayoutParams: LinearLayout.LayoutParams
    private var tabContainer: LinearLayout = LinearLayout(context)
    val tabCount: Int
        get() = tabContainer.childCount
    private var tabTextSize = 15f
    private var tabTextColor = Color.BLACK
    private var indicatorHeight = 10
    private var indicatorColor = Color.BLACK
    private var tabHorizontalPadding = 20
    private var tabVerticalPadding = 15
    private var tabColor = Color.TRANSPARENT
    private var selectedTabTextColor = 0
    private var textAllCaps = true
    private var rectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var onTabClickListener: OnTabClickListener? = null
    var tabTitles: MutableList<String> = arrayListOf()
    private var tabScrollX = 0
    private var indicatorRect = RectF()

    private var isDivide: Boolean = false
        set(isDivide) {
            if (isDivide != field) {
                field = isDivide
            }
        }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        isFillViewport = true
        isHorizontalScrollBarEnabled = false
        divideTabLayoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
        )
        wrappedTabLayoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        )
        rectPaint.style = Paint.Style.FILL
        tabContainer.layoutParams = LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        tabContainer.orientation = LinearLayout.HORIZONTAL
        addView(tabContainer)
        initAttrs(context, attrs, defStyleAttr)
    }

    private fun initAttrs(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.DoraTabBar, defStyleAttr, 0)
        indicatorColor = a.getColor(R.styleable.DoraTabBar_dview_indicatorColor, Color.CYAN)
        indicatorHeight = a.getDimensionPixelSize(R.styleable.DoraTabBar_dview_indicatorHeight, indicatorHeight)
        tabHorizontalPadding = a.getDimensionPixelSize(R.styleable.DoraTabBar_dview_tabHorizontalPadding, tabHorizontalPadding)
        tabVerticalPadding = a.getDimensionPixelSize(R.styleable.DoraTabBar_dview_tabVerticalPadding, tabVerticalPadding)
        tabColor = a.getColor(
                R.styleable.DoraTabBar_dview_tabColor,
                tabColor
        )
        tabTextColor = a.getColor(R.styleable.DoraTabBar_dview_tabTextColor, tabTextColor)
        selectedTabTextColor = a.getColor(
                R.styleable.DoraTabBar_dview_tabSelectedTextColor,
            Color.CYAN
        )
        tabTextSize = a.getDimension(
                R.styleable.DoraTabBar_dview_tabTextSize, tabTextSize
        )
        isDivide = a.getBoolean(R.styleable.DoraTabBar_dview_isDivide, isDivide)
        textAllCaps = a.getBoolean(R.styleable.DoraTabBar_dview_textAllCaps, textAllCaps)
        a.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        drawIndicator(canvas)
    }

    private fun drawIndicator(canvas: Canvas) {
        rectPaint.color = indicatorColor
        val currentTab = tabContainer.getChildAt(position)
        if (currentTab != null) {
            var left = currentTab.left.toFloat()
            var right = currentTab.right.toFloat()
            if (positionOffset > 0f && position >= 0 && position < tabCount - 1 && toPosition >= 0
                    && toPosition < tabCount - 1) {
                val moveToTab = tabContainer.getChildAt(toPosition)
                val tabLeft = moveToTab.left.toFloat()
                val tabRight = moveToTab.right.toFloat()
                left = positionOffset * tabLeft + (1f - positionOffset) * left
                right = positionOffset * tabRight + (1f - positionOffset) * right
            }
            indicatorRect[left, (height - indicatorHeight).toFloat(), right] =
                    height.toFloat()
            canvas.drawRect(indicatorRect, rectPaint)
        }
    }

    fun setTitles(titles: Array<String>) {
        for (title in titles) {
            addTextTab(title)
        }
    }

    interface OnTabClickListener {
        fun onTabClick(view: View, position: Int)
    }

    fun setOnTabClickListener(l: OnTabClickListener) {
        onTabClickListener = l
    }

    /**
     * 通常使用在跟viewPager联动。
     */
    fun offsetTabForViewPager(position: Int, positionOffset: Float) {
        toPosition = position + 1
        offsetTab(position, positionOffset)
    }

    private fun offsetTab(position: Int, positionOffset: Float) {
        if (position != this.position || positionOffset != this.positionOffset) {
            this.position = position
            this.positionOffset = positionOffset
            updateSelectedTab()
        }
    }

    fun offsetTabOnClick(position: Int) {
        toPosition = position
        val animator = ValueAnimator.ofObject(
                AnimationEvaluator(), 0, 1)
        animator.addUpdateListener { animation: ValueAnimator ->
            val value = animation.animatedValue as Float
            offsetTab(this.position, value)
            invalidate()
        }
        animator.interpolator = LinearInterpolator()
        animator.setDuration(300).start()
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                this@DoraTabBar.position = position
                positionOffset = 0f
                updateSelectedTab()
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
    }

    private inner class AnimationEvaluator : TypeEvaluator<Float> {
        override fun evaluate(fraction: Float, startValue: Float, endValue: Float): Float {
            return if (endValue > startValue) {
                startValue + fraction * (endValue - startValue)
            } else {
                startValue - fraction * (startValue - endValue)
            }
        }
    }

    private fun scrollToChild(position: Int, offsetPixels: Int) {
        if (tabCount == 0) {
            return
        }
        val scrollOffset = (width - getTabWidth(position)) / 2
        var scrollX = getTabLeft(position) + offsetPixels
        if (position > 0 || offsetPixels > 0) {
            scrollX -= scrollOffset
        }
        if (scrollX != this.tabScrollX) {
            this.tabScrollX = scrollX
            smoothScrollTo(scrollX, 0)
        }
    }

    fun getTabView(position: Int): View {
        return tabContainer.getChildAt(position)
    }

    fun getTabWidth(position: Int): Int {
        return getTabView(position).width
    }

    fun getTabLeft(position: Int): Int {
        return getTabView(position).left
    }

    fun getTabRight(position: Int): Int {
        return getTabView(position).right
    }

    val tabContainerWidth: Int
        get() = tabContainer.width

    private fun invalidateView() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            invalidate()
        } else {
            postInvalidate()
        }
    }

    fun setSelectedPosition(position: Int) {
        this.position = position
        updateSelectedTab()
    }

    fun updateSelectedTab() {
        for (index in 0 until tabTitles.size) {
            val tabView = getTabView(index) as TextView
            if (index == position) {
                tabView.setTextColor(selectedTabTextColor)
            } else {
                tabView.setTextColor(tabTextColor)
            }
        }
        scrollToChild(position, (positionOffset * getTabWidth(position)).toInt())
        invalidateView()
    }

    fun addTextTab(title: String) {
        addTextTab(tabTitles.size, title)
    }

    fun addTextTab(position: Int, title: String) {
        val textTab = TextView(context)
        textTab.text = title
        if (this.position == position) {
            textTab.setTextColor(selectedTabTextColor)
        } else {
            textTab.setTextColor(tabTextColor)
        }
        textTab.textSize = tabTextSize
        textTab.gravity = Gravity.CENTER
        textTab.setSingleLine()
        textTab.isFocusable = true
        textTab.setPaddingRelative(tabHorizontalPadding, tabVerticalPadding, tabHorizontalPadding, tabVerticalPadding)
        textTab.setOnClickListener { v ->
            offsetTabOnClick(position)
            onTabClickListener?.onTabClick(v, position)
        }
        tabContainer.addView(
                textTab,
                position,
                if (isDivide) divideTabLayoutParams else wrappedTabLayoutParams
        )
        tabTitles.add(title)
    }

    init {
        init(context, attrs, defStyleAttr)
    }
}