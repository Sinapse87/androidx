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

package androidx.compose.ui

import androidx.compose.ui.layout.LayoutCoordinates

/**
 * Invoke [onGloballyPositioned] with the [LayoutCoordinates] of the element when the
 * global position of the content may have changed.
 * Note that it will be called **after** a composition when the coordinates are finalized.
 *
 * Usage example:
 * @sample androidx.compose.ui.samples.onGloballyPositionedSample
 */
// TODO: Inline this.
fun Modifier.onGloballyPositioned(
    onGloballyPositioned: (LayoutCoordinates) -> Unit
) = this.then(object : OnGloballyPositionedModifier {
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        onGloballyPositioned(coordinates)
    }
})

@Deprecated(
    "Use onGloballyPositioned() instead",
    replaceWith = ReplaceWith("this.onGloballyPositioned(onPositioned)")
)
fun Modifier.onPositioned(
    onPositioned: (LayoutCoordinates) -> Unit
) = onGloballyPositioned(onPositioned)

@Deprecated(
    "Use OnGloballyPositionedModifier instead",
    replaceWith = ReplaceWith("onGlobalPositionChangedModifier")
)
interface OnPositionedModifier : OnGloballyPositionedModifier {
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) = onPositioned(coordinates)
    /**
     * Called with the final LayoutCoordinates of the Layout after measuring.
     * Note that it will be called after a composition when the coordinates are finalized.
     * The position in the modifier chain makes no difference in either
     * the [LayoutCoordinates] argument or when the [onPositioned] is called.
     */
    fun onPositioned(coordinates: LayoutCoordinates)
}

/**
 * A modifier whose [onGloballyPositioned] is called with the final LayoutCoordinates of the
 * Layout when the global position of the content may have changed.
 * Note that it will be called after a composition when the coordinates are finalized.
 *
 * Usage example:
 * @sample androidx.compose.ui.samples.onGloballyPositionedSample
 */
interface OnGloballyPositionedModifier : Modifier.Element {
    /**
     * Called with the final LayoutCoordinates of the Layout after measuring.
     * Note that it will be called after a composition when the coordinates are finalized.
     * The position in the modifier chain makes no difference in either
     * the [LayoutCoordinates] argument or when the [onGloballyPositioned] is called.
     */
    fun onGloballyPositioned(coordinates: LayoutCoordinates)
}
