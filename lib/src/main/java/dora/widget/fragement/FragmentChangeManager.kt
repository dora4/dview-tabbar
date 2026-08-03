package dora.widget.fragement

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

class FragmentChangeManager(
    private val fragmentManager: FragmentManager, private val containerViewId: Int,
    private val fragments: ArrayList<Fragment>
) {
    var currentTab: Int = 0
        private set

    init {
        initFragments()
    }

    private fun initFragments() {
        for (fragment in fragments) {
            fragmentManager.beginTransaction().add(containerViewId, fragment).hide(fragment)
                .commit()
        }
        setFragments(0)
    }

    fun setFragments(index: Int) {
        for (i in fragments.indices) {
            val ft = fragmentManager.beginTransaction()
            val fragment = fragments[i]
            if (i == index) {
                ft.show(fragment)
            } else {
                ft.hide(fragment)
            }
            ft.commit()
        }
        this.currentTab = index
    }

    val currentFragment: Fragment
        get() = fragments[this.currentTab]
}