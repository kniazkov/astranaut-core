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
package com.kniazkov.astranaut.core.algorithms;

import com.kniazkov.astranaut.core.base.Builder;
import com.kniazkov.astranaut.core.base.DiffTree;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.EmptyTree;
import com.kniazkov.astranaut.core.base.Hole;
import com.kniazkov.astranaut.core.base.Insert;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.Pattern;
import com.kniazkov.astranaut.core.base.PatternNode;
import com.kniazkov.astranaut.core.base.SimpleAbstractType;
import com.kniazkov.astranaut.core.base.Tree;
import com.kniazkov.astranaut.core.utils.JsonDeserializer;
import com.kniazkov.astranaut.core.utils.JsonSerializer;
import com.kniazkov.astranaut.core.utils.TreeJsonException;
import com.kniazkov.astranaut.core.utils.TypeResolver;
import com.kniazkov.astranaut.core.utils.UnknownTypes;
import com.kniazkov.json.Json;
import com.kniazkov.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Structural round trips including occurrence tags and executable service nodes.
 * @since 0.0.1
 */
class JsonRoundTripTest {
    /**
     * Common root type name.
     */
    private static final String ROOT = "Root";

    @Test
    void emptyStatesAndRootOperationsRetainContainerKind() {
        for (final Tree before : List.of(EmptyTree.INSTANCE, Tree.createDraft("A"))) {
            for (final Tree after : List.of(EmptyTree.INSTANCE, Tree.createDraft("B"))) {
                final DiffTree difference = new PositionalDiff().compare(before, after);
                final DiffTree restored = (DiffTree) this.copy(difference);
                Assertions.assertTrue(before.deepCompare(restored.getBefore()));
                Assertions.assertTrue(after.deepCompare(restored.getAfter()));
            }
        }
        Assertions.assertTrue(this.copy(EmptyTree.INSTANCE).getRoot().isEmpty());
    }

    @Test
    void twoInsertionsAndTheirOrderSurvive() {
        final Tree before = Tree.createDraft(JsonRoundTripTest.ROOT);
        final Tree after = Tree.createDraft("Root(A,B)");
        final DiffTree original = new PositionalDiff().compare(before, after);
        final DiffTree restored = (DiffTree) this.copy(original);
        Assertions.assertTrue(original.deepCompare(restored));
        Assertions.assertTrue(before.deepCompare(restored.getBefore()));
        Assertions.assertTrue(after.deepCompare(restored.getAfter()));
        Assertions.assertEquals(2, restored.getRoot().orElseThrow().getChildCount());
    }

    @Test
    void ordinaryActionPayloadsAreNeverExecutedDuringReading() {
        final Tree source = new Tree(
            DraftNode.create(
            JsonRoundTripTest.ROOT, "", new Insert(
            DraftNode.create(
            "A"
        )
        )
        )
        );
        final Tree restored = this.copy(source);
        Assertions.assertTrue(source.deepCompare(restored));
        Assertions.assertInstanceOf(Insert.class, restored.getRoot().orElseThrow().getChild(0));
    }

    @Test
    void bothHoleKindsAndAbstractConstraintsSurvive() {
        final SimpleAbstractType constraint = new SimpleAbstractType("Expression");
        final List<Node> holes = List.of(new Hole(4), new Hole(constraint, 7));
        final Node original = PatternNode.snapshot(
            DraftNode.create(
                JsonRoundTripTest.ROOT
            ), holes, List.of(
                Optional.empty(), Optional.empty()
            )
        );
        final String text = new JsonSerializer(new Pattern(original)).serialize();
        final TypeResolver resolver = name -> Optional.of(constraint)
            .filter(type -> type.getName().equals(name)).map(type -> type);
        final Tree restored = new JsonDeserializer(text, resolver).convert();
        Assertions.assertInstanceOf(Pattern.class, restored);
        Assertions.assertEquals(original, restored.getRoot().orElseThrow());
        final Hole typed = (Hole) restored.getRoot().orElseThrow().getChild(1);
        Assertions.assertSame(constraint, typed.getConstraint().orElseThrow());
        Assertions.assertEquals(7, typed.getNumber());
        Assertions.assertThrows(
            TreeJsonException.class,
            () -> new JsonDeserializer(
                text, name -> Optional.empty(), UnknownTypes.REJECT
            ).convert()
        );
    }

    @Test
    void optionalRightSlotAndSchemaFreeAdapterKeepTheirBindings() {
        final Builder builder = OptionalPair.TYPE.createBuilder();
        Assertions.assertTrue(builder.setChild("right", DraftNode.create("Item")));
        final Node original = builder.build();
        final TypeResolver resolver = name -> Optional.of(OptionalPair.TYPE)
            .filter(type -> type.getName().equals(name)).map(type -> type);
        final Tree restored = new JsonDeserializer(
            new JsonSerializer(new Tree(original)).serialize(), resolver
        ).convert();
        Assertions.assertEquals(original, restored.getRoot().orElseThrow());
        Assertions.assertTrue(restored.getRoot().orElseThrow().getChild("left").isEmpty());
        final Node adapter = new GuardedNode(original, nodes -> true);
        final Tree adapted = new JsonDeserializer(
            new JsonSerializer(new Tree(adapter)).serialize(),
            name -> Optional.of(adapter.getType()).filter(type -> type.getName().equals(name))
                .map(type -> type)
        ).convert();
        Assertions.assertEquals(adapter, adapted.getRoot().orElseThrow());
    }

    @Test
    void mixedEdgesAndRepeatedObjectsKeepOccurrenceTags() {
        final Node shared = DraftNode.create("Leaf");
        final DraftNode.Constructor builder = new DraftNode.Constructor();
        builder.setName("Mixed");
        final List<Node> nodes = List.of(shared, shared, shared);
        final List<Optional<String>> tags = List.of(
            Optional.empty(), Optional.of(
            "left"
        ), Optional.of(
            "right"
        )
        );
        Assertions.assertTrue(builder.setChildren(nodes, tags));
        final Node original = builder.build();
        final Node restored = this.copy(new Tree(original)).getRoot().orElseThrow();
        Assertions.assertEquals(original, restored);
        Assertions.assertEquals(
            tags, java.util.stream.IntStream.range(0, restored.getChildCount())
            .mapToObj(restored::getChildTag).toList()
        );
        Assertions.assertInstanceOf(DraftNode.class, restored);
        final Node scaffold = PatternNode.snapshot(DraftNode.create("Mixed"), nodes, tags);
        final Node adapter = new GuardedNode(scaffold, values -> values.size() == 3);
        final Tree adapted = new JsonDeserializer(
            new JsonSerializer(new Tree(adapter)).serialize(),
            name -> Optional.of(adapter.getType()).filter(type -> type.getName().equals(name))
                .map(type -> type)
        ).convert();
        Assertions.assertEquals(adapter, adapted.getRoot().orElseThrow());
        final DiffTree difference = new PositionalDiff().compare(
            new Tree(original), new Tree(DraftNode.create("Changed"))
        );
        Assertions.assertEquals(
            original,
            ((DiffTree) this.copy(difference)).getBefore().getRoot().orElseThrow()
        );
    }

    @Test
    void duplicateAndInvalidTagsAreRejected() throws Exception {
        final DraftNode.Constructor builder = new DraftNode.Constructor();
        builder.setName(JsonRoundTripTest.ROOT);
        builder.setChild("left", DraftNode.create("A"));
        builder.setChild("right", DraftNode.create("B"));
        final String text = new JsonSerializer(new Tree(builder.build())).serialize();
        final JsonObject document = Json.parse(text).objectValueOrThrow();
        final JsonObject root = document.get(
            "nodes"
        ).arrayValueOrThrow().getLast().objectValueOrThrow();
        root.get(
            "children"
        ).arrayValueOrThrow().getLast().objectValueOrThrow().addString(
            "tag", "left"
        );
        Assertions.assertThrows(
            TreeJsonException.class,
            () -> new JsonDeserializer(document.toString(), name -> Optional.empty()).convert()
        );
        root.get(
            "children"
        ).arrayValueOrThrow().getLast().objectValueOrThrow().addNumber(
            "tag", 2
        );
        Assertions.assertThrows(
            TreeJsonException.class,
            () -> new JsonDeserializer(document.toString(), name -> Optional.empty()).convert()
        );
    }

    @Test
    void concreteTagAndBuilderRefusalsAreControlled() throws Exception {
        final Builder builder = OptionalPair.TYPE.createBuilder();
        Assertions.assertTrue(builder.setChild("right", DraftNode.create("Item")));
        final Node original = builder.build();
        final String text = new JsonSerializer(new Tree(original)).serialize();
        final JsonObject document = Json.parse(text).objectValueOrThrow();
        document.get("nodes").arrayValueOrThrow().getLast().objectValueOrThrow()
            .get("children").arrayValueOrThrow().getFirst().objectValueOrThrow()
            .addString("tag", "unknown");
        final TypeResolver resolver = name -> Optional.of(OptionalPair.TYPE)
            .filter(type -> type.getName().equals(name)).map(type -> type);
        Assertions.assertThrows(
            TreeJsonException.class,
            () -> new JsonDeserializer(document.toString(), resolver).convert()
        );
        final Node guarded = new GuardedNode(original, values -> false);
        Assertions.assertThrows(
            TreeJsonException.class,
            () -> new JsonDeserializer(
                text,
                name -> Optional.of(guarded.getType()).filter(type -> type.getName().equals(name))
                    .map(type -> type)
            ).convert()
        );
    }

    @Test
    @Timeout(30)
    void deepAndWideUnicodeTreesAvoidRecursiveJsonNesting() {
        Node root = DraftNode.create("Text", "Кириллица 😀\n\t\"\\[]{}");
        for (int depth = 0; depth < 20_000; depth += 1) {
            root = DraftNode.create("Branch", "", root);
        }
        final Tree deep = new Tree(root);
        Assertions.assertTrue(deep.deepCompare(this.copy(deep)));
        final List<Node> nodes = new ArrayList<>(20_000);
        for (int index = 0; index < 20_000; index += 1) {
            nodes.add(DraftNode.create("Leaf", Integer.toString(index)));
        }
        final Tree wide = new Tree(
            DraftNode.create(
            JsonRoundTripTest.ROOT, "", nodes.toArray(
            Node[]::new
        )
        )
        );
        Assertions.assertTrue(wide.deepCompare(this.copy(wide)));
    }

    /**
     * Uses the default draft fallback without a provider.
     * @param tree Source
     * @return Reconstructed structure
     * @checkstyle NonStaticMethodCheck (6 lines)
     */
    private Tree copy(final Tree tree) {
        return new JsonDeserializer(
            new JsonSerializer(tree).serialize(), name -> Optional.empty()
        ).convert();
    }
}
