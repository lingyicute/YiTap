package app.yitap.ui.preferences.components.layout

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.yitap.ui.theme.dividerColor

@Composable
fun PreferenceDivider(
    modifier: Modifier = Modifier,
    startIndent: Dp = 0.dp,
    endIndent: Dp = 0.dp,
) {
    HorizontalDivider(
        modifier = modifier
            .padding(start = startIndent + 16.dp, end = endIndent + 16.dp),
        color = dividerColor(),
    )
}
