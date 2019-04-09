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
    private static String tryLockScript;

    private static String unLockScript;

    private static String tryLockScriptSha;

    private static String unLockScriptSha;

    RedisWriteLock(){
        // write-lock reentrant-write-lock uuid leaseTime
        tryLockScript = "if redis.call('SET',KEYS[1],ARGV[1],'NX','PX',ARGV[2]) then " +
                            "redis.call('SET',KEYS[2],1,'PX',ARGV[2]);" +
                            "return 1;" +
                        "else " +
                            "if (redis.call('GET',KEYS[1])== ARGV[1]) then " +
                                "local count = redis.call('GET',KEYS[2]);" +
                                "if not count then " +
                                    "redis.call('SET',KEYS[2],1,'PX',ARGV[2]);" +
                                    "return 1;" +
                                "else " +
                                    "count = tonumber(count) + 1;" +
                                    "redis.call('SET',KEYS[2],count,'PX',ARGV[2]);" +
                                    "return count;" +
                                "end;" +
                            "else " +
                                "return 0;" +
                            "end;" +
                        "end;";
        if(tryLockScriptSha == null || "".equals(tryLockScriptSha)){
            tryLockScriptSha = JedisTemplate.operate().scriptLoad(tryLockScript);
        }

        //write-lock reentrant-write-lock uuid
        unLockScript = "if (redis.call('GET',KEYS[1])== ARGV[1]) then " +
                            "local count = redis.call('GET',KEYS[2]);" +
                            "if count then " +
                                "if (tonumber(count) > 1) then " +
                                    "count = tonumber(count) - 1;" +
                                    "local live = redis.call('PTTL',KEYS[2]);" +
                                    "redis.call('SET',KEYS[2],count,'PX',live);" +
                                    //success unlock reentrant-write-lock
                                    "return count;" +
                                "else " +
                                    "redis.call('DEL',KEYS[2]);" +
                                    "redis.call('DEL',KEYS[1]);" +
                                    //success unlock
                                    "return 0;" +
                                "end;" +
                            "else " +
                                "redis.call('DEL',KEYS[1]);" +
                            "return 0;" +
                            "end;" +
                         "else " +
                            //fail unlock, thread not get the lock
                            "return -1;" +
                         "end;";
        if(unLockScriptSha == null || "".equals(unLockScriptSha)){
            unLockScriptSha = JedisTemplate.operate().scriptLoad(unLockScript);
        }
    }

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

        for(;;){
            if(System.currentTimeMillis() > waitUntilTime){
                return false;
            }
            Long res;
            if(tryLockScriptSha != null && !"" .equals(tryLockScriptSha)) {
                res = (Long) JedisTemplate.operate().evalsha(tryLockScriptSha, 2, RedisReadWriteLock.getWriteLockKey(name), RedisReadWriteLock.getReentrantWriteLockKey(name), RedisReadWriteLock.getThreadUid(), leastTimeLong.toString());
            }else {
                res = (Long) JedisTemplate.operate().eval(tryLockScript, 2, RedisReadWriteLock.getWriteLockKey(name), RedisReadWriteLock.getReentrantWriteLockKey(name), RedisReadWriteLock.getThreadUid(), leastTimeLong.toString());
                tryLockScriptSha = JedisTemplate.operate().scriptLoad(tryLockScript);
            }

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
        Long res;
        if(unLockScriptSha != null && !"" .equals(unLockScriptSha)){
            res = (Long) JedisTemplate.operate().evalsha(unLockScriptSha, 2, RedisReadWriteLock.getWriteLockKey(name), RedisReadWriteLock.getReentrantWriteLockKey(name), RedisReadWriteLock.getThreadUid());
        }else{
            res = (Long) JedisTemplate.operate().eval(unLockScript, 2, RedisReadWriteLock.getWriteLockKey(name), RedisReadWriteLock.getReentrantWriteLockKey(name), RedisReadWriteLock.getThreadUid());
            unLockScriptSha = JedisTemplate.operate().scriptLoad(unLockScript);
        }

        if(res.equals(0L)){
            log.debug("success unlock write lock,  writeLock = {}", RedisReadWriteLock.getWriteLockKey(name));
        }else if(res.equals(-1L)){
            log.debug("fail unlock, thread not get the lock,  writeLock = {}, thread = {}", RedisReadWriteLock.getReentrantWriteLockKey(name), RedisReadWriteLock.getThreadUid());
        }else {
            log.debug("success unlock reentrant write lock,  reentrantWriteLock = {}, count left = {}", RedisReadWriteLock.getReentrantWriteLockKey(name), res);
        }
    }

}
