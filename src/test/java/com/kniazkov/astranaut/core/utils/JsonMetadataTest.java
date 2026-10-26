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

import com.kniazkov.astranaut.core.base.DefaultFragment;
import com.kniazkov.astranaut.core.base.DefaultPosition;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.EmptyFragment;
import com.kniazkov.astranaut.core.base.Fragment;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.Source;
import com.kniazkov.astranaut.core.base.Tree;
import com.kniazkov.astranaut.core.base.Type;
import com.kniazkov.json.Json;
import com.kniazkov.json.JsonObject;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Extended diagnostics remain asymmetric and never read source contents.
 * @since 0.0.1
 */
class JsonMetadataTest {
    @Test
    void modesHaveIdenticalStructureAndNeverReadSourceText() throws Exception {
        final Source source = (begin, end) -> {
            throw new AssertionError("Source text must never be accessed");
        };
        final Fragment fragment = new DefaultFragment(
            new DefaultPosition(source, 2, 3), new DefaultPosition(source, 4, 5)
        );
        final Node node = new Located(fragment);
        final Tree original = new Tree(node);
        final String compact = new JsonSerializer(original).serialize();
        final JsonSerializer writer = new JsonSerializer(original, JsonSerializer.Mode.EXTENDED);
        final String extended = writer.serialize();
        Assertions.assertEquals(extended, writer.serialize());
        Assertions.assertFalse(compact.contains("metadata"));
        Assertions.assertTrue(extended.contains("readonly-value"));
        final JsonObject document = Json.parse(extended).objectValueOrThrow();
        final JsonObject record = document.get("nodes").arrayValueOrThrow().getFirst()
            .objectValueOrThrow();
        final JsonObject origin = record.get("metadata").objectValueOrThrow().get("origin")
            .objectValueOrThrow();
        Assertions.assertEquals(2, origin.get("begin").objectValueOrThrow().get("row").intValue());
        Assertions.assertFalse(extended.contains("sourceText"));
        record.addString("metadata", "deliberately not an object");
        for (final String text : java.util.List.of(compact, extended, document.toString())) {
            final Tree restored = new JsonDeserializer(text, name -> Optional.empty()).convert();
            Assertions.assertTrue(original.deepCompare(restored));
            Assertions.assertEquals(
                EmptyFragment.INSTANCE, restored.getRoot().orElseThrow().getFragment()
            );
            Assertions.assertTrue(restored.getRoot().orElseThrow().getProperties().isEmpty());
        }
    }

    /**
     * Adapter exposing provenance and readonly properties.
     * @param fragment Source location
     * @since 0.0.1
     */
    private record Located(Fragment fragment) implements Node {
        @Override
        public Type getType() {
            return DraftNode.create("Located").getType();
        }

        @Override
        public String getData() {
            return "Unicode 😀";
        }

        @Override
        public int getChildCount() {
            return 0;
        }

        @Override
        public Node getChild(final int index) {
            return java.util.List.<Node>of().get(index);
        }

        @Override
        public Fragment getFragment() {
            return this.fragment;
        }

        @Override
        public Map<String, String> getProperties() {
            return Map.of("custom", "readonly-value");
        }
    }
}
