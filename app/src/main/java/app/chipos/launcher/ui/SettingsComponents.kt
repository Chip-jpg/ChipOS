package app.chipos.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.DisposableEffect
import app.chipos.launcher.data.BackState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.chipos.launcher.data.KeyState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/** Text and icon color on a focused (light) glass surface. */
internal val DarkInk = Color(0xFF111114)

/**
 * The large glass sheet every settings page sits on: a title and a scrolling list.
 *
 * [content] receives a FocusRequester; attach it to the first row so the remote works immediately.
 * While [overlayOpen] is true the list cannot take focus (so the remote stays inside the dialog),
 * and when the overlay closes focus returns to the first row.
 */
@Composable
internal fun SettingsSheet(
    title: String,
    overlayOpen: Boolean = false,
    overlay: @Composable () -> Unit = {},
    content: LazyListScope.(first: FocusRequester) -> Unit,
) {
    val first = remember { FocusRequester() }
    val listState = rememberLazyListState()

    val focusManager = LocalFocusManager.current
    LaunchedEffect(overlayOpen) {
        if (!overlayOpen) {
            delay(80)
            val atTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
            try {
                if (atTop) {
                    first.requestFocus()
                } else {
                    // The first row is scrolled out of view (for example after a dialog opened from a
                    // row near the bottom): focus the topmost row that is on screen instead.
                    focusManager.moveFocus(FocusDirection.Down)
                }
            } catch (e: Exception) {
                // Nothing focusable yet; the first D-pad press will pick a target.
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.30f))
            .padding(horizontal = 48.dp, vertical = 27.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(Modifier.widthIn(max = 1100.dp).fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .liquidGlass(SheetShape, restTop = 0.16f, restBottom = 0.05f)
                    .clip(SheetShape)
                    .padding(horizontal = 36.dp, vertical = 28.dp),
            ) {
                GlassText(text = title, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                LazyColumn(
                    state = listState,
                    modifier = Modifier.focusProperties { canFocus = !overlayOpen },
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 20.dp),
                ) {
                    content(this, first)
                }
            }
            if (overlayOpen) overlay()
        }
    }
}

@Composable
internal fun SectionTitle(text: String) {
    GlassText(
        text = text.uppercase(),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = Color.White.copy(alpha = 0.75f),
        modifier = Modifier.padding(start = 8.dp, top = 10.dp),
    )
}

/**
 * One glass row. Use [toggle] for an on/off switch, [value] for text on the right, or [trailing]
 * for custom content. The row fills with light when focused and dips slightly when pressed.
 */
@Composable
internal fun GlassRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    toggle: Boolean? = null,
    trailing: (@Composable (ink: Color) -> Unit)? = null,
    compact: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    // Replaced when focus is lost, which clears a press whose key-up went to another element
    // (for example when a dialog opens during a long press).
    var interaction by remember { mutableStateOf(MutableInteractionSource()) }
    val pressed by interaction.collectIsPressedAsState()

    val focus = animateFloatAsState(if (focused) 1f else 0f, tween(180), label = "rowFocus")
    val press = animateFloatAsState(if (pressed && focused) 1f else 0f, tween(100), label = "rowPress")
    val ink = if (focused) DarkInk else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val s = 1f + 0.015f * focus.value - 0.03f * press.value
                scaleX = s
                scaleY = s
            }
            .liquidGlass(
                ContinuousCornerShape(26.dp),
                restTop = 0.10f,
                restBottom = 0.04f,
                focus = { focus.value },
            )
            .then(modifier)
            .onFocusChanged {
                    if (focused && !it.isFocused) interaction = MutableInteractionSource()
                    focused = it.isFocused
                }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 26.dp, vertical = if (compact) 11.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            GlassText(title, fontSize = if (compact) 19.sp else 21.sp, fontWeight = FontWeight.Bold, color = ink)
            if (subtitle != null) {
                GlassText(
                    text = subtitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ink.copy(alpha = 0.72f),
                    maxLines = 2,
                )
            }
        }
        if (toggle != null) {
            GlassSwitch(on = toggle, focused = focused)
        } else if (trailing != null) {
            trailing(ink)
        } else if (value != null) {
            GlassText(
                text = value,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = ink,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

/** A pill switch: the knob slides across and the track lights up when on. */
@Composable
internal fun GlassSwitch(on: Boolean, focused: Boolean) {
    val position = animateFloatAsState(if (on) 1f else 0f, tween(180), label = "switch")
    val trackColor = if (focused) DarkInk else Color.White

    Box(
        Modifier
            .size(width = 56.dp, height = 32.dp)
            .liquidGlass(CircleShape, restTop = 0.14f, restBottom = 0.06f)
            .background(trackColor.copy(alpha = 0.55f * position.value), CircleShape)
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .offset { IntOffset((24.dp * position.value).roundToPx(), 0) }
                .size(26.dp)
                .background(if (focused && !on) DarkInk else Color.White, CircleShape),
        )
    }
}

/**
 * A glass dialog over a dimmed backdrop. Back closes it. [content] receives a FocusRequester for
 * whichever control should have focus first.
 */
@Composable
internal fun GlassDialog(
    title: String,
    message: String? = null,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(first: FocusRequester) -> Unit,
) {
    BackHandler(onBack = onDismiss)
    // While a dialog is open, Back closes it (not the screen behind it).
    DisposableEffect(Unit) {
        BackState.inner++
        onDispose { BackState.inner-- }
    }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        // If the dialog was opened by holding Select, wait for the button to be released first.
        // Otherwise the still-held key (and its release) would activate the dialog's first row.
        snapshotFlow { KeyState.selectHeld }.first { !it }
        delay(100)
        try {
            first.requestFocus()
        } catch (e: Exception) {
            // Falls back to the first D-pad press.
        }
    }

    BoxWithConstraints(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.50f)),
        contentAlignment = Alignment.Center,
    ) {
        // Tall dialogs (many options) stay inside the screen and scroll with the remote.
        val dialogShape = ContinuousCornerShape(32.dp)
        Column(
            Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth(0.9f)
                .heightIn(max = maxHeight - 48.dp)
                .liquidGlass(dialogShape, restTop = 0.30f, restBottom = 0.16f)
                .clip(dialogShape)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassText(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            if (message != null) {
                GlassText(
                    text = message,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 4,
                )
            }
            content(first)
        }
    }
}

/** A glass text field. The on-screen keyboard opens when it gains focus. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun GlassField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    showKeyboardOnFocus: Boolean = true,
) {
    var focused by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current

    Box(
        Modifier
            .fillMaxWidth()
            .liquidGlass(
                ContinuousCornerShape(22.dp),
                restTop = if (focused) 0.26f else 0.12f,
                restBottom = if (focused) 0.16f else 0.05f,
            ),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color.White,
                fontFamily = ChipFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
            ),
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = modifier
                .fillMaxWidth()
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused && showKeyboardOnFocus) keyboard?.show()
                }
                .onPreviewKeyEvent { event ->
                    // Without auto-open, pressing Select on the field opens the keyboard.
                    val select = event.key == Key.DirectionCenter || event.key == Key.Enter
                    if (!showKeyboardOnFocus && select && event.type == KeyEventType.KeyUp) {
                        keyboard?.show()
                    }
                    false
                },
            decorationBox = { inner ->
                Box(Modifier.padding(horizontal = 22.dp, vertical = 16.dp)) {
                    if (value.isEmpty()) {
                        GlassText(
                            text = hint,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.55f),
                        )
                    }
                    inner()
                }
            },
        )
    }
}
