package com.moepus.flerovium.functions;

import java.util.Arrays;

public class IntFlatMap<V> {
    private int[] keys;
    private V[] values;
    private int size = 0;

    @SuppressWarnings("unchecked")
    public IntFlatMap(int capacity) {
        this.keys = new int[capacity];
        this.values = (V[]) new Object[capacity];
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public V get(int key) {
        int index = findIndex(key);
        return index >= 0 ? values[index] : null;
    }

    public void put(int key, V value) {
        if (size >= keys.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        keys[size] = key;
        values[size] = value;
        size++;
    }

    public V remove(int key) {
        int index = findIndex(key);
        if (index < 0) {
            return null;
        }
        V oldValue = values[index];
        shift(index);
        return oldValue;
    }

    private int findIndex(int key) {
        return Arrays.binarySearch(keys, key);
    }

    private void shift(int index) {
        System.arraycopy(keys, index + 1, keys, index, size - index - 1);
        System.arraycopy(values, index + 1, values, index, size - index - 1);
        values[size - 1] = null;
        size--;
    }

    public void resize(int newCapacity) {
        keys = Arrays.copyOf(keys, newCapacity);
        values = Arrays.copyOf(values, newCapacity);
    }

    public void sort() {
        if (size != keys.length) {
            resize(size);
        }
        quickSort(0, size - 1);
    }

    private void quickSort(int low, int high) {
        if (low < high) {
            int pi = partition(low, high);
            quickSort(low, pi - 1);
            quickSort(pi + 1, high);
        }
    }

    private int partition(int low, int high) {
        int pivot = keys[high];
        int i = (low - 1);
        for (int j = low; j < high; j++) {
            if (keys[j] < pivot) {
                i++;
                swap(i, j);
            }
        }
        swap(i + 1, high);
        return i + 1;
    }

    private void swap(int i, int j) {
        int tempintey = keys[i];
        keys[i] = keys[j];
        keys[j] = tempintey;

        V tempValue = values[i];
        values[i] = values[j];
        values[j] = tempValue;
    }

    public void clear() {
        Arrays.fill(keys, 0, size, 0);
        Arrays.fill(values, 0, size, null);
        size = 0;
    }
}