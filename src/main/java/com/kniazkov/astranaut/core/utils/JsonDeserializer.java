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

import com.kniazkov.astranaut.core.base.ConstructionException;
import com.kniazkov.astranaut.core.base.DiffTree;
import com.kniazkov.astranaut.core.base.EmptyTree;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.Pattern;
import com.kniazkov.astranaut.core.base.Tree;
import com.kniazkov.astranaut.core.utils.json.Fields;
import com.kniazkov.astranaut.core.utils.json.JsonInput;
import com.kniazkov.astranaut.core.utils.json.NodeReader;
import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonObject;
import java.util.List;
import java.util.Objects;

/**
 * Strict structural reconstruction through builders. Metadata is intentionally ignored.
 * Unknown types use DRAFT unless a caller explicitly chooses another policy.
 * @since 0.0.1
 */
public final class JsonDeserializer {
    /**
     * Wire input.
     */
    private final String source;

    /**
     * Configured structural reader.
     */
    private final NodeReader reader;

    /**
     * Uses explicit type resolution and the documented draft fallback.
     * @param source JSON text
     * @param resolver Known types
     */
    public JsonDeserializer(final String source, final TypeResolver resolver) {
        this(source, resolver, UnknownTypes.DRAFT);
    }

    /**
     * Configures an explicit unknown-type handler.
     * @param source JSON text
     * @param resolver Known types
     * @param unknown Fallback policy
     */
    public JsonDeserializer(final String source, final TypeResolver resolver,
        final UnknownTypeHandler unknown) {
        this.source = Objects.requireNonNull(source);
        this.reader = new NodeReader(resolver, unknown);
    }

    /**
     * Reads structure only; malformed input is never replaced with an empty tree.
     * @return Reconstructed tree
     * @throws TreeJsonException If the wire format or requested structure is invalid
     */
    public Tree convert() {
        final JsonObject document = JsonInput.parse(this.source);
        if (Fields.index(Fields.required(document, "version"), 2) != 1) {
            throw new TreeJsonException("Unsupported tree JSON version");
        }
        final String kind = Fields.string(document, "kind");
        final List<Node> nodes = this.reader.read(Fields.array(document, "nodes"));
        final JsonElement root = Fields.required(document, "root");
        final Tree result;
        if (root.isNull()) {
            if (!nodes.isEmpty() || "pattern".equals(kind)) {
                throw new TreeJsonException("Invalid empty tree");
            }
            result = JsonDeserializer.empty(kind);
        } else {
            final Node node = nodes.get(Fields.index(root, nodes.size()));
            result = JsonDeserializer.populated(kind, node);
        }
        JsonDeserializer.validate(result);
        return result;
    }

    /**
     * Restores a nonempty container without interpreting payload nodes.
     * @param kind Container kind
     * @param node Root
     * @return Container
     * @throws TreeJsonException If the kind is invalid
     */
    private static Tree populated(final String kind, final Node node) {
        return switch (kind) {
            case "tree" -> new Tree(node);
            case "diff" -> new DiffTree(node);
            case "pattern" -> new Pattern(node);
            default -> throw new TreeJsonException("Unknown tree kind: ".concat(kind));
        };
    }

    /**
     * Ensures a declared difference has valid endpoints before publication.
     * @param tree Reconstructed container
     * @throws TreeJsonException If either projection refuses
     */
    private static void validate(final Tree tree) {
        if (tree instanceof DiffTree difference) {
            try {
                difference.getBefore();
                difference.getAfter();
            } catch (final ConstructionException failure) {
                throw new TreeJsonException(
                    "Invalid difference: ".concat(failure.getMessage()), failure
                );
            }
        }
    }

    /**
     * Restores explicit empty state without manufacturing a node.
     * @param kind Container kind
     * @return Empty tree
     * @throws TreeJsonException If the container kind is invalid
     */
    private static Tree empty(final String kind) {
        return switch (kind) {
            case "tree" -> EmptyTree.INSTANCE;
            case "diff" -> new DiffTree();
            default -> throw new TreeJsonException("Unknown empty tree kind: ".concat(kind));
        };
    }
}
