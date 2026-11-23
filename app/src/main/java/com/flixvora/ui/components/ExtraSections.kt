package com.flixvora.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixvora.ui.theme.BrandBlue
import com.flixvora.ui.theme.BrandGreen
import com.flixvora.ui.theme.BrandGreenInk
import com.flixvora.ui.theme.ErrorRed
import com.flixvora.ui.theme.OutlineSubtle
import com.flixvora.ui.theme.SurfaceOverlay
import com.flixvora.ui.theme.SurfaceRaised
import com.flixvora.ui.theme.TextPrimary
import com.flixvora.ui.theme.TextSecondary
import com.flixvora.ui.theme.TextTertiary

data class PartySession(
    val title: String,
    val poster: String,
    val isLive: Boolean,
    val statusText: String,
    val people: Int
)

/** Group-watch rail: LIVE sessions blink, scheduled ones stay muted. */
@Composable
fun WatchPartySection(sessions: List<PartySession>, onClick: (PartySession) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        MbSectionHeader(
            title = "Watch Party",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            items(sessions) { session ->
                WatchPartyCard(session) { onClick(session) }
            }
        }
    }
}

/**
 * Demo party card: 176dp wide, green-tinted hairline border, LIVE marker,
 * title over the current title, an overlapping avatar stack and a time line.
 */
@Composable
private fun WatchPartyCard(session: PartySession, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(176.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceRaised)
            .border(1.dp, BrandGreen.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (session.isLive) {
                LiveDot()
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = if (session.isLive) "LIVE" else "SCHEDULED",
                fontSize = 7.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (session.isLive) ErrorRed else TextTertiary,
                letterSpacing = 0.7.sp
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            session.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            session.poster,
            fontSize = 9.sp,
            color = TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(9.dp))
        AvatarStack(session.people)
        Spacer(Modifier.height(8.dp))
        Text(
            session.statusText,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextTertiary
        )
    }
}

@Composable
private fun LiveDot() {
    val transition = rememberInfiniteTransition(label = "live")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "live_alpha"
    )
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(Color(0xFFEF4444).copy(alpha = alpha))
    )
}

@Composable
private fun AvatarStack(count: Int) {
    // Demo stack: 21dp coloured initials overlapping by 6dp, then a +N pill.
    val shown = count.coerceIn(1, 3)
    val palette = listOf(
        BrandGreen, BrandBlue, Color(0xFFF59E0B), Color(0xFFBE123C), Color(0xFFA855F7)
    )
    val initials = listOf("S", "K", "V", "R", "A")
    Box(modifier = Modifier.width((15 + shown * 15).dp).height(21.dp)) {
        repeat(shown) { i ->
            Box(
                modifier = Modifier
                    .offset(x = (i * 15).dp)
                    .size(21.dp)
                    .clip(CircleShape)
                    .background(palette[i % palette.size])
                    .border(1.5.dp, SurfaceRaised, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials[i % initials.size],
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandGreenInk
                )
            }
        }
        if (count > shown) {
            Box(
                modifier = Modifier
                    .offset(x = (shown * 15).dp)
                    .size(21.dp)
                    .clip(CircleShape)
                    .background(SurfaceOverlay)
                    .border(1.5.dp, SurfaceRaised, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "+${count - shown}",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun TopTenSection(
    items: List<com.flixvora.data.model.MovieSubject>,
    onClick: (com.flixvora.data.model.MovieSubject) -> Unit
) {
    if (items.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        MbSectionHeader(
            title = "Top 10 Today",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items.take(10)) { movie ->
                val rank = items.indexOf(movie) + 1
                MbTop10Card(rank = rank, movie = movie) { onClick(movie) }
            }
        }
    }
}

/**
 * Upcoming rail. The demo pairs each poster with a release-date chip and a
 * "booked" count, which is what makes the section read as a schedule rather
 * than another poster shelf.
 */
@Composable
fun ComingSoonSection(
    items: List<com.flixvora.data.model.MovieSubject>,
    onClick: (com.flixvora.data.model.MovieSubject) -> Unit
) {
    if (items.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        MbSectionHeader(
            title = "Coming Soon",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items.take(12)) { movie ->
                ComingSoonCard(movie) { onClick(movie) }
            }
        }
    }
}

@Composable
private fun ComingSoonCard(
    movie: com.flixvora.data.model.MovieSubject,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(104.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceRaised)
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(6.dp))
        ) {
            coil.compose.AsyncImage(
                model = movie.poster,
                contentDescription = movie.name,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Date chip pinned to the bottom-left corner of the art.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "SOON",
                    fontSize = 6.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandGreen,
                    letterSpacing = 0.5.sp
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = movie.name,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = movie.year?.takeIf { it.isNotBlank() && it != "Not available" }?.let { "$it booked" } ?: "Upcoming",
            fontSize = 8.5.sp,
            color = TextTertiary,
            maxLines = 1
        )
    }
}
