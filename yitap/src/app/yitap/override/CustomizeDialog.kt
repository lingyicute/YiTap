package app.yitap.override

import android.app.Activity
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.yitap.preferences.preferenceManager
import app.yitap.preferences2.asState
import app.yitap.preferences2.preferenceManager2
import app.yitap.ui.preferences.PreferenceActivity
import app.yitap.ui.preferences.components.controls.SwitchPreference
import app.yitap.ui.preferences.components.layout.ClickableIcon
import app.yitap.ui.preferences.components.layout.PreferenceGroup
import app.yitap.ui.preferences.navigation.Routes
import app.yitap.ui.util.addIfNotNull
import app.yitap.util.navigationBarsOrDisplayCutoutPadding
import com.android.launcher3.LauncherAppState
import com.android.launcher3.R
import com.android.launcher3.util.ComponentKey
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.launch

@Composable
fun CustomizeDialog(
    icon: Drawable,
    title: String,
    onTitleChange: (String) -> Unit,
    defaultTitle: String,
    launchSelectIcon: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .navigationBarsOrDisplayCutoutPadding()
            .fillMaxWidth(),
    ) {
        val iconPainter = rememberDrawablePainter(drawable = icon)
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 24.dp)
                .clip(MaterialTheme.shapes.small)
                .addIfNotNull(launchSelectIcon) {
                    clickable(onClick = it)
                }
                .padding(all = 8.dp),
        ) {
            Image(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.size(54.dp),
            )
        }
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            trailingIcon = {
                if (title != defaultTitle) {
                    ClickableIcon(
                        painter = painterResource(id = R.drawable.ic_undo),
                        onClick = { onTitleChange(defaultTitle) },
                    )
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
            ),
            shape = MaterialTheme.shapes.large,
            label = { Text(text = stringResource(id = R.string.label)) },
            isError = title.isEmpty(),
        )
        content?.invoke()
    }
}

@Composable
fun CustomizeAppDialog(
    icon: Drawable,
    defaultTitle: String,
    componentKey: ComponentKey,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
) {
    val prefs = preferenceManager()
    val preferenceManager2 = preferenceManager2()
    val coroutineScope = rememberCoroutineScope()
    val showComponentNames by preferenceManager2.showComponentNames.asState()
    val hiddenApps by preferenceManager2.hiddenApps.asState()
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }

    val request = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        onClose()
    }

    Log.d("TEST", "${Routes.SELECT_ICON}/$componentKey")

    val openIconPicker = {
        val destination = "${Routes.SELECT_ICON}/$componentKey/"
        request.launch(PreferenceActivity.createIntent(context, destination))
    }

    DisposableEffect(key1 = null) {
        title = prefs.customAppName[componentKey] ?: defaultTitle
        onDispose {
            val previousTitle = prefs.customAppName[componentKey]
            val newTitle = if (title != defaultTitle) title else null
            if (newTitle != previousTitle) {
                prefs.customAppName[componentKey] = newTitle
                val model = LauncherAppState.getInstance(context).model
                model.onPackageChanged(componentKey.componentName.packageName, componentKey.user)
            }
        }
    }
    CustomizeDialog(
        icon = icon,
        title = title,
        onTitleChange = { title = it },
        defaultTitle = defaultTitle,
        launchSelectIcon = openIconPicker,
        modifier = modifier,
    ) {
        PreferenceGroup(
            description = componentKey.componentName.flattenToString(),
            showDescription = showComponentNames,
        ) {
            val stringKey = componentKey.toString()
            SwitchPreference(
                checked = hiddenApps.contains(stringKey),
                label = stringResource(id = R.string.hide_from_drawer),
                onCheckedChange = { newValue ->
                    val newSet = hiddenApps.toMutableSet()
                    if (newValue) newSet.add(stringKey) else newSet.remove(stringKey)
                    coroutineScope.launch {
                        preferenceManager2.hiddenApps.set(value = newSet)
                    }
                },
            )
        }
    }
}
