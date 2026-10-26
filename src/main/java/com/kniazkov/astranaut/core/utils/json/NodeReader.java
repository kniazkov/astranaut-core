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
package com.kniazkov.astranaut.core.utils.json;

import com.kniazkov.astranaut.core.algorithms.OccurrenceReplacer;
import com.kniazkov.astranaut.core.base.AbstractType;
import com.kniazkov.astranaut.core.base.Delete;
import com.kniazkov.astranaut.core.base.DiffNode;
import com.kniazkov.astranaut.core.base.Hole;
import com.kniazkov.astranaut.core.base.Insert;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.PatternNode;
import com.kniazkov.astranaut.core.base.Replace;
import com.kniazkov.astranaut.core.base.Type;
import com.kniazkov.astranaut.core.utils.TreeJsonException;
import com.kniazkov.astranaut.core.utils.TypeResolver;
import com.kniazkov.astranaut.core.utils.UnknownTypeHandler;
import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Restores structural records in dependency order through checked builders.
 * @since 0.0.1
 */
public final class NodeReader {
    /**
     * Known concrete and abstract types.
     */
    private final TypeResolver resolver;

    /**
     * Explicit fallback.
     */
    private final UnknownTypeHandler unknown;

    /**
     * Configures type resolution.
     * @param resolver Known types
     * @param unknown Unknown types
     */
    public NodeReader(final TypeResolver resolver, final UnknownTypeHandler unknown) {
        this.resolver = Objects.requireNonNull(resolver);
        this.unknown = Objects.requireNonNull(unknown);
    }

    /**
     * Decodes the node table without recursion.
     * @param table Records
     * @return Completed nodes
     */
    public List<Node> read(final JsonArray table) {
        final List<Node> nodes = new ArrayList<>(table.size());
        for (final JsonElement value : table) {
            nodes.add(this.node(Fields.object(value), nodes));
        }
        return nodes;
    }

    /**
     * Restores one record, checking edge tags and builder results.
     * @param record Record
     * @param previous Available references
     * @return Node
     * @throws TreeJsonException If reconstruction refuses or changes the requested content
     */
    private Node node(final JsonObject record, final List<Node> previous) {
        final String name = Fields.string(record, "type");
        final String kind = Fields.string(record, "kind");
        final OccurrenceReplacer.Content content = NodeReader.content(record, previous);
        final Type type = this.type(record, content.tags());
        final Node result = OccurrenceReplacer.rebuild(type, content)
            .orElseThrow(() -> new TreeJsonException("Builder refused type: ".concat(name)));
        if (!result.getTypeName().equals(name)) {
            throw new TreeJsonException("Type/kind mismatch: ".concat(name));
        }
        Node wrapped = result;
        if ("diff".equals(kind)) {
            wrapped = new DiffNode((PatternNode) result);
        }
        if (!NodeWriter.kind(wrapped).equals(kind)) {
            throw new TreeJsonException("Builder changed node kind");
        }
        return wrapped;
    }

    /**
     * Selects a concrete service or application descriptor.
     * @param record Record
     * @param tags Occurrence tags
     * @return Concrete descriptor
     * @throws TreeJsonException If a kind or concrete target is invalid
     */
    private Type type(final JsonObject record, final List<Optional<String>> tags) {
        final String kind = Fields.string(record, "kind");
        final Type result;
        switch (kind) {
            case "insert":
                result = Insert.TYPE;
                break;
            case "delete":
                result = Delete.TYPE;
                break;
            case "replace":
                result = Replace.TYPE;
                break;
            case "hole":
                result = this.hole(record).getType();
                break;
            case "node":
            case "pattern":
            case "diff":
                result = this.application(record, tags);
                break;
            default:
                throw new TreeJsonException("Unknown node kind: ".concat(kind));
        }
        return result;
    }

    /**
     * Resolves ordinary concrete types or independent scaffold descriptors.
     * @param record Structural record
     * @param tags Occurrence tags
     * @return Concrete descriptor
     * @throws TreeJsonException If the target is abstract
     */
    private Type application(final JsonObject record, final List<Optional<String>> tags) {
        final AbstractType resolved = this.resolve(Fields.string(record, "type"), tags);
        if (!(resolved instanceof Type concrete)) {
            throw new TreeJsonException("Cannot instantiate abstract type");
        }
        Type result = concrete;
        if (!"node".equals(Fields.string(record, "kind"))) {
            result = PatternNode.descriptor(concrete, tags);
        }
        return result;
    }

    /**
     * Resolves an optional typed capture constraint; the builder will parse its slot.
     * @param record Record
     * @return Service descriptor prototype
     */
    private Hole hole(final JsonObject record) {
        Hole result = new Hole(0);
        if (record.containsKey("constraint")) {
            result = new Hole(this.resolve(Fields.string(record, "constraint"), List.of()), 0);
        }
        return result;
    }

    /**
     * Applies the selected fallback only for missing descriptors.
     * @param name Requested name
     * @param tags Occurrence layout
     * @return Matching descriptor
     * @throws TreeJsonException If a resolver violates the name contract
     */
    private AbstractType resolve(final String name, final List<Optional<String>> tags) {
        if (name.isEmpty()) {
            throw new TreeJsonException("Empty type name");
        }
        final AbstractType result = this.resolver.resolve(name)
            .orElseGet(() -> this.unknown.resolve(name, tags));
        if (result == null || !name.equals(result.getName())) {
            throw new TreeJsonException("Resolver returned a different type: ".concat(name));
        }
        return result;
    }

    /**
     * Reads edges with unique tags and references to already completed nodes.
     * @param record Record
     * @param previous Available nodes
     * @return Structural content
     * @throws TreeJsonException If tags repeat or are not strings
     */
    private static OccurrenceReplacer.Content content(final JsonObject record,
        final List<Node> previous) {
        final List<Node> children = new ArrayList<>(0);
        final List<Optional<String>> tags = new ArrayList<>(0);
        final Set<String> unique = new HashSet<>();
        for (final JsonElement value : Fields.array(record, "children")) {
            final JsonObject edge = Fields.object(value);
            children.add(
                previous.get(
                Fields.index(
                Fields.required(
                edge, "node"
            ), previous.size()
            )
            )
            );
            Optional<String> tag = Optional.empty();
            if (edge.containsKey("tag")) {
                final String name = Fields.string(edge, "tag");
                if (!unique.add(name)) {
                    throw new TreeJsonException("Duplicate child tag: ".concat(name));
                }
                tag = Optional.of(name);
            }
            tags.add(tag);
        }
        return new OccurrenceReplacer.Content(Fields.string(record, "data"), children, tags);
    }
}
