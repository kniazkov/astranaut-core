package org.cqfn.astranaut.core.testing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.cqfn.astranaut.core.Action;
import org.cqfn.astranaut.core.Builder;
import org.cqfn.astranaut.core.ChildDescriptor;
import org.cqfn.astranaut.core.DifferenceNode;
import org.cqfn.astranaut.core.Fragment;
import org.cqfn.astranaut.core.Insert;
import org.cqfn.astranaut.core.Node;
import org.cqfn.astranaut.core.PatternNode;
import org.cqfn.astranaut.core.Type;
import org.cqfn.astranaut.core.algorithms.DeepTraversal;
import org.cqfn.astranaut.core.algorithms.DifferenceTreeBuilder;
import org.cqfn.astranaut.core.algorithms.Identical;
import org.cqfn.astranaut.core.algorithms.NodeSelector;
import org.cqfn.astranaut.core.algorithms.PatternBuilder;
import org.cqfn.astranaut.core.algorithms.Subtree;
import org.cqfn.astranaut.core.algorithms.mapping.TopDownMapper;
import org.cqfn.astranaut.core.algorithms.patching.PatternMatcher;
import org.cqfn.astranaut.core.exceptions.WrongFileExtension;
import org.cqfn.astranaut.core.utils.TreeVisualizer;

/**
 * Testing class.
 */
public class T06_PatternMatching {
    /**
     * Starting point.
     * @param args Program arguments
     */
    public static void main(String[] args) {
        if (args.length < 3) {
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

        final Node other;
        try {
            other = JavaParser.INSTANCE.read(args[2]);
        } catch (IOException ignored) {
            System.err.println("Could not parse " + args[2]);
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

        final DifferenceNode simplifiedDiffTree = new DifferenceNode(simplifiedBranch);
        final PatternBuilder patternBuilder = new PatternBuilder(new DifferenceNode(simplifiedDiffTree));

        final Set<Set<Node>> identical = new Identical(simplifiedDiffTree).get();
        final int[] holeNumber = new int[1];
        holeNumber[0] = 1;
        final Set<Node> transformedToHoles = new HashSet<>();
        for (final Set<Node> set : identical) {
            final Iterator<Node> identicalIterator = set.iterator();
            final Node couldBeAHole = identicalIterator.next();
            if (couldBeAHole.getTypeName().equals("SimpleName")) {
                patternBuilder.makeHole(couldBeAHole, holeNumber[0]);
                transformedToHoles.add(couldBeAHole);
                while (identicalIterator.hasNext()) {
                    final Node nextHolePrototype = identicalIterator.next();
                    patternBuilder.makeHole(nextHolePrototype, holeNumber[0]);
                    transformedToHoles.add(nextHolePrototype);
                }
                holeNumber[0]++;
            }
        }

        new DeepTraversal(simplifiedDiffTree).findAll(node -> {
            final String typename = node.getTypeName();
            if ((typename.equals("Modifier") || typename.equals("SimpleName")) &&
                    !transformedToHoles.contains(node)) {
                patternBuilder.makeHole(node, holeNumber[0]);
                holeNumber[0]++;
            }
            return false;
        });

        final PatternNode pattern = patternBuilder.getRoot();

        final PatternMatcher matcher = new PatternMatcher(other);
        final Set<Node> matched = matcher.match(pattern);

        String image = args[2] + ".matched.png";
        TreeVisualizer visualizer = new TreeVisualizer(new HighlightedNode(other, matched));
        try {
            visualizer.visualize(new File(image));
        } catch (IOException | WrongFileExtension ignored) {
            System.err.println("Could not write " + image);
        }
    }

    static class HighlightedNode implements Node {
        private final Node prototype;

        private final List<HighlightedNode> children;

        private final TypeImpl type;

        HighlightedNode(Node prototype, Set<Node> highlighted) {
            this.prototype = prototype;
            this.children = new ArrayList<>(prototype.getChildCount());
            for (Node child : prototype) {
                this.children.add(new HighlightedNode(child, highlighted));
            }
            this.type = new TypeImpl(prototype.getType(), highlighted.contains(prototype));
        }

        @Override
        public Fragment getFragment() {
            return prototype.getFragment();
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public String getData() {
            return prototype.getData();
        }

        @Override
        public int getChildCount() {
            return children.size();
        }

        @Override
        public Node getChild(int index) {
            return children.get(index);
        }

        private static class TypeImpl implements Type {
            private final Type prototype;

            private final boolean highlighted;

            private TypeImpl(Type prototype, boolean highlighted) {
                this.prototype = prototype;
                this.highlighted = highlighted;
            }

            @Override
            public String getName() {
                return prototype.getName();
            }

            @Override
            public List<ChildDescriptor> getChildTypes() {
                return prototype.getChildTypes();
            }

            @Override
            public List<String> getHierarchy() {
                return prototype.getHierarchy();
            }

            @Override
            public String getProperty(String name) {
                if (highlighted && name.equals("color")) {
                    return "red";
                }
                return prototype.getProperty(name);
            }

            @Override
            public Builder createBuilder() {
                return prototype.createBuilder();
            }
        }
    }
}
