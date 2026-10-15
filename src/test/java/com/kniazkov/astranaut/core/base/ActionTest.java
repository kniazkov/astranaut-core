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

import com.kniazkov.astranaut.core.example.LittleTrees;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests covering actions, i.e. {@link Action} interface and inherited classes.
 * @since 0.0.1
 */
class ActionTest {
    /**
     * The 'Insert' type.
     */
    private static final String INSERT_TYPE = "Insert";

    /**
     * The 'Replace' type.
     */
    private static final String REPLACE_TYPE = "Replace";

    /**
     * The 'Delete' type.
     */
    private static final String DELETE_TYPE = "Delete";

    /**
     * The 'color' property.
     */
    private static final String COLOR_PROPERTY = "color";

    /**
     * The expected color property.
     */
    private static final String EXPECTED_COLOR = "blue";

    @Test
    void testInsertAction() {
        final Node inserted = LittleTrees.createReturnStatement(null);
        final Action action = new Insert(inserted);
        Assertions.assertEquals(EmptyFragment.INSTANCE, action.getFragment());
        Assertions.assertEquals("", action.getData());
        Assertions.assertEquals(1, action.getChildCount());
        Assertions.assertEquals(inserted, action.getChild(0));
        Assertions.assertNull(action.getChild(1));
        final Type type = action.getType();
        Assertions.assertEquals(ActionTest.INSERT_TYPE, type.getName());
        Assertions.assertTrue(type.getChildDescriptors().isEmpty());
        final java.util.Set<AbstractType> hierarchy = type.getBaseTypes();
        Assertions.assertFalse(hierarchy.isEmpty());
        Assertions.assertTrue(hierarchy.contains(Action.TYPE));
        Assertions.assertEquals(
            ActionTest.EXPECTED_COLOR,
            type.getProperties().getOrDefault(ActionTest.COLOR_PROPERTY, "")
        );
        Assertions.assertTrue(action.toString().startsWith(ActionTest.INSERT_TYPE));
        final Builder builder = type.createBuilder();
        builder.setFragment(EmptyFragment.INSTANCE);
        Assertions.assertTrue(builder.setData(""));
        Assertions.assertFalse(builder.setData("abracadabra"));
        Assertions.assertFalse(builder.isValid());
        Assertions.assertThrows(ConstructionException.class, builder::build);
        Assertions.assertTrue(builder.setChildren(Collections.singletonList(inserted)));
        Assertions.assertFalse(builder.setChildren(Arrays.asList(inserted, inserted)));
        final Node created = builder.build();
        Assertions.assertEquals(ActionTest.INSERT_TYPE, created.getTypeName());
    }

    @Test
    void testReplaceAction() {
        final Node before = LittleTrees.createVariable("x");
        final Node after = LittleTrees.createIntegerLiteral(0);
        final Action action = new Replace(before, after);
        Assertions.assertEquals(EmptyFragment.INSTANCE, action.getFragment());
        Assertions.assertEquals("", action.getData());
        Assertions.assertEquals(2, action.getChildCount());
        Assertions.assertEquals(before, action.getChild(0));
        Assertions.assertEquals(after, action.getChild(1));
        Assertions.assertNull(action.getChild(2));
        final Type type = action.getType();
        Assertions.assertEquals(ActionTest.REPLACE_TYPE, type.getName());
        Assertions.assertTrue(type.getChildDescriptors().isEmpty());
        final java.util.Set<AbstractType> hierarchy = type.getBaseTypes();
        Assertions.assertFalse(hierarchy.isEmpty());
        Assertions.assertTrue(hierarchy.contains(Action.TYPE));
        Assertions.assertEquals(
            ActionTest.EXPECTED_COLOR,
            type.getProperties().getOrDefault(ActionTest.COLOR_PROPERTY, "")
        );
        Assertions.assertTrue(action.toString().startsWith(ActionTest.REPLACE_TYPE));
        final Builder builder = type.createBuilder();
        builder.setFragment(EmptyFragment.INSTANCE);
        Assertions.assertTrue(builder.setData(""));
        Assertions.assertFalse(builder.setData("it's a kind of magic"));
        Assertions.assertFalse(builder.isValid());
        Assertions.assertThrows(ConstructionException.class, builder::build);
        Assertions.assertTrue(builder.setChildren(Arrays.asList(before, after)));
        Assertions.assertFalse(builder.setChildren(Collections.singletonList(before)));
        Assertions.assertTrue(builder.isValid());
        final Node created = builder.build();
        Assertions.assertEquals(ActionTest.REPLACE_TYPE, created.getTypeName());
    }

    @Test
    void testDeleteAction() {
        final Node deleted = LittleTrees.createReturnStatement(null);
        final Action action = new Delete(deleted);
        Assertions.assertEquals(EmptyFragment.INSTANCE, action.getFragment());
        Assertions.assertEquals("", action.getData());
        Assertions.assertEquals(1, action.getChildCount());
        Assertions.assertEquals(deleted, action.getChild(0));
        Assertions.assertNull(action.getChild(1));
        final Type type = action.getType();
        Assertions.assertEquals(ActionTest.DELETE_TYPE, type.getName());
        Assertions.assertTrue(type.getChildDescriptors().isEmpty());
        final java.util.Set<AbstractType> hierarchy = type.getBaseTypes();
        Assertions.assertFalse(hierarchy.isEmpty());
        Assertions.assertTrue(hierarchy.contains(Action.TYPE));
        Assertions.assertEquals(
            ActionTest.EXPECTED_COLOR,
            type.getProperties().getOrDefault(ActionTest.COLOR_PROPERTY, "")
        );
        Assertions.assertTrue(action.toString().startsWith(ActionTest.DELETE_TYPE));
        final Builder builder = type.createBuilder();
        builder.setFragment(EmptyFragment.INSTANCE);
        Assertions.assertTrue(builder.setData(""));
        Assertions.assertFalse(builder.setData("I hate syntax trees"));
        Assertions.assertFalse(builder.isValid());
        Assertions.assertThrows(ConstructionException.class, builder::build);
        Assertions.assertTrue(builder.setChildren(Collections.singletonList(deleted)));
        Assertions.assertFalse(builder.setChildren(Arrays.asList(deleted, deleted)));
        final Node created = builder.build();
        Assertions.assertEquals(ActionTest.DELETE_TYPE, created.getTypeName());
    }
}
