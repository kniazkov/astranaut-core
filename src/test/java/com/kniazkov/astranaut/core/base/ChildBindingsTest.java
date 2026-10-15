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
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Binding state tests independent of any generated builder.
 * @since 0.0.1
 */
class ChildBindingsTest {

    /**
     * The two child tag.
     */
    private static final String TWO_TAG = "two";

    @Test
    void fullAssignmentRetainsSourceOrderForIndexedEdits() {
        final AbstractType first = new SimpleAbstractType("first");
        final AbstractType second = new SimpleAbstractType("second");
        final ChildBindings state = new ChildBindings(
            ChildDescriptor.create()
                .required(first, "one")
                .required(second, ChildBindingsTest.TWO_TAG)
                .build()
        );
        final Node left = DraftNode.create("first");
        final Node right = DraftNode.create("second");
        Assertions.assertTrue(state.setChildren(List.of(right, left)));
        Assertions.assertFalse(state.setChild(0, left));
        Assertions.assertTrue(state.removeChild(0));
        Assertions.assertEquals(Optional.of(left), state.getChild("one"));
        Assertions.assertTrue(state.getChild(ChildBindingsTest.TWO_TAG).isEmpty());
        Assertions.assertFalse(state.isValid());
    }

    @Test
    void repeatedSlotsKeepOrderAndAllowRemoval() {
        final ChildBindings state = new ChildBindings(
            ChildDescriptor.create().repeated(new SimpleAbstractType("item")).build()
        );
        final Node item = DraftNode.create("item");
        Assertions.assertTrue(state.setChildren(List.of(item, item, item)));
        final List<Node> snapshot = state.getChildren();
        Assertions.assertTrue(state.removeChild(1));
        Assertions.assertTrue(state.isValid());
        Assertions.assertEquals(3, snapshot.size());
        Assertions.assertEquals(2, state.getChildren().size());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> snapshot.add(item));
    }

    @Test
    void ambiguousResetAndNullInputLeaveBindingsIntact() {
        final AbstractType type = new SimpleAbstractType("item");
        final ChildBindings state = new ChildBindings(
            ChildDescriptor.create()
                .optional(type, "one")
                .optional(type, ChildBindingsTest.TWO_TAG)
                .build()
        );
        final Node item = DraftNode.create("item");
        Assertions.assertTrue(state.setChild(ChildBindingsTest.TWO_TAG, item));
        Assertions.assertFalse(state.setChildren(List.of(item)));
        Assertions.assertThrows(
            NullPointerException.class, () -> state.setChild(ChildBindingsTest.TWO_TAG, null)
        );
        Assertions.assertEquals(Optional.of(item), state.getChild(ChildBindingsTest.TWO_TAG));
        Assertions.assertTrue(state.getChild("one").isEmpty());
        Assertions.assertTrue(state.setChild(ChildBindingsTest.TWO_TAG, item));
        Assertions.assertEquals(1, state.getChildren().size());
    }
}
