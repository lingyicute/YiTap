package app.yitap.smartspace

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import app.yitap.ui.preferences.PreferenceActivity
import app.yitap.ui.preferences.navigation.Routes

class SmartspacePreferencesShortcut : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(PreferenceActivity.createIntent(this, Routes.SMARTSPACE_WIDGET))
        finish()
    }
}
