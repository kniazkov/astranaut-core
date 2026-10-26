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

import com.kniazkov.astranaut.core.example.green.Addition;
import com.kniazkov.astranaut.core.example.green.IntegerLiteral;
import com.kniazkov.astranaut.core.example.green.Variable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The same public contract for allocator-backed and independent schema-free builders.
 * @since 0.0.1
 */
class SharedBuilderContractTest {
    /**
     * Implementations under the same tests.
     */
    private static final List<Type> TYPES = List.of(Addition.TYPE, IndependentPair.TYPE);

    /**
     * First position.
     */
    private static final String LEFT = "left";

    /**
     * Second position.
     */
    private static final String RIGHT = "right";

    @Test
    void incompleteEditsKeepInsertionOrderAcrossValidationAndBuild() {
        for (final Type type : SharedBuilderContractTest.TYPES) {
            this.edit(type.createBuilder());
        }
        Assertions.assertTrue(IndependentPair.TYPE.getChildDescriptors().isEmpty());
    }

    @Test
    void everyRejectedMutationPreservesObservableState() {
        final Node wrong = DraftNode.create("Wrong");
        final List<Predicate<Builder>> rejected = List.of(
            builder -> builder.setData("forbidden"),
            builder -> builder.setChildren(List.of()),
            builder -> builder.setChildren(List.of(wrong, wrong)),
            builder -> builder.setChild(SharedBuilderContractTest.LEFT, wrong),
            builder -> builder.setChild(0, wrong),
            builder -> builder.setChild(-1, wrong),
            builder -> builder.setChild(2, wrong),
            builder -> builder.setChild("unknown", wrong),
            builder -> builder.removeChild(-1),
            builder -> builder.removeChild(2),
            builder -> builder.removeChild("unknown")
        );
        for (final Type type : SharedBuilderContractTest.TYPES) {
            for (final Predicate<Builder> mutation : rejected) {
                this.reject(type.createBuilder(), mutation);
            }
        }
    }

    @Test
    void fullReplacementDropsBindingsAndDoesNotRetainInputList() {
        final Node left = new Variable.Constructor().build();
        final Node right = new IntegerLiteral.Constructor().build();
        for (final Type type : SharedBuilderContractTest.TYPES) {
            final Builder builder = type.createBuilder();
            Assertions.assertTrue(builder.setChild(SharedBuilderContractTest.RIGHT, right));
            final List<Node> input = new ArrayList<>(List.of(left, right));
            Assertions.assertTrue(builder.setChildren(input));
            input.clear();
            final Node before = builder.build();
            Assertions.assertTrue(builder.setChild(0, right));
            Assertions.assertSame(right, builder.build().getChild(0));
            Assertions.assertSame(left, before.getChild(0));
            Assertions.assertTrue(builder.setChildren(List.of(left, left)));
            final Node shared = builder.build();
            this.tags(shared);
            Assertions.assertSame(shared.getChild(0), shared.getChild(1));
        }
    }

    @Test
    void independentlyCreatedBuildersMatchSequentialResults() throws Exception {
        final List<Callable<Node>> tasks = new ArrayList<>(128);
        for (int index = 0; index < 128; index += 1) {
            final Type type = SharedBuilderContractTest.TYPES.get(index % 2);
            tasks.add(() -> this.edit(type.createBuilder()));
        }
        final List<Node> expected = new ArrayList<>(tasks.size());
        for (final Callable<Node> task : tasks) {
            expected.add(task.call());
        }
        try (var executor = Executors.newFixedThreadPool(4)) {
            final var results = executor.invokeAll(tasks);
            for (int index = 0; index < results.size(); index += 1) {
                Assertions.assertEquals(expected.get(index), results.get(index).get());
                this.tags(results.get(index).get());
            }
        }
    }

    /**
     * Exercises temporary invalidity, replacement, removal, and repeated construction.
     * @param builder Fresh implementation
     * @return Stable result
     */
    private Node edit(final Builder builder) {
        final Node left = new Variable.Constructor().build();
        final Node right = new IntegerLiteral.Constructor().build();
        Assertions.assertFalse(builder.isValid());
        Assertions.assertThrows(ConstructionException.class, builder::build);
        Assertions.assertTrue(builder.setChild(SharedBuilderContractTest.RIGHT, left));
        Assertions.assertFalse(builder.setChildren(List.of(left)));
        Assertions.assertFalse(builder.isValid());
        Assertions.assertThrows(ConstructionException.class, builder::build);
        Assertions.assertTrue(builder.setChild(0, right));
        Assertions.assertTrue(builder.setChild(SharedBuilderContractTest.LEFT, left));
        Assertions.assertTrue(builder.isValid());
        final Node before = builder.build();
        Assertions.assertEquals(before, builder.build());
        this.tags(before);
        Assertions.assertTrue(builder.removeChild(SharedBuilderContractTest.RIGHT));
        Assertions.assertFalse(builder.isValid());
        Assertions.assertThrows(ConstructionException.class, builder::build);
        Assertions.assertTrue(builder.setChild(0, right));
        Assertions.assertTrue(builder.setChild(SharedBuilderContractTest.RIGHT, left));
        Assertions.assertTrue(builder.removeChild(1));
        Assertions.assertFalse(builder.isValid());
        Assertions.assertTrue(builder.setChild(SharedBuilderContractTest.RIGHT, right));
        Assertions.assertTrue(builder.setChild(SharedBuilderContractTest.LEFT, left));
        Assertions.assertEquals(before, builder.build());
        Assertions.assertSame(left, before.getChild(0));
        Assertions.assertSame(right, before.getChild(1));
        return before;
    }

    /**
     * Proves one rejected call preserves state, including hidden insertion order.
     * @param builder Fresh implementation
     * @param mutation Expected refusal
     */
    private void reject(final Builder builder, final Predicate<Builder> mutation) {
        final Node left = new Variable.Constructor().build();
        final Node right = new IntegerLiteral.Constructor().build();
        Assertions.assertTrue(builder.setChild(SharedBuilderContractTest.RIGHT, right));
        Assertions.assertTrue(builder.setChild(SharedBuilderContractTest.LEFT, left));
        final Node before = builder.build();
        Assertions.assertFalse(mutation.test(builder));
        Assertions.assertTrue(builder.isValid());
        Assertions.assertEquals(before, builder.build());
        Assertions.assertSame(left, builder.build().getChild(0));
        Assertions.assertSame(right, builder.build().getChild(1));
        Assertions.assertTrue(builder.setChild(0, left));
        Assertions.assertSame(left, builder.build().getChild(1));
        Assertions.assertSame(right, before.getChild(1));
    }

    /**
     * Checks both directions of occurrence tags.
     * @param node Completed pair
     */
    private void tags(final Node node) {
        Assertions.assertEquals(Optional.of(SharedBuilderContractTest.LEFT), node.getChildTag(0));
        Assertions.assertEquals(Optional.of(SharedBuilderContractTest.RIGHT), node.getChildTag(1));
        Assertions.assertSame(
            node.getChild(0), node.getChild(SharedBuilderContractTest.LEFT).orElseThrow()
        );
        Assertions.assertSame(
            node.getChild(1), node.getChild(SharedBuilderContractTest.RIGHT).orElseThrow()
        );
    }
}
