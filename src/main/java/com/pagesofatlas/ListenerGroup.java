package com.pagesofatlas;

import java.util.*;
import java.util.function.Consumer;

/** Independent listener slots backed by an optional upstream single-listener notifier. */
public final class ListenerGroup {
    private final Map<Object, Runnable> listeners = new LinkedHashMap<>();
    private final Consumer<Runnable> upstream;
    public ListenerGroup() { this(null); }
    public ListenerGroup(Consumer<Runnable> upstream) { this.upstream = upstream; }
    public Consumer<Runnable> slot() {
        Object key = new Object();
        return listener -> {
            if (listener == null) listeners.remove(key); else listeners.put(key, listener);
            if (upstream != null) upstream.accept(listeners.isEmpty() ? null : this::fire);
        };
    }
    public void fire() { listeners.values().forEach(Runnable::run); }
}
