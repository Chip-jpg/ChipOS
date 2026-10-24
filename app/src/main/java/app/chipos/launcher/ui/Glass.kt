package app.chipos.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NoFocus: () -> Float = { 0f }

/** How much of the white veil on glass surfaces is kept. Lower = more see-through. */
private const val VEIL_SCALE = 0.47f

/** How strongly the blurred wallpaper shows inside glass. Lower = more of the sharp wallpaper behind. */
private const val BACKDROP_ALPHA = 0.51f

/** Shape of the large glass sheets (All apps, Settings). */
internal val SheetShape = ContinuousCornerShape(40.dp)

/**
 * A rectangle with "continuous" corners, the smooth superellipse-like curve used by Apple hardware
 * and iOS, instead of a plain circular arc. The curve starts earlier along each edge and eases into
 * the corner, so there is no visible point where the straight edge turns into the curve.
 *
 * [radius] is comparable to a normal corner radius. To nest shapes so they look concentric, use
 * inner radius = outer radius - the padding between them.
 */
class ContinuousCornerShape(private val radius: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { radius.toPx() }
        val w = size.width
        val h = size.height
        // Where the curve leaves each edge, measured from the corner. A plain arc would use r.
        val p = minOf(r * 1.35f, w / 2f, h / 2f)
        // Bezier handle length. Slightly longer than a circular arc's 0.5523 gives the squircle feel.
        val k = p * 0.60f
        val path = Path().apply {
            moveTo(p, 0f)
            lineTo(w - p, 0f)
            cubicTo(w - p + k, 0f, w, p - k, w, p)
            lineTo(w, h - p)
            cubicTo(w, h - p + k, w - p + k, h, w - p, h)
            lineTo(p, h)
            cubicTo(p - k, h, 0f, h - p + k, 0f, h - p)
            lineTo(0f, p)
            cubicTo(0f, p - k, p - k, 0f, p, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * A "liquid glass" surface, drawn behind the content in a single pass.
 *
 * Android 9 and 10 cannot blur what is behind a view, so the wallpaper is blurred ahead of time
 * ([GlassBackdrop]) and the part behind this surface is drawn inside it. On top of that:
 *  1. a thin white veil, a little lighter at the top;
 *  2. a sheen across the top, like light on a curved pane;
 *  3. a small glint near the upper left corner;
 *  4. a faint glow gathering along the bottom edge;
 *  5. a soft light just inside the edge, where real glass bends the light;
 *  6. a hairline rim, bright along the top, dim on the sides, medium along the bottom;
 *  7. when focused, a near-solid white fill fades in over everything.
 *
 * All brushes are built once per size and reused. [focus] is read while drawing, so animating it
 * redraws this surface only and never recomposes anything.
 *
 * It does not clip children. Add `.clip(shape)` after it where content can overflow (scrolling lists).
 *
 * @param restTop opacity of the white veil at the top edge when resting.
 * @param restBottom opacity of the white veil at the bottom edge when resting.
 * @param focus 0 = resting, 1 = focused.
 * @param frost draw the blurred wallpaper inside. Turn off for small pieces that sit on another glass surface.
 */
fun Modifier.liquidGlass(
    shape: Shape,
    restTop: Float = 0.30f,
    restBottom: Float = 0.12f,
    focus: () -> Float = NoFocus,
    frost: Boolean = true,
): Modifier = composed {
    // Where this surface is on the screen, so it knows which part of the backdrop to draw.
    var origin by remember { mutableStateOf(Offset.Zero) }

    this
        .onGloballyPositioned { origin = it.positionInWindow() }
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val clip = outline.toPath()
            val rimWidth = 1.5.dp.toPx()
            val edgeWidth = 6.dp.toPx() // half of this falls inside the shape
            // With a backdrop behind it the veil can be thinner; without one it carries the whole look.
            val lift = if (frost) 0.05f else 0f

            val body = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = (restTop + lift) * VEIL_SCALE),
                    Color.White.copy(alpha = (restBottom + lift) * VEIL_SCALE),
                ),
            )
            val focusFill = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.94f), Color.White.copy(alpha = 0.88f)),
            )
            val topSheen = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.10f),
                0.5f to Color.Transparent,
                startY = 0f,
                endY = size.height,
            )
            // Small and tied to the surface's height, so wide shelves do not get a huge hazy patch.
            val glint = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(size.width * 0.10f, 0f),
                radius = size.minDimension * 1.4f,
            )
            val lowerGlow = Brush.verticalGradient(
                0.74f to Color.Transparent,
                1f to Color.White.copy(alpha = 0.10f),
                startY = 0f,
                endY = size.height,
            )
            val innerEdge = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.16f),
                0.5f to Color.White.copy(alpha = 0.02f),
                1f to Color.White.copy(alpha = 0.08f),
                startY = 0f,
                endY = size.height,
            )
            val rim = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.70f),
                0.45f to Color.White.copy(alpha = 0.14f),
                1f to Color.White.copy(alpha = 0.38f),
                startY = 0f,
                endY = size.height,
            )

            onDrawBehind {
                val f = focus()
                if (frost) {
                    clipPath(clip) { drawBackdrop(origin) }
                }
                drawOutline(outline, body)
                if (f > 0.01f) drawOutline(outline, focusFill, alpha = f)
                val rest = 1f - 0.6f * f
                drawOutline(outline, topSheen, alpha = rest)
                drawOutline(outline, glint, alpha = 1f - 0.55f * f)
                drawOutline(outline, lowerGlow, alpha = rest)
                clipPath(clip) {
                    drawOutline(outline, innerEdge, alpha = rest, style = Stroke(edgeWidth))
                }
                drawOutline(outline, rim, style = Stroke(rimWidth))
            }
        }
}

/**
 * Draws the blurred wallpaper so that it lines up with the screen: [origin] is where this surface's
 * top left corner is in the window. Draw it inside a clip of the surface's shape.
 */
private fun DrawScope.drawBackdrop(origin: Offset) {
    val current = GlassBackdrop.image ?: return
    val previous = GlassBackdrop.previous
    val mix = GlassBackdrop.mix
    val scaleX = GlassBackdrop.screenWidth / current.width
    val scaleY = GlassBackdrop.screenHeight / current.height
    val fading = previous != null && mix < 1f

    withTransform({
        translate(-origin.x, -origin.y)
        scale(scaleX, scaleY, pivot = Offset.Zero)
    }) {
        if (fading && previous != null) {
            drawImage(
                image = previous,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(previous.width, previous.height),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(previous.width, previous.height),
                alpha = BACKDROP_ALPHA,
                filterQuality = FilterQuality.High,
            )
        }
        drawImage(
            image = current,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(current.width, current.height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(current.width, current.height),
            alpha = (if (fading) mix else 1f) * BACKDROP_ALPHA,
            filterQuality = FilterQuality.High,
        )
    }
}

/** The shape's outline as a path, so it can be used to clip. */
private fun Outline.toPath(): Path = when (this) {
    is Outline.Generic -> path
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Rectangle -> Path().apply { addRect(rect) }
}

/**
 * Glass finish drawn over app artwork: a diagonal sheen and a rim that brightens and thickens with
 * [glow]. The surface it is drawn on should clip to [shape]; half of the rim stroke falls outside.
 */
fun Modifier.glassSheen(shape: Shape, glow: () -> Float): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val thin = 3.dp.toPx()
    val thick = 6.dp.toPx()
    val sheen = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.55f),
            Color.Transparent,
            Color.White.copy(alpha = 0.15f),
        ),
        start = Offset.Zero,
        end = Offset(size.width, size.height),
    )
    val rim = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.90f),
            Color.White.copy(alpha = 0.12f),
            Color.White.copy(alpha = 0.50f),
        ),
        start = Offset.Zero,
        end = Offset(size.width, size.height),
    )
    onDrawBehind {
        val g = glow()
        drawOutline(outline, sheen, alpha = 0.22f + 0.30f * g)
        drawOutline(outline, rim, alpha = 0.70f + 0.30f * g, style = Stroke(thin + (thick - thin) * g))
    }
}

/**
 * Text that looks like frosted glass: a bright, plain fill with no shadow or outline. Earlier
 * versions used a gradient brush and a drop shadow, which drew a dark border around the letters on
 * some TVs. The wallpaper's dark scrim keeps white text readable.
 */
@Composable
fun GlassText(
    text: String,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
) {
    // The style is rebuilt only when one of its inputs changes, not on every recomposition.
    val style = remember(color, fontSize, fontWeight, letterSpacing, textAlign) {
        TextStyle(
            color = color,
            fontFamily = ChipFont,
            fontSize = fontSize,
            fontWeight = fontWeight,
            letterSpacing = letterSpacing,
            textAlign = textAlign,
        )
    }
    BasicText(
        text = text,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
        style = style,
    )
}

/** Round glass button with a label that fades in underneath when focused (like the dock). */
@Composable
fun GlassButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    icon: @Composable (color: Color) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    // Replaced when focus is lost, which clears a press whose key-up went to another element
    // (for example when a dialog opens during a long press).
    var interaction by remember { mutableStateOf(MutableInteractionSource()) }
    val pressed by interaction.collectIsPressedAsState()

    // The animated values are read inside draw/layer lambdas, so animating does not recompose.
    val focus = animateFloatAsState(if (focused) 1f else 0f, tween(180), label = "glassFocus")
    val press = animateFloatAsState(if (pressed && focused) 1f else 0f, tween(100), label = "glassPress")
    val iconColor = if (focused) Color(0xFF111114) else Color.White

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    val s = 1f + 0.12f * focus.value - 0.17f * press.value
                    scaleX = s
                    scaleY = s
                }
                .liquidGlass(CircleShape, focus = { focus.value })
                .onFocusChanged {
                    if (focused && !it.isFocused) interaction = MutableInteractionSource()
                    focused = it.isFocused
                }
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            icon(iconColor)
        }
        Spacer(Modifier.height(10.dp))
        GlassText(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { alpha = focus.value },
        )
    }
}

/** Round glass button without a label, for compact controls such as the music player. */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    icon: @Composable (color: Color) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    // Replaced when focus is lost, which clears a press whose key-up went to another element
    // (for example when a dialog opens during a long press).
    var interaction by remember { mutableStateOf(MutableInteractionSource()) }
    val pressed by interaction.collectIsPressedAsState()
    val focus = animateFloatAsState(if (focused) 1f else 0f, tween(180), label = "iconFocus")
    val press = animateFloatAsState(if (pressed && focused) 1f else 0f, tween(100), label = "iconPress")

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val s = 1f + 0.14f * focus.value - 0.19f * press.value
                scaleX = s
                scaleY = s
            }
            .liquidGlass(CircleShape, focus = { focus.value })
            .onFocusChanged {
                    if (focused && !it.isFocused) interaction = MutableInteractionSource()
                    focused = it.isFocused
                }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        icon(if (focused) Color(0xFF111114) else Color.White)
    }
}

/** Two-by-two rounded squares: "all apps". */
@Composable
fun AppsGlyph(color: Color) {
    Canvas(Modifier.size(26.dp)) {
        val gap = size.width * 0.12f
        val cell = (size.width - gap) / 2f
        val radius = CornerRadius(cell * 0.28f)
        for (column in 0..1) {
            for (row in 0..1) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(column * (cell + gap), row * (cell + gap)),
                    size = Size(cell, cell),
                    cornerRadius = radius,
                )
            }
        }
    }
}

/** A gear: a thick ring with eight teeth. */
@Composable
fun GearGlyph(color: Color) {
    Canvas(Modifier.size(28.dp)) {
        val radius = size.minDimension / 2f
        val ringRadius = radius * 0.58f
        drawCircle(color = color, radius = ringRadius, style = Stroke(width = radius * 0.34f))
        for (i in 0 until 8) {
            rotate(degrees = i * 45f, pivot = center) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(center.x - radius * 0.14f, center.y - radius),
                    size = Size(radius * 0.28f, radius * 0.34f),
                    cornerRadius = CornerRadius(radius * 0.06f),
                )
            }
        }
    }
}
