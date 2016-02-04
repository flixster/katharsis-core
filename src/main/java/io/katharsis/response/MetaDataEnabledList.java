package io.katharsis.response;

import io.katharsis.response.MetaInformation;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Object to encapsulate the resource properties along with metadata
 * information allowing easier serialization to JSON.
 */
public class MetaDataEnabledList<T> implements List<T> {

    public MetaInformation metaInformation;
    public List<T> properties;

    public MetaDataEnabledList(List<T> properties) {
        this.properties = properties;
    }

    public void setMetaInformation(MetaInformation metaInformation) {
        this.metaInformation = metaInformation;
    }

    public MetaInformation getMetaInformation() {
        return this.metaInformation;
    }

    @Override
    public int size() {
        return properties.size();
    }

    @Override
    public boolean isEmpty() {
        return properties.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return properties.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return properties.iterator();
    }

    @Override
    public Object[] toArray() {
        return properties.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return properties.toArray(a);
    }

    @Override
    public boolean add(T t) {
        return properties.add(t);
    }

    @Override
    public boolean remove(Object o) {
        return properties.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return properties.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return properties.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        return properties.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return properties.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return properties.retainAll(c);
    }

    @Override
    public void clear() {
        properties.clear();
    }

    @Override
    public T get(int index) {
        return properties.get(index);
    }

    @Override
    public T set(int index, T element) {
        return properties.set(index, element);
    }

    @Override
    public void add(int index, T element) {
        properties.add(index, element);
    }

    @Override
    public T remove(int index) {
        return properties.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return properties.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return properties.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return properties.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return properties.listIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return properties.subList(fromIndex, toIndex);
    }
}
