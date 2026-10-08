package com.tyto.codepilot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tyto.codepilot.ai.Providers
import com.tyto.codepilot.data.UsageRecord
import com.tyto.codepilot.data.UsageStore
import java.util.Calendar

private const val DAYS = 14

private fun startOfDay(offsetDays: Int): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.add(Calendar.DAY_OF_YEAR, offsetDays)
    return cal.timeInMillis
}

private fun startOfMonth(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun tokens(n: Int): String = if (n >= 1000) "%.1fk".format(n / 1000.0) else n.toString()

private fun isFree(r: UsageRecord): Boolean =
    r.model.endsWith(":free") || r.provider == "gemini" || r.provider == "groq"

@Composable
fun UsageScreen(usage: UsageStore) {
    var version by remember { mutableIntStateOf(0) }
    val records = remember(version) { usage.all() }

    val today = startOfDay(0)
    val month = startOfMonth()
    val todayRecords = records.filter { it.time >= today }
    val monthRecords = records.filter { it.time >= month }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile("TODAY", "${todayRecords.size}", "${tokens(todayRecords.sumOf { it.totalTokens })} tokens", Modifier.weight(1f))
            Tile("THIS MONTH", "${monthRecords.size}", "${tokens(monthRecords.sumOf { it.totalTokens })} tokens", Modifier.weight(1f))
            Tile("FAILED", "${monthRecords.count { !it.ok }}", "${monthRecords.count { it.status == 429 }} rate-limited", Modifier.weight(1f))
        }

        if (records.isEmpty()) {
            Text(
                "No calls yet. Run a task on the Chat tab and your model usage shows up here.",
                color = Tyto.Mute,
                fontSize = 13.sp,
            )
            return@Column
        }

        SectionLabel("CALLS · LAST $DAYS DAYS")
        DailyChart(records)

        SectionLabel("BY MODEL")
        val byModel = records.groupBy { it.provider to it.model }
            .map { (key, list) -> Triple(key, list, list.sumOf { it.totalTokens }) }
            .sortedByDescending { it.third }
        val maxTokens = (byModel.maxOfOrNull { it.third } ?: 1).coerceAtLeast(1)
        byModel.forEach { (key, list, total) ->
            val providerName = Providers.byId(key.first).name
            UsageRow(
                title = key.second,
                subtitle = "$providerName · ${list.size} calls · in ${tokens(list.sumOf { it.promptTokens })} · out ${tokens(list.sumOf { it.completionTokens })} · ${list.count { !it.ok }} failed",
                tag = if (isFree(list.first())) "FREE" else "PAID",
                fraction = total.toFloat() / maxTokens,
            )
        }

        SectionLabel("BY PROVIDER")
        val byProvider = records.groupBy { it.provider }
            .map { (id, list) -> Triple(id, list, list.sumOf { it.totalTokens }) }
            .sortedByDescending { it.third }
        val maxProvider = (byProvider.maxOfOrNull { it.third } ?: 1).coerceAtLeast(1)
        byProvider.forEach { (id, list, total) ->
            UsageRow(
                title = Providers.byId(id).name,
                subtitle = "${list.size} calls · ${tokens(total)} tokens",
                tag = null,
                fraction = total.toFloat() / maxProvider,
            )
        }

        TextButton(onClick = {
            usage.clear()
            version++
        }) {
            Text("CLEAR HISTORY", color = Tyto.Mute, fontSize = 12.sp, letterSpacing = 1.5.sp)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Tyto.Crimson, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
}

@Composable
private fun Tile(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .border(BorderStroke(1.dp, Tyto.Line))
            .background(Tyto.Panel)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, color = Tyto.Mute, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Text(value, color = Tyto.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(sub, color = Tyto.Mute, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun UsageRow(title: String, subtitle: String, tag: String?, fraction: Float) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                title,
                color = Tyto.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (tag != null) {
                Text(
                    tag,
                    color = if (tag == "FREE") Tyto.Crimson else Tyto.Mute,
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier
                        .border(BorderStroke(1.dp, if (tag == "FREE") Tyto.Crimson else Tyto.Line))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(4.dp).background(Tyto.Line)) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .background(Tyto.Crimson),
            )
        }
        Text(subtitle, color = Tyto.Mute, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun DailyChart(records: List<UsageRecord>) {
    val starts = (DAYS - 1 downTo 0).map { startOfDay(-it) }
    val counts = starts.mapIndexed { i, start ->
        val end = if (i + 1 < starts.size) starts[i + 1] else Long.MAX_VALUE
        records.count { it.time in start until end }
    }
    val max = (counts.maxOrNull() ?: 1).coerceAtLeast(1)

    Column(Modifier.fillMaxWidth().border(BorderStroke(1.dp, Tyto.Line)).padding(10.dp)) {
        Canvas(Modifier.fillMaxWidth().height(90.dp)) {
            val slot = size.width / DAYS
            val bar = slot * 0.6f
            counts.forEachIndexed { i, count ->
                val h = size.height * count / max
                drawRect(
                    Tyto.Line,
                    topLeft = Offset(i * slot + (slot - bar) / 2f, 0f),
                    size = Size(bar, size.height),
                )
                if (count > 0) {
                    drawRect(
                        Tyto.Crimson,
                        topLeft = Offset(i * slot + (slot - bar) / 2f, size.height - h),
                        size = Size(bar, h),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            starts.forEach { start ->
                val day = Calendar.getInstance().apply { timeInMillis = start }.get(Calendar.DAY_OF_MONTH)
                Text(
                    "$day",
                    color = Tyto.Mute,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}
