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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Addresses distinguish occurrence identity from structural equality.
 * @since 0.0.1
 */
class NodeOccurrenceTest {
    @Test
    void distinguishesSharingAndEqualObjectsButRecognizesTheSamePath() {
        final Node leaf = DraftNode.create("Leaf");
        final Node root = DraftNode.create("Root", "", leaf, leaf, DraftNode.create("Leaf"));
        final NodeOccurrence address = new NodeOccurrence(root);
        Assertions.assertEquals(address, new NodeOccurrence(root));
        Assertions.assertNotEquals(address, new NodeOccurrence(root.deepClone()));
        Assertions.assertNotEquals(address, leaf);
        final List<NodeOccurrence> distinct = List.of(
            address.getChild(0), address.getChild(1), address.getChild(2)
        );
        Assertions.assertEquals(3, new HashSet<>(distinct).size());
        Assertions.assertEquals(address.getChild(1), new NodeOccurrence(root).getChild(1));
        Assertions.assertEquals(
            address.getChild(1).hashCode(), new NodeOccurrence(root).getChild(1).hashCode()
        );
        Assertions.assertSame(leaf, address.getChild(0).getNode());
        Assertions.assertEquals(0, address.getDepth());
        Assertions.assertEquals(-1, address.getIndex());
        Assertions.assertTrue(address.getParent().isEmpty());
        Assertions.assertTrue(address.getTag().isEmpty());
        Assertions.assertTrue(address.getSibling(0).isEmpty());
        Assertions.assertEquals(address, address.getChild(1).getParent().orElseThrow());
        Assertions.assertEquals(
            address.getChild(0), address.getChild(1).getSibling(0).orElseThrow()
        );
        Assertions.assertTrue(address.getChild(0).getSibling(-1).isEmpty());
        Assertions.assertTrue(address.getChild(0).getSibling(3).isEmpty());
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> address.getChild(3));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> address.getChild(-1));
    }

    @Test
    void readsTagsFromEdgesAndRetainsIndependentAncestorIterators() {
        final Node leaf = DraftNode.create("Leaf");
        final Node root = new StructuralNodeFixture(
            DraftNode.create("Root"), List.of(leaf, leaf),
            List.of(Optional.of("left"), Optional.of("right"))
        );
        final NodeOccurrence address = new NodeOccurrence(root);
        Assertions.assertEquals(Optional.of("left"), address.getChild(0).getTag());
        Assertions.assertEquals(Optional.of("right"), address.getChild(1).getTag());
        final Iterable<Node> ancestors = address.getChild(0).getAncestors();
        final Iterator<Node> first = ancestors.iterator();
        final Iterator<Node> second = ancestors.iterator();
        Assertions.assertSame(root, first.next());
        Assertions.assertSame(root, second.next());
        Assertions.assertFalse(first.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, first::next);
        Assertions.assertFalse(address.getAncestors().iterator().hasNext());
    }

    @Test
    void childListsHonorArraySublistEqualityAndMutationContracts() {
        final Node root = DraftNode.create("Root(A,B,C)");
        final List<Node> children = root.getChildrenList();
        final List<Node> expected = List.of(root.getChild(0), root.getChild(1), root.getChild(2));
        Assertions.assertEquals(expected, children);
        Assertions.assertEquals(children, expected);
        Assertions.assertEquals(expected.hashCode(), children.hashCode());
        Assertions.assertArrayEquals(expected.toArray(), children.toArray(new Node[0]));
        Assertions.assertTrue(children.subList(0, 0).isEmpty());
        Assertions.assertTrue(children.subList(3, 3).isEmpty());
        Assertions.assertThrows(
            IndexOutOfBoundsException.class, () -> children.subList(0, 1).get(1)
        );
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> children.get(-1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> children.get(3));
        final Node[] oversized = {root, root, root, root, root};
        Assertions.assertSame(oversized, children.toArray(oversized));
        Assertions.assertNull(oversized[3]);
        Assertions.assertSame(root, oversized[4]);
        Assertions.assertThrows(
            UnsupportedOperationException.class, () -> children.subList(0, 0).clear()
        );
        Assertions.assertThrows(
            UnsupportedOperationException.class, () -> children.removeIf(child -> false)
        );
    }
}
