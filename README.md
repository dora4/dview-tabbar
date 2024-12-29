dview-tabbar
![Release](https://jitpack.io/v/dora4/dview-tabbar.svg)
--------------------------------

##### 卡名：Dora视图 TabBar 
###### 卡片类型：效果怪兽
###### 属性：炎
###### 星级：8
###### 种族：魔法师族
###### 攻击力/防御力：2200/2500
###### 效果：此卡不会因为对方卡的效果而破坏，并可使其无效化。此卡攻击里侧守备表示的怪兽时，若攻击力高于其守备力，则给予对方此卡原攻击力的伤害，并抽一张卡。每当此卡攻击破坏对方怪兽送去墓地时，你可以破坏对方场上一张魔法或陷阱卡。如果此卡在你的回合结束时没有攻击过，你可以选择让对方从卡组抽一张卡，然后将此卡的攻击力提升1000点。

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
