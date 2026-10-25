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

import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.DummyNode;
import com.kniazkov.astranaut.core.base.Node;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link NodeReplacer} class.
 * @since 0.0.1
 */
class NodeReplacerTest {
    @Test
    void testReplacementOfRoot() {
        final Node root =
            this.build(
                "1",
                "",
                this.build(
                    "11",
                    "",
                    this.build(
                        "21",
                        ""
                    ),
                    this.build(
                        "22",
                        ""
                    )
                ),
                this.build("12", ""),
                this.build(
                    "13",
                    "",
                    this.build(
                        "23",
                        ""
                    ),
                    this.build(
                        "24",
                        ""
                    )
                )
            );
        final Node target = this.createTargetTree();
        final NodeReplacer.Result result = new NodeReplacer().replace(root, root, target);
        Assertions.assertEquals(target, result.root());
        Assertions.assertEquals(-1, result.lastChangedChild());
    }

    @Test
    void testReplacementOfRootChild() {
        final Node source =
            this.build(
                "130",
                "",
                this.build(
                    "230",
                    ""
                ),
                this.build(
                    "240",
                    ""
                )
            );
        final Node root =
            this.build(
                "10",
                "",
                this.build(
                    "110",
                    "",
                    this.build(
                        "210",
                        ""
                    ),
                    this.build(
                        "220",
                        ""
                    )
                ),
                this.build("120", ""),
                source
            );
        final Node target = this.createTargetTree();
        final NodeReplacer.Result result = new NodeReplacer().replace(root, source, target);
        Assertions.assertEquals(target, result.root().getChild(2));
        Assertions.assertEquals(2, result.lastChangedChild());
    }

    @Test
    void testReplacementOfRootGrandChild() {
        final Node source = this.build("2300", "");
        final Node left =
            this.build(
            "1100",
            "",
            this.build(
                "2100",
                ""
            ),
            this.build(
                "2200",
                ""
            )
        );
        final Node mid = this.build("1200", "");
        final Node root = this.build(
            "100",
            "",
            left,
            mid,
            this.build(
                "1300",
                "",
                source,
                this.build(
                    "2400",
                    ""
                )
            )
        );
        final Node target = this.createTargetTree();
        final NodeReplacer.Result result = new NodeReplacer().replace(root, source, target);
        Assertions.assertEquals(target, result.root().getChild(2).getChild(0));
        Assertions.assertEquals(left, result.root().getChild(0));
        Assertions.assertEquals(mid, result.root().getChild(1));
        Assertions.assertEquals(2, result.lastChangedChild());
    }

    @Test
    void testReplacementWithoutMatch() {
        final Node root =
            this.build(
                "111",
                "",
                this.build("222", ""),
                this.build("333", "")
        );
        final Node source = this.build("444", "");
        final Node target = this.createTargetTree();
        final NodeReplacer.Result result = new NodeReplacer().replace(root, source, target);
        Assertions.assertEquals(DummyNode.INSTANCE, result.root());
        Assertions.assertEquals(-1, result.lastChangedChild());
    }

    private Node createTargetTree() {
        return this.build(
            "2",
            "",
            this.build(
                "14",
                ""
            )
        );
    }

    /**
     * Creates node for test purposes.
     * @param type The type name
     * @param data The data (in a textual format)
     * @param children The list of children
     * @return A new node
     */
    private Node build(final String type, final String data, final Node... children) {
        final DraftNode.Constructor ctor = new DraftNode.Constructor();
        ctor.setName(type);
        ctor.setData(data);
        ctor.setChildren(Arrays.asList(children));
        return ctor.build();
    }
}
