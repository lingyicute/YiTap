package app.yitap.ui.preferences.components.colorpreference

import app.yitap.theme.color.ColorOption
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

val staticColors: ImmutableList<ColorPreferenceEntry<ColorOption>> = sequenceOf(
    ColorOption.CustomColor(0xFFF32020),
    ColorOption.CustomColor(0xFFF20D69),
    ColorOption.CustomColor(0xFF7452FF),
    ColorOption.CustomColor(0xFF2C41C9),
    ColorOption.YitapBlue,
    ColorOption.CustomColor(0xFF00BAD6),
    ColorOption.CustomColor(0xFF00A399),
    ColorOption.CustomColor(0xFF47B84F),
    ColorOption.CustomColor(0xFFFFBB00),
    ColorOption.CustomColor(0xFFFF9800),
    ColorOption.CustomColor(0xFF7C5445),
    ColorOption.CustomColor(0xFF67818E),
).map(ColorOption::colorPreferenceEntry).toPersistentList()

val dynamicColors: ImmutableList<ColorPreferenceEntry<ColorOption>> =
    sequenceOf(ColorOption.SystemAccent, ColorOption.WallpaperPrimary)
        .filter(ColorOption::isSupported)
        .map(ColorOption::colorPreferenceEntry)
        .toPersistentList()

val dynamicColorsWithDefault: ImmutableList<ColorPreferenceEntry<ColorOption>> =
    (dynamicColors.asSequence() + ColorOption.Default.colorPreferenceEntry).toPersistentList()
