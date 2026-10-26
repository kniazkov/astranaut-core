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
package com.kniazkov.astranaut.core.algorithms.normalization;

import com.kniazkov.astranaut.core.base.Builder;
import com.kniazkov.astranaut.core.base.Fragment;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeWithStructuralEquality;
import com.kniazkov.astranaut.core.base.Type;
import com.kniazkov.astranaut.core.base.TypeWithNameBasedEquality;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * List of properties extracted from the original node.
 * @since 0.0.1
 */
public final class Properties extends NodeWithStructuralEquality implements Node {
    /**
     * Shared type descriptor.
     */
    private static final Type TYPE = new TypeImpl();

    /**
     * List of properties.
     */
    private final List<Property> list;

    /**
     * Constructor.
     * @param map Map, containing properties from original node
     */
    public Properties(final Map<String, String> map) {
        this(Properties.initList(map));
    }

    /**
     * Publishes a sorted immutable snapshot.
     * @param children Unique property nodes
     */
    private Properties(final List<Property> children) {
        this.list = children.stream().sorted(java.util.Comparator.comparing(Property::getName))
            .toList();
    }

    @Override
    public Type getType() {
        return Properties.TYPE;
    }

    @Override
    public String getData() {
        return "";
    }

    @Override
    public int getChildCount() {
        return this.list.size();
    }

    @Override
    public Node getChild(final int index) {
        return this.list.get(index);
    }

    @Override
    public String toString() {
        return Node.toString(this);
    }

    /**
     * Returns all properties as a collection.
     * @return Map containing properties
     */
    public Map<String, String> collectAllProperties() {
        final Map<String, String> map = new TreeMap<>();
        for (final Property property : this.list) {
            map.put(property.getName(), property.getData());
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * Initiates list of property nodes.
     * @param map Map, containing properties from original node
     * @return List of nodes, each containing one property
     */
    private static List<Property> initList(final Map<String, String> map) {
        List<Property> result = Collections.emptyList();
        if (!map.isEmpty()) {
            final List<Property> list = new ArrayList<>(map.size());
            for (final Map.Entry<String, String> property : map.entrySet()) {
                list.add(new Property(property.getKey(), property.getValue()));
            }
            result = Collections.unmodifiableList(list);
        }
        return result;
    }

    /**
     * Builder of node containing list of properties extracted from the original node.
     * @since 0.0.1
     */
    private static final class PropertiesBuilder implements Builder {
        /**
         * List of properties.
         */
        private List<Property> children;

        /**
         * Constructor.
         */
        private PropertiesBuilder() {
            this.children = Collections.emptyList();
        }

        @SuppressWarnings("PMD.UncommentedEmptyMethodBody")
        @Override
        public void setFragment(final Fragment fragment) {
        }

        @Override
        public boolean setData(final String str) {
            return str.isEmpty();
        }

        @Override
        public boolean setChildren(final List<Node> list) {
            final List<Property> properties = new ArrayList<>(list.size());
            final java.util.Set<String> names = new java.util.HashSet<>();
            boolean result = true;
            for (final Node node : list) {
                if (node instanceof Property property && names.add(property.getName())) {
                    properties.add(property);
                } else {
                    result = false;
                    break;
                }
            }
            if (result) {
                this.children = properties;
            }
            return result;
        }

        @Override
        public boolean setChild(final int index, final Node child) {
            boolean result = false;
            if (index >= 0 && index < this.children.size() && child instanceof Property) {
                final List<Node> updated = new ArrayList<>(this.children);
                updated.set(index, child);
                result = this.setChildren(updated);
            }
            return result;
        }

        @Override
        public boolean removeChild(final int index) {
            boolean result = false;
            if (index >= 0 && index < this.children.size()) {
                final List<Node> updated = new ArrayList<>(this.children);
                updated.remove(index);
                result = this.setChildren(updated);
            }
            return result;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Node build() {
            return new Properties(this.children);
        }
    }

    /**
     * Type descriptor independent of node instances.
     * @since 0.0.1
     */
    private static final class TypeImpl extends TypeWithNameBasedEquality implements Type {
        @Override
        public String getName() {
            return "AstranautProperties";
        }

        @Override
        public Builder createBuilder() {
            return new PropertiesBuilder();
        }
    }
}
