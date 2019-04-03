package io.github.grandachn.cronqueue.redis.rock;

import java.util.concurrent.TimeUnit;

/**
 * 基于redis的写锁
 * @Author by guanda
 * @Date 2019/4/3 15:05
 */
public class RedisWriteLock {
    public static void lock(long leaseTime, TimeUnit unit){

    }

    public static boolean tryLock(long waitTime, long leaseTime, TimeUnit unit){
        return true;
    }

    public static void unlock(){

    }
}
