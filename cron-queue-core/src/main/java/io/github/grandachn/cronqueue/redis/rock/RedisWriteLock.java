package io.github.grandachn.cronqueue.redis.rock;

import io.github.grandachn.cronqueue.redis.JedisTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 基于redis的写锁
 * @Author by guanda
 * @Date 2019/4/3 15:05
 */
@Slf4j
public class RedisWriteLock {
    public static void lock(String name){
        tryLock(name, Long.MAX_VALUE, 30, TimeUnit.SECONDS);
    }

    public static boolean tryLock(String name, long waitTime, long leaseTime, TimeUnit unit){
        Long waitUntilTime = unit.toMillis(waitTime) + System.currentTimeMillis();
        Long leastTimeLong = unit.toMillis(leaseTime);
        StringBuilder sctipt = new StringBuilder();
        // write-lock read-lock uuid leaseTime
        sctipt.append("if redis.call('SET',KEYS[1],ARGV[1],'NX','PX',ARGV[2]) then ")
                .append("return 1;")
                .append("else ")
                    .append("if (redis.call('GET',KEYS[1])== ARGV[1]) then ")
                        .append("return 2;")//锁重入，待优化
                    .append("else ")
                        .append("return 0;")
                    .append("end;")
                .append("end;");
        for(;;){
            if(System.currentTimeMillis() > waitUntilTime){
                return false;
            }
            Long res = (Long) JedisTemplate.operate().eval(sctipt.toString(), 1, RedisReadWriteLock.getWriteLockKey(name), RedisReadWriteLock.getThreadUid(), leastTimeLong.toString());
            if(res.equals(1L)){
                //successGetWriteLock
                log.info("success get write lock,  writeLock={}", RedisReadWriteLock.getWriteLockKey(name));
                for(;;){
                    if(JedisTemplate.operate().exists(RedisReadWriteLock.getReadLockKey(name))){
                        log.info("wait read lock release,  readLock={}", RedisReadWriteLock.getReadLockKey(name));
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
            }else if(res.equals(2L)){
                System.out.println("锁重入，待优化");
                break;
            }else{
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

    public static void unlock(){

    }

    public static void main(String[] args) {
        tryLock("a", 30,300, TimeUnit.SECONDS);
        tryLock("a", 30,300, TimeUnit.SECONDS);
    }
}
