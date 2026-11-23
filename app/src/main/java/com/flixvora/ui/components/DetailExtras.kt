package com.flixvora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixvora.ui.theme.OutlineSubtle
import com.flixvora.ui.theme.SurfaceRaised
import com.flixvora.ui.theme.TextPrimary
import com.flixvora.ui.theme.TextSecondary
import com.flixvora.ui.theme.TextTertiary

private val Accent = Color(0xFF1DD171)
private val Surface = Color(0xFF1A1B1F)


private data class QualityRow(
    val label: String,
    val codec: String,
    val size: String,
    val bars: Int,
    val best: Boolean = false
)

/** Higher is better; used to put the strongest stream first. */
private fun streamRank(quality: String): Int = when {
    quality.contains("4K", true) || quality.contains("2160", true) -> 4
    quality.contains("1440", true) -> 3
    quality.contains("1080", true) -> 2
    quality.contains("720", true) -> 1
    else -> 0
}

private fun streamBars(quality: String): Int = streamRank(quality).coerceIn(1, 4)

/**
 * The catalog names a stream only by resolution, so the label describes what the
 * resolution actually guarantees instead of claiming a codec the API never sent.
 */
private fun com.flixvora.data.model.StreamItem.codecLabel(): String = when (streamRank(quality)) {
    4 -> "Ultra HD · HDR"
    3 -> "Quad HD"
    2 -> "Full HD"
    1 -> "HD"
    else -> "Standard"
}

/**
 * Download ladder built from the streams the play-info call actually returned.
 * Nothing is rendered until a stream exists, because a quality row with a made-up
 * file size would mislead the viewer about what they are about to download.
 */
@Composable
fun QualityLadderSection(
    streams: List<com.flixvora.data.model.StreamItem> = emptyList(),
    onPick: (String) -> Unit = {}
) {
    val rows = streams
        .distinctBy { it.quality }
        .sortedByDescending { streamRank(it.quality) }
        .mapIndexed { index, stream ->
            QualityRow(
                label = stream.quality,
                codec = stream.codecLabel(),
                size = stream.size.ifBlank { "Size unavailable" },
                bars = streamBars(stream.quality),
                best = index == 0
            )
        }
    if (rows.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        SectionHeader(title = "Download Quality")
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Surface)
                    .border(
                        1.dp,
                        if (row.best) Accent.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.06f),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onPick(row.label) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (row.best) Accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f))
                        .padding(vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        row.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (row.best) Accent else TextPrimary
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(row.codec, fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                    Text(row.size, fontSize = 10.sp, color = TextSecondary)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    repeat(4) { i ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height((5 + i * 4).dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(
                                    if (i < row.bars) Accent.copy(alpha = 0.85f)
                                    else Color.White.copy(alpha = 0.14f)
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Spec table. Every row is a label/value pair, which is how the demo presents
 * production facts; values fall back to an em dash when the payload is empty.
 */
@Composable
fun DetailsTableSection(rows: List<Pair<String, String>>) {
    if (rows.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(
            "Details",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceRaised)
        ) {
            rows.forEachIndexed { index, (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        label,
                        fontSize = 11.sp,
                        color = TextTertiary,
                        modifier = Modifier.width(84.dp)
                    )
                    Text(
                        value.ifBlank { "\u2014" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (index != rows.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp)
                            .height(1.dp)
                            .background(OutlineSubtle)
                    )
                }
            }
        }
    }
}
