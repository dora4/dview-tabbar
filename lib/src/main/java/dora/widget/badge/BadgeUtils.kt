package dora.widget.badge

import android.view.View
import android.widget.RelativeLayout

object BadgeUtils {

    @JvmStatic
    fun showBadge(badgeView: BadgeView, num: Int) {
        val lp = badgeView.layoutParams as RelativeLayout.LayoutParams
        val dm = badgeView.resources.displayMetrics
        badgeView.visibility = View.VISIBLE
        if (num <= 0) {
            badgeView.setStrokeWidth(0)
            badgeView.text = ""
            lp.width = (5 * dm.density).toInt()
            lp.height = (5 * dm.density).toInt()
            badgeView.layoutParams = lp
        } else {
            lp.height = (18 * dm.density).toInt()
            when (num) {
                in 1..<10 -> {
                    lp.width = (18 * dm.density).toInt()
                    badgeView.text = "$num"
                }
                in 10..<100 -> {
                    lp.width = RelativeLayout.LayoutParams.WRAP_CONTENT
                    badgeView.setPadding((6 * dm.density).toInt(), 0, (6 * dm.density).toInt(), 0)
                    badgeView.text = "$num"
                }
                else -> {
                    lp.width = RelativeLayout.LayoutParams.WRAP_CONTENT
                    badgeView.setPadding((6 * dm.density).toInt(), 0, (6 * dm.density).toInt(), 0)
                    badgeView.text = "99+"
                }
            }
            badgeView.layoutParams = lp
        }
    }

    fun setBadgeSize(badgeView: BadgeView, size: Int) {
        val lp = badgeView.layoutParams as RelativeLayout.LayoutParams
        lp.width = size
        lp.height = size
        badgeView.layoutParams = lp
    }
}
