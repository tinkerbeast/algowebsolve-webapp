package com.algowebsolve.webapp.reactivemq;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

public class FixedQueue<E> implements Queue<E> {

    Queue<E> queue = null;
    int maxSize = -1;

    FixedQueue(Queue<E> queue, int maxSize) {
        this.queue = queue;
        this.maxSize = maxSize;
    }

    @Override
    public int size() {
        return this.queue.size();
    }

    @Override
    public boolean isEmpty() {
        return this.queue.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.queue.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return this.queue.iterator();
    }

    @Override
    public Object[] toArray() {
        return new this.queue.toArray();
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        return this.queue.toArray(ts);
    }

    @Override
    public boolean add(E e) {
        if (this.queue.size() >= maxSize) {
            E item = this.queue.remove();
        }

        return  this.queue.add(e);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        for (E item: collection) {
            this.add(item);
        }
    }

    @Override
    public boolean offer(E e) {
        if (this.queue.size() >= maxSize) {
            E item = this.queue.poll();
            if (item == null) {
                return  false;
            }
        }

        return  this.queue.offer(e);
    }

    @Override
    public boolean remove(Object o) {
        return this.queue.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return this.queue.containsAll(collection);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return this.queue.retainAll(collection);
    }

    @Override
    public void clear() {
        this.queue.clear();
    }

    @Override
    public E remove() {
        return this.queue.remove();
    }

    @Override
    public E poll() {
        return this.queue.poll();
    }

    @Override
    public E element() {
        return this.queue.element();
    }

    @Override
    public E peek() {
        return this.queue.peek();
    }
}
