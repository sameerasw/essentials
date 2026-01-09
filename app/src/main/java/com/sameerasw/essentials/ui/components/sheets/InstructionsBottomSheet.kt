package com.sameerasw.essentials.ui.components.sheets

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer

data class InstructionStep(
    val instruction: String,
    val imageRes: Int
)

data class InstructionSection(
    val title: String,
    val iconRes: Int,
    val description: String? = null,
    val steps: List<InstructionStep> = emptyList(),
    val links: List<Pair<String, String>> = emptyList() // Pair(label, url)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructionsBottomSheet(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val sections = listOf(
        InstructionSection(
            title = "Accessibility, Notification and Overlay permissions",
            iconRes = R.drawable.rounded_security_24,
            steps = listOf(
                InstructionStep(
                    instruction = "You may get this access denied message if you try to grant sensitive permissions such as accessibility, notification listener or overlay permissions. To grant it, check the steps below.",
                    imageRes = R.drawable.accessibility_1
                ),
                InstructionStep(
                    instruction = "1. Go to app info page of Essentials.",
                    imageRes = R.drawable.accessibility_2
                ),
                InstructionStep(
                    instruction = "2. Open the 3-dot menu and select 'Allow restricted settings'. You may have to authenticate with biometrics. Once done, Try to grant the permission again.",
                    imageRes = R.drawable.accessibility_3
                )
            )
        ),
        InstructionSection(
            title = "Shizuku",
            iconRes = R.drawable.rounded_adb_24,
            description = "Shizuku is a powerful tool that allows apps to use system APIs directly with ADB or root permissions. It is required for features like Maps min mode, App Freezer. And willa ssist granting some permissions such as WRITE_SECURE_SETTINGS. \n\nBut the Play Store version of Shizuku might be outdated and will probably be unusable on recent Android versions so in that case, please get the latest version from the github or an up-to-date fork of it.",
            links = listOf(
                "Shizuku GitHub" to "https://github.com/RikkaApps/Shizuku",
                "Shizuku (TuoZi) GitHub" to "https://github.com/yangFenTuoZi/Shizuku"
            )
        ),
        InstructionSection(
            title = "Maps power saving mode",
            iconRes = R.drawable.rounded_navigation_24,
            description = "This feature automatically triggers Google Maps power saving mode which is currently exclusive to the Pixel 10 series. A community member discovered that it is still usable on any Android device by launching the maps minMode activity with root privileges. \n\nAnd then, I had it automated with Tasker to automatically trigger when the screen turns off during a navigation session and then was able to achieve the same with just runtime Shizuku permissions. \n\nIt is intended to be shown over the AOD of Pixel 10 series so because of that, you may see an occasional message popping up on the display that it does not support landscape mode. That is not avoidable by the app and you can ignore."
        ),
        InstructionSection(
            title = "Silent sound mode",
            iconRes = R.drawable.rounded_volume_off_24,
            description = "You may have noticed that the silent mode also triggers DND. \n\nThis is due to how the Android implemented it as even if we use the same API to switch to vibrate mode, it for some reason turns on DND along with the silent mode and this is not avoidable at this moment. :("
        ),
        InstructionSection(
            title = "What is freeze?",
            iconRes = R.drawable.rounded_mode_cool_24,
            description = "Pause and stay away from app distractions while saving a little bit of power preventing apps running in the background. Suitable for rarely used apps. \n\nNot recommended for any communication services as they will not notify you in an emergency unless you unfreeze them. \n\nHighly advised to not freeze system apps as they can lead to system instability. Proceed with caution, You were warned. \n\nInspired by Hail <3"
        ),
        InstructionSection(
            title = "Are app lock and screen locked security actually secure?",
            iconRes = R.drawable.rounded_security_24,
            description = "Absolutely not. \n\nAny 3rd party application can not 100% interfere with regular device interactions and even the app lock is only an overlay above selected apps to prevent interacting with them. There are workarounds and it is not foolproof. \n\nSame goes with the screen locked security feature which detects someone trying to interact with the network tiles which for some reason are still accessible for anyone on Pixels. So if they try hard enough they might still be able to change them and especially if you have a flight mode QS tile added, this app can not prevent interactions with it. \n\nThese features are made just as experiments for light usage and would never recommend as strong security and privacy solutions. \n\nSecure alternatives:\n - App lock: Private Space and Secure folder on Pixels and Samsung\n - Preventing mobile networks access: Make sure your theft protection and offline/ power off find my device settings are on. You may look into Graphene OS as well."
        ),
        InstructionSection(
            title = "Statusbar icons",
            iconRes = R.drawable.rounded_interests_24,
            description = "You may notice that even after resetting the statusbar icons, Some icons such as device rotation, wired headphone icons may stay visible. This is due to how the statubar blacklist is implemented in Android and how your OEM may have customized them. \nYou may need further adjustments. \n\nAlso not all icon visibility options may work as they depend on the OEM implementations and availability."
        ),
        InstructionSection(
            title = "Notification lighting does not work",
            iconRes = R.drawable.rounded_blur_linear_24,
            description = "It depends on the OEM. Some like OneUI does not seem to allow overlays above the AOD preventing the lighting effects being shown. In this case, try the ambient display as a workaround. "
        ),
        InstructionSection(
            title = "Button remap does not work while display is off",
            iconRes = R.drawable.rounded_switch_access_3_24,
            description = "Some OEMs limit the accessibility service reporting once the display is actually off but they may still work while the AOD is on. \nIn this case, you may able to use button remaps with AOD on but not with off. \n\nAs a workaround, you will need to use Shizuku permissions and turn on the 'Use Shizuku' toggle in button remap settings which identifies and listen to hardware input events.\nThis is not guaranteed to work on all devices and needs testing.\n\nAnd even if it's on, Shizuku method only will be used when it's needed. Otherwise it will always fallback to Accessibility which also handles the blocking of the actual input during long press."
        ),
        InstructionSection(
            title = "Flashlight brightness does not work",
            iconRes = R.drawable.rounded_flashlight_on_24,
            description = """
                Only a limited number of devices got hardware and software support adjusting the flashlight intensity. 
                
                'The minimum version of Android is 13 (SDK33).
                Flashlight brightness control only supports HAL version 3.8 and higher, so among the supported devices, the latest ones (For example, Pixel 6/7, Samsung S23, etc.)'
                polodarb/Flashlight-Tiramisu
                """.trimIndent()
        ),
        InstructionSection(
            title = "What the hell is this app?",
            iconRes = R.drawable.ic_stat_name,
            description = """
                Good question,
                
                I always wanted to extract the most out of my devices as I've been a rooted user for ever since I got my first Project Treble device. And I've been loving the Tasker app which is like the god when comes automation and utilizing every possible API and internal features of Android.
                
                So I am not unrooted and back on stock Android beta experience and wanted to get the most out from what is possible with given privileges. Might as well share them. So with my beginner knowledge in Kotlin Jetpack and with the support of many research and assist tools and also the great community, I built an all-in-one app containing everything I wanted to be in my Android with given permissions. And here it is.
                
                Feature requests are welcome, I will consider and see if they are achievable with available permissions and my skills. Nowadays what is not possible. :)
                
                Why not on Play Store?
                I don't wanna risk getting my Developer account banned due to the highly sensitive and internal permissions and APIs being used in the app. But with the way Android sideloading is headed, let's see what we have to do. I do understand the concerns of sideloaded apps being malicious.
                While we are at the topic, Checkout my other app AirSync if you are a mac + Android user. *shameless plug*
                
                Enjoy, Keep building! (っ◕‿◕)っ
                """.trimIndent()
        )
    )

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Help & Guides",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                )
            }

            item {
                RoundedCardContainer {
                    sections.forEach { section ->
                        ExpandableGuideSection(section)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Need more support? Reach out,",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            item {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    maxItemsInEachRow = 3
                ) {
                    Button(
                        onClick = {
                            val websiteUrl = "https://github.com/sameerasw/essentials"
                            val intent = Intent(Intent.ACTION_VIEW, websiteUrl.toUri())
                            context.startActivity(intent)
                        },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.brand_github),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GitHub")
                    }

                    OutlinedButton(
                        onClick = {
                            val mailUri = "mailto:mail@sameerasw.com".toUri()
                            val emailIntent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
                                putExtra(Intent.EXTRA_SUBJECT, "Hello from Essentials")
                            }
                            try {
                                context.startActivity(Intent.createChooser(emailIntent, "Send email"))
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, "No email app available", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_mail_24),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Email")
                    }

                    OutlinedButton(
                        onClick = {
                            val websiteUrl = "https://t.me/tidwib"
                            val intent = Intent(Intent.ACTION_VIEW, websiteUrl.toUri())
                            context.startActivity(intent)
                        },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.brand_telegram),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Support Group")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpandableGuideSection(section: InstructionSection) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "arrow_rotation")
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) MaterialTheme.colorScheme.surfaceBright else MaterialTheme.colorScheme.surfaceContainerLow
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = section.iconRes),
                            contentDescription = null,
                            tint = if (expanded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    painter = painterResource(id = R.drawable.rounded_keyboard_arrow_down_24),
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotation)
                )
            }

            // Content
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (section.description != null) {
                        Text(
                            text = section.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(fraction = 0.95f)
                        )
                    }

                    if (section.steps.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            section.steps.forEachIndexed { index, step ->
                                InstructionStepItem(
                                    stepNumber = index + 1,
                                    instruction = step.instruction,
                                    imageRes = step.imageRes
                                )
                            }
                        }
                    }

                    if (section.links.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(fraction = 0.95f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            section.links.forEach { (label, url) ->
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.brand_github),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstructionStepItem(
    stepNumber: Int,
    instruction: String,
    imageRes: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = instruction,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )

        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "Step $stepNumber Image",
            modifier = Modifier
                .fillMaxWidth(fraction = 0.95f)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.FillWidth
        )
    }
}
