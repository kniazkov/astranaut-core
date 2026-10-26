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

import com.kniazkov.astranaut.core.base.Delete;
import com.kniazkov.astranaut.core.base.DiffNode;
import com.kniazkov.astranaut.core.base.EmptyFragment;
import com.kniazkov.astranaut.core.base.Fragment;
import com.kniazkov.astranaut.core.base.Hole;
import com.kniazkov.astranaut.core.base.Insert;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.PatternNode;
import com.kniazkov.astranaut.core.base.Position;
import com.kniazkov.astranaut.core.base.Replace;
import com.kniazkov.astranaut.core.utils.TreeJsonException;
import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Iterative postorder encoding; children refer only to preceding table entries.
 * @since 0.0.1
 */
public final class NodeWriter {
    /**
     * Whether metadata is included.
     */
    private final boolean extended;

    /**
     * Chooses the metadata mode.
     * @param extended Include metadata without source text
     */
    public NodeWriter(final boolean extended) {
        this.extended = extended;
    }

    /**
     * Encodes an acyclic immutable graph without recursive Java or JSON nesting.
     * @param root Root
     * @param table Output table
     * @return Root index
     */
    public int write(final Node root, final JsonArray table) {
        final Map<Node, Integer> ids = new IdentityHashMap<>();
        final Set<Node> active = Collections.newSetFromMap(new IdentityHashMap<>());
        final List<Frame> stack = new ArrayList<>(0);
        stack.add(new Frame(root));
        active.add(root);
        while (!stack.isEmpty()) {
            final Frame frame = stack.getLast();
            if (frame.index < frame.node.getChildCount()) {
                final Node child = frame.node.getChild(frame.index);
                frame.index += 1;
                if (!ids.containsKey(child)) {
                    NodeWriter.descend(child, active, stack);
                }
            } else {
                this.record(frame.node, table.createObject(), ids);
                ids.put(frame.node, table.size() - 1);
                active.remove(frame.node);
                stack.removeLast();
            }
        }
        return ids.get(root);
    }

    /**
     * Distinguishes ordinary data from executable service nodes.
     * @param node Node
     * @return Wire kind
     */
    static String kind(final Node node) {
        final String result;
        if (node instanceof Hole) {
            result = "hole";
        } else if (node instanceof Insert) {
            result = "insert";
        } else if (node instanceof Delete) {
            result = "delete";
        } else if (node instanceof Replace) {
            result = "replace";
        } else if (node instanceof DiffNode) {
            result = "diff";
        } else if (node instanceof PatternNode) {
            result = "pattern";
        } else {
            result = "node";
        }
        return result;
    }

    /**
     * Schedules one unfinished child, rejecting cycles.
     * @param child Child
     * @param active Ancestors
     * @param stack Traversal frames
     * @throws TreeJsonException If the child is an ancestor
     */
    private static void descend(final Node child, final Set<Node> active, final List<Frame> stack) {
        if (!active.add(child)) {
            throw new TreeJsonException("Cyclic tree");
        }
        stack.add(new Frame(child));
    }

    /**
     * Encodes one node and occurrence edges.
     * @param node Node
     * @param result Record
     * @param ids Completed children
     */
    private void record(final Node node, final JsonObject result, final Map<Node, Integer> ids) {
        result.addString("kind", NodeWriter.kind(node));
        result.addString("type", node.getTypeName());
        result.addString("data", node.getData());
        final JsonArray children = result.createArray("children");
        for (int index = 0; index < node.getChildCount(); index += 1) {
            final JsonObject edge = children.createObject();
            edge.addNumber("node", ids.get(node.getChild(index)));
            node.getChildTag(index).ifPresent(tag -> edge.addString("tag", tag));
        }
        if (node instanceof Hole hole) {
            hole.getConstraint().ifPresent(type -> result.addString("constraint", type.getName()));
        }
        if (this.extended) {
            NodeWriter.metadata(node, result.createObject("metadata"));
        }
    }

    /**
     * Writes explicit metadata fields; never reflects over Source or calls getCode.
     * @param node Node
     * @param result Metadata object
     */
    private static void metadata(final Node node, final JsonObject result) {
        final JsonObject properties = result.createObject("properties");
        node.getProperties().entrySet().stream().sorted(Map.Entry.comparingByKey())
            .forEach(entry -> properties.addString(entry.getKey(), entry.getValue()));
        final JsonObject type = result.createObject("typeProperties");
        node.getType().getProperties().entrySet().stream().sorted(Map.Entry.comparingByKey())
            .forEach(entry -> type.addString(entry.getKey(), entry.getValue()));
        final JsonArray bases = result.createArray("baseTypes");
        node.getType().getBaseTypes().stream().map(value -> value.getName()).sorted()
            .forEach(bases::addString);
        final Fragment fragment = node.getFragment();
        if (!fragment.equals(EmptyFragment.INSTANCE)) {
            final JsonObject origin = result.createObject("origin");
            NodeWriter.position(fragment.getBegin(), origin.createObject("begin"));
            NodeWriter.position(fragment.getEnd(), origin.createObject("end"));
        }
    }

    /**
     * Encodes coordinates and filename only.
     * @param position Position
     * @param result Output
     */
    private static void position(final Position position, final JsonObject result) {
        result.addString("file", position.getSource().getFileName());
        result.addNumber("row", position.getRow());
        result.addNumber("column", position.getColumn());
    }

    /**
     * Explicit DFS frame.
     * @since 0.0.1
     */
    private static final class Frame {
        /**
         * Current node.
         */
        private final Node node;

        /**
         * Next child.
         */
        private int index;

        /**
         * Starts a node.
         * @param node Node
         */
        private Frame(final Node node) {
            this.node = node;
        }
    }
}
