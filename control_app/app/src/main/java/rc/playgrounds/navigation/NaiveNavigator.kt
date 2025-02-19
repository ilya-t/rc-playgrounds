package rc.playgrounds.navigation

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.testspace.R

class NaiveNavigator(a: AppCompatActivity) {
    private val mainRoot = a.findViewById<View>(R.id.layer_main)
    private val configRoot = a.findViewById<View>(R.id.layer_config)
    fun openMain() {
        mainRoot.isVisible = true
        configRoot.isVisible = false
    }

    fun openConfig() {
        mainRoot.isVisible = false
        configRoot.isVisible = true
    }
}