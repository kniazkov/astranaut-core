/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 Ivan Kniazkov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.kniazkov.astranaut.core.base;

import java.util.List;

/**
 * Mutable, independently implemented node builder. Builders need not be thread safe.
 * Individual edits may leave required positions empty. A rejected edit is atomic:
 * data, child order and tags remain unchanged. Building never mutates builder state.
 * @since 0.0.1
 */
public interface Builder {
    /**
     * Associate a new fragment with the node.
     * @param fragment A new fragment
     */
    void setFragment(Fragment fragment);

    /**
     * Associate a new data with the node (in a textual format).
     * @param str Data as a string
     * @return Result of operation, {@code true} if the new data is suitable for this type of node
     */
    boolean setData(String str);

    /**
     * Replaces all children and their bindings. Accepts only a complete, unambiguous
     * allocation, including every required child; data is validated separately.
     * @param list A list of children
     * @return Result of operation, {@code true} if the new children list is suitable
     *  for this type of node
     */
    boolean setChildren(List<Node> list);

    /**
     * Sets a named child, retaining its position if already present.
     * @param tag Child tag
     * @param child Replacement child
     * @return Whether accepted; unknown tags and incompatible children are rejected
     */
    default boolean setChild(final String tag, final Node child) {
        return false;
    }

    /**
     * Replaces a child in the current insertion order, preserving its tag.
     * @param index Current child index, not a schema index
     * @param child Replacement child
     * @return Whether accepted; invalid indexes never append
     */
    default boolean setChild(final int index, final Node child) {
        return false;
    }

    /**
     * Removes a named child. Removing a required child is permitted.
     * @param tag Child tag
     * @return Whether a child was removed
     */
    default boolean removeChild(final String tag) {
        return false;
    }

    /**
     * Removes a child in current insertion order. Subsequent indexes shift.
     * @param index Current child index
     * @return Whether a child was removed
     */
    default boolean removeChild(final int index) {
        return false;
    }

    /**
     * Checks if the Creator is in a valid state,
     * that is, whether it can create a new node based on its internal data.
     * @return Checking result
     */
    boolean isValid();

    /**
     * Creates a valid node detached from mutable builder state.
     * @return A node
     * @throws ConstructionException If the current state cannot produce a valid node
     */
    Node build();
}
