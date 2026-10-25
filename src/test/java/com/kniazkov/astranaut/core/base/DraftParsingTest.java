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

import com.kniazkov.astranaut.core.algorithms.DepthCalculator;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Strict finite draft parsing, escaped data and stack-safe description round trips.
 * @since 0.0.1
 */
class DraftParsingTest {
    @Test
    void rejectsMalformedDescriptionsPromptlyAndDoesNotPublishPartialIndex() {
        final List<String> malformed = List.of(
            "", " ", "A(B", "A(B,)", "A(,B)", "A(B C)", "A)tail", "A#", "A(B #C)",
            "A<\"unterminated", "A<'unterminated", "A<\"x\"", "A<$>", "A<\"x\\",
            "A<\"\\q\">", "A<\"\\u12\">", "A<\"\\u12xz\">", "A<\"x\"> trailing"
        );
        Assertions.assertTimeoutPreemptively(
            Duration.ofSeconds(5), () -> {
                for (final String text : malformed) {
                    final Map<String, Set<Node>> index = new TreeMap<>();
                    final Node sentinel = DraftNode.create("Sentinel");
                    index.put("existing", Set.of(sentinel));
                    final IllegalArgumentException failure = Assertions.assertThrows(
                        IllegalArgumentException.class, () -> DraftNode.create(text, index), text
                    );
                    Assertions.assertTrue(failure.getMessage().contains("UTF-16 offset"));
                    Assertions.assertEquals(Map.of("existing", Set.of(sentinel)), index);
                }
            }
        );
    }

    @Test
    void roundTripsQuotesBackslashesControlUnitsAndUnicode() {
        final String data = "Кириллица 😀\0\uffff\t\r\n\b\f\"'\\<>(),";
        final Node original = DraftNode.create("Root", data, DraftNode.create("Leaf", data));
        final String description = original.toString();
        Assertions.assertEquals(original, DraftNode.create(description));
        Assertions.assertTrue(description.contains("\\n"));
        Assertions.assertTrue(description.contains("\\u0000"));
        final DraftNode.Constructor builder = new DraftNode.Constructor();
        builder.setName("Root");
        builder.setData(data);
        builder.setChildren(original.getChildrenList());
        Assertions.assertEquals(description, builder.toString());
        Assertions.assertEquals("it's", DraftNode.create("A<'it\\'s'>").getData());
        Assertions.assertEquals("A", DraftNode.create("A<\"\\u0041\">").getData());
        Assertions.assertEquals("\0\uffff", DraftNode.create("A<\"\0\uffff\">").getData());
        Assertions.assertEquals("Root(Leaf)", DraftNode.create(" Root ( Leaf ( ) ) ").toString());
    }

    @Test
    void builderAndNodeUseTheSameFormatForChildrenWithoutCustomToString() {
        final Char.Constructor character = new Char.Constructor();
        character.setValue('x');
        final Node child = character.build();
        final DraftNode.Constructor builder = new DraftNode.Constructor();
        Assertions.assertEquals("", builder.toString());
        builder.setChildren(List.of(child, child));
        Assertions.assertFalse(builder.isValid());
        Assertions.assertEquals("(Char<\"x\">, Char<\"x\">)", builder.toString());
        builder.setName("Root");
        final Node node = builder.build();
        final String expected = "Root(Char<\"x\">, Char<\"x\">)";
        Assertions.assertEquals(expected, builder.toString());
        Assertions.assertEquals(expected, node.toString());
        Assertions.assertEquals(node, DraftNode.create(builder.toString()));
        Assertions.assertSame(child, node.getChild(0));
        Assertions.assertSame(child, node.getChild(1));
        builder.setData("changed");
        Assertions.assertEquals(expected, node.toString());
        Assertions.assertEquals(builder.build().toString(), builder.toString());
    }

    @Test
    void parsesAndFormatsThirtyThousandNestedNodesWithoutTheJavaStack() {
        final String description = "A(".repeat(30_000)
            .concat("Leaf").concat(")".repeat(30_000));
        final Map<String, Set<Node>> index = new TreeMap<>();
        final Node root = DraftNode.create(description, index);
        Assertions.assertEquals(30_000, index.get("A").size());
        Assertions.assertEquals(1, index.get("Leaf").size());
        final DraftNode.Constructor builder = new DraftNode.Constructor();
        builder.setName(root.getTypeName());
        builder.setChildren(root.getChildrenList());
        Assertions.assertEquals(description, builder.toString());
        Assertions.assertEquals(30_001, new DepthCalculator().calculate(root));
        Assertions.assertEquals(description, root.toString());
        Assertions.assertEquals(root, DraftNode.create(root.toString()));
    }
}
