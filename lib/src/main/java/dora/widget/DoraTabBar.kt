package dora.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseArray
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.toColorInt
import androidx.core.view.isNotEmpty
import dora.widget.badge.BadgeUtils
import dora.widget.badge.BadgeView
import dora.widget.tabbar.R
import java.util.Locale
import kotlin.math.max

/**
 * DoraTabBar
 *
 * 一个支持横向滚动的 TabBar 控件。
 *
 * 主要功能：
 *
 * 1. 支持多个 Tab 横向排列。
 * 2. 支持 Tab 标题。
 * 3. 支持选中 / 未选中状态。
 * 4. 支持选中 / 未选中图标。
 * 5. 支持图标位于文字的上、下、左、右。
 * 6. 支持三种 Indicator 样式：
 *      - 普通下划线
 *      - 三角形
 *      - Block 背景块
 * 7. 支持 Tab 底部 / 顶部整体 Underline。
 * 8. 支持 Tab 之间的 Divider。
 * 9. 支持 Badge 小红点 / 数字角标。
 * 10. 支持 Tab 点击后自动滚动到中间位置。
 * 11. 支持保存和恢复当前选中的 Tab。
 *
 * 注意：
 * 当前控件继承自 HorizontalScrollView，因此 Tab 数量较多时
 * 可以通过横向滚动查看所有 Tab。
 */
class DoraTabBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {

    /**
     * 所有 Tab 数据。
     *
     * 数据本身与实际 Tab View 分离：
     *
     * tabs：
     *      保存 Tab 的数据模型。
     *
     * tabContainer：
     *      保存真正显示出来的 Tab View。
     */
    private val tabs = arrayListOf<DoraTab>()

    /**
     * Tab 的实际容器。
     *
     * HorizontalScrollView
     *      └── LinearLayout
     *              ├── Tab 0
     *              ├── Tab 1
     *              ├── Tab 2
     *              └── ...
     */
    private val tabContainer: LinearLayout

    /**
     * Indicator 动画时长。
     *
     * 0：
     *      立即切换。
     *
     * > 0：
     *      使用动画切换。
     */
    private var indicatorAnimationDuration = 250L

    /**
     * 当前选中的 Tab 下标。
     */
    private var currentTab = 0

    /**
     * 当前 Tab 数量。
     *
     * 通过 setTabs() / notifyDataSetChanged() 自动更新。
     */
    var tabCount: Int = 0
        private set

    /**
     * Indicator 当前绘制区域。
     *
     * 该 Rect 主要根据当前 Tab 的位置计算。
     */
    private val indicatorRect = Rect()

    /**
     * 当前 Tab 对应的区域。
     *
     * 主要用于保存 Indicator 计算出来的 Tab 范围。
     */
    private val tabRect = Rect()

    /**
     * Indicator 动画当前左边位置。
     */
    private var indicatorLeft = 0f

    /**
     * Indicator 动画当前右边位置。
     */
    private var indicatorRight = 0f

    /**
     * Indicator 动画对象。
     */
    private var indicatorAnimator: ValueAnimator? = null

    /**
     * Indicator 使用的 Drawable。
     *
     * 主要用于绘制普通 Indicator 和 Block Indicator。
     */
    private val indicatorDrawable = GradientDrawable()

    /**
     * Indicator 初始化标志。
     */
    private var indicatorInitialized = false

    /**
     * 用于绘制整体 Underline。
     */
    private val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * 用于绘制 Tab 之间的分割线。
     */
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * 用于绘制三角形 Indicator。
     */
    private val trianglePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * 用于计算文字高度。
     *
     * Badge 定位时需要使用文字高度。
     */
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * 记录某一个 Tab 的 Badge Margin 是否已经初始化。
     *
     * key：
     *      Tab position
     *
     * value：
     *      是否已经初始化过 Badge 位置。
     */
    private val initMap = SparseArray<Boolean>()

    /**
     * 三角形 Indicator 的 Path。
     */
    private val trianglePath = Path()

    /**
     * Indicator 类型。
     *
     * 默认使用普通 Indicator。
     */
    @IndicatorType
    private var indicatorType: Int = INDICATOR_NORMAL

    /**
     * Tab 左右 Padding。
     */
    private var tabPadding = 0f

    /**
     * 是否给 Tab 设置固定分割模式。
     */
    private var tabIsDivided = false

    /**
     * Tab 固定宽度。
     *
     * <= 0 表示不使用固定宽度。
     */
    private var tabWidth = 0f

    /**
     * Indicator 颜色。
     */
    private var indicatorColor = 0

    /**
     * Indicator 宽度。
     *
     * < 0 时表示根据当前 Tab 宽度处理。
     */
    private var indicatorWidth = 0f

    /**
     * Indicator 高度。
     *
     * < 0 时表示根据控件高度和上下 Margin 自动计算。
     */
    private var indicatorHeight = 0f

    /**
     * Indicator 圆角半径。
     */
    private var indicatorCornerRadius = 0f

    /**
     * Indicator 左侧 Margin。
     */
    var indicatorMarginLeft: Float = 0f
        private set

    /**
     * Indicator 顶部 Margin。
     */
    var indicatorMarginTop: Float = 0f
        private set

    /**
     * Indicator 右侧 Margin。
     */
    var indicatorMarginRight: Float = 0f
        private set

    /**
     * Indicator 底部 Margin。
     */
    var indicatorMarginBottom: Float = 0f
        private set

    /**
     * Indicator 所在位置。
     *
     * 常见值：
     *      Gravity.TOP
     *      Gravity.BOTTOM
     */
    private var indicatorGravity = 0

    /**
     * 整体 Underline 的颜色。
     */
    private var underlineColor = 0

    /**
     * 整体 Underline 的高度。
     */
    private var underlineHeight = 0f

    /**
     * 整体 Underline 的位置。
     */
    private var underlineGravity = 0

    /**
     * Tab 之间 Divider 的颜色。
     */
    private var dividerColor = 0

    /**
     * Tab 之间 Divider 的宽度。
     */
    private var dividerWidth = 0f

    /**
     * Divider 上下 Padding。
     */
    private var dividerPadding = 0f

    /**
     * Tab 文字大小，单位为 px。
     */
    private var tabTextSize = 0f

    /**
     * 当前选中 Tab 的文字颜色。
     */
    private var tabTextSelectedColor = 0

    /**
     * 未选中 Tab 的文字颜色。
     */
    private var tabTextUnselectedColor = 0

    /**
     * 文字加粗模式。
     *
     * 具体取值：
     *      TEXT_BOLD_NONE
     *      TEXT_BOLD_WHEN_SELECT
     *      TEXT_BOLD_BOTH
     */
    @TextBoldMode
    private var tabTextBold: Int = TEXT_BOLD_NONE

    /**
     * 是否将 Tab 标题转换成大写。
     */
    private var tabTextAllCaps = false

    /**
     * 是否显示 Tab Icon。
     */
    private var iconVisible = false

    /**
     * Icon 所在方向。
     */
    private var iconGravity = 0

    /**
     * Icon 固定宽度。
     *
     * <= 0 表示使用 Drawable 原始宽度。
     */
    private var iconWidth = 0f

    /**
     * Icon 固定高度。
     *
     * <= 0 表示使用 Drawable 原始高度。
     */
    private var iconHeight = 0f

    /**
     * Icon 与标题之间的间距。
     */
    private var iconMargin = 0f

    /**
     * 控件的高度。
     */
    private var viewHeight = 0

    /**
     * 上一次滚动的位置。
     *
     * 避免重复调用 scrollTo()。
     */
    private var lastScrollX = 0

    /**
     * 是否开启点击 Tab 后自动吸附 / 居中。
     *
     * 当前代码中该属性仅保存状态，
     * 实际滚动逻辑仍由 scrollToCurrentTab() 完成。
     */
    private var snapOnTabClick = false

    /**
     * Tab 选择监听器。
     */
    private var onTabSelectListener: OnTabSelectListener? = null

    init {
        // 让 HorizontalScrollView 尽可能占满父容器宽度。
        isFillViewport = true
        // 允许当前 View 自己执行 onDraw()。
        // 因为 Indicator、Divider、Underline 都需要由自身绘制。
        setWillNotDraw(false)
        // 不裁剪子 View。
        // Badge 等元素如果超出 Tab 范围，需要允许显示。
        clipChildren = false
        clipToPadding = false
        // 创建真正承载 Tab View 的 LinearLayout。
        tabContainer = LinearLayout(context)
        // 将 Tab 容器添加到 HorizontalScrollView 中。
        addView(tabContainer)
        // 从 XML 属性中读取配置。
        obtainAttributes(context, attrs)
        val height =
            attrs?.getAttributeValue("http://schemas.android.com/apk/res/android", "layout_height")
        if (height == LayoutParams.MATCH_PARENT.toString() + "") {
        } else if (height == LayoutParams.WRAP_CONTENT.toString() + "") {
        } else {
            val systemAttrs = intArrayOf(android.R.attr.layout_height)
            context.withStyledAttributes(attrs, systemAttrs) {
                viewHeight = getDimensionPixelSize(0, LayoutParams.WRAP_CONTENT)
            }
        }
    }

    /**
     * 从 XML AttributeSet 中读取 DoraTabBar 的所有自定义属性。
     *
     * 所有 dview_xxx 属性最终都会保存到对应成员变量中。
     */
    private fun obtainAttributes(context: Context, attrs: AttributeSet?) {
        context.withStyledAttributes(attrs, R.styleable.DoraTabBar) {
            // Indicator 类型。
            indicatorType =
                getInt(
                    R.styleable.DoraTabBar_dview_tb_indicatorType,
                    INDICATOR_NORMAL
                )
            // Indicator 颜色。
            //
            // Block 默认使用蓝灰色，
            // 其它样式默认使用白色。
            indicatorColor = getColor(
                R.styleable.DoraTabBar_dview_tb_indicatorColor,
                if (indicatorType == INDICATOR_BLOCK) {
                    "#4CAF50".toColorInt()
                } else {
                    "#FFFFFF".toColorInt()
                }
            )
            // Indicator 宽度。
            //
            // Triangle 默认宽度 10dp。
            // 其它样式默认 -1，表示根据 Tab 宽度处理。
            indicatorWidth = getDimension(
                R.styleable.DoraTabBar_dview_tb_indicatorWidth,
                dp2px(
                    (if (indicatorType == INDICATOR_TRIANGLE) 10 else -1).toFloat()
                ).toFloat()
            )
            // Indicator 高度。
            //
            // Triangle 默认 4dp。
            // Block 默认 -1，表示自动撑满。
            // Normal 默认 2dp。
            indicatorHeight = getDimension(
                R.styleable.DoraTabBar_dview_tb_indicatorHeight,
                dp2px(
                    (
                            if (indicatorType == INDICATOR_TRIANGLE) {
                                4
                            } else if (indicatorType == INDICATOR_BLOCK) {
                                -1
                            } else {
                                2
                            }
                            ).toFloat()
                ).toFloat()
            )
            // Indicator 圆角。
            //
            // Block 默认 -1，表示自动根据高度计算最大圆角。
            // Normal 默认 0。
            indicatorCornerRadius = getDimension(
                R.styleable.DoraTabBar_dview_tb_indicatorCornerRadius,
                dp2px(
                    (if (indicatorType == INDICATOR_BLOCK) -1 else 0).toFloat()
                ).toFloat()
            )
            // Indicator 四周 Margin。
            indicatorMarginLeft = getDimension(
                R.styleable.DoraTabBar_dview_tb_indicatorMarginLeft,
                dp2px(0f).toFloat()
            )
            indicatorMarginTop = getDimension(
                R.styleable.DoraTabBar_dview_tb_indicatorMarginTop,
                dp2px(
                    (if (indicatorType == INDICATOR_BLOCK) 7 else 0).toFloat()
                ).toFloat()
            )
            indicatorMarginRight = getDimension(
                R.styleable.DoraTabBar_dview_tb_indicatorMarginRight,
                dp2px(0f).toFloat()
            )
            indicatorMarginBottom = getDimension(
                R.styleable.DoraTabBar_dview_tb_indicatorMarginBottom,
                dp2px(
                    (if (indicatorType == INDICATOR_BLOCK) 7 else 0).toFloat()
                ).toFloat()
            )
            // Indicator 在顶部还是底部。
            indicatorGravity =
                getInt(
                    R.styleable.DoraTabBar_dview_tb_indicatorGravity,
                    Gravity.BOTTOM
                )
            // Indicator 动画时长。
            indicatorAnimationDuration =
                getInt(
                    R.styleable.DoraTabBar_dview_tb_indicatorAnimationDuration,
                    250
                ).toLong()
            // 整体 Underline。
            underlineColor =
                getColor(
                    R.styleable.DoraTabBar_dview_tb_underlineColor,
                    "#FFFFFF".toColorInt()
                )
            underlineHeight =
                getDimension(
                    R.styleable.DoraTabBar_dview_tb_underlineHeight,
                    dp2px(0f).toFloat()
                )
            underlineGravity =
                getInt(
                    R.styleable.DoraTabBar_dview_tb_underlineGravity,
                    Gravity.BOTTOM
                )
            // Tab Divider。
            dividerColor =
                getColor(
                    R.styleable.DoraTabBar_dview_tb_dividerColor,
                    "#FFFFFF".toColorInt()
                )
            dividerWidth =
                getDimension(
                    R.styleable.DoraTabBar_dview_tb_dividerWidth,
                    dp2px(0f).toFloat()
                )
            dividerPadding =
                getDimension(
                    R.styleable.DoraTabBar_dview_tb_dividerPadding,
                    dp2px(12f).toFloat()
                )
            // Tab 文字。
            tabTextSize = getDimension(
                R.styleable.DoraTabBar_dview_tb_tabTextSize,
                sp2px(14f).toFloat()
            )
            tabTextSelectedColor =
                getColor(
                    R.styleable.DoraTabBar_dview_tb_tabSelectedTextColor,
                    "#FFFFFF".toColorInt()
                )
            tabTextUnselectedColor =
                getColor(
                    R.styleable.DoraTabBar_dview_tb_tabUnselectedTextColor,
                    "#AAFFFFFF".toColorInt()
                )
            tabTextBold =
                getInt(
                    R.styleable.DoraTabBar_dview_tb_tabTextBold,
                    TEXT_BOLD_NONE
                )
            tabTextAllCaps =
                getBoolean(
                    R.styleable.DoraTabBar_dview_tb_tabTextAllCaps,
                    false
                )
            // Icon。
            iconVisible =
                getBoolean(
                    R.styleable.DoraTabBar_dview_tb_iconVisible,
                    true
                )
            iconGravity =
                getInt(
                    R.styleable.DoraTabBar_dview_tb_iconGravity,
                    Gravity.TOP
                )
            iconWidth =
                getDimension(
                    R.styleable.DoraTabBar_dview_tb_iconWidth,
                    dp2px(0f).toFloat()
                )
            iconHeight =
                getDimension(
                    R.styleable.DoraTabBar_dview_tb_iconHeight,
                    dp2px(0f).toFloat()
                )
            iconMargin =
                getDimension(
                    R.styleable.DoraTabBar_dview_tb_iconMargin,
                    dp2px(2.5f).toFloat()
                )
            // Tab 是否平均分割。
            tabIsDivided =
                getBoolean(
                    R.styleable.DoraTabBar_dview_tb_tabIsDivided,
                    false
                )
            // Tab 固定宽度。
            tabWidth =
                getDimension(
                    R.styleable.DoraTabBar_dview_tb_tabWidth,
                    dp2px(-1f).toFloat()
                )
            // Tab Padding。
            //
            // 如果 Tab 使用固定宽度或分割模式，
            // 默认不再额外增加左右 Padding。
            tabPadding =
                getDimension(
                    R.styleable.DoraTabBar_dview_tb_tabPadding,
                    (
                            if (tabIsDivided || tabWidth > 0) {
                                dp2px(0f)
                            } else {
                                dp2px(20f)
                            }
                            ).toFloat()
                )
        }
    }

    /**
     * dp 转 px。
     *
     * 参数：
     * dpVal - dp 数值。
     *
     * 返回：
     * 对应的 px 整数值。
     */
    private fun dp2px(dpVal: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dpVal,
            context.resources.displayMetrics
        ).toInt()
    }

    /**
     * sp 转 px。
     *
     * 参数：
     * spVal - sp 数值。
     *
     * 返回：
     * 对应的 px 整数值。
     */
    private fun sp2px(spVal: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            spVal,
            context.resources.displayMetrics
        ).toInt()
    }

    /**
     * 设置全部 Tab 数据。
     *
     * 设置完成后会立即重新创建所有 Tab View。
     *
     * Kotlin：
     * setTabs(tab1, tab2, tab3)
     *
     * Java：
     * setTabs(new DoraTab[]{tab1, tab2, tab3})
     */
    fun setTabs(
        vararg tabs: DoraTab
    ): DoraTabBar {
        this.tabs.clear()
        this.tabs.addAll(tabs)
        notifyDataSetChanged()
        return this
    }

    /**
     * 使用 ArrayList 设置全部 Tab 数据。
     *
     * 主要方便 Java 调用。
     *
     * Java：
     * setTabs(new ArrayList<>(...))
     */
    fun setTabs(
        tabs: ArrayList<DoraTab>
    ): DoraTabBar {
        this.tabs.clear()
        this.tabs.addAll(tabs)
        notifyDataSetChanged()
        return this
    }

    /**
     * 设置纯 Text Tab。
     *
     * Kotlin：
     * setTextTabs("首页", "消息", "我的")
     *
     * Java：
     * setTextTabs(new String[]{"首页", "消息", "我的"})
     */
    fun setTextTabs(
        vararg titles: String
    ): DoraTabBar {
        tabs.clear()
        titles.forEach { title ->
            tabs.add(
                DoraTab(
                    tabTitle = title
                )
            )
        }
        notifyDataSetChanged()
        return this
    }

    /**
     * 使用 ArrayList 设置纯 Text Tab。
     *
     * 主要方便 Java 调用。
     */
    fun setTextTabs(
        titles: ArrayList<String>
    ): DoraTabBar {
        tabs.clear()
        titles.forEach { title ->
            tabs.add(
                DoraTab(
                    tabTitle = title
                )
            )
        }
        notifyDataSetChanged()
        return this
    }

    /**
     * 设置纯 Icon Tab。
     *
     * Pair.first  = 选中状态 Icon。
     * Pair.second = 未选中状态 Icon。
     *
     * Kotlin：
     * setIconTabs(
     *     R.drawable.ic_home_selected to R.drawable.ic_home,
     *     R.drawable.ic_mine_selected to R.drawable.ic_mine
     * )
     *
     * Java：
     * setIconTabs(new Pair[]{
     *     new Pair<>(R.drawable.ic_home_selected, R.drawable.ic_home),
     *     new Pair<>(R.drawable.ic_mine_selected, R.drawable.ic_mine)
     * })
     */
    fun setIconTabs(
        vararg icons: Pair<Int, Int>
    ): DoraTabBar {
        tabs.clear()
        icons.forEach { (selectedIcon, unselectedIcon) ->
            tabs.add(
                DoraTab(
                    tabTitle = "",
                    tabSelectedIcon = selectedIcon,
                    tabUnselectedIcon = unselectedIcon
                )
            )
        }
        notifyDataSetChanged()
        return this
    }

    /**
     * 使用 ArrayList 设置纯 Icon Tab。
     *
     * 主要方便 Java 调用。
     */
    fun setIconTabs(
        icons: ArrayList<Pair<Int, Int>>
    ): DoraTabBar {
        tabs.clear()
        icons.forEach { (selectedIcon, unselectedIcon) ->
            tabs.add(
                DoraTab(
                    tabTitle = "",
                    tabSelectedIcon = selectedIcon,
                    tabUnselectedIcon = unselectedIcon
                )
            )
        }
        notifyDataSetChanged()
        return this
    }

    /**
     * 添加一个 Tab。
     *
     * 添加完成后会自动刷新 TabBar。
     */
    fun addTab(
        tab: DoraTab
    ): DoraTabBar {
        tabs.add(tab)
        notifyDataSetChanged()
        return this
    }

    /**
     * 添加多个 Tab。
     *
     * Kotlin：
     * addTab(tab1, tab2, tab3)
     *
     * Java：
     * addTab(new DoraTab[]{tab1, tab2, tab3})
     */
    fun addTab(
        vararg tabs: DoraTab
    ): DoraTabBar {
        this.tabs.addAll(tabs)
        notifyDataSetChanged()
        return this
    }

    /**
     * 使用 ArrayList 添加多个 Tab。
     *
     * 主要方便 Java 调用。
     */
    fun addTab(
        tabs: ArrayList<DoraTab>
    ): DoraTabBar {
        this.tabs.addAll(tabs)
        notifyDataSetChanged()
        return this
    }

    /**
     * 添加一个 Tab 到指定位置。
     *
     * position 超出范围时自动限制到合法位置。
     */
    fun addTab(
        position: Int,
        tab: DoraTab
    ): DoraTabBar {
        val index = position.coerceIn(0, tabs.size)
        tabs.add(index, tab)
        notifyDataSetChanged()
        return this
    }

    /**
     * 添加多个 Tab 到指定位置。
     *
     * Tab 会按照传入顺序插入。
     */
    fun addTab(
        position: Int,
        vararg tabs: DoraTab
    ): DoraTabBar {
        val index = position.coerceIn(0, this.tabs.size)
        this.tabs.addAll(
            index,
            tabs.toList()
        )
        notifyDataSetChanged()
        return this
    }

    /**
     * 使用 ArrayList 添加多个 Tab 到指定位置。
     *
     * 主要方便 Java 调用。
     */
    fun addTab(
        position: Int,
        tabs: ArrayList<DoraTab>
    ): DoraTabBar {
        val index = position.coerceIn(0, this.tabs.size)
        this.tabs.addAll(
            index,
            tabs
        )
        notifyDataSetChanged()
        return this
    }

    /**
     * 添加一个纯文字 Tab。
     *
     * 不设置 Icon。
     */
    fun addTextTab(
        title: String
    ): DoraTabBar {
        return addTab(
            DoraTab(
                tabTitle = title
            )
        )
    }

    /**
     * 添加多个纯文字 Tab。
     *
     * Kotlin：
     * addTextTab("首页", "消息", "我的")
     *
     * Java：
     * addTextTab(new String[]{"首页", "消息", "我的"})
     */
    fun addTextTab(
        vararg titles: String
    ): DoraTabBar {
        titles.forEach { title ->
            tabs.add(
                DoraTab(
                    tabTitle = title
                )
            )
        }
        notifyDataSetChanged()
        return this
    }

    /**
     * 使用 ArrayList 添加多个纯文字 Tab。
     *
     * 主要方便 Java 调用。
     */
    fun addTextTab(
        titles: ArrayList<String>
    ): DoraTabBar {
        titles.forEach { title ->
            tabs.add(
                DoraTab(
                    tabTitle = title
                )
            )
        }
        notifyDataSetChanged()
        return this
    }

    /**
     * 添加一个纯文字 Tab 到指定位置。
     */
    fun addTextTab(
        position: Int,
        title: String
    ): DoraTabBar {
        return addTab(
            position,
            DoraTab(
                tabTitle = title
            )
        )
    }

    /**
     * 添加多个纯文字 Tab 到指定位置。
     */
    fun addTextTab(
        position: Int,
        vararg titles: String
    ): DoraTabBar {
        val tabList = ArrayList<DoraTab>(titles.size)
        titles.forEach { title ->
            tabList.add(
                DoraTab(
                    tabTitle = title
                )
            )
        }
        return addTab(
            position,
            tabList
        )
    }

    /**
     * 使用 ArrayList 添加多个纯文字 Tab 到指定位置。
     *
     * 主要方便 Java 调用。
     */
    fun addTextTab(
        position: Int,
        titles: ArrayList<String>
    ): DoraTabBar {
        val tabList = ArrayList<DoraTab>(titles.size)
        titles.forEach { title ->
            tabList.add(
                DoraTab(
                    tabTitle = title
                )
            )
        }
        return addTab(
            position,
            tabList
        )
    }

    /**
     * 添加一个纯 Icon Tab。
     *
     * @param selectedIcon 选中状态 Icon。
     * @param unselectedIcon 未选中状态 Icon。
     */
    fun addIconTab(
        @DrawableRes selectedIcon: Int,
        @DrawableRes unselectedIcon: Int
    ): DoraTabBar {
        return addTab(
            DoraTab(
                tabTitle = "",
                tabSelectedIcon = selectedIcon,
                tabUnselectedIcon = unselectedIcon
            )
        )
    }

    /**
     * 添加多个纯 Icon Tab。
     *
     * Pair.first  = 选中 Icon。
     * Pair.second = 未选中 Icon。
     *
     * Kotlin：
     *
     * addIconTab(
     *     R.drawable.ic_home_selected to R.drawable.ic_home,
     *     R.drawable.ic_message_selected to R.drawable.ic_message,
     *     R.drawable.ic_mine_selected to R.drawable.ic_mine
     * )
     */
    fun addIconTab(
        vararg icons: Pair<Int, Int>
    ): DoraTabBar {
        icons.forEach { (selectedIcon, unselectedIcon) ->
            tabs.add(
                DoraTab(
                    tabTitle = "",
                    tabSelectedIcon = selectedIcon,
                    tabUnselectedIcon = unselectedIcon
                )
            )
        }
        notifyDataSetChanged()
        return this
    }

    /**
     * 使用 ArrayList 添加多个纯 Icon Tab。
     *
     * 主要方便 Java 调用。
     */
    fun addIconTab(
        icons: ArrayList<Pair<Int, Int>>
    ): DoraTabBar {
        icons.forEach { (selectedIcon, unselectedIcon) ->
            tabs.add(
                DoraTab(
                    tabTitle = "",
                    tabSelectedIcon = selectedIcon,
                    tabUnselectedIcon = unselectedIcon
                )
            )
        }
        notifyDataSetChanged()
        return this
    }

    /**
     * 添加一个纯 Icon Tab 到指定位置。
     */
    fun addIconTab(
        position: Int,
        @DrawableRes selectedIcon: Int,
        @DrawableRes unselectedIcon: Int
    ): DoraTabBar {
        return addTab(
            position,
            DoraTab(
                tabTitle = "",
                tabSelectedIcon = selectedIcon,
                tabUnselectedIcon = unselectedIcon
            )
        )
    }

    /**
     * 添加多个纯 Icon Tab 到指定位置。
     *
     * Kotlin：
     *
     * addIconTab(
     *     1,
     *     R.drawable.ic_home_selected to R.drawable.ic_home,
     *     R.drawable.ic_message_selected to R.drawable.ic_message
     * )
     */
    fun addIconTab(
        position: Int,
        vararg icons: Pair<Int, Int>
    ): DoraTabBar {
        val tabList = ArrayList<DoraTab>(icons.size)
        icons.forEach { (selectedIcon, unselectedIcon) ->
            tabList.add(
                DoraTab(
                    tabTitle = "",
                    tabSelectedIcon = selectedIcon,
                    tabUnselectedIcon = unselectedIcon
                )
            )
        }
        return addTab(
            position,
            tabList
        )
    }

    /**
     * 使用 ArrayList 添加多个纯 Icon Tab 到指定位置。
     *
     * 主要方便 Java 调用。
     */
    fun addIconTab(
        position: Int,
        icons: ArrayList<Pair<Int, Int>>
    ): DoraTabBar {
        val tabList = ArrayList<DoraTab>(icons.size)
        icons.forEach { (selectedIcon, unselectedIcon) ->
            tabList.add(
                DoraTab(
                    tabTitle = "",
                    tabSelectedIcon = selectedIcon,
                    tabUnselectedIcon = unselectedIcon
                )
            )
        }
        return addTab(
            position,
            tabList
        )
    }

    /**
     * 根据 tabs 数据重新创建 Tab View。
     *
     * 流程：
     *
     * 1. 清空旧 Tab。
     * 2. 更新 Tab 数量。
     * 3. 根据 Icon 位置选择不同布局。
     * 4. 创建每一个 Tab。
     * 5. 最后统一更新 Tab 样式。
     */
    fun notifyDataSetChanged() {
        // 删除旧 View。
        tabContainer.removeAllViews()
        // 更新数量。
        tabCount = tabs.size
        // 获取经过 RTL/LTR 处理后的真实 Gravity。
        val realGravity = resolveIconGravity()
        for (i in 0 until tabCount) {
            // 根据 Icon 位置选择对应布局。
            val tabView = when (realGravity) {
                Gravity.LEFT -> {
                    inflate(
                        context,
                        R.layout.layout_tab_left,
                        null
                    )
                }
                Gravity.RIGHT -> {
                    inflate(
                        context,
                        R.layout.layout_tab_right,
                        null
                    )
                }
                Gravity.BOTTOM -> {
                    inflate(
                        context,
                        R.layout.layout_tab_bottom,
                        null
                    )
                }
                Gravity.TOP -> {
                    inflate(
                        context,
                        R.layout.layout_tab_top,
                        null
                    )
                }
                else -> {
                    inflate(
                        context,
                        R.layout.layout_tab_top,
                        null
                    )
                }
            }
            // 将 position 保存到 View tag。
            tabView.tag = i
            // 将 Tab 加入容器。
            addTab(
                i,
                tabs[i],
                tabView
            )
        }
        // 创建完所有 Tab 后统一应用样式。
        updateTabStyles()
    }

    /**
     * 获取处理 RTL / LTR 后的真实 Icon Gravity。
     *
     * 例如：
     *
     * Gravity.START
     *
     * 在 LTR 下：
     *      等价于 LEFT。
     *
     * 在 RTL 下：
     *      等价于 RIGHT。
     */
    private fun resolveIconGravity(): Int {
        return Gravity.getAbsoluteGravity(
            iconGravity,
            layoutDirection
        )
    }

    /**
     * 创建并添加一个 Tab。
     *
     * 参数：
     *
     * position：
     *      Tab 下标。
     *
     * entity：
     *      Tab 数据。
     *
     * tabView：
     *      Tab 对应的布局 View。
     */
    private fun addTab(
        position: Int,
        entity: DoraTab,
        tabView: View
    ) {
        // 获取标题 TextView。
        val tvTabTitle = tabView.findViewById<TextView>(R.id.tv_tab_title)
        // 获取图标 ImageView。
        val ivTabIcon = tabView.findViewById<ImageView>(R.id.iv_tab_icon)
        // 设置标题。
        tvTabTitle.text = entity.tabTitle
        // 默认先设置未选中图标。
        entity.tabUnselectedIcon?.let {
            ivTabIcon.setImageResource(
                it
            )
        }
        // 获取 Tab 内容容器。
        val contentLayout = tabView.findViewById<LinearLayout>(R.id.ll_tab)
        contentLayout?.let {
            when (resolveIconGravity()) {
                // Icon 在左边：
                // 横向排列：Icon + Text。
                Gravity.LEFT -> {
                    it.orientation = LinearLayout.HORIZONTAL
                    it.gravity = Gravity.CENTER_VERTICAL
                }
                // Icon 在右边：
                // 横向排列：Text + Icon。
                Gravity.RIGHT -> {
                    it.orientation = LinearLayout.HORIZONTAL
                    it.gravity = Gravity.CENTER_VERTICAL
                    // 重新调整子 View 顺序。
                    it.removeAllViews()
                    it.addView(
                        tvTabTitle,
                        LinearLayout.LayoutParams(
                            LayoutParams.WRAP_CONTENT,
                            LayoutParams.WRAP_CONTENT
                        )
                    )
                    it.addView(
                        ivTabIcon,
                        LinearLayout.LayoutParams(
                            LayoutParams.WRAP_CONTENT,
                            LayoutParams.WRAP_CONTENT
                        )
                    )
                }
                // Icon 在底部：
                // 垂直排列：Text + Icon。
                Gravity.BOTTOM -> {
                    it.orientation = LinearLayout.VERTICAL
                    it.gravity = Gravity.CENTER
                    it.removeAllViews()
                    it.addView(tvTabTitle)
                    it.addView(ivTabIcon)
                }
                // Icon 在顶部：
                // 垂直排列：Icon + Text。
                Gravity.TOP -> {
                    it.orientation = LinearLayout.VERTICAL
                    it.gravity = Gravity.CENTER
                }
                // 未知情况默认按照顶部处理。
                else -> {
                    it.orientation = LinearLayout.VERTICAL
                    it.gravity = Gravity.CENTER
                }
            }
        }
        /**
         * Tab 点击事件。
         */
        tabView.setOnClickListener {
            if (currentTab != position) {
                currentTab = position
                updateTabSelection(position)
                if (indicatorAnimationDuration > 0) {
                    animateIndicatorTo(position)
                    smoothScrollToCurrentTab()
                } else {
                    updateIndicatorImmediately(position)
                    scrollToCurrentTab()
                }
                onTabSelectListener?.onTabSelected(position)
            } else {
                onTabSelectListener?.onTabReselected(position)
            }
        }

        /**
         * 创建 Tab LayoutParams。
         *
         * 如果设置了固定宽度，则使用 tabWidth。
         * 否则使用 WRAP_CONTENT。
         */
        val lp = LinearLayout.LayoutParams(
            if (tabWidth > 0) {
                tabWidth.toInt()
            } else {
                LayoutParams.WRAP_CONTENT
            },
            LayoutParams.MATCH_PARENT
        )
        // 添加到指定位置。
        tabContainer.addView(
            tabView,
            position,
            lp
        )
    }

    /**
     * 更新所有 Tab 的基础样式。
     *
     * 包括：
     *
     * - 文字颜色
     * - 文字大小
     * - Padding
     * - 大小写
     * - 粗体
     * - Icon 显示隐藏
     * - Icon 尺寸
     * - Icon Margin
     */
    private fun updateTabStyles() {
        for (i in 0..<this.tabCount) {
            // 获取当前 Tab View。
            val v = tabContainer.getChildAt(i)
            // 获取标题。
            val tvTabTitle = v.findViewById<View?>(R.id.tv_tab_title) as TextView?
            // 获取 Icon。
            val ivTabIcon = v.findViewById<ImageView>(R.id.iv_tab_icon)
            if (tvTabTitle != null) {
                // 根据当前 Tab 是否选中设置文字颜色。
                tvTabTitle.setTextColor(
                    if (i == currentTab) {
                        tabTextSelectedColor
                    } else {
                        tabTextUnselectedColor
                    }
                )
                // 设置文字大小。
                tvTabTitle.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    tabTextSize
                )
                // 设置左右 Padding。
                tvTabTitle.setPadding(
                    tabPadding.toInt(),
                    0,
                    tabPadding.toInt(),
                    0
                )
                // 是否转换成大写。
                if (tabTextAllCaps) {
                    tvTabTitle.text =
                        tvTabTitle.text
                            .toString()
                            .uppercase(Locale.getDefault())
                }
                // 两种模式下直接设置粗体状态。
                if (tabTextBold == TEXT_BOLD_BOTH) {
                    // 所有 Tab 都加粗。
                    tvTabTitle.paint.isFakeBoldText = true
                } else if (tabTextBold == TEXT_BOLD_NONE) {
                    // 所有 Tab 都取消粗体。
                    tvTabTitle.paint.isFakeBoldText = false
                }
            }
            // Icon 是否显示。
            if (iconVisible) {
                ivTabIcon.visibility = VISIBLE
                // 根据配置生成 Icon LayoutParams。
                val lp = LinearLayout.LayoutParams(
                    if (iconWidth <= 0) {
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    } else {
                        iconWidth.toInt()
                    },
                    if (iconHeight <= 0) {
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    } else {
                        iconHeight.toInt()
                    }
                )
                // 根据 Icon 位置设置间距。
                if (resolveIconGravity() == Gravity.LEFT) {
                    // Icon 在左边，所以右侧留间距。
                    lp.rightMargin = iconMargin.toInt()
                } else if (resolveIconGravity() == Gravity.RIGHT) {
                    // Icon 在右边，所以左侧留间距。
                    lp.leftMargin = iconMargin.toInt()
                } else if (resolveIconGravity() == Gravity.BOTTOM) {
                    // Icon 在底部，所以顶部留间距。
                    lp.topMargin = iconMargin.toInt()
                } else if (resolveIconGravity() == Gravity.TOP) {
                    // Icon 在顶部，所以底部留间距。
                    lp.bottomMargin = iconMargin.toInt()
                } else {
                    lp.bottomMargin = iconMargin.toInt()
                }
                ivTabIcon.layoutParams = lp
            } else {
                // 不显示 Icon。
                ivTabIcon.visibility = GONE
            }
        }
    }

    /**
     * 立即将 Indicator 设置到指定 Tab。
     *
     * 不执行动画。
     */
    private fun updateIndicatorImmediately(
        position: Int
    ) {
        val tab = tabContainer.getChildAt(position) ?: return
        var left = tab.left.toFloat()
        var right = tab.right.toFloat()
        if (indicatorWidth >= 0) {
            val width = indicatorWidth
            left = tab.left + (tab.width - width) / 2f
            right = left + width
        }
        indicatorLeft = left
        indicatorRight = right
        invalidate()
    }

    private fun animateIndicatorTo(
        position: Int
    ) {
        val tab = tabContainer.getChildAt(position)
                ?: return
        val startLeft = indicatorLeft
        val startRight = indicatorRight
        var targetLeft = tab.left.toFloat()
        var targetRight = tab.right.toFloat()
        if (indicatorWidth >= 0) {
            val width = indicatorWidth
            targetLeft =
                tab.left +
                        (tab.width - width) / 2f
            targetRight =
                targetLeft + width
        }
        indicatorAnimator?.cancel()
        indicatorAnimator =
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = indicatorAnimationDuration
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    val fraction =
                        it.animatedValue as Float
                    indicatorLeft = startLeft + (targetLeft - startLeft) * fraction
                    indicatorRight =
                        startRight +
                                (targetRight - startRight) *
                                fraction
                    invalidate()
                }
                start()
            }
    }

    /**
     * 平滑滚动到当前 Tab。
     *
     * 与 scrollToCurrentTab() 的区别：
     *
     * scrollToCurrentTab()
     *      立即滚动。
     *
     * smoothScrollToCurrentTab()
     *      平滑滚动。
     */
    private fun smoothScrollToCurrentTab() {
        if (tabCount <= 0) {
            return
        }
        val tab =
            tabContainer.getChildAt(currentTab)
                ?: return
        var newScrollX = tab.left
        // 尽量让当前 Tab 居中。
        newScrollX -=
            width / 2 -
                    tab.width / 2
        // 防止滚动到负数。
        newScrollX = newScrollX.coerceAtLeast(0)
        if (newScrollX != lastScrollX) {
            lastScrollX = newScrollX
            smoothScrollTo(
                newScrollX,
                0
            )
        }
    }

    /**
     * 将当前 Tab 滚动到 HorizontalScrollView 中间。
     *
     * 例如：
     *
     * Tab 总数很多时点击最右侧 Tab，
     * 如果当前 Tab 超出屏幕范围，
     * 则会自动调整 scrollX。
     */
    private fun scrollToCurrentTab() {
        // 没有 Tab 时无需处理。
        if (tabCount <= 0) {
            return
        }
        // 获取当前 Tab。
        val tab = tabContainer.getChildAt(currentTab)
        // 默认滚动到当前 Tab 左边。
        var newScrollX = tab.left
        // 调整为让 Tab 尽量居中。
        newScrollX -= width / 2 - tab.width / 2
        // 避免重复滚动。
        if (newScrollX != lastScrollX) {
            lastScrollX = newScrollX
            // 执行横向滚动。
            scrollTo(
                newScrollX,
                0
            )
        }
    }

    /**
     * 更新指定 Tab 的选中状态。
     *
     * 主要负责：
     *
     * 1. 更新文字颜色。
     * 2. 根据配置设置选中加粗。
     *
     * Indicator 的实际绘制由 onDraw() 完成。
     */
    private fun updateTabSelection(position: Int) {
        for (i in 0..<this.tabCount) {
            // 获取 Tab。
            val tabView =
                tabContainer.getChildAt(i)
                    ?: continue
            // 当前 Tab 是否选中。
            val isSelect = i == position
            // 获取标题。
            val tvTabTitle = tabView.findViewById<View?>(R.id.tv_tab_title) as TextView?
            if (tvTabTitle != null) {
                // 设置文字颜色。
                tvTabTitle.setTextColor(
                    if (isSelect) {
                        tabTextSelectedColor
                    } else {
                        tabTextUnselectedColor
                    }
                )
                // 如果配置为仅选中状态加粗，
                // 则当前 Tab 加粗，其它 Tab 取消加粗。
                if (tabTextBold == TEXT_BOLD_WHEN_SELECT) {
                    tvTabTitle.paint.isFakeBoldText = isSelect
                }
            }
        }
    }

    /**
     * 计算 Indicator 当前应该绘制的区域。
     */
    private fun calcIndicatorRect() {
        indicatorRect.left = indicatorLeft.toInt()
        indicatorRect.right = indicatorRight.toInt()
        tabRect.set(
            indicatorRect.left,
            0,
            indicatorRect.right,
            height
        )
    }

    /**
     * 绘制：
     *
     * 1. Divider
     * 2. Underline
     * 3. Indicator
     *
     * Indicator 支持：
     *
     * STYLE_NORMAL
     * STYLE_TRIANGLE
     * STYLE_BLOCK
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 编辑器预览模式或者没有 Tab 时不绘制。
        if (isInEditMode || this.tabCount <= 0) {
            return
        }
        // 当前控件高度。
        val height = getHeight()
        // 当前控件左 Padding。
        val paddingLeft = getPaddingLeft()
        /**
         * 绘制 Tab Divider。
         */
        if (dividerWidth > 0) {
            dividerPaint.strokeWidth = dividerWidth
            dividerPaint.color = dividerColor
            // 最后一个 Tab 后面不需要 Divider，
            // 因此只遍历到 tabCount - 2。
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
        /**
         * 绘制整体 Underline。
         *
         * Underline 与当前 Tab Indicator 不同：
         * 它覆盖整个 tabContainer。
         */
        if (underlineHeight > 0) {
            rectPaint.color = underlineColor
            if (underlineGravity == Gravity.BOTTOM) {
                // 绘制在底部。
                canvas.drawRect(
                    paddingLeft.toFloat(),
                    height - underlineHeight,
                    (tabContainer.width + paddingLeft).toFloat(),
                    height.toFloat(),
                    rectPaint
                )
            } else {
                // 非 Bottom 默认绘制在顶部。
                canvas.drawRect(
                    paddingLeft.toFloat(),
                    0f,
                    (tabContainer.width + paddingLeft).toFloat(),
                    underlineHeight,
                    rectPaint
                )
            }
        }
        ensureIndicatorPosition()
        // 计算当前 Indicator 区域。
        calcIndicatorRect()
        /**
         * Triangle Indicator。
         */
        if (indicatorType == INDICATOR_TRIANGLE) {
            if (indicatorHeight > 0) {
                trianglePaint.color = indicatorColor
                // 清空旧 Path。
                trianglePath.reset()
                // 左下角。
                trianglePath.moveTo(
                    (paddingLeft + indicatorRect.left).toFloat(),
                    height.toFloat()
                )
                // 顶部中心点。
                trianglePath.lineTo(
                    (
                            paddingLeft +
                                    indicatorRect.left / 2 +
                                    indicatorRect.right / 2
                            ).toFloat(),
                    height - indicatorHeight
                )
                // 右下角。
                trianglePath.lineTo(
                    (paddingLeft + indicatorRect.right).toFloat(),
                    height.toFloat()
                )
                // 闭合 Path。
                trianglePath.close()
                // 绘制三角形。
                canvas.drawPath(
                    trianglePath,
                    trianglePaint
                )
            }
            /**
             * Block Indicator。
             *
             * 表现为一个带圆角的矩形背景。
             */
        } else if (indicatorType == INDICATOR_BLOCK) {
            // 高度 < 0 表示自动计算高度。
            if (indicatorHeight < 0) {
                indicatorHeight =
                    height -
                            this.indicatorMarginTop -
                            this.indicatorMarginBottom
            }
            if (indicatorHeight > 0) {
                // 如果圆角没有设置，
                // 或圆角超过高度的一半，
                // 则最多取高度的一半。
                if (
                    indicatorCornerRadius < 0 ||
                    indicatorCornerRadius > indicatorHeight / 2
                ) {
                    indicatorCornerRadius =
                        indicatorHeight / 2
                }
                // 设置背景颜色。
                indicatorDrawable.setColor(indicatorColor)
                // 设置矩形区域。
                indicatorDrawable.setBounds(
                    paddingLeft +
                            indicatorMarginLeft.toInt() +
                            indicatorRect.left,
                    indicatorMarginTop.toInt(),
                    (
                            paddingLeft +
                                    indicatorRect.right -
                                    this.indicatorMarginRight
                            ).toInt(),
                    (
                            this.indicatorMarginTop +
                                    indicatorHeight
                            ).toInt()
                )
                // 设置圆角。
                indicatorDrawable.cornerRadius =
                    indicatorCornerRadius
                // 绘制。
                indicatorDrawable.draw(canvas)
            }

            /**
             * Normal Indicator。
             *
             * 默认是一个普通矩形下划线。
             */
        } else {
            if (indicatorHeight > 0) {
                indicatorDrawable.setColor(indicatorColor)
                // Indicator 在底部。
                if (indicatorGravity == Gravity.BOTTOM) {
                    indicatorDrawable.setBounds(
                        paddingLeft +
                                indicatorMarginLeft.toInt() +
                                indicatorRect.left,
                        height -
                                indicatorHeight.toInt() -
                                indicatorMarginBottom.toInt(),
                        paddingLeft +
                                indicatorRect.right -
                                indicatorMarginRight.toInt(),
                        height -
                                indicatorMarginBottom.toInt()
                    )

                } else {
                    // Indicator 在顶部。
                    indicatorDrawable.setBounds(
                        paddingLeft +
                                indicatorMarginLeft.toInt() +
                                indicatorRect.left,
                        indicatorMarginTop.toInt(),
                        paddingLeft +
                                indicatorRect.right -
                                indicatorMarginRight.toInt(),
                        indicatorHeight.toInt() +
                                indicatorMarginTop.toInt()
                    )
                }
                // 设置圆角。
                indicatorDrawable.cornerRadius = indicatorCornerRadius
                // 绘制 Indicator。
                indicatorDrawable.draw(canvas)
            }
        }
    }

    /**
     * 设置 Indicator 的 Gravity。
     *
     * 常用：
     *
     * Gravity.TOP
     * Gravity.BOTTOM
     */
    fun setIndicatorGravity(indicatorGravity: Int) {
        this.indicatorGravity = indicatorGravity
        // 参数发生变化后重新绘制。
        invalidate()
    }

    /**
     * 设置 Indicator 四周 Margin。
     *
     * 参数单位为 dp。
     */
    fun setIndicatorMargin(
        indicatorMarginLeft: Float,
        indicatorMarginTop: Float,
        indicatorMarginRight: Float,
        indicatorMarginBottom: Float
    ) {
        this.indicatorMarginLeft =
            dp2px(indicatorMarginLeft).toFloat()
        this.indicatorMarginTop =
            dp2px(indicatorMarginTop).toFloat()
        this.indicatorMarginRight =
            dp2px(indicatorMarginRight).toFloat()
        this.indicatorMarginBottom =
            dp2px(indicatorMarginBottom).toFloat()
        invalidate()
    }

    /**
     * 设置 Indicator 动画时长。
     *
     * @param duration 动画时长，单位：ms。
     *
     * 0：
     *      点击 Tab 时立即切换。
     *
     * > 0：
     *      点击 Tab 时执行平滑动画。
     */
    fun setIndicatorAnimationDuration(
        duration: Long
    ) {
        this.indicatorAnimationDuration = duration.coerceAtLeast(0L)
        invalidate()
    }

    /**
     * 设置整体 Underline 的位置。
     */
    fun setUnderlineGravity(
        underlineGravity: Int
    ) {
        this.underlineGravity = underlineGravity
        invalidate()
    }

    /**
     * 设置点击 Tab 后是否执行吸附。
     */
    fun setSnapOnTabClick(
        snapOnTabClick: Boolean
    ) {
        this.snapOnTabClick = snapOnTabClick
    }

    /**
     * 设置 Tab 左右 Padding。
     *
     * 单位：dp。
     */
    fun setTabPadding(
        padding: Float
    ) {
        this.tabPadding = dp2px(padding).toFloat()
        updateTabStyles()
    }

    /**
     * 设置 Tab 是否分割。
     */
    fun setTabIsDivided(
        isDivided: Boolean
    ) {
        this.tabIsDivided = isDivided
        updateTabStyles()
    }

    /**
     * 设置 Tab 固定宽度。
     *
     * 单位：dp。
     */
    fun setTabWidth(
        width: Float
    ) {
        this.tabWidth = dp2px(width).toFloat()
        updateTabStyles()
    }

    /**
     * 设置 Indicator 类型。
     *
     * 可选：
     *
     * INDICATOR_NORMAL
     * INDICATOR_TRIANGLE
     * INDICATOR_BLOCK
     */
    fun setIndicatorType(@IndicatorType type: Int) {
        this.indicatorType = type
        invalidate()
    }

    /**
     * 设置 Indicator 颜色。
     */
    fun setIndicatorColor(
        @ColorInt color: Int
    ) {
        this.indicatorColor = color
        invalidate()
    }

    /**
     * 设置 Indicator 宽度。
     *
     * 单位：px。
     */
    fun setIndicatorWidth(
        width: Float
    ) {
        this.indicatorWidth = width
        invalidate()
    }

    /**
     * 设置 Indicator 宽度。
     *
     * 单位：dp。
     */
    fun setIndicatorWidthInDp(
        width: Float
    ) {
        this.indicatorWidth = dp2px(width).toFloat()
        invalidate()
    }

    /**
     * 设置 Indicator 高度。
     *
     * 单位：px。
     */
    fun setIndicatorHeight(
        height: Float
    ) {
        this.indicatorHeight = height
        invalidate()
    }

    /**
     * 设置 Indicator 高度。
     *
     * 单位：dp。
     */
    fun setIndicatorHeightInDp(
        height: Float
    ) {
        this.indicatorHeight = dp2px(height).toFloat()
        invalidate()
    }

    /**
     * 设置 Indicator 圆角半径。
     *
     * 单位：px。
     */
    fun setIndicatorCornerRadius(
        radius: Float
    ) {
        this.indicatorCornerRadius = radius
        invalidate()
    }

    /**
     * 设置 Indicator 圆角半径。
     *
     * 单位：dp。
     */
    fun setIndicatorCornerRadiusInDp(
        radius: Float
    ) {
        this.indicatorCornerRadius = dp2px(radius).toFloat()
        invalidate()
    }

    /**
     * 设置整体 Underline 颜色。
     */
    fun setUnderlineColor(
        @ColorInt color: Int
    ) {
        this.underlineColor = color
        invalidate()
    }

    /**
     * 设置整体 Underline 高度。
     *
     * 当前方法直接接收 px。
     */
    fun setUnderlineHeight(
        height: Float
    ) {
        this.underlineHeight = height
        invalidate()
    }

    /**
     * 设置 Divider 颜色。
     */
    fun setDividerColor(
        @ColorInt color: Int
    ) {
        this.dividerColor = color
        invalidate()
    }

    /**
     * 设置 Divider 宽度。
     */
    fun setDividerWidth(
        width: Float
    ) {
        this.dividerWidth = width
        invalidate()
    }

    /**
     * 设置 Divider 上下 Padding。
     */
    fun setDividerPadding(
        padding: Float
    ) {
        this.dividerPadding = padding
        invalidate()
    }

    /**
     * 设置 Tab 文字大小。
     *
     * 参数单位：px。
     */
    fun setTabTextSize(
        textSize: Float
    ) {
        this.tabTextSize = textSize
        updateTabStyles()
    }

    /**
     * 设置 Tab 文字大小。
     *
     * 参数单位：sp。
     */
    fun setTabTextSizeInSp(
        textSize: Float
    ) {
        this.tabTextSize = sp2px(textSize).toFloat()
        updateTabStyles()
    }

    /**
     * 设置选中 Tab 的文字颜色。
     */
    fun setTabTextSelectedColor(
        @ColorInt color: Int
    ) {
        this.tabTextSelectedColor = color
        updateTabStyles()
    }

    /**
     * 设置未选中 Tab 的文字颜色。
     */
    fun setTabTextUnselectedColor(
        @ColorInt color: Int
    ) {
        this.tabTextUnselectedColor = color
        updateTabStyles()
    }

    /**
     * 设置文字加粗模式。
     *
     * 可选：
     *
     * TEXT_BOLD_NONE
     * TEXT_BOLD_WHEN_SELECT
     * TEXT_BOLD_BOTH
     */
    fun setTabTextBold(
        @TextBoldMode boldType: Int
    ) {
        this.tabTextBold = boldType
        updateTabStyles()
    }

    /**
     * 设置是否全部转换成大写。
     */
    fun setTabTextAllCaps(
        isTextAllCaps: Boolean
    ) {
        this.tabTextAllCaps = isTextAllCaps
        updateTabStyles()
    }

    /**
     * 获取指定 Tab 的标题 TextView。
     *
     * 方便外部进一步自定义 TextView。
     */
    fun getTitleView(
        tab: Int
    ): TextView? {
        val tabView = tabContainer.getChildAt(tab)
        return tabView.findViewById<View?>(
            R.id.tv_tab_title
        ) as TextView?
    }

    /**
     * 显示指定 Tab 的数字 Badge。
     *
     * 如果 position 超过 Tab 数量，
     * 当前实现会自动使用最后一个 Tab。
     */
    fun showBadge(
        position: Int,
        num: Int
    ) {
        var position = position
        // 防止 position 超出范围。
        if (position >= this.tabCount) {
            position = this.tabCount - 1
        }
        // 获取对应 Tab。
        val tabView = tabContainer.getChildAt(position)
        // 获取 Badge。
        val badgeView =
            tabView.findViewById<View?>(
                R.id.bv_num
            ) as BadgeView?
        if (badgeView != null) {
            // 显示数字。
            BadgeUtils.showBadge(
                badgeView,
                num
            )
            // 如果已经初始化过位置，
            // 则不重复计算。
            if (
                initMap.get(position) != null &&
                initMap.get(position)
            ) {
                return
            }
            // Icon 不显示时，
            // Badge 使用一个简单的默认位置。
            if (!iconVisible) {
                setBadgeMargin(
                    position,
                    2f,
                    2f
                )
            } else {
                // Icon 显示时，
                // 根据 Icon 是左右还是上下排列，
                // 使用不同的 Badge 间距。
                setBadgeMargin(
                    position,
                    0f,
                    (
                            if (
                                resolveIconGravity() == Gravity.LEFT ||
                                resolveIconGravity() == Gravity.RIGHT
                            ) {
                                4
                            } else {
                                0
                            }
                            ).toFloat()
                )
            }
            // 标记当前 Badge 已初始化。
            initMap.put(
                position,
                true
            )
        }
    }

    /**
     * 显示指定 Tab 的 Badge。
     *
     * 不显示数字时默认传入 0。
     */
    fun showBadge(
        position: Int
    ) {
        var position = position
        if (position >= this.tabCount) {
            position = this.tabCount - 1
        }
        showBadge(
            position,
            0
        )
    }

    /**
     * 隐藏指定 Tab 的 Badge。
     */
    fun hideBadge(
        position: Int
    ) {
        var position = position
        if (position >= this.tabCount) {
            position = this.tabCount - 1
        }
        val tabView =
            tabContainer.getChildAt(position)
        val badgeView =
            tabView.findViewById<View?>(
                R.id.bv_num
            ) as BadgeView?
        if (badgeView != null) {
            badgeView.visibility = GONE
        }
    }

    /**
     * 设置 Badge 的 Margin。
     *
     * 参数：
     *
     * position：
     *      Tab 下标。
     *
     * leftPadding：
     *      Badge 左侧偏移，单位 dp。
     *
     * bottomPadding：
     *      Badge 垂直偏移，单位 dp。
     *
     * 这里会根据：
     *
     * - Tab 高度
     * - 文字高度
     * - Icon 高度
     * - Icon Margin
     * - Icon Gravity
     *
     * 综合计算 Badge 的 topMargin。
     */
    fun setBadgeMargin(
        position: Int,
        leftPadding: Float,
        bottomPadding: Float
    ) {
        var position = position
        if (position >= this.tabCount) {
            position = this.tabCount - 1
        }
        val tabView = tabContainer.getChildAt(position)
        val badgeView = tabView.findViewById<View>(R.id.bv_num) as BadgeView?
        if (badgeView != null) {
            // 设置文字大小，用于计算文字高度。
            textPaint.textSize = tabTextSize
            // 获取文字实际高度。
            val textHeight = textPaint.descent() - textPaint.ascent()
            // Badge 当前 LayoutParams。
            val lp = badgeView.layoutParams as MarginLayoutParams
            // 默认使用配置中的 Icon 高度。
            var iconH = iconHeight
            var margin = 0f
            if (iconVisible) {
                // 没有指定 Icon 高度时，
                // 使用 Drawable 的 intrinsicHeight。
                if (iconH <= 0) {
                    if (tabs[position].tabSelectedIcon != null) {
                    iconH =
                        ContextCompat
                            .getDrawable(
                                context,
                                tabs[position].tabSelectedIcon!!
                            )
                            ?.intrinsicHeight
                            ?.toFloat()
                            ?: 0f
                        }
                }
                // Icon 与文字之间的间距。
                margin = iconMargin
            }
            /**
             * Icon 在顶部或底部时：
             *
             * 整体内容高度 =
             *      文字高度 + Icon 高度 + Icon Margin
             *
             * 然后让 Badge 尽量垂直居中。
             */
            if (
                iconGravity == Gravity.TOP ||
                iconGravity == Gravity.BOTTOM
            ) {
                lp.leftMargin =
                    dp2px(leftPadding)
                lp.topMargin =
                    if (viewHeight > 0) {
                        (viewHeight - textHeight - iconH - margin).toInt() / 2 - dp2px(bottomPadding)
                    } else {
                        dp2px(bottomPadding)
                    }
            } else {
                /**
                 * Icon 左右排列时：
                 *
                 * 文字和 Icon 是同一水平行，
                 * 因此取二者较大的高度。
                 */
                lp.leftMargin =
                    dp2px(leftPadding)
                lp.topMargin =
                    if (viewHeight > 0) {
                        (viewHeight - max(textHeight, iconH)).toInt() / 2 - dp2px(bottomPadding)
                    } else {
                        dp2px(bottomPadding)
                    }
            }
            // 应用新的 Margin。
            badgeView.layoutParams = lp
        }
    }

    /**
     * 获取指定位置的 BadgeView。
     *
     * 方便外部对 BadgeView 做进一步定制。
     */
    fun getBadgeView(
        position: Int
    ): BadgeView? {
        var position = position
        if (position >= this.tabCount) {
            position = this.tabCount - 1
        }
        val tabView = tabContainer.getChildAt(position)
        return tabView.findViewById<View?>(
            R.id.bv_num
        ) as BadgeView?
    }

    /**
     * 设置 Tab 选择监听器。
     */
    fun setOnTabSelectListener(
        listener: OnTabSelectListener
    ) {
        this.onTabSelectListener = listener
    }

    /**
     * 主动设置当前选中的 Tab。
     *
     * 如果 position 超出合法范围，
     * 则直接忽略此次调用。
     */
    fun setCurrentTab(
        position: Int,
        animate: Boolean = false
    ) {
        if (position !in 0..<tabCount) {
            return
        }
        currentTab = position
        updateTabSelection(position)
        if (animate) {
            animateIndicatorTo(position)
            smoothScrollToCurrentTab()
        } else {
            indicatorAnimator?.cancel()
            val tab = tabContainer.getChildAt(position) ?: return
            indicatorLeft = tab.left.toFloat()
            indicatorRight = tab.right.toFloat()
            if (indicatorWidth >= 0) {
                val width = indicatorWidth
                indicatorLeft = tab.left + (tab.width - width) / 2f
                indicatorRight = indicatorLeft + width
            }
            scrollToCurrentTab()
            invalidate()
        }
    }

    /**
     * 获取指定 Tab 对应的 Indicator 左右位置。
     */
    private fun getIndicatorRectForTab(
        tabView: View
    ): Pair<Float, Float> {
        var left = tabView.left.toFloat()
        var right = tabView.right.toFloat()
        if (indicatorWidth >= 0) {
            val width = indicatorWidth
            left = tabView.left + (tabView.width - width) / 2f
            right = left + width
        }
        return left to right
    }

    /**
     * 根据 ViewPager2 的滑动状态更新 Indicator。
     *
     * @param position 当前页面位置。
     * @param positionOffset 当前页面滑动偏移量，范围 0f ~ 1f。
     *
     * 例如：
     *
     * position = 0
     * positionOffset = 0f
     *      Indicator 在 Tab 0。
     *
     * position = 0
     * positionOffset = 0.5f
     *      Indicator 位于 Tab 0 和 Tab 1 中间。
     *
     * position = 0
     * positionOffset = 1f
     *      Indicator 到达 Tab 1。
     */
    fun setIndicatorPosition(
        position: Int,
        positionOffset: Float
    ) {
        if (tabCount <= 0) {
            return
        }
        if (position !in 0 until tabCount) {
            return
        }
        val currentView = tabContainer.getChildAt(position) ?: return
        val nextPosition =
            if (positionOffset > 0f) {
                (position + 1).coerceAtMost(tabCount - 1)
            } else {
                position
            }
        val nextView = tabContainer.getChildAt(nextPosition) ?: return
        val offset =
            positionOffset.coerceIn(0f, 1f)
        val currentRect =
            getIndicatorRectForTab(currentView)
        val nextRect =
            getIndicatorRectForTab(nextView)
        indicatorLeft = currentRect.first +
                    (nextRect.first - currentRect.first) * offset
        indicatorRight = currentRect.second +
                    (nextRect.second - currentRect.second) * offset
        // ViewPager2 正在滑动时，同步更新当前选中 Tab。
        if (offset >= 0.5f) {
            updateTabSelection(nextPosition)
        } else {
            updateTabSelection(position)
        }
        invalidate()
    }

    private fun ensureIndicatorPosition() {
        if (indicatorInitialized) {
            return
        }
        val tab = tabContainer.getChildAt(currentTab) ?: return
        val rect = getIndicatorRectForTab(tab)
        indicatorLeft = rect.first
        indicatorRight = rect.second
        indicatorInitialized = true
    }

    /**
     * 保存当前 View 状态。
     *
     * 除了父类原本保存的状态之外，
     * 额外保存 currentTab。
     */
    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        // 保存父类状态。
        bundle.putParcelable(
            "instanceState",
            super.onSaveInstanceState()
        )
        // 保存当前 Tab。
        bundle.putInt(
            "currentTab",
            currentTab
        )
        return bundle
    }

    /**
     * 恢复 View 状态。
     *
     * 恢复：
     *
     * 1. 父类状态。
     * 2. 当前选中的 Tab。
     * 3. 当前 Tab 的滚动位置。
     */
    override fun onRestoreInstanceState(
        state: Parcelable?
    ) {
        var state = state
        if (state is Bundle) {
            val bundle = state
            // 恢复当前 Tab。
            currentTab =
                bundle.getInt(
                    "currentTab"
                )
            // 获取父类保存的状态。
            state =
                bundle.getParcelable(
                    "instanceState"
                )
            // 如果当前 Tab 不是第一个，
            // 并且 Tab 容器已经有子 View，
            // 则恢复选中样式和滚动位置。
            if (
                currentTab != 0 &&
                tabContainer.isNotEmpty()
            ) {
                updateTabSelection(currentTab)
                scrollToCurrentTab()
            }
        }
        // 最后恢复父类状态。
        super.onRestoreInstanceState(
            state
        )
    }

    /**
     * Tab 数据模型。
     *
     * tabTitle：
     *      Tab 标题。
     *
     * tabSelectedIcon：
     *      Tab 选中时使用的 Drawable。
     *
     * tabUnselectedIcon：
     *      Tab 未选中时使用的 Drawable。
     */
    data class DoraTab(

        val tabTitle: String,

        @get:DrawableRes
        val tabSelectedIcon: Int? = null,

        @get:DrawableRes
        val tabUnselectedIcon: Int? = null
    )

    /**
     * Tab 选择监听器。
     */
    interface OnTabSelectListener {

        /**
         * 用户选择了一个新的 Tab。
         */
        fun onTabSelected(
            position: Int
        )

        /**
         * 用户再次点击当前已经选中的 Tab。
         */
        fun onTabReselected(
            position: Int
        )
    }

    /**
     * 文字加粗模式限制。
     */
    @IntDef(
        TEXT_BOLD_NONE,
        TEXT_BOLD_WHEN_SELECT,
        TEXT_BOLD_BOTH
    )
    @Retention(AnnotationRetention.SOURCE)
    @Target(
        AnnotationTarget.FIELD,
        AnnotationTarget.PROPERTY,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.FUNCTION
    )
    private annotation class TextBoldMode

    /**
     * Indicator 类型限制。
     */
    @IntDef(
        INDICATOR_NORMAL,
        INDICATOR_TRIANGLE,
        INDICATOR_BLOCK
    )
    @Retention(AnnotationRetention.SOURCE)
    @Target(
        AnnotationTarget.FIELD,
        AnnotationTarget.PROPERTY,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.FUNCTION
    )
    private annotation class IndicatorType

    companion object {

        /**
         * 普通 Indicator。
         *
         * 通常表现为顶部 / 底部的一条横线。
         */
        private const val INDICATOR_NORMAL = 0

        /**
         * 三角形 Indicator。
         */
        private const val INDICATOR_TRIANGLE = 1

        /**
         * Block 背景 Indicator。
         */
        private const val INDICATOR_BLOCK = 2

        /**
         * 不加粗。
         */
        private const val TEXT_BOLD_NONE = 0

        /**
         * 仅选中的 Tab 加粗。
         */
        private const val TEXT_BOLD_WHEN_SELECT = 1

        /**
         * 所有 Tab 都加粗。
         */
        private const val TEXT_BOLD_BOTH = 2
    }
}
