package com.rc.playgrounds.presentation.quickconfig

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun RenderDashboard(viewModel: QuickConfigViewModel.DashboardVisible) {
    val columns: List<ElementGroup> = viewModel.elementGroups

    val maxRows = columns.maxOfOrNull { it.size() } ?: 0
    val tileSize = 72.dp
    val gap = 12.dp
    val corner = 10.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        columns.forEach { col: ElementGroup ->
            val padTop = (maxRows - col.size()) / 2
            val padBottom = maxRows - col.size() - padTop

            Column(
                modifier = Modifier.wrapContentWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                RenderSquareTile(
                    label = col.title,
                    isActiveToggle = col.active,
                    size = tileSize,
                    corner = corner,
                )

                col.elements.forEach { tile ->
                    RenderSquareTile(
                        label = tile.title,
                        isActiveToggle = tile.active,
                        size = tileSize,
                        corner = corner
                    )
                }

                repeat(padBottom) { EmptySquare(tileSize) }
            }
        }
    }
}

private fun ElementGroup.size(): Int {
    return this.elements.size
}

@Composable
private fun EmptySquare(size: Dp) {
    Spacer(Modifier.size(size))
}

@Composable
private fun RenderSquareTile(
    label: String,
    isActiveToggle: Boolean,
    size: Dp,
    corner: Dp
) {
    val isTitle = false
    val borderWidth = if (!isTitle && isActiveToggle) 3.dp else 1.dp
    val borderColor = if (!isTitle && isActiveToggle)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outline

    val bgColor =
        if (isTitle) MaterialTheme.colorScheme.surfaceVariant
        else MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .size(size)
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
            style = if (isTitle)
                MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
            else
                MaterialTheme.typography.bodyMedium
        )
    }
}


@Preview
@Composable
fun Preview() {
    RenderDashboard(
        QuickConfigViewModel.DashboardVisible(
            elementGroups = listOf(
                ElementGroup(
                    title = "modes",
                    elements = listOf(
                        Element(
                            active = true,
                            title = "normal",
                        ),
                        Element(
                            active = true,
                            title = "crawling",
                        ),
                        Element(
                            active = false,
                            title = "max long",
                        ),
                    ),
                    active = true,
                ),
                ElementGroup(
                    title = "resolution",
                    elements = listOf(
                        Element(
                            active = false,
                            title = "320x240",
                        ),
                        Element(
                            active = false,
                            title = "640x480",
                        ),
                        Element(
                            active = false,
                            title = "800x600",
                        ),
                        Element(
                            active = false,
                            title = "1024x768",
                        ),
                    ),
                    active = false,
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