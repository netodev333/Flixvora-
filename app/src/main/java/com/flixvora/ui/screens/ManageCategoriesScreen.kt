package com.flixvora.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.flixvora.ui.theme.*
import com.flixvora.ui.viewmodel.ADULT_CATEGORY_ID
import com.flixvora.ui.viewmodel.MovieViewModel

/**
 * Catalogue tabs the user can add to the home feed. The 18+ entry is kept in
 * the list so its id is still known, but it is filtered out of the UI unless
 * adult content is enabled in Settings — offering it as a plain toggle
 * contradicted that setting.
 */
val availableMovieBoxCategories = listOf(
    3 to "Education",
    4 to "Music",
    ADULT_CATEGORY_ID to "18+",
    19 to "Western TV Series",
    23 to "Kids Movies",
    28 to "Nollywood",
    37 to "Combat",
    38 to "Cooking",
    40 to "Courses"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    viewModel: MovieViewModel,
    onBack: () -> Unit
) {
    val activeCategories by viewModel.activeCategories.collectAsState()
    val activeIds = activeCategories.map { it.first }

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianDark
                )
            )
        },
        containerColor = ObsidianDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Text(
                text = "Customize your Home Feed",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            val offeredCategories = remember(viewModel.adultContentEnabled) {
                availableMovieBoxCategories.filter { cat ->
                    viewModel.adultContentEnabled || cat.first != ADULT_CATEGORY_ID
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(offeredCategories) { cat ->
                    val isAdded = activeIds.contains(cat.first)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ObsidianCard, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = cat.second,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )

                        IconButton(
                            onClick = {
                                if (isAdded) {
                                    viewModel.removeCategory(cat.first)
                                } else {
                                    viewModel.addCategory(cat.first, cat.second)
                                }
                            }
                        ) {
                            if (isAdded) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "Remove",
                                    tint = CinematicRed
                                )
                            } else {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
