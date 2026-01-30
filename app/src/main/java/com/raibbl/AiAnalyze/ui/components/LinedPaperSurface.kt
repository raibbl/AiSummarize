import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun LinedPaperSurface(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
    lineSpacing: Dp = 26.dp,
    marginFromLeft: Dp = 44.dp,
    cornerRadius: Dp = 18.dp,
    content: @Composable () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val paperColor = cs.surface
    val borderColor = cs.outline.copy(alpha = 0.18f)
    val ruleColor = cs.onSurface.copy(alpha = 0.05f)
    val marginColor = cs.primary.copy(alpha = 0.14f)

    Surface(
        modifier = modifier
            .drawBehind {
                val spacingPx = lineSpacing.toPx()
                val marginPx = marginFromLeft.toPx()

                // vertical margin line
                drawLine(
                    color = marginColor,
                    start = Offset(marginPx, 0f),
                    end = Offset(marginPx, size.height),
                    strokeWidth = 2f
                )

                // horizontal ruled lines
                var y = spacingPx
                while (y < size.height) {
                    drawLine(
                        color = ruleColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.5f
                    )
                    y += spacingPx
                }
            },
        color = paperColor,
        tonalElevation = 1.dp,
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(cornerRadius),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(Modifier.padding(contentPadding)) {
            content()
        }
    }
}
