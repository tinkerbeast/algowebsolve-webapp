package com.algowebsolve.webapp.reactivemq;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class FixedMap<K, V> implements Map<K, V> {

    Map<K, V> map = null;
    FixedQueue<K> fixedQueue = null;
    int maxSize = -1;

    FixedMap(Map<K, V> map, int maxSize) {
        this.map = map;
        this.maxSize = maxSize;
        this.fixedQueue = new FixedQueue<>(new LinkedList<>(), maxSize);
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public boolean containsKey(Object o) {
        return this.map.containsKey(o);
    }

    @Override
    public boolean containsValue(Object o) {
        return this.map.containsValue(o);
    }

    @Override
    public V get(Object o) {
        return this.map.get(o);
    }

    @Override
    public V put(K k, V v) {
        if (this.map.size() >= this.maxSize) {
            K key = this.fixedQueue.remove();
            this.map.remove(key);
        }

        this.fixedQueue.add(k);
        return this.map.put(k, v);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> mapobj) {
        if (mapobj.size() > this.maxSize) {
            throw new UnsupportedOperationException("Invalid operation putAll on FixedMap for map size greater than maxSize");
        } else {
            for (Map.Entry<? extends K, ? extends V> item: mapobj.entrySet()) {
                this.put(item.getKey(), item.getValue());
            }
        }
    }

    @Override
    public V remove(Object o) {
        return this.map.remove(o);
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @Override
    public Set<K> keySet() {
        return this.map.keySet();
    }

    @Override
    public Collection<V> values() {
        return this.map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return this.map.entrySet();
    }
}
