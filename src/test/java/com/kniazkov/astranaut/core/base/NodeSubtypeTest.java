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

import com.kniazkov.astranaut.core.example.LittleTrees;
import com.kniazkov.astranaut.core.example.green.AssignableExpression;
import com.kniazkov.astranaut.core.example.green.Expression;
import com.kniazkov.astranaut.core.example.green.IntegerLiteral;
import com.kniazkov.astranaut.core.example.green.Statement;
import com.kniazkov.astranaut.core.example.green.Variable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Descriptor-based subtype queries directly on nodes.
 * @since 0.0.1
 */
class NodeSubtypeTest {
    @Test
    void recognizesSelfAndTransitiveBases() {
        final Node node = LittleTrees.createVariable("x");
        Assertions.assertTrue(node.isSubtypeOf(Variable.TYPE));
        Assertions.assertTrue(node.isSubtypeOf(AssignableExpression.TYPE));
        Assertions.assertTrue(node.isSubtypeOf(Expression.TYPE));
        Assertions.assertFalse(node.isSubtypeOf(Statement.TYPE));
        Assertions.assertFalse(node.isSubtypeOf(IntegerLiteral.TYPE));
    }

    @Test
    void acceptsEquivalentDescriptorsRegardlessOfNodeData() {
        final Node first = DraftNode.create("Literal", "first");
        final Node second = DraftNode.create("Literal", "second");
        Assertions.assertNotSame(first.getType(), second.getType());
        Assertions.assertTrue(first.isSubtypeOf(second.getType()));
        Assertions.assertTrue(second.isSubtypeOf(first.getType()));
        Assertions.assertFalse(first.isSubtypeOf(DraftNode.create("Other").getType()));
    }

    @Test
    void rejectsNullType() {
        final Node node = LittleTrees.createIntegerLiteral(1);
        Assertions.assertThrows(NullPointerException.class, () -> node.isSubtypeOf(null));
    }
}
