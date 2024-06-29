package app.yitap.theme.color

import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import app.yitap.ui.preferences.components.controls.ListPreferenceEntry
import com.android.launcher3.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

enum class ColorMode(
    @StringRes val labelResourceId: Int,
) {
    AUTO(
        labelResourceId = R.string.managed_by_yitap,
    ),
    LIGHT(
        labelResourceId = R.string.color_light,
    ),
    DARK(
        labelResourceId = R.string.color_dark,
    ),
    ;

    companion object {
        fun values() = listOf(
            AUTO,
            LIGHT,
            DARK,
        )

        fun fromString(string: String) = values().firstOrNull { it.toString() == string }

        fun entries(): ImmutableList<ListPreferenceEntry<ColorMode>> = values().map {
            ListPreferenceEntry(value = it) { stringResource(id = it.labelResourceId) }
        }.toPersistentList()
    }
}
