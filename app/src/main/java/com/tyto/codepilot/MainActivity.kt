package com.tyto.codepilot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tyto.codepilot.ai.Providers
import com.tyto.codepilot.data.SettingsStore
import com.tyto.codepilot.data.UsageStore
import com.tyto.codepilot.ui.ChatScreen
import com.tyto.codepilot.ui.ChatState
import com.tyto.codepilot.ui.OwlMark
import com.tyto.codepilot.ui.PulseDot
import com.tyto.codepilot.ui.SettingsScreen
import com.tyto.codepilot.ui.Tyto
import com.tyto.codepilot.ui.TytoSplash
import com.tyto.codepilot.ui.TytoTheme
import com.tyto.codepilot.ui.UsageScreen
import com.tyto.codepilot.ui.tytoBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TytoTheme {
                App()
            }
        }
    }
}

private enum class Tab(val title: String) {
    Chat("CHAT"),
    Usage("USAGE"),
    Settings("SETTINGS"),
}

@Composable
private fun App() {
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    val usage = remember { UsageStore(context) }
    val chat = remember { ChatState(settings) }
    val scope = rememberCoroutineScope()
    var splash by remember { mutableStateOf(true) }
    var tab by remember { mutableStateOf(if (settings.isConfigured) Tab.Chat else Tab.Settings) }
    var providerId by remember { mutableStateOf(settings.selectedProvider) }

    if (splash) {
        TytoSplash { splash = false }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .tytoBackground()
            .systemBarsPadding()
            .imePadding(),
    ) {
        TopBar(providerId) { tab = Tab.Settings }
        Box(Modifier.weight(1f)) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(80)) },
                label = "tabs",
            ) { current ->
                when (current) {
                    Tab.Chat -> ChatScreen(settings, usage, chat, scope)
                    Tab.Usage -> UsageScreen(usage)
                    Tab.Settings -> SettingsScreen(settings) {
                        providerId = settings.selectedProvider
                        tab = Tab.Chat
                    }
                }
            }
        }
        NavBar(tab) { tab = it }
    }
}

@Composable
private fun TopBar(providerId: String, onProviderClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(Tyto.Line, Offset(0f, size.height), Offset(size.width, size.height), 1f)
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OwlMark(Modifier.size(28.dp))
        Text("TYTO", color = Tyto.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 6.sp)
        Spacer(Modifier.weight(1f))
        Row(
            Modifier
                .border(BorderStroke(1.dp, Tyto.Crimson))
                .clickable(onClick = onProviderClick)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PulseDot()
            Text(Providers.byId(providerId).name.uppercase(), color = Tyto.White, fontSize = 11.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun NavBar(current: Tab, onSelect: (Tab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind { drawLine(Tyto.Line, Offset(0f, 0f), Offset(size.width, 0f), 1f) },
    ) {
        Tab.values().forEach { tab ->
            val active = tab == current
            val color by animateColorAsState(
                if (active) Tyto.Crimson else Tyto.Mute,
                animationSpec = tween(100),
                label = "navColor",
            )
            Column(
                Modifier
                    .weight(1f)
                    .clickable { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.height(2.dp).fillMaxWidth().background(if (active) Tyto.Crimson else Tyto.Obsidian))
                Text(
                    tab.title,
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(vertical = 14.dp),
                )
            }
        }
    }
}
