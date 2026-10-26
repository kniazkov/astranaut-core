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
import com.kniazkov.astranaut.core.base.ConstructionException;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeOccurrence;
import com.kniazkov.astranaut.core.base.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Rebuilds only the ancestors of one occurrence using their builders.
 * Other occurrences, including shared copies of the addressed node, remain unchanged.
 * A refused reconstruction returns empty; programming errors propagate.
 * @since 0.0.1
 */
public final class OccurrenceReplacer {
    /**
     * Replaces one address in the original immutable root.
     * Data, fragments, child order and tags of ancestors are preserved; derived properties
     * are supplied by their builders. Unsupported builder reconstruction is a refusal.
     * @param occurrence Address in the original root
     * @param replacement New subtree
     * @return New root, or empty if an ancestor cannot be reconstructed correctly
     * @checkstyle NonStaticMethodCheck (45 lines)
     */
    public Optional<Node> replace(final NodeOccurrence occurrence, final Node replacement) {
        Objects.requireNonNull(occurrence);
        Objects.requireNonNull(replacement);
        Optional<Node> result = Optional.of(replacement);
        NodeOccurrence current = occurrence;
        while (result.isPresent() && current.getParent().isPresent()) {
            final NodeOccurrence parent = current.getParent().orElseThrow();
            final List<Node> children = new ArrayList<>(parent.getNode().getChildrenList());
            children.set(current.getIndex(), result.orElseThrow());
            result = OccurrenceReplacer.rebuild(parent.getNode(), children);
            current = parent;
        }
        return result;
    }

    /**
     * Restores an ancestor without accepting reordered or retagged children.
     * @param original Original ancestor
     * @param children Desired compact child sequence
     * @return Rebuilt ancestor or a refusal
     */
    static Optional<Node> rebuild(final Node original, final List<Node> children) {
        return OccurrenceReplacer.rebuild(
            original, new Content(
                original.getData(), children,
                java.util.stream.IntStream.range(0, original.getChildCount())
                    .mapToObj(original::getChildTag).toList()
            )
        );
    }

    /**
     * Rebuilds requested content, refusing reordered or retagged results.
     * @param original Source descriptor and provenance
     * @param content Requested content
     * @return New node or refusal
     */
    static Optional<Node> rebuild(final Node original, final Content content) {
        return OccurrenceReplacer.rebuild(original, content, original.getType());
    }

    /**
     * Restores content through an explicit concrete output descriptor.
     * @param original Source provenance and identity
     * @param content Requested content
     * @param type Concrete output type
     * @return New node or refusal
     */
    static Optional<Node> rebuild(final Node original, final Content content, final Type type) {
        Optional<Node> result = Optional.empty();
        try {
            final Builder builder = type.createBuilder();
            builder.setFragment(original.getFragment());
            if (builder.setData(content.data())
                && OccurrenceReplacer.bind(builder, content)
                && builder.isValid()) {
                final Node built = builder.build();
                if (OccurrenceReplacer.matches(original, built, content)) {
                    result = Optional.of(built);
                }
            }
        } catch (final ConstructionException ignored) {
            result = Optional.empty();
        }
        return result;
    }

    /**
     * Prefers explicit named bindings; otherwise delegates full allocation to the builder.
     * @param builder New builder
     * @param content Desired children and tags
     * @return Whether the builder accepted the bindings
     */
    private static boolean bind(final Builder builder, final Content content) {
        final List<Node> children = content.children();
        boolean accepted = !children.isEmpty();
        for (int index = 0; accepted && index < children.size(); index += 1) {
            final Optional<String> tag = content.tags().get(index);
            accepted = tag.isPresent() && builder.setChild(tag.orElseThrow(), children.get(index));
        }
        if (!accepted) {
            accepted = builder.setChildren(children);
        }
        return accepted;
    }

    /**
     * Checks that reconstruction preserved the requested occurrence edges exactly.
     * Identity is intentional: untouched subtrees must remain shared.
     * @param original Original ancestor
     * @param built Rebuilt ancestor
     * @param content Requested children, data and tags
     * @return Whether the builder preserved the requested structure and metadata
     */
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static boolean matches(final Node original, final Node built,
        final Content content) {
        final List<Node> children = content.children();
        boolean result = original.getType().equals(built.getType())
            && content.data().equals(built.getData())
            && original.getFragment().equals(built.getFragment())
            && children.size() == built.getChildCount();
        for (int index = 0; result && index < children.size(); index += 1) {
            result = built.getChild(index) == children.get(index)
                && content.tags().get(index).equals(built.getChildTag(index));
        }
        return result;
    }

    /**
     * Explicit ordered content shared by replacement and template substitution.
     * @param data Data
     * @param children Child objects
     * @param tags Occurrence tags
     * @since 0.0.1
     */
    record Content(String data, List<Node> children, List<Optional<String>> tags) { }

}
