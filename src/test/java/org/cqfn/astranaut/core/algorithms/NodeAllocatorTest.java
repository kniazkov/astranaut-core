/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Ivan Kniazkov
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
package org.cqfn.astranaut.core.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.cqfn.astranaut.core.base.ChildDescriptor;
import org.cqfn.astranaut.core.base.DraftNode;
import org.cqfn.astranaut.core.base.Node;
import org.cqfn.astranaut.core.example.green.Addition;
import org.cqfn.astranaut.core.example.green.IntegerLiteral;
import org.cqfn.astranaut.core.example.green.Variable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Testing {@link NodeAllocator} class.
 * @since 1.0
 */
@SuppressWarnings("PMD.TooManyMethods")
class NodeAllocatorTest {
    /**
     * The 'AssignableExpression' string.
     */
    private static final String ASSIGN_EXPR = "AssignableExpression";

    /**
     * The 'Expression' string.
     */
    private static final String EXPRESSION = "Expression";

    @Test
    void testAllNonOptionalChildrenMapping() {
        final boolean result = this.testMapping(
            Arrays.asList(
                new ChildDescriptor("A"),
                new ChildDescriptor("B")
            ),
            Arrays.asList(
                new NodeDescriptor("A", 0),
                new NodeDescriptor("B", 1)
            ),
            true
        );
        Assertions.assertTrue(result);
    }

    @Test
    void testIncorrectOrderChildren() {
        final boolean result = this.testMapping(
            ChildDescriptor.create()
                .required("A").required("B").optional("C").optional("D")
                .build(),
            Arrays.asList(
                new NodeDescriptor("D", 3),
                new NodeDescriptor("B", 1),
                new NodeDescriptor("A", 0)
            ),
            true
        );
        Assertions.assertTrue(result);
    }

    @Test
    void testOneNodeNotMapped() {
        final boolean result = this.testMapping(
            Arrays.asList(
                new ChildDescriptor("A")
            ),
            Arrays.asList(
                new NodeDescriptor("B")
            ),
            false
        );
        Assertions.assertTrue(result);
    }

    @Test
    void testAllVariousChildrenMapping() {
        final boolean result = this.testMapping(
            Arrays.asList(
                new ChildDescriptor("A", false),
                new ChildDescriptor("B", true),
                new ChildDescriptor("B", false),
                new ChildDescriptor("A", true)
            ),
            Arrays.asList(
                new NodeDescriptor("A", 0),
                new NodeDescriptor("B", 1),
                new NodeDescriptor("B", 2),
                new NodeDescriptor("A", 3)
            ),
            true
        );
        Assertions.assertTrue(result);
    }

    @Test
    void testOnlyNonOptionalFromVariousChildrenMapping() {
        final boolean result = this.testMapping(
            Arrays.asList(
                new ChildDescriptor("A", false),
                new ChildDescriptor("B", true),
                new ChildDescriptor("C", false),
                new ChildDescriptor("B", false),
                new ChildDescriptor("A", true)
            ),
            Arrays.asList(
                new NodeDescriptor("A", 0),
                new NodeDescriptor("C", 2),
                new NodeDescriptor("B", 3)
            ),
            true
        );
        Assertions.assertTrue(result);
    }

    @Test
    void testNonOptionalAndOptionalFromVariousChildrenMapping() {
        final boolean result = this.testMapping(
            Arrays.asList(
                new ChildDescriptor("A", false),
                new ChildDescriptor("B", true),
                new ChildDescriptor("C", false),
                new ChildDescriptor("B", false),
                new ChildDescriptor("A", true)
            ),
            Arrays.asList(
                new NodeDescriptor("A", 0),
                new NodeDescriptor("B", 1),
                new NodeDescriptor("C", 2),
                new NodeDescriptor("B", 3)
            ),
            true
        );
        Assertions.assertTrue(result);
    }

    @Test
    void testNonOptionalAndOptionalOfSameTypeMapping() {
        final boolean result = this.testMapping(
            Arrays.asList(
                new ChildDescriptor("A", false),
                new ChildDescriptor("B", true),
                new ChildDescriptor("B", false),
                new ChildDescriptor("B", false)
            ),
            Arrays.asList(
                new NodeDescriptor("A", 0),
                new NodeDescriptor("B", 2),
                new NodeDescriptor("B", 3)
            ),
            true
        );
        Assertions.assertTrue(result);
    }

    @Test
    void testNonOptionalOfSameTypeAsOptionalMapping() {
        final boolean result = this.testMapping(
            Arrays.asList(
                new ChildDescriptor("A", true),
                new ChildDescriptor("B", true),
                new ChildDescriptor("C", true),
                new ChildDescriptor("C", false),
                new ChildDescriptor("D", false),
                new ChildDescriptor("D", true)
            ),
            Arrays.asList(
                new NodeDescriptor("C", 3),
                new NodeDescriptor("D", 4)
            ),
            true
        );
        Assertions.assertTrue(result);
    }

    @Test
    void testMissingNonOptionalChildMapping() {
        final boolean result = this.testMapping(
            Arrays.asList(
                new ChildDescriptor("A", true),
                new ChildDescriptor("B", true),
                new ChildDescriptor("C", false),
                new ChildDescriptor("D", true)
            ),
            Arrays.asList(
                new NodeDescriptor("B", 1),
                new NodeDescriptor("D", 3)
            ),
            false
        );
        Assertions.assertTrue(result);
    }

    @Test
    void testExtraChildMapping() {
        final boolean result = this.testMapping(
            Arrays.asList(
                new ChildDescriptor("A", true),
                new ChildDescriptor("B", true),
                new ChildDescriptor("C", false)
            ),
            Arrays.asList(
                new NodeDescriptor("A", 0),
                new NodeDescriptor("B", 1),
                new NodeDescriptor("B", 2)
            ),
            false
        );
        Assertions.assertTrue(result);
    }

    @Test
    void testOneNotMatchedNode() {
        final boolean result = this.testMapping(
            Arrays.asList(
                new ChildDescriptor("A", false),
                new ChildDescriptor("B", true),
                new ChildDescriptor("B", true),
                new ChildDescriptor("A", true)
            ),
            Arrays.asList(
                new NodeDescriptor("A", 0),
                new NodeDescriptor("B", 1),
                new NodeDescriptor("B", 2),
                new NodeDescriptor("C", 3)
            ),
            false
        );
        Assertions.assertTrue(result);
    }

    @Test
    void testSideBySideControversialOptionalNodes() {
        final boolean result = this.testMapping(
            Arrays.asList(
                new ChildDescriptor("A", false),
                new ChildDescriptor("B", true),
                new ChildDescriptor("B", true),
                new ChildDescriptor("A", false)
            ),
            Arrays.asList(
                new NodeDescriptor("A", 0),
                new NodeDescriptor("B", 1),
                new NodeDescriptor("A", 3)
            ),
            false
        );
        Assertions.assertTrue(result);
    }

    @Test
    void testThreeControversialOptionalNodes() {
        final boolean result = this.testMapping(
            Arrays.asList(
                new ChildDescriptor("A", false),
                new ChildDescriptor("B", true),
                new ChildDescriptor("B", true),
                new ChildDescriptor("B", true),
                new ChildDescriptor("A", false)
            ),
            Arrays.asList(
                new NodeDescriptor("A", 0),
                new NodeDescriptor("B", 1),
                new NodeDescriptor("B", 2),
                new NodeDescriptor("A", 4)
            ),
            false
        );
        Assertions.assertTrue(result);
    }

    @Test
    void testSeparatedControversialOptionalNodes() {
        final boolean result = this.testMapping(
            Arrays.asList(
                new ChildDescriptor("A", false),
                new ChildDescriptor("B", true),
                new ChildDescriptor("C", true),
                new ChildDescriptor("B", true),
                new ChildDescriptor("D", true),
                new ChildDescriptor("A", false)
            ),
            Arrays.asList(
                new NodeDescriptor("A", 0),
                new NodeDescriptor("B", 1),
                new NodeDescriptor("D", 4),
                new NodeDescriptor("A", 5)
            ),
            false
        );
        Assertions.assertTrue(result);
    }

    @Test
    void testIncorrectNumberOfChildren() {
        final boolean result = this.testMapping(
            Arrays.asList(
                new ChildDescriptor("A", false),
                new ChildDescriptor("B", true)
            ),
            Arrays.asList(
                new NodeDescriptor("A", 0),
                new NodeDescriptor("B", 1),
                new NodeDescriptor("C", 2)
            ),
            false
        );
        Assertions.assertTrue(result);
    }

    @Test
    void testMappingWithInheritance() {
        final List<ChildDescriptor> descriptors = Arrays.asList(
            new ChildDescriptor(NodeAllocatorTest.EXPRESSION),
            new ChildDescriptor(NodeAllocatorTest.EXPRESSION)
        );
        final NodeAllocator mapper = new NodeAllocator(descriptors);
        IntegerLiteral.Constructor icr = new IntegerLiteral.Constructor();
        icr.setData("7");
        final Node first = icr.createNode();
        icr = new IntegerLiteral.Constructor();
        icr.setData("11");
        final Node second = icr.createNode();
        final Addition.Constructor acr = new Addition.Constructor();
        acr.setChildrenList(Arrays.asList(first, second));
        final Node addition = acr.createNode();
        icr = new IntegerLiteral.Constructor();
        icr.setData("13");
        final Node third = icr.createNode();
        final Node[] mapping = new Node[2];
        final boolean result = mapper.allocate(mapping, Arrays.asList(addition, third));
        Assertions.assertTrue(result);
        Assertions.assertEquals(addition, mapping[0]);
        Assertions.assertEquals(third, mapping[1]);
    }

    @Test
    void testMappingWithTwoLevelInheritance() {
        final List<ChildDescriptor> descriptors = Arrays.asList(
            new ChildDescriptor(NodeAllocatorTest.ASSIGN_EXPR),
            new ChildDescriptor(NodeAllocatorTest.EXPRESSION)
        );
        final NodeAllocator mapper = new NodeAllocator(descriptors);
        final Variable.Constructor ctor = new Variable.Constructor();
        ctor.setData("x");
        final Node left = ctor.createNode();
        ctor.setData("y");
        final Node right = ctor.createNode();
        final Node[] mapping = new Node[2];
        final boolean result = mapper.allocate(mapping, Arrays.asList(left, right));
        Assertions.assertTrue(result);
    }

    @Test
    void testEmptyInput() {
        final boolean result = this.testMapping(
            Collections.emptyList(),
            Collections.emptyList(),
            true
        );
        Assertions.assertTrue(result);
    }

    @Test
    void testEmptyChildrenList() {
        final boolean result = this.testMapping(
            Collections.singletonList(new ChildDescriptor("A", true)),
            Collections.emptyList(),
            true
        );
        Assertions.assertTrue(result);
    }

    @Test
    void testEmptyDescriptions() {
        final boolean result = this.testMapping(
            Collections.emptyList(),
            Collections.singletonList(new NodeDescriptor("A", 0)),
            false
        );
        Assertions.assertTrue(result);
    }

    @Test
    void testBadDestination() {
        final List<ChildDescriptor> descriptors = Arrays.asList(
            new ChildDescriptor("A"),
            new ChildDescriptor("B")
        );
        final NodeAllocator allocator = new NodeAllocator(descriptors);
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> allocator.allocate(new Node[1], Collections.emptyList())
        );
    }

    /**
     * Common test for the {@link NodeAllocator} class.
     * @param types The list of types
     * @param nodes The list of nodes
     * @param success If expected success mapping
     * @return Result of testing, {@Code true} if test passed
     */
    private boolean testMapping(final List<ChildDescriptor> types,
        final List<NodeDescriptor> nodes,
        final boolean success) {
        final List<Node> children = new ArrayList<>(nodes.size());
        for (final NodeDescriptor node : nodes) {
            final DraftNode.Constructor ctor = new DraftNode.Constructor();
            ctor.setName(node.getTypeName());
            ctor.setData(node.getIndex());
            children.add(ctor.createNode());
        }
        final Node[] mapping = new Node[types.size()];
        final NodeAllocator mapper = new NodeAllocator(types);
        boolean result = mapper.allocate(mapping, children);
        if (success && result) {
            for (int index = 0; index < mapping.length; index = index + 1) {
                if (mapping[index] != null && Integer.parseInt(mapping[index].getData()) != index) {
                    result = false;
                    break;
                }
            }
        } else if (!success) {
            result = !result;
        }
        return result;
    }

    /**
     * Node descriptor for testing.
     * @since 1.0
     */
    private static final class NodeDescriptor {
        /**
         * Name of the node type.
         */
        private final String type;

        /**
         * Expected index after mapping.
         */
        private final int index;

        /**
         * Constructor.
         * @param type Name of the node type
         * @param index Expected index after mapping
         */
        NodeDescriptor(final String type, final int index) {
            this.type = type;
            this.index = index;
        }

        /**
         * Constructor.
         * @param type Name of the node type
         */
        NodeDescriptor(final String type) {
            this(type, -1);
        }

        /**
         * Returns the name of the node type.
         * @return The name
         */
        public String getTypeName() {
            return this.type;
        }

        /**
         * Returns expected index after mapping.
         * @return The index (as a string)
         */
        public String getIndex() {
            return String.valueOf(this.index);
        }

        @Override
        public String toString() {
            return new StringBuilder()
                .append(this.type)
                .append(':')
                .append(this.index)
                .toString();
        }
    }
}
