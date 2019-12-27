package com.algowebsolve.webapp.reactivemq;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class TheadSafeMap<K, V> implements Map<K, V> {

    Map<K, V> map = null;

    TheadSafeMap(Map<K, V> map) {
        this.map = map;
    }

    @Override
    public synchronized int size() {
        return this.map.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public synchronized boolean containsKey(Object o) {
        return this.map.containsKey(o);
    }

    @Override
    public synchronized boolean containsValue(Object o) {
        return this.map.containsValue(o);
    }

    @Override
    public synchronized V get(Object o) {
        return this.map.get(o);
    }

    @Override
    public synchronized V put(K k, V v) {
        return this.map.put(k, v);
    }

    @Override
    public synchronized V remove(Object o) {
        return this.map.remove(o);
    }

    @Override
    public synchronized void putAll(Map<? extends K, ? extends V> map) {
        this.map.putAll(map);
    }

    @Override
    public synchronized void clear() {
        this.map.clear();
    }

    @Override
    public synchronized Set<K> keySet() {
        return this.map.keySet();
    }

    @Override
    public synchronized Collection<V> values() {
        return this.map.values();
    }

    @Override
    public synchronized Set<Entry<K, V>> entrySet() {
        return this.map.entrySet();
    }
}
