package io.github.grandachn.cronqueue.redis.rock;

import io.github.grandachn.cronqueue.exception.CommonException;
import io.github.grandachn.cronqueue.redis.JedisTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 基于redis的读锁
 * @Author by guanda
 * @Date 2019/4/3 15:05
 */
@Slf4j
public class RedisReadLock {
    public static void lock(String name, long leaseTime, TimeUnit unit){
        tryLock(name, Long.MAX_VALUE, leaseTime, TimeUnit.SECONDS);
    }

    public static boolean tryLock(String name, long waitTime, long leaseTime, TimeUnit unit){
        Long waitUntilTime = unit.toMillis(waitTime) + System.currentTimeMillis();
        Long leastTimeLong = unit.toMillis(leaseTime);
        StringBuilder sctipt = new StringBuilder();
        // write-lock read-lock uuid leaseTime
        sctipt.append("if not redis.call('GET',KEYS[1]) then ")
                .append("redis.call('HSET',KEYS[2],KEYS[3],ARGV[1]);")
                .append("local t = redis.call('PTTL', KEYS[2]);")
                .append("redis.call('PEXPIRE', KEYS[2], math.max(t, ARGV[1]));")
                .append("return 1;")
                .append("else ")
                .append("return 0;")
                .append("end;");
        Long res;
        for(;;){
            if(System.currentTimeMillis() > waitUntilTime){
                return false;
            }
            res = (Long) JedisTemplate.operate().eval(sctipt.toString(), 3, RedisReadWriteLock.getWriteLockKey(name), RedisReadWriteLock.getReadLockKey(name), RedisReadWriteLock.getThreadUid(), leastTimeLong.toString());
            if(res.equals(1L)){
                //successGetReadLock
                log.info("success get read lock,  readLock={}", RedisReadWriteLock.getReadLockKey(name));
                break;
            }else {
                //need to wait write lock to be released
                log.info("wait write lock release,  writeLock={}", RedisReadWriteLock.getWriteLockKey(name));
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    log.error("wait write lock release exception", e);
                }
            }
        }
        return true;
    }

    public void unlock(){

    }

    public static void main(String[] args) {
        tryLock("b", 30,300, TimeUnit.SECONDS);
    }
}
