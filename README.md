dview-tabbar
![Release](https://jitpack.io/v/dora4/dview-tabbar.svg)
--------------------------------

#### 卡片
![DORA视图 多界掌控者](https://github.com/user-attachments/assets/0df8e362-0fcc-4ba1-82f2-52a64a8a424d)

#### Gradle依赖配置

```groovy
// 添加以下代码到项目根目录下的build.gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
// 添加以下代码到app模块的build.gradle
dependencies {
    implementation 'com.github.dora4:dview-tabbar:1.14'
}
```

#### 使用控件

##### DoraStaticTabBar
```xml
<dora.widget.DoraStaticTabBar
    android:id="@+id/tabBar"
    android:layout_width="200dp"
    android:layout_height="wrap_content"
    android:layout_marginVertical="8dp"
    android:layout_gravity="center_horizontal"
    app:dview_tb_barColor="@color/white"
    app:dview_tb_barStrokeColor="@color/primary"
    app:dview_tb_barStrokeWidth="1dp"
    app:dview_tb_indicatorColor="@color/primary"
    app:dview_tb_indicatorCornerRadius="18dp"
    app:dview_tb_tabSelectedTextColor="@color/white"
    app:dview_tb_tabUnselectedTextColor="@color/primary"
    app:dview_tb_tabTextBold="SELECT"/>
```
```kotlin
binding.tabBar.setTextTabs(arrayOf("未掌握", "已掌握"))
binding.tabBar.setOnTabSelectListener(object : DoraStaticTabLayout.OnTabSelectListener {

    override fun onTabSelected(position: Int) {
        binding.tabBar.setCurrentTab(position)
        switchPage(position)
    }

    override fun onTabReselected(position: Int) {
    }
})
```

##### DoraTabBar
```xml
<dora.widget.DoraTabBar
    android:id="@+id/tabBar"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@color/white"
    android:paddingVertical="10dp"
    app:dview_tb_tabIsDivided="false"
    app:dview_tb_tabUnselectedTextColor="@color/colorTextNormal"
    app:dview_tb_tabSelectedTextColor="@color/sky_blue"
    app:dview_tb_tabTextBold="SELECT"
    app:dview_tb_indicatorType="NORMAL"
    app:dview_tb_indicatorColor="@color/sky_blue"
    app:dview_tb_tabPadding="10dp" />
```

```kotlin
binding.tabBar.setTabs(
                DoraTabBar.DoraTab("频道1"),
                DoraTabBar.DoraTab("频道2")
            )
binding.tabBar.addTab(DoraTabBar.DoraTab("频道3"))
binding.tabBar.setOnTabSelectListener(
    object : DoraTabBar.OnTabSelectListener {

        override fun onTabSelected(
            position: Int
        ) {
            // DoraTabBar直接在参数指定是否需要动画
            binding.tabBar.setCurrentTab(position, true)
            when (position) {
                0 -> {
                    showPage(pageOne)
                }
                1 -> {
                    showPage(pageTwo)
                }
                2 -> {
                    showPage(pageThree)
                }
            }
        }

        override fun onTabReselected(
            position: Int
        ) {
        }
    }
)
```

#### 示例代码

https://github.com/dora4/dora_samples

