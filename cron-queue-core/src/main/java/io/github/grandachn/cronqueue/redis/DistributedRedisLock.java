package io.github.grandachn.cronqueue.redis;

import io.github.grandachn.cronqueue.conf.RedissonConf;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.config.Config;

import java.util.concurrent.TimeUnit;

/**
 * redis 分布式锁 (推荐使用本项目实现的RedisReadWriteLock)
 * @Author by guanda
 * @Date 2019/3/29 17:36
 */
@Slf4j
@Deprecated
public class DistributedRedisLock {
    private static Redisson redisson;

    private static final String LOCK_TITLE = "redisLock_";

    static {
        Config config = new Config();
        if(!RedissonConf.PASSWORD.equals("")){
            config.useSingleServer()
                    .setAddress("redis://"+ RedissonConf.ADDRESS + ":" + RedissonConf.PORT)
                    .setPassword(RedissonConf.PASSWORD)
                    .setPingConnectionInterval(60);
        }else{
            config.useSingleServer()
                    .setAddress("redis://"+ RedissonConf.ADDRESS + ":" + RedissonConf.PORT)
                    .setPingConnectionInterval(60);
        }
        redisson = (Redisson) Redisson.create(config);
    }

    public static boolean acquire(String lockName){
        String key = LOCK_TITLE + lockName;
        RLock lock = redisson.getLock(key);
        lock.lock(2, TimeUnit.MINUTES);
        log.debug("[redisLock lock] lockKey={}, thread={}", key, Thread.currentThread().getName());
        return true;
    }

    public static boolean tryAcquire(String lockName){
        String key = LOCK_TITLE + lockName;
        RLock lock = redisson.getLock(key);
        try {
            boolean res = lock.tryLock(0,10, TimeUnit.SECONDS);
            if(res){
                log.debug("[redisLock tryAcquire true] lockKey={}, thread={}", key, Thread.currentThread().getName());
                return true;
            }
        } catch (InterruptedException e) {
            log.error("[redisLock 获取失败]", e);
        }
        log.debug("[redisLock 获取失败] lockKey={}, thread={}", key, Thread.currentThread().getName());
        return false;
    }

    public  static void release(String lockName){
        String key = LOCK_TITLE + lockName;
        RLock mylock = redisson.getLock(key);
        mylock.unlock();
        log.debug("[redisLock unlock] lockKey={}, thread={}", key, Thread.currentThread().getName());
    }

    public static void releaseReadLock(String lockName){
        String key = LOCK_TITLE + lockName;
        RReadWriteLock mylock = redisson.getReadWriteLock(key);
        mylock.readLock().unlock();
        log.debug("[redisReadLock unlock] lockKey={}, thread={}", key, Thread.currentThread().getName());
    }

    public static void releaseWriteLock(String lockName){
        String key = LOCK_TITLE + lockName;
        RReadWriteLock mylock = redisson.getReadWriteLock(key);
        mylock.writeLock().unlock();
        log.debug("[redisWriteLock unlock] lockKey={}, thread={}", key, Thread.currentThread().getName());
    }

    public static void acquireReadLock(String lockName){
        String key = LOCK_TITLE + lockName;
        RReadWriteLock lock = redisson.getReadWriteLock(key);
        lock.readLock().lock(20, TimeUnit.SECONDS);
        log.debug("[redisReadLock lock] lockKey={}, thread={}", key, Thread.currentThread().getName());
    }

    public static void acquireWriteLock(String lockName){
        String key = LOCK_TITLE + lockName;
        RReadWriteLock lock = redisson.getReadWriteLock(key);
        lock.writeLock().lock(20, TimeUnit.SECONDS);
        log.debug("[redisWriteLock lock] lockKey={}, thread={}", key, Thread.currentThread().getName());
    }

}
