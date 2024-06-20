package org.cqfn.astranaut.core.testing;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import org.cqfn.astranaut.core.Action;
import org.cqfn.astranaut.core.DifferenceNode;
import org.cqfn.astranaut.core.Insert;
import org.cqfn.astranaut.core.Node;
import org.cqfn.astranaut.core.algorithms.DeepTraversal;
import org.cqfn.astranaut.core.algorithms.DifferenceTreeBuilder;
import org.cqfn.astranaut.core.algorithms.NodeSelector;
import org.cqfn.astranaut.core.algorithms.Subtree;
import org.cqfn.astranaut.core.algorithms.mapping.TopDownMapper;
import org.cqfn.astranaut.core.exceptions.WrongFileExtension;
import org.cqfn.astranaut.core.utils.TreeVisualizer;

/**
 * Testing class.
 */
public class T04_SimplifyBranch {
    /**
     * Starting point.
     * @param args Program arguments
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Not enough arguments");
            return;
        }

        final Node before;
        try {
            before = JavaParser.INSTANCE.read(args[0]);
        } catch (IOException ignored) {
            System.err.println("Could not parse " + args[0]);
            return;
        }

        final Node after;
        try {
            after = JavaParser.INSTANCE.read(args[1]);
        } catch (IOException ignored) {
            System.err.println("Could not parse " + args[1]);
            return;
        }

        final DifferenceTreeBuilder diffBuilder = new DifferenceTreeBuilder(before);
        diffBuilder.build(after, TopDownMapper.INSTANCE);

        final DifferenceNode diff = diffBuilder.getRoot();

        Set<Node> branches = new NodeSelector(diff)
                .select((node, parents) -> node.belongsToGroup("MethodDeclaration"));
        Node selectedBranch = null;
        for (Node branch : branches) {
            Optional<Node> action = new DeepTraversal(branch)
                    .findFirst(node -> node instanceof Action);
            if (action.isPresent()) {
                selectedBranch = branch;
                break;
            }
        }

        if (selectedBranch == null) {
            System.out.println("No branches found");
            return;
        }

        final Set<Node> saved = new HashSet<>();
        new NodeSelector(selectedBranch).select((node, parents) -> {
            if (node instanceof Action) {
                saved.add(node);
                new DeepTraversal(node).findAll(child -> {
                    saved.add(child);
                    return false;
                });
                Iterator<Node> parentsIterator = parents.iterator();
                if (node instanceof Insert) {
                    final Node parent = parentsIterator.next();
                    new DeepTraversal(parent).findAll(child -> {
                        saved.add(child);
                        return false;
                    });
                }
                while (parentsIterator.hasNext()) {
                    final Node parent = parentsIterator.next();
                    saved.add(parent);
                    for (Node relative : parent) {
                        if (!saved.contains(relative)) {
                            final String typename = relative.getTypeName();
                            if (typename.equals("Modifier") || typename.equals("SimpleName")
                                    || typename.equals("VoidType")) {
                                saved.add(relative);
                            }
                        }
                    }
                }
            }
            return false;
        });

        final Subtree subtree = new Subtree(selectedBranch, Subtree.INCLUDE);
        final Node simplifiedBranch = subtree.create(saved);

        String image = args[0] + ".simplified_branch.png";
        TreeVisualizer visualizer = new TreeVisualizer(simplifiedBranch);
        try {
            visualizer.visualize(new File(image));
        } catch (IOException | WrongFileExtension ignored) {
            System.err.println("Could not write " + image);
        }
    }
}
