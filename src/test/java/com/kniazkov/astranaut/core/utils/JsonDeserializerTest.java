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
package com.kniazkov.astranaut.core.utils;

import com.kniazkov.astranaut.core.base.Builder;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.Tree;
import com.kniazkov.astranaut.core.example.LittleTrees;
import com.kniazkov.astranaut.core.example.green.GreenFactory;
import com.kniazkov.astranaut.core.example.green.IntegerLiteral;
import com.kniazkov.json.Json;
import com.kniazkov.json.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Strict wire parsing, concrete construction and explicit unknown-type policy.
 * @since 0.0.1
 */
class JsonDeserializerTest {
    @Test
    void existingFixtureTreesRetainTheirStructures() throws Exception {
        final List<String> files = List.of(
            "tree_containing_insert_action.json", "tree_containing_insert_first_action.json",
            "tree_containing_delete_action.json", "pattern_with_hole.json"
        );
        final List<Tree> expected = List.of(
            LittleTrees.createTreeWithInsertAction(), LittleTrees.createTreeWithInsertFirstAction(),
            LittleTrees.createTreeWithDeleteAction(), LittleTrees.createPatternWithHole()
        );
        for (int index = 0; index < files.size(); index += 1) {
            final String source = Files.readString(
                Path.of(
                "src/test/resources/json", files.get(
                index
            )
            )
            );
            final Tree restored = new JsonDeserializer(
                source, name -> Optional.ofNullable(GreenFactory.INSTANCE.getType(name))
            ).convert();
            Assertions.assertTrue(expected.get(index).deepCompare(restored), files.get(index));
        }
    }

    @Test
    void reconstructsConcreteNodesAndRejectsTheirInvalidData() throws Exception {
        final Builder builder = IntegerLiteral.TYPE.createBuilder();
        Assertions.assertTrue(builder.setData("123"));
        final Tree original = new Tree(builder.build());
        final String text = new JsonSerializer(original).serialize();
        final TypeResolver resolver = name -> Optional.ofNullable(
            GreenFactory.INSTANCE.getType(
            name
        )
        );
        final Tree restored = new JsonDeserializer(text, resolver, UnknownTypes.REJECT).convert();
        Assertions.assertInstanceOf(IntegerLiteral.class, restored.getRoot().orElseThrow());
        Assertions.assertTrue(original.deepCompare(restored));
        final JsonObject document = Json.parse(text).objectValueOrThrow();
        document.get("nodes").arrayValueOrThrow().getFirst().objectValueOrThrow()
            .addString("data", "not an integer");
        Assertions.assertThrows(
            TreeJsonException.class,
            () -> new JsonDeserializer(document.toString(), resolver).convert()
        );
    }

    @Test
    void unknownTypesUseOnlyTheSelectedPolicy() {
        final String text = new JsonSerializer(Tree.createDraft("Unknown")).serialize();
        final TypeResolver absent = name -> Optional.empty();
        Assertions.assertInstanceOf(
            DraftNode.class,
            new JsonDeserializer(text, absent).convert().getRoot().orElseThrow()
        );
        Assertions.assertThrows(
            TreeJsonException.class,
            () -> new JsonDeserializer(text, absent, UnknownTypes.REJECT).convert()
        );
        Assertions.assertThrows(
            TreeJsonException.class,
            () -> new JsonDeserializer(
                text, absent,
                (name, tags) -> DraftNode.create("Other").getType()
            ).convert()
        );
    }

    @Test
    void malformedDocumentsAreNotEmptyTrees() {
        for (final String text : List.of("{}", "null", "[]", "~", "{", "{\"root\": {}}")) {
            Assertions.assertThrows(
                TreeJsonException.class,
                () -> new JsonDeserializer(text, name -> Optional.empty()).convert(), text
            );
        }
    }

    @Test
    void structuralFieldsAreRequiredAndNeverCoerced() throws Exception {
        final String text = new JsonSerializer(Tree.createDraft("Root(A)")).serialize();
        for (final String field : List.of("type", "kind", "data", "children")) {
            final JsonObject document = Json.parse(text).objectValueOrThrow();
            document.get(
                "nodes"
            ).arrayValueOrThrow().getFirst().objectValueOrThrow().remove(
                field
            );
            Assertions.assertThrows(
                TreeJsonException.class,
                () -> new JsonDeserializer(
                    document.toString(), name -> Optional.empty()
                ).convert()
            );
        }
        for (final String index : List.of("-1", "0.5", "999999999999999999999999999999999", "0")) {
            final JsonObject document = Json.parse(text).objectValueOrThrow();
            final JsonObject leaf = document.get("nodes").arrayValueOrThrow().getFirst()
                .objectValueOrThrow();
            leaf.get(
                "children"
            ).arrayValueOrThrow().createObject().addElement(
                "node", Json.parse(
                index
            )
            );
            Assertions.assertThrows(
                TreeJsonException.class,
                () -> new JsonDeserializer(
                    document.toString(), name -> Optional.empty()
                ).convert()
            );
        }
    }

    @Test
    void limitsJsonNestingBeforeCallingTheRecursiveLibrary() {
        final String text = "[".repeat(20_000).concat("0").concat("]".repeat(20_000));
        Assertions.assertThrows(
            TreeJsonException.class,
            () -> new JsonDeserializer(text, name -> Optional.empty()).convert()
        );
    }
}
