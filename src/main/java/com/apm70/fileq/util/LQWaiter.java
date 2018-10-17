package com.apm70.fileq.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LQWaiter {
    private final String name;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = this.lock.newCondition();

    public LQWaiter(final String name) {
        this.name = name;
    }

    public void await() {
        this.lock.lock();
        try {
            LQWaiter.log.debug("++++++++await+++++++" + this.name);
            this.condition.await();
        } catch (final InterruptedException e) {
        } finally {
            this.lock.unlock();
        }
    }

    public void await(final long seconds) {
        this.lock.lock();
        try {
            LQWaiter.log.debug("++++++++await+++++++" + this.name);
            this.condition.await(seconds, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
        } finally {
            this.lock.unlock();
        }
    }

    public void signal() {
        this.lock.lock();
        try {
            LQWaiter.log.debug("++++++++signal+++++++" + this.name);
            this.condition.signal();
        } finally {
            this.lock.unlock();
        }
    }
}
