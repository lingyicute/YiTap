package app.yitap.icons

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IconPickerItem(
    val packPackageName: String,
    val drawableName: String,
    val label: String,
    val type: IconType,
) : Parcelable {
    fun toIconEntry() = IconEntry(
        packPackageName = packPackageName,
        name = drawableName,
        type = type,
    )
}
