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

import com.kniazkov.astranaut.core.base.ConstructionException;
import com.kniazkov.astranaut.core.base.Delete;
import com.kniazkov.astranaut.core.base.DiffNode;
import com.kniazkov.astranaut.core.base.EmptyTree;
import com.kniazkov.astranaut.core.base.Hole;
import com.kniazkov.astranaut.core.base.Insert;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.PatternNode;
import com.kniazkov.astranaut.core.base.PrototypeBasedNode;
import com.kniazkov.astranaut.core.base.Replace;
import com.kniazkov.astranaut.core.base.Tree;
import com.kniazkov.astranaut.core.base.Type;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Iterative projection of immutable difference scaffolds. Operation payloads are opaque:
 * a selected payload may itself contain actions which are ordinary input data.
 * @since 0.0.1
 */
public final class DiffProjection {
    /**
     * Whether to select the later state.
     */
    private final boolean after;

    /**
     * Chooses a side.
     * @param after Later state when true
     */
    public DiffProjection(final boolean after) {
        this.after = after;
    }

    /**
     * Rebuilds scaffolds through their original target builders, preserving edge tags.
     * Pure subtree projection may be memoized by identity; edge occurrences stay distinct.
     * @param source Difference structure
     * @return Selected state
     * @throws ConstructionException If a projected parent refuses its children
     */
    public Tree project(final Tree source) {
        return source.getRoot().flatMap(this::evaluate).map(Tree::new).orElse(EmptyTree.INSTANCE);
    }

    /**
     * Projects one root with an explicit dependency stack.
     * @param root Difference root
     * @return Selected node or absence
     */
    private Optional<Node> evaluate(final Node root) {
        final Map<Node, Optional<Node>> done = new IdentityHashMap<>();
        final List<Node> stack = new ArrayList<>(1);
        stack.add(root);
        while (!stack.isEmpty()) {
            Interruption.check();
            final Node node = stack.getLast();
            if (done.containsKey(node)) {
                stack.removeLast();
            } else if (node instanceof PrototypeBasedNode && !(node instanceof Hole)) {
                DiffProjection.expand(node, stack, done);
            } else {
                done.put(node, this.select(node));
            }
        }
        return done.get(root);
    }

    /**
     * Selects an action payload without traversing inside it.
     * @param node Action or unchanged opaque subtree
     * @return Selected node, or absence
     */
    private Optional<Node> select(final Node node) {
        Optional<Node> result = Optional.of(node);
        if (node instanceof Insert insert) {
            result = Optional.empty();
            if (this.after) {
                result = Optional.of(insert.getAfter());
            }
        } else if (node instanceof Delete delete) {
            result = Optional.empty();
            if (!this.after) {
                result = Optional.of(delete.getBefore());
            }
        } else if (node instanceof Replace replace) {
            result = Optional.of(replace.getBefore());
            if (this.after) {
                result = Optional.of(replace.getAfter());
            }
        }
        return result;
    }

    /**
     * Schedules dependencies or finishes a ready scaffold.
     * @param node Scaffold
     * @param stack Dependency stack
     * @param done Completed projections
     */
    private static void expand(final Node node, final List<Node> stack,
        final Map<Node, Optional<Node>> done) {
        boolean ready = true;
        for (final Node child : node.getChildrenList()) {
            if (!done.containsKey(child)) {
                stack.add(child);
                ready = false;
            }
        }
        if (ready) {
            done.put(node, Optional.of(DiffProjection.rebuild(node, done)));
        }
    }

    /**
     * Restores one scaffold with compacted children and their original edge tags.
     * @param node Scaffold
     * @param done Projected dependencies
     * @return Valid ordinary node
     */
    private static Node rebuild(final Node node, final Map<Node, Optional<Node>> done) {
        final List<Node> children = new ArrayList<>(node.getChildCount());
        final List<Optional<String>> tags = new ArrayList<>(node.getChildCount());
        for (int index = 0; index < node.getChildCount(); index += 1) {
            final Optional<Node> child = done.get(node.getChild(index));
            if (child.isPresent()) {
                children.add(child.orElseThrow());
                tags.add(node.getChildTag(index));
            }
        }
        final Type type;
        if (node instanceof PatternNode pattern) {
            type = pattern.getTargetType();
        } else if (node instanceof DiffNode difference) {
            type = difference.getTargetType();
        } else {
            type = node.getType();
        }
        return OccurrenceReplacer.rebuild(
            node, new OccurrenceReplacer.Content(node.getData(), children, tags), type
        ).orElseThrow(() -> new ConstructionException("Difference projection refused by builder"));
    }
}
