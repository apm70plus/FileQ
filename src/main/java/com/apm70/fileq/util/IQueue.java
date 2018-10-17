package com.apm70.fileq.util;

public interface IQueue<E> {

    int size();

    boolean add(E e);

    E poll();

    E peek();
}
