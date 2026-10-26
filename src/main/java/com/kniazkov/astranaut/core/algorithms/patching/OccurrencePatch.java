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
package com.kniazkov.astranaut.core.algorithms.patching;

import com.kniazkov.astranaut.core.algorithms.OccurrenceReplacer;
import com.kniazkov.astranaut.core.algorithms.OccurrenceWalker;
import com.kniazkov.astranaut.core.algorithms.TemplateSubstitutor;
import com.kniazkov.astranaut.core.algorithms.conversion.Extracted;
import com.kniazkov.astranaut.core.base.ConstructionException;
import com.kniazkov.astranaut.core.base.Fragment;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeOccurrence;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Attempt-local commands keyed by occurrence, never by shared node identity.
 * @since 0.0.1
 */
final class OccurrencePatch {
    /**
     * Subtree replacements, including deletion as an empty sequence.
     */
    private final Map<NodeOccurrence, List<Node>> replacements = new HashMap<>();

    /**
     * Ordered insertions at original child boundaries.
     */
    private final Map<NodeOccurrence, Map<Integer, List<Node>>> insertions = new HashMap<>();

    /**
     * Records a replacement.
     * @param address Occurrence
     * @param nodes Replacement sequence
     */
    void replace(final NodeOccurrence address, final List<Node> nodes) {
        this.replacements.put(address, List.copyOf(nodes));
    }

    /**
     * Records an insertion before an original child index, including EOF.
     * @param parent Parent occurrence
     * @param index Boundary
     * @param node Inserted node
     */
    void insert(final NodeOccurrence parent, final int index, final Node node) {
        this.insertions.computeIfAbsent(parent, key -> new HashMap<>())
            .computeIfAbsent(index, key -> new ArrayList<>(1)).add(node);
    }

    /**
     * Commits successful speculative commands, copying mutable containers.
     * @param other Successful attempt
     */
    void merge(final OccurrencePatch other) {
        this.replacements.putAll(other.replacements);
        other.insertions.forEach(
            (parent, boundaries) -> boundaries.forEach(
                (index, nodes) -> nodes.forEach(node -> this.insert(parent, index, node))
            )
        );
    }

    /**
     * Reconstructs all local edits atomically, pruning replaced and deleted subtrees.
     * @param root Local root
     * @param captures Successful local captures
     * @return Output sequence, or refusal
     */
    Optional<List<Node>> apply(final Node root, final Extracted captures) {
        Optional<List<Node>> result;
        try {
            final NodeOccurrence address = new NodeOccurrence(root);
            final List<NodeOccurrence> order = new ArrayList<>(0);
            new OccurrenceWalker(address).findAll(
                current -> {
                    order.add(current);
                    return this.replacements.containsKey(current);
                }
            );
            final Evaluation state = new Evaluation(
                new HashMap<>(),
                new TemplateSubstitutor(Fragment.fromNodes(List.of(root))), captures
            );
            for (final NodeOccurrence current : order.reversed()) {
                List<Node> output = this.replacements.get(current);
                if (output == null) {
                    output = List.of(this.rebuild(current, state));
                } else {
                    output = state.expand(output);
                }
                state.done().put(current, output);
            }
            result = Optional.of(state.done().get(address));
        } catch (final ConstructionException ignored) {
            result = Optional.empty();
        }
        return result;
    }

    /**
     * Builds a parent with the original unaffected edge bindings.
     * @param address Parent
     * @param state Local state
     * @return Valid parent
     * @throws ConstructionException If the builder refuses
     */
    private Node rebuild(final NodeOccurrence address,
        final Evaluation state) {
        final Node node = address.getNode();
        final Map<Integer, List<Node>> added = this.insertions.getOrDefault(address, Map.of());
        final List<Node> children = new ArrayList<>(node.getChildCount());
        final List<Optional<String>> tags = new ArrayList<>(node.getChildCount());
        for (int index = 0; index <= node.getChildCount(); index += 1) {
            final List<Node> inserted = state.expand(added.getOrDefault(index, List.of()));
            children.addAll(inserted);
            inserted.forEach(child -> tags.add(Optional.empty()));
            if (index < node.getChildCount()) {
                final List<Node> sequence = state.done().get(address.getChild(index));
                children.addAll(sequence);
                final Optional<String> tag = node.getChildTag(index);
                if (tag.isPresent() && sequence.size() > 1) {
                    throw new ConstructionException("Tagged patch hole expanded to multiple nodes");
                }
                sequence.forEach(child -> tags.add(tag));
            }
        }
        Node result = node;
        if (!OccurrencePatch.unchanged(node, children, tags)) {
            result = OccurrenceReplacer.rebuild(
                node, new OccurrenceReplacer.Content(node.getData(), children, tags)
            ).orElseThrow(() -> new ConstructionException("Local patch refused by parent"));
        }
        return result;
    }

    /**
     * Unchanged objects are retained; newly allocated equal objects still keep their provenance.
     * @param node Parent
     * @param children Children
     * @param tags Bindings
     * @return Whether all edges are untouched
     */
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static boolean unchanged(final Node node, final List<Node> children,
        final List<Optional<String>> tags) {
        boolean result = node.getChildCount() == children.size();
        for (int index = 0; result && index < children.size(); index += 1) {
            result = node.getChild(index) == children.get(index)
                && node.getChildTag(index).equals(tags.get(index));
        }
        return result;
    }

    /**
     * Local immutable-template evaluation context.
     * @param done Built occurrences
     * @param substitutor Matched provenance policy
     * @param captures Captured objects
     * @since 0.0.1
     */
    private record Evaluation(Map<NodeOccurrence, List<Node>> done,
        TemplateSubstitutor substitutor, Extracted captures) {
        /**
         * Expands output templates, preserving captured objects.
         * @param templates Output expressions
         * @return Expanded sequence
         * @throws ConstructionException If output construction fails
         */
        private List<Node> expand(final List<Node> templates) {
            final List<Node> result = new ArrayList<>(templates.size());
            for (final Node template : templates) {
                result.addAll(
                    this.substitutor.substituteSequence(template, this.captures)
                        .orElseThrow(() -> new ConstructionException("Patch output refused"))
                );
            }
            return List.copyOf(result);
        }
    }
}
