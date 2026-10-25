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
package com.kniazkov.astranaut.core.algorithms.mapping;

import com.kniazkov.astranaut.core.algorithms.ExtNodeCreator;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.ExtNode;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link Section} class.
 * @since 0.0.1
 */
class SectionTest {
    @Test
    void testBaseInterface() {
        final ExtNodeCreator creator = new ExtNodeCreator();
        final ExtNode first = creator.create(DraftNode.create("A(B,C)"));
        final ExtNode second = creator.create(DraftNode.create("A(B,C,D)"));
        final Section section = new Section(first, second);
        final List<ExtNode> left = section.getLeft();
        Assertions.assertSame(left.get(0), first.getExtChild(0));
        final List<ExtNode> right = section.getRight();
        Assertions.assertSame(right.get(0), second.getExtChild(0));
        Assertions.assertTrue(section.hasNode(first.getExtChild(1)));
        Assertions.assertTrue(section.hasNode(second.getExtChild(1)));
        Assertions.assertFalse(section.hasNode(first));
    }

    @Test
    void removeNode() {
        final ExtNodeCreator creator = new ExtNodeCreator();
        final ExtNode left = creator.create(DraftNode.create("A(E,F,Q)"));
        final ExtNode right = creator.create(DraftNode.create("A(G,H,I)"));
        final Section initial = new Section(left, right);
        final Section same = initial.removeNode(left);
        Assertions.assertSame(initial, same);
        initial.setFlag(Section.FLAG_NO_IDENTICAL);
        Section section = initial.removeNode(right.getExtChild(1));
        Assertions.assertFalse(section.hasNode(right.getExtChild(1)));
        Assertions.assertEquals(5, section.getLeft().size() + section.getRight().size());
        section = section.removeNode(left.getExtChild(0));
        Assertions.assertFalse(section.hasNode(left.getExtChild(0)));
        Assertions.assertSame(left.getExtChild(0), section.getPrevious());
        Assertions.assertEquals(4, section.getLeft().size() + section.getRight().size());
        section = section.removeNode(left.getExtChild(2));
        Assertions.assertFalse(section.hasNode(left.getExtChild(2)));
        Assertions.assertSame(left.getExtChild(0), section.getPrevious());
        Assertions.assertEquals(3, section.getLeft().size() + section.getRight().size());
        section = section.removeNode(left.getExtChild(1));
        Assertions.assertFalse(section.hasNode(left.getExtChild(1)));
        Assertions.assertEquals(2, section.getLeft().size() + section.getRight().size());
        section = section.removeNode(right.getExtChild(0));
        Assertions.assertFalse(section.hasNode(right.getExtChild(0)));
        Assertions.assertEquals(1, section.getLeft().size() + section.getRight().size());
        Assertions.assertTrue(section.isFlagSet(Section.FLAG_NO_IDENTICAL));
        section = section.removeNode(right.getExtChild(2));
        Assertions.assertNull(section);
        section = initial.removeNode(right.getExtChild(0));
        section = section.removeNode(right.getExtChild(1));
        section = section.removeNode(right.getExtChild(2));
        Assertions.assertTrue(section.getRight().isEmpty());
        section = section.removeNode(left.getExtChild(0));
        section = section.removeNode(left.getExtChild(1));
        section = section.removeNode(left.getExtChild(2));
        Assertions.assertNull(section);
    }

    @Test
    void removeNodes() {
        final ExtNodeCreator creator = new ExtNodeCreator();
        final ExtNode left = creator.create(DraftNode.create("A(J,K,L)"));
        final ExtNode right = creator.create(DraftNode.create("A(M,N,O)"));
        final Section section = new Section(left, right);
        Section.Division pair = section.removeNodes(left, right);
        Assertions.assertNull(pair.before());
        Assertions.assertEquals(
            6,
            pair.after().getLeft().size() + pair.after().getRight().size()
        );
        section.setFlag(Section.FLAG_NO_IDENTICAL);
        pair = section.removeNodes(left.getExtChild(1), right.getExtChild(1));
        Assertions.assertEquals(
            2,
            pair.before().getLeft().size() + pair.before().getRight().size()
        );
        Assertions.assertTrue(pair.before().isFlagSet(Section.FLAG_NO_IDENTICAL));
        Assertions.assertTrue(pair.after().isFlagSet(Section.FLAG_NO_IDENTICAL));
        Assertions.assertSame(pair.before().getLeft().get(0), left.getExtChild(0));
        Assertions.assertSame(pair.before().getRight().get(0), right.getExtChild(0));
        Assertions.assertEquals(
            2,
            pair.after().getLeft().size() + pair.after().getRight().size()
        );
        Assertions.assertSame(pair.after().getLeft().get(0), left.getExtChild(2));
        Assertions.assertSame(pair.after().getRight().get(0), right.getExtChild(2));
        Assertions.assertSame(left.getExtChild(1), pair.after().getPrevious());
        pair = pair.before().removeNodes(left.getExtChild(0), right.getExtChild(0));
        Assertions.assertNull(pair.before());
        Assertions.assertNull(pair.after());
        pair = section.removeNodes(left.getExtChild(0), right.getExtChild(2));
        Assertions.assertTrue(pair.before().getLeft().isEmpty());
        Assertions.assertEquals(2, pair.before().getRight().size());
        Assertions.assertEquals(2, pair.after().getLeft().size());
        Assertions.assertTrue(pair.after().getRight().isEmpty());
    }
}
