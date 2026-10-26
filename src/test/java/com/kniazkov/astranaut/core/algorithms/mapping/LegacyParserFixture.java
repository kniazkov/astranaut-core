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

import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.Tree;
import com.kniazkov.json.Json;
import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonObject;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;

/**
 * Reads archived parser fixtures, whose historical format is not the public wire protocol.
 * @since 0.0.1
 */
final class LegacyParserFixture {
    /**
     * Utility class.
     */
    private LegacyParserFixture() { }

    /**
     * Loads an ordinary historical tree iteratively.
     * @param text Archived JSON
     * @return Draft tree
     */
    static Tree read(final String text) {
        final JsonObject document = Assertions.assertDoesNotThrow(() -> Json.parse(text))
            .objectValueOrThrow();
        final JsonObject root = document.get("root").objectValueOrThrow();
        final List<JsonObject> pending = new ArrayList<>(0);
        final Map<JsonObject, Node> done = new IdentityHashMap<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            final JsonObject item = pending.getLast();
            final JsonArray children = item.element(
                "children"
            ).map(
                value -> value.arrayValueOrThrow()
            )
                .orElseGet(JsonArray::new);
            final List<JsonObject> missing = children.stream().map(
                value -> value.objectValueOrThrow()
            )
                .filter(value -> !done.containsKey(value)).toList();
            if (missing.isEmpty()) {
                final Node node = DraftNode.create(
                    item.get("type").stringValueOrThrow(),
                    item.element("data").map(value -> value.stringValueOrThrow()).orElse(""),
                    children.stream().map(value -> done.get(value.objectValueOrThrow()))
                        .toArray(Node[]::new)
                );
                done.put(item, node);
                pending.removeLast();
            } else {
                pending.addAll(missing);
            }
        }
        return new Tree(done.get(root));
    }
}
