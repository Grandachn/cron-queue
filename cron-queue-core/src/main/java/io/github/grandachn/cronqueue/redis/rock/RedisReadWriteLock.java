package io.github.grandachn.cronqueue.redis.rock;

import io.github.grandachn.cronqueue.conf.RedisLockConf;
import io.github.grandachn.cronqueue.redis.JedisConnectPoll;

import java.util.UUID;

/**
 * 基于redis的读写锁（可重入）
 * @Author by guanda
 * @Date 2019/4/3 14:58
 */
public class RedisReadWriteLock {
    private static volatile RedisReadLock redisReadLock;
    private static volatile RedisWriteLock redisWriteLock;

    public static RedisReadLock readLock(){
        if(redisReadLock == null){
            synchronized (RedisReadLock.class){
                if (redisReadLock == null){
                    redisReadLock = new RedisReadLock();
                }
            }
        }
        return redisReadLock;
    }

    public RedisWriteLock writeLock(){
        if(redisWriteLock == null){
            synchronized (RedisWriteLock.class){
                if (redisWriteLock == null){
                    redisWriteLock = new RedisWriteLock();
                }
            }
        }
        return redisWriteLock;
    }

    public static String getReadLockKey(String name){
        return RedisLockConf.READ_LOCK_PREFIX + name;
    }

    public static String getWriteLockKey(String name){
        return RedisLockConf.WRITE_LOCK_PREFIX + name;
    }

    public static String getThreadUid(){
        return JedisConnectPoll.JEDIS_CONNECT_POLL_UUID.toString() + ":" + Thread.currentThread().getId();
    }
}
