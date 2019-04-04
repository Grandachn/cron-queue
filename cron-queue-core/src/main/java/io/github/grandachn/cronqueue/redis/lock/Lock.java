package io.github.grandachn.cronqueue.redis.lock;

import java.util.concurrent.TimeUnit;

/**
 * @Author by guanda
 * @Date 2019/4/4 17:06
 */
public interface Lock {
    void lock(String name);
    void lock(String name, long leaseTime, TimeUnit unit);
    boolean tryLock(String name, long waitTime, long leaseTime, TimeUnit unit);
    void unlock(String name);
}
