package edu.gemini.spModel.config;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.config2.*;

/**
 * Support for building a graph of sequence config information.
 */
public enum ConfigGraph {
    instance;

    /**
     * Specifies how to build the graph nodes.  Provided by the client to
     * create the graph.
     */
    public static interface Builder<T> {
        T container(ItemKey path, ImList<T> children);
        T leaf(ItemKey path, Object value);
    }

    private static final class Node {
        final ItemKey path;
        final ImList<ItemEntry> items;

        Node(ItemKey parent, ItemEntry item) {
            this.path  = immediateChild(parent, item.getKey());
            this.items = ImCollections.singletonList(item);
        }

        Node(ItemKey path, ImList<ItemEntry> items) {
            this.path  = path;
            this.items = items;
        }

        boolean isLeaf() {
            return (items.size() == 0) || items.head().getKey().equals(path);
        }

        private static ItemKey immediateChild(ItemKey parent, ItemKey decendent) {
            ItemKey up = decendent.getParent();
            if ((up == parent) || up.equals(parent)) return decendent;
            return immediateChild(parent, up);
        }

        Option<Node> add(ItemEntry ie) {
            if ((path != null) && !path.isParentOf(ie.getKey())) return None.instance();
            return new Some<Node>(new Node(path, items.cons(ie)));
        }

        ImList<Node> children() {
            if (isLeaf()) return ImCollections.emptyList();

            ItemEntry init = items.last();
            ImList<Node> seed = ImCollections.singletonList(new Node(path, init));

            return items.initial().foldRight(seed, new Function2<ItemEntry, ImList<Node>, ImList<Node>>() {
                @Override public ImList<Node> apply(ItemEntry cur, final ImList<Node> res) {
                    Node cp = res.head();
                    Option<Node> updated = cp.add(cur);
                    if (updated.isEmpty()) {
                        return res.cons(new Node(path, cur));
                    } else {
                        return res.tail().cons(updated.getValue());
                    }
                }
            });
        }
    }

    private <T> T graph(Node p, final Builder<T> builder) {
        if (p.isLeaf()) {
            return builder.leaf(p.path, p.items.head().getItemValue());
        } else {
            ImList<T> children = p.children().map(new MapOp<Node, T>() {
                @Override public T apply(Node cur) { return graph(cur, builder); }
            });
            return builder.container(p.path, children);
        }
    }

    /**
     * Builds a graph of sequence configuration information from the data in
     * the given Config.  The contained ItemKeys are broken down by path and
     * common parent keys form graph nodes that contain more specialized
     * configuration information.  The supplied builder is responsible for the
     * actual construction of the graph nodes, which are of parameterized type
     * T.  This method handles figuring out which bits of configuration data
     * form the leaves and parent nodes and passing them to the builder.
     */
    public <T> ImList<T> graph(Config c, final Builder<T> builder) {
        Node root = new Node(null, DefaultImList.create(c.itemEntries()));
        return root.children().map(new MapOp<Node, T>() {
            @Override public T apply(Node cur) { return graph(cur, builder); }
        });
    }
}
