/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Ivan Kniazkov
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
package org.cqfn.astranaut.core.algorithms;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.cqfn.astranaut.core.base.Builder;
import org.cqfn.astranaut.core.base.DiffNode;
import org.cqfn.astranaut.core.base.DiffTree;
import org.cqfn.astranaut.core.base.DraftNode;
import org.cqfn.astranaut.core.base.Hole;
import org.cqfn.astranaut.core.base.Node;
import org.cqfn.astranaut.core.base.Pattern;
import org.cqfn.astranaut.core.base.Replace;
import org.cqfn.astranaut.core.base.Tree;
import org.cqfn.astranaut.core.example.green.Addition;
import org.cqfn.astranaut.core.example.green.ExpressionStatement;
import org.cqfn.astranaut.core.example.green.IntegerLiteral;
import org.cqfn.astranaut.core.example.green.Return;
import org.cqfn.astranaut.core.example.green.SimpleAssignment;
import org.cqfn.astranaut.core.example.green.StatementBlock;
import org.cqfn.astranaut.core.example.green.Variable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Testing {@link PatternBuilder} class.
 * @since 1.1.0
 */
class PatternBuilderTest {
    @Test
    void creatingPatternWithHole() {
        Builder ctor = new Variable.Constructor();
        ctor.setData("a");
        final Node var = ctor.createNode();
        ctor = new IntegerLiteral.Constructor();
        ctor.setData("1");
        final Node second = ctor.createNode();
        ctor = new Addition.Constructor();
        ctor.setChildrenList(Arrays.asList(var, second));
        final Node addition = ctor.createNode();
        ctor = new Variable.Constructor();
        ctor.setData("x");
        final Node variable = ctor.createNode();
        ctor = new SimpleAssignment.Constructor();
        ctor.setChildrenList(Arrays.asList(variable, addition));
        final Node assignment = ctor.createNode();
        ctor = new ExpressionStatement.Constructor();
        ctor.setChildrenList(Collections.singletonList(assignment));
        final Node stmt = ctor.createNode();
        final PatternBuilder builder = new PatternBuilder(new Tree(stmt));
        builder.makeHole(var, 1);
        final Pattern pattern = builder.getPattern();
        Assertions.assertNotNull(pattern);
        final DepthFirstWalker traversal = new DepthFirstWalker(pattern.getRoot());
        final Optional<Node> hole = traversal.findFirst(node -> node instanceof Hole);
        Assertions.assertTrue(hole.isPresent());
        Assertions.assertEquals("#1",  hole.get().getData());
    }

    @Test
    void creatingPatternFromSubtree() {
        Builder ctor = new Variable.Constructor();
        ctor.setData("a");
        final Node var = ctor.createNode();
        ctor = new IntegerLiteral.Constructor();
        ctor.setData("1");
        final Node before = ctor.createNode();
        ctor = new Addition.Constructor();
        ctor.setChildrenList(Arrays.asList(var, before));
        final Node addition = ctor.createNode();
        ctor = new Variable.Constructor();
        ctor.setData("x");
        final Node variable = ctor.createNode();
        ctor = new SimpleAssignment.Constructor();
        ctor.setChildrenList(Arrays.asList(variable, addition));
        final Node assignment = ctor.createNode();
        ctor = new ExpressionStatement.Constructor();
        ctor.setChildrenList(Collections.singletonList(assignment));
        final Node stmt = ctor.createNode();
        ctor = new Return.Constructor();
        final Node ret = ctor.createNode();
        ctor = new StatementBlock.Constructor();
        ctor.setChildrenList(Arrays.asList(stmt, ret));
        final Node block = ctor.createNode();
        final Tree tree = new Tree(block);
        ctor = new IntegerLiteral.Constructor();
        ctor.setData("2");
        final Node after = ctor.createNode();
        final DiffTreeBuilder diffbuilder = new DiffTreeBuilder(tree);
        diffbuilder.replaceNode(before, after);
        final DiffTree diff = diffbuilder.getDiffTree();
        final Set<Node> exclude = new HashSet<>();
        exclude.add(ret);
        final Tree subtree = new Tree(
            new SubtreeBuilder(diff, SubtreeBuilder.EXCLUDE).create(exclude)
        );
        final PatternBuilder ptbuilder = new PatternBuilder(subtree);
        ptbuilder.makeHole(var, 1);
        final Pattern pattern = ptbuilder.getPattern();
        final DepthFirstWalker traversal = new DepthFirstWalker(pattern.getRoot());
        Assertions.assertTrue(traversal.findFirst(node -> node instanceof Replace).isPresent());
        Assertions.assertTrue(traversal.findFirst(node -> node instanceof Hole).isPresent());
    }

    @Test
    void wrongHole() {
        final PatternBuilder builder = new PatternBuilder(
            new DiffTree(
                new DiffNode(
                    DraftNode.create("X")
                )
            )
        );
        final boolean result = builder.makeHole(DraftNode.create("A"), 0);
        Assertions.assertFalse(result);
    }
}
