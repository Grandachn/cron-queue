package io.github.grandachn.cronqueue.redis;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.config.Config;

import java.util.concurrent.TimeUnit;

/**
 * redis 分布式锁
 * @Author by guanda
 * @Date 2019/3/29 17:36
 */
@Slf4j
public class DistributedRedisLock {
    //从配置类中获取redisson对象
    private static Redisson redisson;

    private static final String LOCK_TITLE = "redisLock_";

    static {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://10.11.9.123:6379").setPassword("crs-e7mw5os6:c2G1t2BVr5#B");
        redisson = (Redisson) Redisson.create(config);
    }

    //加锁
    public static boolean acquire(String lockName){
        //声明key对象
        String key = LOCK_TITLE + lockName;
        //获取锁对象
        RLock lock = redisson.getLock(key);
        //加锁，并且设置锁过期时间，防止死锁的产生
        lock.lock(2, TimeUnit.MINUTES);

        log.debug("[redisLock lock] lockKey={}, thread={}", key, Thread.currentThread().getName());
        return true;
    }

    //加锁
    public static boolean tryAcquire(String lockName){
        //声明key对象
        String key = LOCK_TITLE + lockName;
        //获取锁对象
        RLock lock = redisson.getLock(key);
        //加锁，并且设置锁过期时间，防止死锁的产生
//        mylock.lock(2, TimeUnit.MINUTES);
        boolean res = false;
        try {
            res = lock.tryLock(0, 10, TimeUnit.SECONDS);
            if(res){ //成功
                log.debug("[redisLock lock] lockKey={}, thread={}", key, Thread.currentThread().getName());
                return true;
            }
        } catch (InterruptedException e) {
            log.error("[redisLock 获取失败]", e);
        }
        log.info("[redisLock 获取失败] lockKey={}, thread={}", key, Thread.currentThread().getName());
        return false;
    }

    //锁的释放
    public static void release(String lockName){
        //必须是和加锁时的同一个key
        String key = LOCK_TITLE + lockName;
        //获取所对象
        RLock mylock = redisson.getLock(key);
        //释放锁（解锁）
        mylock.unlock();
        log.debug("[redisLock unlock] lockKey={}, thread={}", key, Thread.currentThread().getName());
    }
}
