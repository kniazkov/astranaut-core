/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2024 Ivan Kniazkov
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
package org.cqfn.astranaut.core;

/**
 * Node created based on a prototype.
 *
 * @since 1.1.5
 */
public interface PrototypeBasedNode {
    /**
     * Returns the prototype of the node
     * @return Prototype node
     */
    Node getPrototype();

    /**
     * Transforms node to action, if possible.
     * @return Action or {@code null}
     */
    default Action toAction() {
        Action result = null;
        Node prototype = this.getPrototype();
        while (true) {
            if (prototype instanceof Action) {
                result = (Action) prototype;
                break;
            }
            if (prototype instanceof PrototypeBasedNode) {
                prototype = ((PrototypeBasedNode) prototype).getPrototype();
            } else {
                break;
            }
        }
        return result;
    }
}
