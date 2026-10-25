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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Immutable provenance as a half-open range [begin, end) in one immutable source.
 * EmptyFragment means absent provenance; an empty range still has a real location.
 * @since 0.0.1
 */
@SuppressWarnings("PMD.ProhibitPublicStaticMethods")
public interface Fragment {
    /**
     * Returns the first position of the fragment.
     * @return The first position.
     */
    Position getBegin();

    /**
     * Returns the exclusive end position of the fragment.
     * @return The exclusive end position.
     */
    Position getEnd();

    /**
     * Returns a string representation of the fragment.
     * @return String representation of the fragment
     */
    default String getCode() {
        return this.getBegin().getSource().getFragmentAsString(this.getBegin(), this.getEnd());
    }

    /**
     * Returns a formatted string representing the position of the fragment.
     * @return A formatted string describing the position of the fragment
     */
    default String getPositionAsString() {
        final String result;
        final Position begin = this.getBegin();
        final Position end = this.getEnd();
        final String path = begin.getSource().getFileName();
        if (path.isEmpty()) {
            result = String.format(
                "%d.%d-%d.%d",
                begin.getRow(),
                begin.getColumn(),
                end.getRow(),
                end.getColumn()
            );
        } else {
            result = String.format(
                "%s, %d.%d-%d.%d",
                path,
                begin.getRow(),
                begin.getColumn(),
                end.getRow(),
                end.getColumn()
            );
        }
        return result;
    }

    /**
     * Creates a fragment from one or more positions.
     *  Returns an empty fragment if none provided, a single-point fragment if one,
     *  or a range fragment if multiple. Regardless of input order, the earliest and
     *  latest positions are used to form a correct fragment.
     * @param positions Positions to build fragment from
     * @return Corresponding fragment instance
     */
    static Fragment fromPositions(final Position... positions) {
        return Fragment.fromPositions(Arrays.asList(positions));
    }

    /**
     * Creates a fragment from one or more positions.
     *  Returns an empty fragment if none provided, a single-point fragment if one,
     *  or a range fragment if multiple. Regardless of input order, the earliest and
     *  latest positions are used to form a correct fragment.
     * @param positions List of positions to build fragment from
     * @return Corresponding fragment instance
     */
    static Fragment fromPositions(final List<Position> positions) {
        final Fragment result;
        if (positions.isEmpty()) {
            result = EmptyFragment.INSTANCE;
        } else if (positions.size() == 1) {
            result = new DefaultFragment(positions.get(0), positions.get(0));
        } else {
            result = new DefaultFragment(Position.bounds(positions));
        }
        return result;
    }

    /**
     * Creates a fragment that spans all given nodes.
     *  Returns an empty fragment if the list is empty, or the fragment of the single node
     *  if there's only one. For multiple nodes, uses the earliest and latest positions
     *  (ignoring nodes with empty fragments) to create a proper range.
     * Include every matched node, including discarded delimiters, in this list.
     * @param nodes List of nodes
     * @return Combined range, or absent provenance when real sources differ
     */
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    static Fragment fromNodes(final List<Node> nodes) {
        final List<Position> positions = new ArrayList<>(nodes.size());
        Source source = null;
        boolean common = true;
        for (final Node node : nodes) {
            final Fragment fragment = node.getFragment();
            if (!fragment.equals(EmptyFragment.INSTANCE)) {
                final Source current = fragment.getBegin().getSource();
                common = common && (source == null || source == current)
                    && current == fragment.getEnd().getSource();
                source = current;
                positions.add(fragment.getBegin());
                positions.add(fragment.getEnd());
            }
        }
        Fragment result = EmptyFragment.INSTANCE;
        if (common) {
            result = Fragment.fromPositions(positions);
        }
        return result;
    }
}
