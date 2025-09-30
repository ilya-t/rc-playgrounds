package com.rc.playgrounds.presentation.quickconfig

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
                    isFocused = col.focused,
                    corner = corner,
                )

                col.elements.forEach { tile ->
                    RenderSquareTile(
                        label = tile.title,
                        isActiveToggle = tile.active,
                        size = tileSize,
                        isFocused = tile.focused,
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
    isFocused: Boolean,
    size: Dp,
    corner: Dp
) {
    val isTitle = false

    val borderWidth = if (isActiveToggle) 2.dp else 1.dp
    val borderColor = if (isActiveToggle)
        Color(0xFF1D3557).copy(alpha = 0.6f)
    else
        Color.Gray.copy(alpha = 0.4f)

    val bgColor = if (isActiveToggle)
        Color(0xFF1D3557).copy(alpha = 0.25f)
    else
        Color.Gray.copy(alpha = 0.15f)

    val actualSize = if (isFocused) size + 16.dp else size

    Box(
        modifier = Modifier
            .size(actualSize)
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
                MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            else
                MaterialTheme.typography.bodyMedium.copy(color = Color.White)
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