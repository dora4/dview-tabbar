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
    implementation 'com.github.dora4:dview-tabbar:1.3'
}
```

#### 使用控件

```xml
<dora.widget.DoraTabBar
    android:id="@+id/tabBar"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:dview_tabIsDivided="true"
    app:dview_tabPadding="15dp"
    app:dview_indicatorColor="@color/primary"
    app:dview_tabSelectedTextColor="@color/primary" />
```

```kotlin
binding.tabBar.setTabs(arrayOf(DoraTab(), DoraTab(), DoraTab()))

binding.tabBar.setOnTabSelectListener(
    object : DoraTabBar.OnTabSelectListener {

        override fun onTabSelected(
            position: Int
        ) {
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

