/*
 * Copyright 2022, YiTap
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.yitap.ui.preferences.about

import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.yitap.ui.preferences.LocalIsExpandedScreen
import app.yitap.ui.preferences.components.NavigationActionPreference
import app.yitap.ui.preferences.components.controls.ClickablePreference
import app.yitap.ui.preferences.components.layout.PreferenceGroup
import app.yitap.ui.preferences.components.layout.PreferenceLayout
import com.android.launcher3.BuildConfig
import com.android.launcher3.R

private enum class Role(val descriptionResId: Int) {
    Development(descriptionResId = R.string.development),
    DevOps(descriptionResId = R.string.devops),
    QuickSwitchMaintenance(descriptionResId = R.string.quickswitch_maintenance),
    Support(descriptionResId = R.string.support),
    SupportAndPr(descriptionResId = R.string.support_and_pr),
}

private data class TeamMember(
    val name: String,
    val role: Role,
    val photoUrl: String,
    val socialUrl: String,
)

private data class Link(
    @DrawableRes val iconResId: Int,
    @StringRes val labelResId: Int,
    val url: String,
)

private val product = listOf(
    TeamMember(
        name = "lingyicute",
        role = Role.Development,
        photoUrl = "https://avatars.githubusercontent.com/u/160479206",
        socialUrl = "https://github.com/lingyicute",
    ),
)

private val supportAndPr = listOf(
    TeamMember(
        name = "Amogh Lele",
        role = Role.DevOps,
        photoUrl = "https://avatars.githubusercontent.com/u/31761843",
        socialUrl = "https://www.linkedin.com/in/amogh-lele/",
    ),
)

private val links = listOf(
    Link(
        iconResId = R.drawable.ic_new_releases,
        labelResId = R.string.news,
        url = "https://t.me/lyi_channel",
    ),
    Link(
        iconResId = R.drawable.ic_github,
        labelResId = R.string.github,
        url = "https://github.com/lingyicute/YiTap",
    ),
    Link(
        iconResId = R.drawable.ic_help,
        labelResId = R.string.support,
        url = "https://t.me/lingyicute2323",
    ),
)

object AboutRoutes {
    const val LICENSES = "licenses"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun About(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    PreferenceLayout(
        label = stringResource(id = R.string.about_label),
        modifier = modifier,
        backArrowVisible = LocalIsExpandedScreen.current,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_home_comp),
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(id = R.string.derived_app_name),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = BuildConfig.VERSION_DISPLAY_NAME,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = {
                        val commitUrl = "https://github.com/lingyicute/YiTap/commit/${BuildConfig.COMMIT_HASH}"
                        context.startActivity(Intent(Intent.ACTION_VIEW, commitUrl.toUri()))
                    },
                ),
            )
            Spacer(modifier = Modifier.requiredHeight(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                links.forEach { link ->
                    YitapLink(
                        iconResId = link.iconResId,
                        label = stringResource(id = link.labelResId),
                        modifier = Modifier.weight(weight = 1f),
                        url = link.url,
                    )
                }
            }
        }
        PreferenceGroup(heading = stringResource(id = R.string.product)) {
            product.forEach {
                ContributorRow(
                    name = it.name,
                    description = stringResource(it.role.descriptionResId),
                    url = it.socialUrl,
                    photoUrl = it.photoUrl,
                )
            }
        }
        PreferenceGroup(heading = stringResource(id = R.string.support_and_pr)) {
            supportAndPr.forEach {
                ContributorRow(
                    name = it.name,
                    description = stringResource(it.role.descriptionResId),
                    url = it.socialUrl,
                    photoUrl = it.photoUrl,
                )
            }
        }
        PreferenceGroup {
            NavigationActionPreference(
                label = stringResource(id = R.string.acknowledgements),
                destination = AboutRoutes.LICENSES,
            )
            ClickablePreference(
                label = stringResource(id = R.string.translate),
                onClick = {
                    val webpage = Uri.parse(CROWDIN_URL)
                    val intent = Intent(Intent.ACTION_VIEW, webpage)
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    }
                },
            )
        }
    }
}

private const val CROWDIN_URL = "https://github.com/lingyicute/YiTap/issues"
