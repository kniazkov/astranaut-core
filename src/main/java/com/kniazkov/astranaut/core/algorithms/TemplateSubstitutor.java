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

import com.kniazkov.astranaut.core.algorithms.conversion.Extracted;
import com.kniazkov.astranaut.core.base.Action;
import com.kniazkov.astranaut.core.base.ConstructionException;
import com.kniazkov.astranaut.core.base.Delete;
import com.kniazkov.astranaut.core.base.EmptyTree;
import com.kniazkov.astranaut.core.base.Hole;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.PatternNode;
import com.kniazkov.astranaut.core.base.PatternOperator;
import com.kniazkov.astranaut.core.base.Tree;
import com.kniazkov.astranaut.core.base.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Iterative immutable template expansion. Captured nodes retain identity, type and
 * provenance. Every constructed parent is validated by its own builder; refusal is
 * atomic. This does not implement the tree-wide rewriting strategy of the engine.
 * @since 0.0.1
 */
public final class TemplateSubstitutor {
    /**
     * Expands a root: zero nodes is an empty tree, one is a root, several is refusal.
     * Unexpected exceptions propagate; only construction refusal is caught.
     * @param template Template root
     * @param captures Successful matching captures
     * @return New tree, or refusal
     * @checkstyle NonStaticMethodCheck (30 lines)
     */
    public Optional<Tree> substitute(final Node template, final Extracted captures) {
        Optional<Tree> result = Optional.empty();
        try {
            final List<Node> nodes = TemplateSubstitutor.expand(template, captures.fork());
            if (nodes.isEmpty()) {
                result = Optional.of(EmptyTree.INSTANCE);
            } else if (nodes.size() == 1) {
                result = Optional.of(new Tree(nodes.getFirst()));
            }
        } catch (final ConstructionException ignored) {
            result = Optional.empty();
        }
        return result;
    }

    /**
     * Evaluates bottom-up without consuming the Java call stack.
     * @param template Root
     * @param captures Captures
     * @return Expanded sequence
     */
    private static List<Node> expand(final Node template, final Extracted captures) {
        final Map<Node, List<Node>> done = new IdentityHashMap<>();
        final Deque<Node> pending = new ArrayDeque<>();
        pending.push(template);
        while (!pending.isEmpty()) {
            final Node node = pending.peek();
            if (done.containsKey(node)) {
                pending.pop();
                continue;
            }
            final List<Node> missing = TemplateSubstitutor.dependencies(node).stream()
                .filter(child -> !done.containsKey(child)).toList();
            if (missing.isEmpty()) {
                done.put(node, TemplateSubstitutor.finish(node, captures, done));
                pending.pop();
            } else {
                missing.reversed().forEach(pending::push);
            }
        }
        return done.get(template);
    }

    /**
     * Selects only output dependencies; deleted and before subtrees are not rebuilt.
     * @param node Template node
     * @return Dependencies
     */
    private static List<Node> dependencies(final Node node) {
        final List<Node> result;
        if (node instanceof Hole || node instanceof Delete) {
            result = List.of();
        } else if (node instanceof Action action) {
            result = List.of(action.getAfter());
        } else if (node instanceof PatternOperator operator
            && operator.getKind() == PatternOperator.Kind.WITH_DATA) {
            result = operator.getChild(0).getChildrenList();
        } else {
            result = node.getChildrenList();
        }
        return result;
    }

    /**
     * Splices captures and operations or builds an ordinary parent.
     * @param node Template
     * @param captures Captures
     * @param done Expanded children
     * @return Output sequence
     */
    private static List<Node> finish(final Node node, final Extracted captures,
        final Map<Node, List<Node>> done) {
        final List<Node> result;
        if (node instanceof Hole hole) {
            result = captures.getNodes(hole.getNumber());
        } else if (node instanceof Delete) {
            result = List.of();
        } else if (node instanceof Action action) {
            result = done.get(action.getAfter());
        } else if (node instanceof PatternOperator operator) {
            result = TemplateSubstitutor.expression(operator, captures, done);
        } else {
            result = List.of(TemplateSubstitutor.rebuild(node, node.getData(), done));
        }
        return result;
    }

    /**
     * Expands output sequences or data templates.
     * @param operator Expression
     * @param captures Captures
     * @param done Expanded dependencies
     * @return Output sequence
     * @throws ConstructionException If a matching-only expression is used as output
     */
    private static List<Node> expression(final PatternOperator operator, final Extracted captures,
        final Map<Node, List<Node>> done) {
        final List<Node> result;
        if (operator.getKind() == PatternOperator.Kind.SEQUENCE) {
            result = operator.getChildrenList().stream().flatMap(child -> done.get(child).stream())
                .toList();
        } else if (operator.getKind() == PatternOperator.Kind.WITH_DATA) {
            final StringBuilder text = new StringBuilder();
            final List<Node> expressions = operator.getChildrenList();
            for (final Node child : expressions.subList(1, expressions.size())) {
                if (!(child instanceof PatternOperator item)) {
                    throw new ConstructionException("Expected a data expression");
                }
                text.append(TemplateSubstitutor.data(item, captures));
            }
            result = List.of(
                TemplateSubstitutor.rebuild(operator.getChild(0), text.toString(), done)
            );
        } else {
            throw new ConstructionException("Matching expression cannot be substituted");
        }
        return result;
    }

    /**
     * Reads a data literal or capture.
     * @param item Data expression
     * @param captures Captures
     * @return String
     * @throws ConstructionException If this is not a data expression
     */
    private static String data(final PatternOperator item, final Extracted captures) {
        return switch (item.getKind()) {
            case DATA -> item.getData();
            case DATA_HOLE -> captures.getData(Integer.parseInt(item.getData()));
            default -> throw new ConstructionException("Expected a data literal or hole");
        };
    }

    /**
     * Delegates all children together to the parent builder, preserving occurrence tags.
     * @param template Parent
     * @param data Resolved data
     * @param done Expanded children
     * @return Valid parent
     * @throws ConstructionException If a tag expands ambiguously or the builder refuses
     */
    private static Node rebuild(final Node template, final String data,
        final Map<Node, List<Node>> done) {
        final List<Node> children = new ArrayList<>(template.getChildCount());
        final List<Optional<String>> tags = new ArrayList<>(template.getChildCount());
        for (int index = 0; index < template.getChildCount(); index += 1) {
            final List<Node> sequence = done.get(template.getChild(index));
            final Optional<String> tag = template.getChildTag(index);
            if (tag.isPresent() && sequence.size() > 1) {
                throw new ConstructionException("A tagged position cannot expand to several nodes");
            }
            children.addAll(sequence);
            sequence.forEach(child -> tags.add(tag));
        }
        Type type = template.getType();
        if (template instanceof PatternNode pattern) {
            type = pattern.getTargetType();
        }
        return OccurrenceReplacer.rebuild(
            template, new OccurrenceReplacer.Content(data, children, tags), type
        ).orElseThrow(() -> new ConstructionException("Template children refused by builder"));
    }
}
