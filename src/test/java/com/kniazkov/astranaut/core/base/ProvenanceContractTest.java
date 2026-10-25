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

import com.kniazkov.astranaut.core.utils.parsing.StringSource;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Immutable ranges, distinct source identity and complete matched-span provenance.
 * @since 0.0.1
 */
class ProvenanceContractTest {
    @Test
    void synthesizedNodeCoversDiscardedDelimitersButCapturedChildRetainsItsRange() {
        final StringSource source = new StringSource("(x)");
        final List<Node> matched = new ArrayList<>(source.parseIntoList());
        final Node captured = matched.get(1);
        final DraftNode.Constructor builder = new DraftNode.Constructor();
        builder.setName("Group");
        builder.setChildren(List.of(captured));
        builder.setFragment(Fragment.fromNodes(matched));
        final Node result = builder.build();
        Assertions.assertEquals("(x)", result.getFragment().getCode());
        Assertions.assertSame(captured, result.getChild(0));
        Assertions.assertEquals("x", captured.getFragment().getCode());
        Assertions.assertEquals(source.positionAt(3), result.getFragment().getEnd());
    }

    @Test
    void unrelatedSourcesNeverProduceAnInventedRange() {
        final StringSource first = new StringSource("x");
        final StringSource second = new StringSource("x");
        final Node left = first.iterator().next();
        final Node right = second.iterator().next();
        Assertions.assertEquals(left, right);
        Assertions.assertEquals(EmptyFragment.INSTANCE, Fragment.fromNodes(List.of(left, right)));
        Assertions.assertEquals(
            EmptyFragment.INSTANCE,
            Fragment.fromNodes(List.of(DraftNode.create("Synthetic"), right, left))
        );
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new DefaultFragment(first.positionAt(0), second.positionAt(1))
        );
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> first.positionAt(0).compareTo(second.positionAt(0))
        );
        Assertions.assertNotEquals(first.positionAt(0), second.positionAt(0));
        Assertions.assertEquals(
            "x", Fragment.fromNodes(List.of(left, DraftNode.create("Missing"))).getCode()
        );
        Assertions.assertEquals(
            EmptyFragment.INSTANCE, Fragment.fromNodes(List.of(DraftNode.create("Missing")))
        );
    }

    @Test
    void rangesSnapshotExternalPositionValuesAndDistinguishEmptyFromAbsent() {
        final StringSource source = new StringSource("abc");
        final MutablePosition begin = new MutablePosition(source);
        final MutablePosition end = new MutablePosition(source);
        end.column = 3;
        final Fragment range = new DefaultFragment(begin, end);
        begin.column = 2;
        end.column = 4;
        Assertions.assertEquals("ab", range.getCode());
        Assertions.assertEquals(source.positionAt(0), range.getBegin());
        Assertions.assertEquals(source.positionAt(2), range.getEnd());
        final Fragment empty = new DefaultFragment(source.positionAt(1), source.positionAt(1));
        Assertions.assertEquals("", empty.getCode());
        Assertions.assertNotEquals(EmptyFragment.INSTANCE, empty);
        Assertions.assertNotEquals(empty, EmptyFragment.INSTANCE);
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new DefaultFragment(source.positionAt(2), source.positionAt(1))
        );
    }

    @Test
    void characterBuilderDistinguishesUnsetFromNulAndPublishesImmutableSnapshots() {
        final Char.Constructor builder = new Char.Constructor();
        Assertions.assertFalse(builder.isValid());
        Assertions.assertThrows(ConstructionException.class, builder::build);
        builder.setValue('\0');
        Assertions.assertTrue(builder.isValid());
        final Node nul = builder.build();
        Assertions.assertEquals("\0", nul.getData());
        Assertions.assertFalse(builder.setData(""));
        Assertions.assertEquals("\0", builder.build().getData());
        Assertions.assertTrue(builder.setData("x"));
        Assertions.assertEquals("\0", nul.getData());
        Assertions.assertEquals("x", builder.build().getData());
        Assertions.assertThrows(NullPointerException.class, () -> builder.setFragment(null));
    }

    /**
     * Mutable external coordinates used to verify defensive value snapshots.
     * @since 0.0.1
     */
    private static final class MutablePosition implements Position {
        /**
         * Immutable source snapshot.
         */
        private final Source source;

        /**
         * External mutable column.
         */
        private int column;

        /**
         * Creates coordinates.
         * @param source Immutable source
         */
        private MutablePosition(final Source source) {
            this.source = source;
            this.column = 1;
        }

        @Override
        public Source getSource() {
            return this.source;
        }

        @Override
        public int getRow() {
            return 1;
        }

        @Override
        public int getColumn() {
            return this.column;
        }
    }
}
