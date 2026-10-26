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
 * Independent immutable draft snapshots with explicit occurrence tags.
 * @since 0.0.1
 */
class DraftTagsTest {
    @Test
    void completeLayoutsRejectInvalidTagsAtomically() {
        final String tag = "right";
        final DraftNode.Constructor builder = new DraftNode.Constructor();
        builder.setName("Draft");
        final Node shared = DraftNode.create("Leaf");
        final List<Node> children = List.of(shared, shared);
        Assertions.assertTrue(
            builder.setChildren(
            children,
            List.of(Optional.empty(), Optional.of(tag))
        )
        );
        final Node snapshot = builder.build();
        Assertions.assertFalse(builder.setChildren(children, List.of(Optional.of(tag))));
        Assertions.assertFalse(
            builder.setChildren(
            children,
            List.of(Optional.of("same"), Optional.of("same"))
        )
        );
        Assertions.assertEquals(snapshot, builder.build());
        Assertions.assertSame(shared, snapshot.getChild(tag).orElseThrow());
        Assertions.assertTrue(builder.removeChild(tag));
        Assertions.assertFalse(builder.removeChild(tag));
        builder.addChild(DraftNode.create("Other"));
        Assertions.assertTrue(builder.build().getChildTag(1).isEmpty());
        Assertions.assertEquals(Optional.of(tag), snapshot.getChildTag(1));
        Assertions.assertThrows(
            UnsupportedOperationException.class,
            () -> snapshot.getChildrenList().clear()
        );
    }

    @Test
    void namedEditsAndUnnamedReplacementKeepParallelListsAligned() {
        final String tag = "slot";
        final DraftNode.Constructor builder = new DraftNode.Constructor();
        builder.setName("Draft");
        final Node first = DraftNode.create("First");
        final Node second = DraftNode.create("Second");
        Assertions.assertTrue(builder.setChild(tag, first));
        final Node snapshot = builder.build();
        Assertions.assertTrue(builder.setChild(tag, second));
        Assertions.assertEquals(1, builder.build().getChildCount());
        Assertions.assertSame(second, builder.build().getChild(tag).orElseThrow());
        Assertions.assertTrue(builder.setChildren(List.of(first, second)));
        Assertions.assertTrue(builder.build().getChild(tag).isEmpty());
        Assertions.assertSame(first, snapshot.getChild(tag).orElseThrow());
        Assertions.assertTrue(builder.removeChild(0));
        Assertions.assertTrue(builder.build().getChildTag(0).isEmpty());
    }
}
