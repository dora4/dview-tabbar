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
    implementation 'com.github.dora4:dview-tabbar:1.6'
}
```

#### 使用控件

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
            app:dview_tb_indicatorType="NORMAL"
            app:dview_tb_indicatorColor="@color/sky_blue"
            app:dview_tb_tabPadding="10dp" />
```

```kotlin
binding.tabBar.setTabs(arrayOf(DoraTab("频道1"), DoraTab("频道2"), DoraTab("频道3")))

binding.tabBar.setOnTabSelectListener(
    object : DoraTabBar.OnTabSelectListener {

        override fun onTabSelected(
            position: Int
        ) {
            // 修改指示器的选中位置，手动设置，为滑动指示器的性能预留空间
            binding.tabBar.setCurrentTab(position)
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

