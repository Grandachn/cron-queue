package io.github.grandachn.cronqueue.redis.lock;

import io.github.grandachn.cronqueue.redis.JedisTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 基于redis的写锁
 * @Author by guanda
 * @Date 2019/4/3 15:05
 */
@Slf4j
public class RedisWriteLock implements Lock{
    @Override
    public void lock(String name){
        tryLock(name, Long.MAX_VALUE, 30, TimeUnit.SECONDS);
    }

    @Override
    public void lock(String name, long leaseTime, TimeUnit unit){
        tryLock(name, Long.MAX_VALUE, leaseTime, unit);
    }

    @Override
    public boolean tryLock(String name, long waitTime, long leaseTime, TimeUnit unit){
        Long waitUntilTime = unit.toMillis(waitTime) + System.currentTimeMillis();
        if(waitUntilTime < 0){
            waitUntilTime = Long.MAX_VALUE;
        }
        Long leastTimeLong = unit.toMillis(leaseTime);
        StringBuilder sctipt = new StringBuilder();
        // write-lock reentrant-write-lock uuid leaseTime
        sctipt.append("if redis.call('SET',KEYS[1],ARGV[1],'NX','PX',ARGV[2]) then ")
                    .append("redis.call('SET',KEYS[2],1,'PX',ARGV[2]);")
                    .append("return 1;")
                .append("else ")
                    .append("if (redis.call('GET',KEYS[1])== ARGV[1]) then ")
                        .append("local count = redis.call('GET',KEYS[2]);")
                        .append("if not count then ")
                            .append("redis.call('SET',KEYS[2],1,'PX',ARGV[2]);")
                            .append("return 1;")
                        .append("else ")
                            .append("count = tonumber(count) + 1;")
                            .append("redis.call('SET',KEYS[2],count,'PX',ARGV[2]);")
                            .append("return count;")
                        .append("end;")
                    .append("else ")
                        .append("return 0;")
                    .append("end;")
                .append("end;");
        for(;;){
            if(System.currentTimeMillis() > waitUntilTime){
                return false;
            }
            Long res = (Long) JedisTemplate.operate().eval(sctipt.toString(), 2, RedisReadWriteLock.getWriteLockKey(name), RedisReadWriteLock.getReentrantWriteLockKey(name), RedisReadWriteLock.getThreadUid(), leastTimeLong.toString());
            if(res.equals(1L)){
                //successGetWriteLock
                log.debug("success get write lock,  writeLock = {}", RedisReadWriteLock.getWriteLockKey(name));
                for(;;){
                    if(JedisTemplate.operate().exists(RedisReadWriteLock.getReadLockKey(name))){
                        log.debug("wait read lock release,  readLock = {}", RedisReadWriteLock.getReadLockKey(name));
                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                        } catch (InterruptedException e) {
                            log.error("wait read lock release exception", e);
                        }
                    }else{
                        break;
                    }
                }
                break;
            }else if(res.equals(0L)){
                //need to wait write lock to be released
                log.debug("wait write lock release,  writeLock = {}", RedisReadWriteLock.getWriteLockKey(name));
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    log.error("wait write lock release exception", e);
                }
            }else{
                log.debug("success in reentrant write lock,  reentrantWriteLock = {}, count now = {}", RedisReadWriteLock.getReentrantWriteLockKey(name), res);
                break;
            }
        }
        return true;
    }

    @Override
    public void unlock(String name){
        StringBuilder sctipt = new StringBuilder();
        //write-lock reentrant-write-lock uuid
        sctipt.append("if (redis.call('GET',KEYS[1])== ARGV[1]) then ")
                    .append("local count = redis.call('GET',KEYS[2]);")
                    .append("if count then ")
                        .append("if (tonumber(count) > 1) then ")
                            .append("count = tonumber(count) - 1;")
                            .append("local live = redis.call('PTTL',KEYS[2]);")
                            .append("redis.call('SET',KEYS[2],count,'PX',live);")
                            //success unlock reentrant-write-lock
                            .append("return count;")
                        .append("else ")
                            .append("redis.call('DEL',KEYS[2]);")
                            .append("redis.call('DEL',KEYS[1]);")
                            //success unlock
                            .append("return 0;")
                        .append("end;")
                    .append("else ")
                        .append("redis.call('DEL',KEYS[1]);")
                        .append("return 0;")
                    .append("end;")
                .append("else ")
                    //fail unlock, thread not get the lock
                    .append("return -1;")
                .append("end;");
        Long res = (Long) JedisTemplate.operate().eval(sctipt.toString(), 2, RedisReadWriteLock.getWriteLockKey(name), RedisReadWriteLock.getReentrantWriteLockKey(name), RedisReadWriteLock.getThreadUid());
        if(res.equals(0L)){
            log.debug("success unlock write lock,  writeLock = {}", RedisReadWriteLock.getWriteLockKey(name));
        }else if(res.equals(-1L)){
            log.debug("fail unlock, thread not get the lock,  writeLock = {}, thread = {}", RedisReadWriteLock.getReentrantWriteLockKey(name), RedisReadWriteLock.getThreadUid());
        }else {
            log.debug("success unlock reentrant write lock,  reentrantWriteLock = {}, count left = {}", RedisReadWriteLock.getReentrantWriteLockKey(name), res);
        }
    }

}
