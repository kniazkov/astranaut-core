package org.cqfn.astranaut.core.testing;

import java.io.IOException;
import org.cqfn.astranaut.core.Node;

/**
 * Interface for reading sources and converting them to ASTranaut format.
 */
public interface Reader {
    /**
     * Reads the source file and converts it into an ASTranaut format tree.
     * @param path Path to source file
     * @return Root of the tree
     */
    Node read(String path) throws IOException;
}
