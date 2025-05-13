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
    implementation 'com.github.dora4:dview-tabbar:1.1'
}
```

#### 使用控件

```kotlin
        binding.tabBar.addTextTab("频道1")
        binding.tabBar.addTextTab("频道2")
        binding.tabBar.addTextTab("频道3")
        binding.tabBar.setOnTabClickListener(object : DoraTabBar.OnTabClickListener {

            override fun onTabClick(view: View, position: Int) {
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
        })
```

#### 示例代码

https://github.com/dora4/dora_samples

