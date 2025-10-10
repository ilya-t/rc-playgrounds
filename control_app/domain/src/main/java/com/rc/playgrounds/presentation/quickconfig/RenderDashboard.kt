package com.rc.playgrounds.presentation.quickconfig

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Render(viewModel: QuickConfigViewModel.Visible) {
    BoxWithConstraints {
        val isPortrait = maxWidth < maxHeight

        if (isPortrait) {
            Column {
                DescriptionBox(viewModel)
                RenderDashboard(viewModel)
            }
        } else {
            Row {
                RenderDashboard(viewModel)
                DescriptionBox(viewModel)
            }
        }
    }
}

@Composable
fun DescriptionBox(viewModel: QuickConfigViewModel.Visible) {
    Row {
        Box(
            modifier = Modifier.width(200.dp)
                .padding(all = 12.dp)
        ) {
            androidx.compose.material.Text(
                text = viewModel.description,
                textAlign = TextAlign.Left,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            )
        }
        RenderDashboard(viewModel)
    }
}

@Composable
fun RenderDashboard(viewModel: QuickConfigViewModel.Visible) {
    val columns: List<ElementGroup> = viewModel.elementGroups

    val tileHeight = 72.dp
    val tileWidth = 120.dp
    val gap = 12.dp
    val corner = 10.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        columns.forEach { col: ElementGroup ->
            // Each column measures its available height and keeps a centered window around focus.
            BoxWithConstraints(
                modifier = Modifier.wrapContentWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                val availableHeight = maxHeight           // height available for this column
                val oneTile = tileHeight + gap            // tile + vertical gap
                val titleSlots = 1                        // group title always visible
                val minSlots = titleSlots + 1             // title + at least 1 element

                // How many tiles (including title) can we show?
                val maxVisibleTiles = maxOf(minSlots, (availableHeight / oneTile).toInt())

                // How many element slots remain after the title?
                val elementSlots = maxVisibleTiles - titleSlots

                // Determine the focused element in this group
                val focusedIndex = col.elements.indexOfFirst { it.focused }.let { if (it == -1) 0 else it }
                val total = col.elements.size

                // Compute a centered window around the focus
                val (start, endExclusive) = computeWindow(total, focusedIndex, elementSlots)

                // Flags for “more” indicators
                val hasMoreAbove = start > 0
                val hasMoreBelow = endExclusive < total

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(gap)
                ) {
                    // Always show the group "title" tile
                    RenderSquareTile(
                        label = col.title,
                        isActiveToggle = col.active,
                        width = tileWidth,
                        height = tileHeight,
                        isFocused = col.focused,
                        corner = corner,
                    )

                    if (hasMoreAbove) {
                        MoreIndicator(width = tileWidth, height = 24.dp)
                    }

                    // Visible window of elements
                    col.elements.subList(start, endExclusive).forEach { tile ->
                        RenderSquareTile(
                            label = tile.title,
                            isActiveToggle = tile.active,
                            width = tileWidth,
                            height = tileHeight,
                            isFocused = tile.focused,
                            corner = corner
                        )
                    }

                    if (hasMoreBelow) {
                        MoreIndicator(width = tileWidth, height = 24.dp)
                    }
                }
            }
        }
    }
}

/**
 * Returns a [start, endExclusive] window of size up to [windowSize] centered on [focus].
 */
private fun computeWindow(total: Int, focus: Int, windowSize: Int): Pair<Int, Int> {
    if (total <= windowSize) return 0 to total
    val half = windowSize / 2
    var start = focus - half
    start = start.coerceAtLeast(0)
    // Ensure we always have windowSize items where possible
    if (start + windowSize > total) {
        start = (total - windowSize).coerceAtLeast(0)
    }
    val end = (start + windowSize).coerceAtMost(total)
    return start to end
}

@Composable
private fun MoreIndicator(width: Dp, height: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "•••",
            style = MaterialTheme.typography.labelLarge.copy(
                color = Color.Gray.copy(alpha = 0.8f),
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun RenderSquareTile(
    label: String,
    isActiveToggle: Boolean,
    isFocused: Boolean,
    width: Dp,
    height: Dp,
    corner: Dp
) {
    val borderWidth = if (isActiveToggle) 2.dp else 1.dp
    val borderColor = when {
        isFocused -> Color(0xFFFF5722).copy(alpha = 0.6f)
        isActiveToggle -> Color(0xFF1D3557).copy(alpha = 0.6f)
        else -> Color.Gray.copy(alpha = 0.4f)
    }
    val bgColor = if (isActiveToggle)
        Color(0xFF1D3557).copy(alpha = 0.25f)
    else
        Color.Gray.copy(alpha = 0.15f)

    val actualHeight = if (isFocused) height + 24.dp else height
    val actualWidth = if (isFocused) width + 24.dp else width

    Box(
        modifier = Modifier
            .width(actualWidth)
            .height(actualHeight)
            .border(borderWidth, borderColor, RoundedCornerShape(corner))
            .background(bgColor, RoundedCornerShape(corner))
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
        )
    }
}



@Preview
@Composable
fun Preview() {
    Box(modifier = Modifier.background(Color.Black)) {
        RenderDashboard(
            QuickConfigViewModel.Visible(
                description = "",
                elementGroups = listOf(
                    ElementGroup(
                        title = "modes",
                        elements = listOf(
                            Element(
                                active = true,
                                focused = false,
                                title = "normal",
                            ),
                            Element(
                                active = true,
                                focused = true,
                                title = "crawling",
                            ),
                            Element(
                                active = false,
                                focused = false,
                                title = "max long",
                            ),
                        ),
                        active = true,
                        focused = false,
                    ),
                    ElementGroup(
                        title = "resolution",
                        elements = listOf(
                            Element(
                                active = false,
                                focused = false,
                                title = "320x240",
                            ),
                            Element(
                                active = false,
                                focused = false,
                                title = "640x480",
                            ),
                            Element(
                                active = false,
                                focused = false,
                                title = "800x600",
                            ),
                            Element(
                                active = false,
                                focused = false,
                                title = "1024x768",
                            ),
                        ),
                        active = false,
                        focused = false,
                    )
                ),
                onButtonUpPressed = {
//                            qualityProvider.nextQuality()
                },
                onButtonDownPressed = {
//                            qualityProvider.prevQuality()
                },
                onButtonLeftPressed = {
//                            shiftSteerOffset(-STEER_OFFSET_STEP)
                },
                onButtonRightPressed = {
//                            shiftSteerOffset(STEER_OFFSET_STEP)
                },
                onApplyButton = { },
                onBackButton = {
                    //    activeScreenProvider.switchTo(Screen.MAIN)
                }
            )
        )
    }
}