package org.cqfn.astranaut.core.testing;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import java.io.IOException;
import java.nio.file.Paths;
import org.cqfn.astranaut.core.Node;

/**
 * Parses Java source code to syntax tree.
 */
public class JavaParser implements Reader {
    /**
     * Instance.
     */
    public static final Reader INSTANCE = new JavaParser();

    /**
     * Private constructor.
     */
    private JavaParser() {
    }

    @Override
    public Node read(String path) throws IOException {
        final CompilationUnit raw = StaticJavaParser.parse(Paths.get(path));
        final JavaRawToNodeConverter converter = new JavaRawToNodeConverter();
        return converter.convert(raw);
    }
}
