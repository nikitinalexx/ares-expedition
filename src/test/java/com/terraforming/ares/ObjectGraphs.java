package com.terraforming.ares;

import com.terraforming.ares.model.Card;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Reflective helpers for checking game copies: a canonical dump of an object graph (to find fields that a copy
 * constructor forgot) and an alias detector (to find mutable objects shared between the original and its copy).
 * Cards are shared singletons by design and are treated as values.
 */
public final class ObjectGraphs {
    private ObjectGraphs() {
    }

    /**
     * path -> value of every leaf in the graph.
     */
    public static Map<String, String> dump(Object root) {
        Map<String, String> out = new TreeMap<>();
        walk(root, "$", out, new IdentityHashMap<>());
        return out;
    }

    /**
     * Paths where the two dumps differ (missing on one side counts as a difference).
     */
    public static List<String> diff(Map<String, String> expected, Map<String, String> actual) {
        List<String> diffs = new ArrayList<>();
        for (Map.Entry<String, String> e : expected.entrySet()) {
            String other = actual.get(e.getKey());
            if (!e.getValue().equals(other)) {
                diffs.add(e.getKey() + ": " + e.getValue() + "  !=  " + other);
            }
        }
        for (String key : actual.keySet()) {
            if (!expected.containsKey(key)) {
                diffs.add(key + ": <missing>  !=  " + actual.get(key));
            }
        }
        return diffs;
    }

    /**
     * Mutable objects (collections, maps, arrays, model beans) reachable from both graphs by the same identity.
     */
    public static List<String> sharedMutableObjects(Object a, Object b) {
        Map<Object, String> inA = new IdentityHashMap<>();
        collectMutable(a, "$", inA, new IdentityHashMap<>());
        Map<Object, String> inB = new IdentityHashMap<>();
        collectMutable(b, "$", inB, new IdentityHashMap<>());
        List<String> shared = new ArrayList<>();
        for (Map.Entry<Object, String> e : inA.entrySet()) {
            String pathInB = inB.get(e.getKey());
            if (pathInB != null) {
                shared.add(e.getValue() + " (same object as copy" + pathInB.substring(1) + ", " + e.getKey().getClass().getSimpleName() + ")");
            }
        }
        return shared;
    }

    private static boolean isLeaf(Object o) {
        Class<?> c = o.getClass();
        return c.isPrimitive() || o instanceof Number || o instanceof Boolean || o instanceof Character
                || o instanceof CharSequence || o instanceof Enum<?> || o instanceof Class<?>
                || o instanceof Card || c.getName().startsWith("java.time");
    }

    private static void walk(Object o, String path, Map<String, String> out, Map<Object, Boolean> seen) {
        if (o == null) {
            out.put(path, "null");
            return;
        }
        if (isLeaf(o)) {
            out.put(path, o instanceof Class<?> c ? c.getSimpleName() : (o instanceof Card ? "Card:" + o.getClass().getSimpleName() : String.valueOf(o)));
            return;
        }
        if (seen.put(o, true) != null) {
            out.put(path, "<seen>");
            return;
        }
        if (o instanceof Map<?, ?> m) {
            out.put(path + ".size", String.valueOf(m.size()));
            Map<String, Object> sorted = new TreeMap<>();
            m.forEach((k, v) -> sorted.put(keyName(k), v));
            sorted.forEach((k, v) -> walk(v, path + "[" + k + "]", out, seen));
        } else if (o instanceof Collection<?> c) {
            out.put(path + ".size", String.valueOf(c.size()));
            int i = 0;
            for (Object item : c) {
                walk(item, path + "[" + i++ + "]", out, seen);
            }
        } else if (o.getClass().isArray()) {
            int n = Array.getLength(o);
            out.put(path + ".length", String.valueOf(n));
            for (int i = 0; i < n; i++) {
                walk(Array.get(o, i), path + "[" + i + "]", out, seen);
            }
        } else {
            for (Field f : fields(o.getClass())) {
                try {
                    walk(f.get(o), path + "." + f.getName(), out, seen);
                } catch (IllegalAccessException | RuntimeException e) {
                    out.put(path + "." + f.getName(), "<unreadable " + e.getClass().getSimpleName() + ">");
                }
            }
        }
    }

    private static String keyName(Object k) {
        return k instanceof Class<?> c ? c.getSimpleName() : String.valueOf(k);
    }

    private static void collectMutable(Object o, String path, Map<Object, String> out, Map<Object, Boolean> seen) {
        if (o == null || isLeaf(o) || seen.put(o, true) != null) {
            return;
        }
        boolean container = o instanceof Collection<?> || o instanceof Map<?, ?> || o.getClass().isArray();
        boolean bean = !container && o.getClass().getName().startsWith("com.terraforming.ares");
        if (container && !isImmutableContainer(o) || bean) {
            out.put(o, path);
        }
        if (o instanceof Map<?, ?> m) {
            m.forEach((k, v) -> collectMutable(v, path + "[" + keyName(k) + "]", out, seen));
        } else if (o instanceof Collection<?> c) {
            int i = 0;
            for (Object item : c) {
                collectMutable(item, path + "[" + i++ + "]", out, seen);
            }
        } else if (o.getClass().isArray() && !o.getClass().getComponentType().isPrimitive()) {
            for (int i = 0; i < Array.getLength(o); i++) {
                collectMutable(Array.get(o, i), path + "[" + i + "]", out, seen);
            }
        } else if (bean) {
            for (Field f : fields(o.getClass())) {
                try {
                    collectMutable(f.get(o), path + "." + f.getName(), out, seen);
                } catch (IllegalAccessException | RuntimeException ignored) {
                }
            }
        }
    }

    // List.of(), Set.of(), Collections.unmodifiable*: sharing them is harmless
    private static boolean isImmutableContainer(Object o) {
        String n = o.getClass().getName();
        return n.startsWith("java.util.ImmutableCollections") || n.contains("Unmodifiable") || n.startsWith("java.util.Collections$Empty")
                || n.startsWith("java.util.Collections$Singleton");
    }

    private static List<Field> fields(Class<?> type) {
        List<Field> result = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers()) || f.isSynthetic()) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    result.add(f);
                } catch (RuntimeException ignored) {
                }
            }
        }
        return result;
    }
}
