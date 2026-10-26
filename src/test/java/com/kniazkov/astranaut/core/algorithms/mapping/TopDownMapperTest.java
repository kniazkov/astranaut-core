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

import com.kniazkov.astranaut.core.algorithms.DiffStrategy;
import com.kniazkov.astranaut.core.algorithms.DiffTreeBuilder;
import com.kniazkov.astranaut.core.base.DefaultFactory;
import com.kniazkov.astranaut.core.base.DiffTree;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.Factory;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.Provider;
import com.kniazkov.astranaut.core.base.Transformer;
import com.kniazkov.astranaut.core.base.Tree;
import com.kniazkov.astranaut.core.utils.JsonDeserializer;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link TopDownMapper} class.
 * @since 0.0.1
 */
@SuppressWarnings("PMD.TooManyMethods")
class TopDownMapperTest {
    /**
     * The folder with test resources.
     */
    private static final String TESTS_PATH = "src/test/resources/heavy/";

    @Test
    void testIdenticalTrees() {
        final String description = "A(B(C, D))";
        final Node first = DraftNode.create(description);
        final Node second = DraftNode.create(description);
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    @Test
    void testPairOfTreesWhereOnlyInsertion() {
        final Node first = DraftNode.create("X()");
        final Node second = DraftNode.create("X(A,B)");
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    @Test
    void testPairOfTreesWhereOnlyDeletion() {
        final Node first = DraftNode.create("X(A,B)");
        final Node second = DraftNode.create("X()");
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    @Test
    void testPairOfTreesWhereOneAndOneInserted() {
        final Node first = DraftNode.create("X(A)");
        final Node second = DraftNode.create("X(A,B)");
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    @Test
    void testPairOfTreesWhereTwoAndOneInserted() {
        final Node first = DraftNode.create("X(A,C)");
        final Node second = DraftNode.create("X(A,B,C)");
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    @Test
    void testNodeInsertedAmongIdenticalNodes() {
        final Node first = DraftNode.create("X(A,A,A,A,C)");
        final Node second = DraftNode.create("X(A,A,A,B,A,C)");
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    @Test
    void testPairOfTreesWhereTwoAndOneDeleted() {
        final Node first = DraftNode.create("X(A,B)");
        final Node second = DraftNode.create("X(A)");
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    @Test
    void testPairOfTreesWhereThreeAndOneReplaced() {
        final Node first = DraftNode.create("X(A,B,C)");
        final Node second = DraftNode.create("X(A,D,C)");
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    @Test
    void testPairOfTreesWhereAllReplaced() {
        final Node first = DraftNode.create("X(A,B,C)");
        final Node second = DraftNode.create("X(D,E,F)");
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    @Test
    void testThreeInsertedAndThreeDeleted() {
        final Node first = DraftNode.create("X(Y,Y,B,B,B,Y)");
        final Node second = DraftNode.create("X(Y,A,A,A,Y,Y)");
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    @Test
    void testPairOfTreesWhereOneAddedAndOneReplaced() {
        final Node first = DraftNode.create("X(A,Y(C,D,E))");
        final Node second = DraftNode.create("X(A,Y(B,C,F,E))");
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    @Test
    void testPairOfTreesWhereOneDeletedAndOneReplaced() {
        final Node first = DraftNode.create("X(A,Y(B,C,D,E))");
        final Node second = DraftNode.create("X(A,Y(C,F,E))");
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    @Test
    void testPairOfTreesWhereTwoAddedAndOneReplaced() {
        final Node first = DraftNode.create("X(A,Y(B,C,D))");
        final Node second = DraftNode.create("X(A,Y(B,E,F,D,F))");
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    @Test
    void testPairOfTreesWhereTwoRemovedAndOneReplaced() {
        final Node first = DraftNode.create("X(A,Y(B,E,F,D,F))");
        final Node second = DraftNode.create("X(A,Y(B,C,D))");
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    @Test
    void testPairOfTreesWhereAllActions() {
        final Node first = DraftNode.create("X(A,B,Y(C,D,E,F,J,K))");
        final Node second = DraftNode.create("X(A,G,Y(H,C,I,E,J,K))");
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    @Test
    void testAddOneAndReplaceOneInDepth() {
        final Node first = DraftNode.create(
            "A(B1,B2,B3,B4(C1,C2(            X(R),D1,D2(E1,E2<'aaa'>,E3)),C3),B5)"
        );
        final Node second = DraftNode.create(
            "A(B1,B2,B3,B4(C1,C2(X(Q1,Q2,Q3),X(R),D1,D2(E1,E2<'bbb'>,E3)),C3),B5)"
        );
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    @Test
    void testsTwoCompletelyDifferentNodes() {
        final Node first = DraftNode.create("A(B,C)");
        final Node second = DraftNode.create("D(E,F)");
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    @Test
    void testInsertedFirstWhileSecondHasActions() {
        final Node first = DraftNode.create("X(A(X),D,E)");
        final Node second = DraftNode.create("X(B,A(Y),D,E)");
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    @Test
    void testDeletedFirstWhileSecondHasActions() {
        final Node first = DraftNode.create("X(B,A(X),D,E)");
        final Node second = DraftNode.create("X(A(Y),D,E)");
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    @Test
    void testSubtreeInsertion() {
        final Node first = DraftNode.create(
            "A(                    X(Y<'2'>),          X(Y<'4'>))"
        );
        final Node second = DraftNode.create(
            "A(X(Y<'0'>),X(Y<'1'>),X(Y<'2'>),X(Y<'3'>),X(Y<'4'>))"
        );
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    @Test
    void firstTestComplexChanges() {
        final Node first = DraftNode.create(
            "A(     X(Y<'1'>,Z,A),X(A,D,C  ))"
        );
        final Node second = DraftNode.create(
            "A(X(Q),X(Y<'2'>,Z,B),X(A,  C,E))"
        );
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    @Test
    void secondTestComplexChanges() {
        final Node first = DraftNode.create(
            "A(В,   В,   B,   B,     X(C,D))"
        );
        final Node second = DraftNode.create(
            "A(X(I),X(H),X(G),X(C,E),X(F)  )"
        );
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    @Test
    void thirdTestComplexChanges() {
        final Node first = DraftNode.create(
            "A(X(B,C),Y(B,C))"
        );
        final Node second = DraftNode.create(
            "A(X(C,D),Y(B<'1'>,C))"
        );
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    @Test
    void firstTestOnRealSyntaxTree() {
        final Tree first = this.readSyntaxTreeFormFile("real_tree_from_java_parser_1.json");
        final Tree second = this.readSyntaxTreeFormFile("real_tree_from_java_parser_2.json");
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTreeBuilder builder = new DiffTreeBuilder(first);
        final boolean building = builder.build(second, mapper);
        Assertions.assertTrue(building);
        final DiffTree diff = builder.getDiffTree();
        final Tree before = diff.getBefore();
        Assertions.assertTrue(before.deepCompare(first));
        final Tree after = diff.getAfter();
        Assertions.assertTrue(after.deepCompare(second));
    }

    @Test
    void testOrderOfInsertions() {
        final Node first = DraftNode.create("A(B,F)");
        final Node second = DraftNode.create("A(B,C,D,E,F)");
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    @Test
    void testOrderOfInsertionsAtBeginning() {
        final Node first = DraftNode.create("A(F)");
        final Node second = DraftNode.create("A(C,D,E,F)");
        final DiffStrategy mapper = TopDownMapper.INSTANCE;
        final DiffTree diff = mapper.compare(new Tree(first), new Tree(second));
        Assertions.assertTrue(new Tree(first).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(second).deepCompare(diff.getAfter()));
    }

    /**
     * Returns content of the specified file.
     * @param name The name of the file
     * @return The file content
     */
    private String getFileContent(final String name) {
        final String file = TopDownMapperTest.TESTS_PATH.concat(name);
        final String source = Assertions.assertDoesNotThrow(() -> Files.readString(Path.of(file)));
        Assertions.assertFalse(source.isEmpty());
        return source;
    }

    /**
     * Reads syntax tree from a JSON file.
     * @param name The name of the file
     * @return Syntax tree
     */
    private Tree readSyntaxTreeFormFile(final String name) {
        final JsonDeserializer deserializer = new JsonDeserializer(
            this.getFileContent(name),
            new Provider() {
                @Override
                public Factory getFactory(final String language) {
                    return DefaultFactory.EMPTY;
                }

                @Override
                public Transformer getTransformer(final String language) {
                    return node -> node;
                }
            }
        );
        final Tree tree = deserializer.convert();
        Assertions.assertEquals("CompilationUnit", tree.getRoot().orElseThrow().getTypeName());
        return tree;
    }
}
