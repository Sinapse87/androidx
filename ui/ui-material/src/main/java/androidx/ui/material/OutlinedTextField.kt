/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// TODO(b/160821157): Replace FocusDetailedState with FocusState2 DEPRECATION
@file:Suppress("DEPRECATION")

package androidx.ui.material

import androidx.compose.Composable
import androidx.compose.Stable
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.remember
import androidx.compose.setValue
import androidx.compose.state
import androidx.ui.core.Alignment
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.core.LayoutDirection
import androidx.ui.core.Modifier
import androidx.ui.core.Placeable
import androidx.ui.core.drawBehind
import androidx.ui.core.focus.FocusModifier
import androidx.ui.core.focus.FocusState
import androidx.ui.core.focus.focusState
import androidx.ui.core.id
import androidx.ui.core.layoutId
import androidx.ui.core.offset
import androidx.compose.foundation.Box
import androidx.compose.foundation.Text
import androidx.compose.foundation.currentTextStyle
import androidx.ui.graphics.Color
import androidx.ui.graphics.Path
import androidx.ui.graphics.RectangleShape
import androidx.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.SoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.constrain
import androidx.ui.unit.Dp
import androidx.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Material Design implementation of an
 * [Outlined TextField](https://material.io/components/text-fields/#outlined-text-field)
 *
 * See example usage:
 * @sample androidx.ui.material.samples.SimpleOutlinedTextFieldSample
 *
 * If apart from input text change you also want to observe the cursor location, selection range,
 * or IME composition use the OutlinedTextField overload with the [TextFieldValue] parameter
 * instead.
 *
 * @param value the input text to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates the text. An
 * updated text comes as a parameter of the callback
 * @param label the label to be displayed inside the text field container. The default text style
 * for internal [Text] is [Typography.caption] when the text field is in focus and
 * [Typography.subtitle1] when the text field is not in focus
 * @param modifier a [Modifier] for this text field
 * @param textStyle the style to be applied to the input text. The default [textStyle] uses the
 * [currentTextStyle] defined by the theme
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 * the input text is empty. The default text style for internal [Text] is [Typography.subtitle1]
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 * container
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 * container
 * @param isErrorValue indicates if the text field's current value is in error. If set to true, the
 * label, bottom indicator and trailing icon will be displayed in [errorColor] color
 * @param visualTransformation transforms the visual representation of the input [value].
 * For example, you can use [androidx.compose.ui.text.input.PasswordVisualTransformation] to create a password
 * text field. By default no visual transformation is applied
 * @param keyboardType the keyboard type to be used with the text field.
 * Note that the input type is not guaranteed. For example, an IME may send a non-ASCII character
 * even if you set the keyboard type to [KeyboardType.Ascii]
 * @param imeAction the IME action honored by the IME. The 'enter' key on the soft keyboard input
 * will show a corresponding icon. For example, search icon may be shown if [ImeAction.Search] is
 * selected. When a user taps on that 'enter' key, the [onImeActionPerformed] callback is called
 * with the specified [ImeAction]
 * @param onImeActionPerformed is triggered when the input service performs an [ImeAction].
 * Note that the emitted IME action may be different from what you specified through the
 * [imeAction] field. The callback also exposes a [SoftwareKeyboardController] instance as a
 * parameter that can be used to request to hide the software keyboard
 * @param onFocusChanged a callback to be invoked when the text field receives or loses focus
 * If the boolean parameter value is `true`, it means the text field has focus, and vice versa
 * @param onTextInputStarted a callback to be invoked when the connection with the platform's text
 * input service (e.g. software keyboard on Android) has been established. Called with the
 * [SoftwareKeyboardController] instance that can be used to request to show or hide the software
 * keyboard
 * @param activeColor the color of the label, bottom indicator and the cursor when the text field is
 * in focus
 * @param inactiveColor the color of either the input text or placeholder when the text field is in
 * focus, and the color of the label and bottom indicator when the text field is not in focus
 * @param errorColor the alternative color of the label, bottom indicator, cursor and trailing icon
 * used when [isErrorValue] is set to true
 */
@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = currentTextStyle(),
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isErrorValue: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Unspecified,
    onImeActionPerformed: (ImeAction, SoftwareKeyboardController?) -> Unit = { _, _ -> },
    onFocusChanged: (Boolean) -> Unit = {},
    onTextInputStarted: (SoftwareKeyboardController) -> Unit = {},
    activeColor: Color = MaterialTheme.colors.primary,
    inactiveColor: Color = MaterialTheme.colors.onSurface,
    errorColor: Color = MaterialTheme.colors.error
) {
    var textFieldValue by state { TextFieldValue() }
    if (textFieldValue.text != value) {
        @OptIn(InternalTextApi::class)
        textFieldValue = TextFieldValue(
            text = value,
            selection = textFieldValue.selection.constrain(0, value.length)
        )
    }

    TextFieldImpl(
        type = TextFieldType.Outlined,
        value = textFieldValue,
        onValueChange = {
            val previousValue = textFieldValue.text
            textFieldValue = it
            if (previousValue != it.text) {
                onValueChange(it.text)
            }
        },
        modifier = modifier,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leading = leadingIcon,
        trailing = trailingIcon,
        isErrorValue = isErrorValue,
        visualTransformation = visualTransformation,
        keyboardType = keyboardType,
        imeAction = imeAction,
        onImeActionPerformed = onImeActionPerformed,
        onFocusChanged = onFocusChanged,
        onTextInputStarted = onTextInputStarted,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        errorColor = errorColor,
        backgroundColor = Color.Unset,
        shape = RectangleShape
    )
}

/**
 * Material Design implementation of an
 * [Outlined TextField](https://material.io/components/text-fields/#outlined-text-field)
 *
 * See example usage:
 * @sample androidx.ui.material.samples.OutlinedTextFieldSample
 *
 * This overload provides access to the input text, cursor position and selection range and
 * IME composition. If you only want to observe an input text change, use the OutlinedTextField
 * overload with the [String] parameter instead.
 *
 * @param value the input [TextFieldValue] to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates values in
 * [TextFieldValue]. An updated [TextFieldValue] comes as a parameter of the callback
 * @param label the label to be displayed inside the text field container. The default text style
 * for internal [Text] is [Typography.caption] when the text field is in focus and
 * [Typography.subtitle1] when the text field is not in focus
 * @param modifier a [Modifier] for this text field
 * @param textStyle the style to be applied to the input text. The default [textStyle] uses the
 * [currentTextStyle] defined by the theme
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 * the input text is empty. The default text style for internal [Text] is [Typography.subtitle1]
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 * container
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 * container
 * @param isErrorValue indicates if the text field's current value is in error state. If set to
 * true, the label, bottom indicator and trailing icon will be displayed in [errorColor] color
 * @param visualTransformation transforms the visual representation of the input [value].
 * For example, you can use [androidx.compose.ui.text.input.PasswordVisualTransformation] to create a password
 * text field. By default no visual transformation is applied
 * @param keyboardType the keyboard type to be used with the text field.
 * Note that the input type is not guaranteed. For example, an IME may send a non-ASCII character
 * even if you set the keyboard type to [KeyboardType.Ascii]
 * @param imeAction the IME action honored by the IME. The 'enter' key on the soft keyboard input
 * will show a corresponding icon. For example, search icon may be shown if [ImeAction.Search] is
 * selected. When a user taps on that 'enter' key, the [onImeActionPerformed] callback is called
 * with the specified [ImeAction]
 * @param onImeActionPerformed is triggered when the input service performs an [ImeAction].
 * Note that the emitted IME action may be different from what you specified through the
 * [imeAction] field. The callback also exposes a [SoftwareKeyboardController] instance as a
 * parameter that can be used to request to hide the software keyboard
 * @param onFocusChanged a callback to be invoked when the text field receives or loses focus
 * If the boolean parameter value is `true`, it means the text field has focus, and vice versa
 * @param onTextInputStarted a callback to be invoked when the connection with the platform's text
 * input service (e.g. software keyboard on Android) has been established. Called with the
 * [SoftwareKeyboardController] instance that can be used to request to show or hide the software
 * keyboard
 * @param activeColor the color of the label, bottom indicator and the cursor when the text field is
 * in focus
 * @param inactiveColor the color of either the input text or placeholder when the text field is in
 * focus, and the color of the label and bottom indicator when the text field is not in focus
 * @param errorColor the alternative color of the label, bottom indicator, cursor and trailing icon
 * used when [isErrorValue] is set to true
 */
@Composable
fun OutlinedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = currentTextStyle(),
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isErrorValue: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Unspecified,
    onImeActionPerformed: (ImeAction, SoftwareKeyboardController?) -> Unit = { _, _ -> },
    onFocusChanged: (Boolean) -> Unit = {},
    onTextInputStarted: (SoftwareKeyboardController) -> Unit = {},
    activeColor: Color = MaterialTheme.colors.primary,
    inactiveColor: Color = MaterialTheme.colors.onSurface,
    errorColor: Color = MaterialTheme.colors.error
) {
    TextFieldImpl(
        type = TextFieldType.Outlined,
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leading = leadingIcon,
        trailing = trailingIcon,
        isErrorValue = isErrorValue,
        visualTransformation = visualTransformation,
        keyboardType = keyboardType,
        imeAction = imeAction,
        onImeActionPerformed = onImeActionPerformed,
        onFocusChanged = onFocusChanged,
        onTextInputStarted = onTextInputStarted,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        errorColor = errorColor,
        backgroundColor = Color.Unset,
        shape = RectangleShape
    )
}

// TODO(b/161297615): Replace the deprecated FocusModifier with the new Focus API.
@Suppress("DEPRECATION")
@Composable
internal fun OutlinedTextFieldLayout(
    textFieldModifier: Modifier = Modifier,
    decoratedTextField: @Composable (Modifier) -> Unit,
    decoratedPlaceholder: @Composable (() -> Unit)?,
    decoratedLabel: @Composable () -> Unit,
    leading: @Composable (() -> Unit)?,
    trailing: @Composable (() -> Unit)?,
    leadingColor: Color,
    trailingColor: Color,
    labelProgress: Float,
    indicatorWidth: Dp,
    indicatorColor: Color,
    focusModifier: FocusModifier,
    emptyInput: Boolean
) {
    val outlinedBorderParams = remember {
        OutlinedBorderParams(
            indicatorWidth,
            indicatorColor
        )
    }
    if (indicatorColor != outlinedBorderParams.color.value ||
        indicatorWidth != outlinedBorderParams.borderWidth.value
    ) {
        outlinedBorderParams.color.value = indicatorColor
        outlinedBorderParams.borderWidth.value = indicatorWidth
    }

    // places leading icon, input field, label, placeholder, trailing icon
    IconsWithTextFieldLayout(
        modifier = textFieldModifier.drawOutlinedBorder(outlinedBorderParams),
        textField = decoratedTextField,
        leading = leading,
        trailing = trailing,
        leadingColor = leadingColor,
        trailingColor = trailingColor,
        onLabelMeasured = {
            val newLabelWidth = it * labelProgress

            // TODO(b/160822875): Replace FocusState.Focused with FocusState2.isFocused.
            @Suppress("DEPRECATION")
            val labelWidth = when {
                focusModifier.focusState == FocusState.Focused -> newLabelWidth
                !emptyInput -> newLabelWidth
                focusModifier.focusState == FocusState.NotFocused && emptyInput -> newLabelWidth
                else -> 0f
            }

            if (outlinedBorderParams.labelWidth.value != labelWidth) {
                outlinedBorderParams.labelWidth.value = labelWidth
            }
        },
        animationProgress = labelProgress,
        placeholder = decoratedPlaceholder,
        label = decoratedLabel
    )
}

/**
 * Layout of the leading and trailing icons and the text field, label and placeholder in
 * [OutlinedTextField].
 * It doesn't use Row to position the icons and middle part because label should not be
 * positioned in the middle part.
\ */
@Composable
private fun IconsWithTextFieldLayout(
    modifier: Modifier = Modifier,
    textField: @Composable (Modifier) -> Unit,
    placeholder: @Composable (() -> Unit)?,
    label: @Composable () -> Unit,
    leading: @Composable (() -> Unit)?,
    trailing: @Composable (() -> Unit)?,
    leadingColor: Color,
    trailingColor: Color,
    animationProgress: Float,
    onLabelMeasured: (Int) -> Unit
) {
    Layout(
        children = {
            if (leading != null) {
                Box(Modifier.layoutId("leading").iconPadding(start = HorizontalIconPadding)) {
                    Decoration(
                        contentColor = leadingColor,
                        children = leading
                    )
                }
            }
            if (trailing != null) {
                Box(Modifier.layoutId("trailing").iconPadding(end = HorizontalIconPadding)) {
                    Decoration(
                        contentColor = trailingColor,
                        children = trailing
                    )
                }
            }
            if (placeholder != null) {
                Box(
                    modifier = Modifier
                        .layoutId(PlaceholderId)
                        .padding(horizontal = TextFieldPadding),
                    children = placeholder
                )
            }

            textField(
                Modifier
                    .layoutId(TextFieldId)
                    .padding(horizontal = TextFieldPadding)
            )

            Box(modifier = Modifier.layoutId(LabelId), children = label)
        },
        modifier = modifier
    ) { measurables, incomingConstraints ->
        // used to calculate the constraints for measuring elements that will be placed in a row
        var occupiedSpaceHorizontally = 0
        val bottomPadding = TextFieldPadding.toIntPx()

        // measure leading icon
        val constraints =
            incomingConstraints.copy(minWidth = 0, minHeight = 0)
        val leadingPlaceable = measurables.find { it.id == "leading" }?.measure(constraints)
        occupiedSpaceHorizontally += widthOrZero(
            leadingPlaceable
        )

        // measure trailing icon
        val trailingPlaceable = measurables.find { it.id == "trailing" }
            ?.measure(constraints.offset(horizontal = -occupiedSpaceHorizontally))
        occupiedSpaceHorizontally += widthOrZero(
            trailingPlaceable
        )

        // measure label
        val labelConstraints = constraints.offset(
            horizontal = -occupiedSpaceHorizontally,
            vertical = -bottomPadding
        )
        val labelPlaceable =
            measurables.first { it.id == LabelId }.measure(labelConstraints)
        onLabelMeasured(labelPlaceable.width)

        // measure text field
        // on top we offset either by default padding or by label's half height if its too big
        // minWidth must not be set to 0 due to how foundation TextField treats zero minWidth
        val topPadding = max(labelPlaceable.height / 2, bottomPadding)
        val textContraints = incomingConstraints.offset(
            horizontal = -occupiedSpaceHorizontally,
            vertical = -bottomPadding - topPadding
        ).copy(minHeight = 0)
        val textFieldPlaceable =
            measurables.first { it.id == TextFieldId }.measure(textContraints)

        // measure placeholder
        val placeholderConstraints = textContraints.copy(minWidth = 0)
        val placeholderPlaceable =
            measurables.find { it.id == PlaceholderId }?.measure(placeholderConstraints)

        val width =
            calculateWidth(
                leadingPlaceable,
                trailingPlaceable,
                textFieldPlaceable,
                labelPlaceable,
                placeholderPlaceable,
                incomingConstraints
            )
        val height =
            calculateHeight(
                leadingPlaceable,
                trailingPlaceable,
                textFieldPlaceable,
                labelPlaceable,
                placeholderPlaceable,
                incomingConstraints,
                density
            )
        layout(width, height) {
            place(
                height,
                width,
                leadingPlaceable,
                trailingPlaceable,
                textFieldPlaceable,
                labelPlaceable,
                placeholderPlaceable,
                animationProgress,
                density
            )
        }
    }
}

/**
 * Calculate the width of the [OutlinedTextField] given all elements that should be
 * placed inside
 */
private fun calculateWidth(
    leadingPlaceable: Placeable?,
    trailingPlaceable: Placeable?,
    textFieldPlaceable: Placeable,
    labelPlaceable: Placeable,
    placeholderPlaceable: Placeable?,
    constraints: Constraints
): Int {
    val middleSection = widthOrZero(
        listOf(
            textFieldPlaceable,
            labelPlaceable,
            placeholderPlaceable
        ).maxByOrNull { widthOrZero(it) }
    )
    val wrappedWidth =
        widthOrZero(leadingPlaceable) + middleSection + widthOrZero(
            trailingPlaceable
        )
    return max(wrappedWidth, constraints.minWidth)
}

/**
 * Calculate the height of the [OutlinedTextField] given all elements that should be
 * placed inside
 */
private fun calculateHeight(
    leadingPlaceable: Placeable?,
    trailingPlaceable: Placeable?,
    textFieldPlaceable: Placeable,
    labelPlaceable: Placeable,
    placeholderPlaceable: Placeable?,
    constraints: Constraints,
    density: Float
): Int {
    // middle section is defined as a height of the text field or placeholder ( whichever is
    // taller) plus 16.dp or half height of the label if it is taller, given that the label
    // is vertically centered to the top edge of the resulting text field's container
    val inputFieldHeight = max(
        textFieldPlaceable.height,
        heightOrZero(placeholderPlaceable)
    )
    val topBottomPadding = TextFieldPadding.value * density
    val middleSectionHeight = inputFieldHeight + topBottomPadding + max(
        topBottomPadding,
        labelPlaceable.height / 2f
    )
    return max(
        listOf(
            heightOrZero(leadingPlaceable),
            heightOrZero(trailingPlaceable),
            middleSectionHeight.roundToInt()
        ).maxOrNull() ?: 0,
        constraints.minHeight
    )
}

/**
 * Places the provided text field, placeholder, label, optional leading and trailing icons inside
 * the [OutlinedTextField]
 */
private fun Placeable.PlacementScope.place(
    height: Int,
    width: Int,
    leadingPlaceable: Placeable?,
    trailingPlaceable: Placeable?,
    textFieldPlaceable: Placeable,
    labelPlaceable: Placeable,
    placeholderPlaceable: Placeable?,
    animationProgress: Float,
    density: Float
) {
    // placed center vertically and to the start edge horizontally
    leadingPlaceable?.place(
        0,
        Alignment.CenterVertically.align(height - leadingPlaceable.height)
    )

    // placed center vertically and to the end edge horizontally
    trailingPlaceable?.place(
        width - trailingPlaceable.width,
        Alignment.CenterVertically.align(height - trailingPlaceable.height)
    )

    // if animation progress is 0, the label will be centered vertically
    // if animation progress is 1, vertically it will be centered to the container's top edge
    // horizontally it is placed after the leading icon
    val labelPositionY =
        Alignment.CenterVertically.align(height - labelPlaceable.height) * (1 -
                animationProgress) - (labelPlaceable.height / 2) * animationProgress
    val labelPositionX = (TextFieldPadding.value * density) +
            widthOrZero(leadingPlaceable) * (1 - animationProgress)
    labelPlaceable.place(labelPositionX.roundToInt(), labelPositionY.roundToInt())

    // placed center vertically and after the leading icon horizontally
    textFieldPlaceable.place(
        widthOrZero(leadingPlaceable),
        Alignment.CenterVertically.align(height - textFieldPlaceable.height)
    )

    // placed center vertically and after the leading icon horizontally
    placeholderPlaceable?.place(
        widthOrZero(leadingPlaceable),
        Alignment.CenterVertically.align(height - placeholderPlaceable.height)
    )
}

/**
 * A draw modifier to draw a border line in [OutlinedTextField]
 */
private fun Modifier.drawOutlinedBorder(
    borderParams: OutlinedBorderParams
): Modifier = drawBehind {
    val padding = TextFieldPadding.value * density
    val innerPadding = OutlinedTextFieldInnerPadding.value * density

    val lineWidth = borderParams.borderWidth.value.value * density
    val width: Float = size.width
    val height: Float = size.height

    val radius = borderParams.cornerRadius.value * density
    val dx = if (radius > width / 2) width / 2 else radius
    val dy = if (radius > height / 2) height / 2 else radius

    val path = Path().apply {
        // width and height minus corners and line width
        val effectiveWidth: Float = width - 2 * dx - lineWidth
        val effectiveHeight: Float = height - 2 * dy - lineWidth

        // top-right corner
        moveTo(width - lineWidth / 2, dy + lineWidth / 2)
        relativeQuadraticBezierTo(0f, -dy, -dx, -dy)

        // top line with gap
        val diff = borderParams.labelWidth.value
        if (diff == 0f) {
            relativeLineTo(-effectiveWidth, 0f)
        } else {
            val effectivePadding = padding - innerPadding - dx - lineWidth / 2
            val gap = diff + 2 * innerPadding
            if (layoutDirection == LayoutDirection.Ltr) {
                relativeLineTo(-effectiveWidth + effectivePadding + gap, 0f)
                relativeMoveTo(-gap, 0f)
                relativeLineTo(-effectivePadding, 0f)
            } else {
                relativeLineTo(-effectivePadding, 0f)
                relativeMoveTo(-gap, 0f)
                relativeLineTo(-effectiveWidth + gap + effectivePadding, 0f)
            }
        }

        // top-left corner and left line
        relativeQuadraticBezierTo(-dx, 0f, -dx, dy)
        relativeLineTo(0f, effectiveHeight)

        // bottom-left corner and bottom line
        relativeQuadraticBezierTo(0f, dy, dx, dy)
        relativeLineTo(effectiveWidth, 0f)

        // bottom-right corner and right line
        relativeQuadraticBezierTo(dx, 0f, dx, -dy)
        relativeLineTo(0f, -effectiveHeight)
    }

    drawPath(
        path = path,
        color = borderParams.color.value,
        style = Stroke(width = lineWidth)
    )
}

/**
 * A data class that stores parameters needed for [drawOutlinedBorder] modifier
 */
@Stable
private class OutlinedBorderParams(
    initialBorderWidth: Dp,
    initialColor: Color
) {
    val borderWidth = mutableStateOf(initialBorderWidth)
    val color = mutableStateOf(initialColor)
    val cornerRadius = OutlinedTextFieldCornerRadius
    val labelWidth = mutableStateOf(0f)
}

// TODO(b/158077409) support shape in OutlinedTextField
private val OutlinedTextFieldCornerRadius = 4.dp
private val OutlinedTextFieldInnerPadding = 4.dp