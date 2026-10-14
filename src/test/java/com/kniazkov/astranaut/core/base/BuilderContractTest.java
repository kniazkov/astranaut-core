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
package com.kniazkov.astranaut.core.base;

import com.kniazkov.astranaut.core.example.green.Addition;
import com.kniazkov.astranaut.core.example.green.IntegerLiteral;
import com.kniazkov.astranaut.core.example.green.Return;
import com.kniazkov.astranaut.core.example.green.Variable;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Consumer-facing contracts for independent builder implementations.
 * @since 0.0.1
 */
class BuilderContractTest {

    /**
     * The left child tag.
     */
    private static final String LEFT_TAG = "left";

    /**
     * The right child tag.
     */
    private static final String RIGHT_TAG = "right";

    @Test
    void editsInInsertionOrderAndBuildsInSchemaOrder() {
        final Builder builder = Addition.TYPE.createBuilder();
        final Node left = new Variable.Constructor().build();
        final Node right = new IntegerLiteral.Constructor().build();
        Assertions.assertTrue(builder.setChild(BuilderContractTest.RIGHT_TAG, left));
        Assertions.assertFalse(builder.isValid());
        Assertions.assertThrows(ConstructionException.class, builder::build);
        Assertions.assertTrue(builder.setChild(0, right));
        Assertions.assertTrue(builder.setChild(BuilderContractTest.LEFT_TAG, left));
        Assertions.assertTrue(builder.isValid());
        final Node node = builder.build();
        Assertions.assertSame(left, node.getChild(0));
        Assertions.assertSame(right, node.getChild(1));
        Assertions.assertTrue(builder.removeChild(0));
        Assertions.assertFalse(builder.isValid());
        Assertions.assertFalse(builder.removeChild(BuilderContractTest.RIGHT_TAG));
        Assertions.assertTrue(builder.setChild(0, right));
        Assertions.assertTrue(builder.setChild(BuilderContractTest.RIGHT_TAG, left));
        Assertions.assertSame(
            right, builder.build().getChild(BuilderContractTest.LEFT_TAG).orElseThrow()
        );
        Assertions.assertSame(left, node.getChild(BuilderContractTest.LEFT_TAG).orElseThrow());
        Assertions.assertSame(right, node.getChild(BuilderContractTest.RIGHT_TAG).orElseThrow());
    }

    @Test
    void rejectionPreservesChildrenDataAndTags() {
        final Builder builder = Addition.TYPE.createBuilder();
        final Node child = new Variable.Constructor().build();
        Assertions.assertTrue(builder.setChildren(List.of(child, child)));
        Assertions.assertFalse(builder.setChildren(List.of(child)));
        Assertions.assertFalse(builder.setData("not empty"));
        Assertions.assertFalse(builder.setChild("missing", child));
        Assertions.assertFalse(
            builder.setChild(BuilderContractTest.LEFT_TAG, DraftNode.create("wrong"))
        );
        Assertions.assertFalse(builder.setChild(-1, child));
        Assertions.assertFalse(builder.setChild(2, child));
        Assertions.assertFalse(builder.removeChild(-1));
        Assertions.assertFalse(builder.removeChild(2));
        Assertions.assertFalse(builder.removeChild("missing"));
        final Node node = builder.build();
        Assertions.assertSame(child, node.getChild(BuilderContractTest.LEFT_TAG).orElseThrow());
        Assertions.assertSame(child, node.getChild(BuilderContractTest.RIGHT_TAG).orElseThrow());
        Assertions.assertEquals("", node.getData());
    }

    @Test
    void removingRequiredChildrenIsAcceptedButInvalidatesConstruction() {
        final Builder builder = Addition.TYPE.createBuilder();
        final Node child = new Variable.Constructor().build();
        Assertions.assertTrue(builder.setChildren(List.of(child, child)));
        Assertions.assertTrue(builder.removeChild(BuilderContractTest.LEFT_TAG));
        Assertions.assertFalse(builder.isValid());
        Assertions.assertThrows(ConstructionException.class, builder::build);
        Assertions.assertTrue(builder.setChild(BuilderContractTest.LEFT_TAG, child));
        Assertions.assertTrue(builder.isValid());
        Assertions.assertEquals(2, builder.build().getChildCount());
    }

    @Test
    void optionalChildCanBeAbsentAndRebound() {
        final Builder builder = Return.TYPE.createBuilder();
        Assertions.assertTrue(builder.isValid());
        Assertions.assertFalse(builder.removeChild("expression"));
        Assertions.assertTrue(builder.setChild("expression", new Variable.Constructor().build()));
        final Node node = builder.build();
        Assertions.assertTrue(builder.setChildren(List.of()));
        Assertions.assertTrue(builder.isValid());
        Assertions.assertTrue(builder.build().getChild("expression").isEmpty());
        Assertions.assertEquals(1, node.getChildCount());
    }

    @Test
    void unknownSchemaSupportsIndexedEditsAndIndependentSnapshots() {
        final DraftNode.Constructor builder = new DraftNode.Constructor();
        Assertions.assertThrows(ConstructionException.class, builder::build);
        builder.setName("draft");
        final Node first = DraftNode.create("first");
        final Node second = DraftNode.create("second");
        Assertions.assertTrue(builder.setChildren(List.of(first, second)));
        final Node old = builder.build();
        Assertions.assertTrue(builder.setChild(0, second));
        Assertions.assertTrue(builder.removeChild(1));
        Assertions.assertFalse(builder.setChild(1, first));
        Assertions.assertFalse(builder.removeChild(1));
        Assertions.assertFalse(builder.setChild("unknown", first));
        Assertions.assertSame(first, old.getChild(0));
        Assertions.assertEquals(2, old.getChildCount());
        Assertions.assertEquals(List.of(second), builder.build().getChildrenList());
    }

    @Test
    void actionsRefuseMissingChildrenAndKeepPublishedSnapshots() {
        for (final Builder builder : List.of(new Insert.Constructor(), new Delete.Constructor())) {
            final Node child = DraftNode.create("child");
            Assertions.assertTrue(builder.setChildren(List.of(child)));
            final Node before = builder.build();
            Assertions.assertTrue(builder.removeChild(0));
            Assertions.assertFalse(builder.isValid());
            Assertions.assertThrows(ConstructionException.class, builder::build);
            Assertions.assertSame(child, before.getChild(0));
            Assertions.assertTrue(builder.setChildren(List.of(child)));
            Assertions.assertTrue(builder.setChild(0, DraftNode.create("replacement")));
            Assertions.assertFalse(builder.setChildren(List.of(child, child)));
            Assertions.assertEquals("replacement", builder.build().getChild(0).getTypeName());
        }
    }

    @Test
    void leavesValidateDataAndRejectChildren() {
        final Char.Constructor builder = new Char.Constructor();
        Assertions.assertFalse(builder.isValid());
        Assertions.assertThrows(ConstructionException.class, builder::build);
        Assertions.assertTrue(builder.setData("a"));
        final Node node = builder.build();
        Assertions.assertFalse(builder.setData(""));
        Assertions.assertFalse(builder.setData("ab"));
        Assertions.assertFalse(builder.setChild(0, node));
        Assertions.assertFalse(builder.removeChild(0));
        Assertions.assertFalse(builder.removeChild("tag"));
        Assertions.assertTrue(builder.setData("b"));
        Assertions.assertEquals("a", node.getData());
        Assertions.assertEquals("b", builder.build().getData());
    }
}
