package io.github.grandachn.cronqueue.redis.lock;

import io.github.grandachn.cronqueue.redis.JedisTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 基于redis的读锁
 * @Author by guanda
 * @Date 2019/4/3 15:05
 */
@Slf4j
public class RedisReadLock implements Lock{
    private static String tryLockScript;

    private static String unLockScript;

    private static String tryLockScriptSha;

    private static String unLockScriptSha;

    RedisReadLock(){
        // write-lock read-lock uuid leaseTime
        tryLockScript = "if not redis.call('GET',KEYS[1]) then " +
                //若没有值，返回的是false
                            "local count = redis.call('HGET',KEYS[2],KEYS[3]);" +
                            "if count then " +
                                "count = tonumber(count) + 1;" +
                                "redis.call('HSET',KEYS[2],KEYS[3],count);" +
                            "else " +
                                "redis.call('HSET',KEYS[2],KEYS[3],1);" +
                            "end;" +
                            "local t = redis.call('PTTL', KEYS[2]);" +
                            "redis.call('PEXPIRE', KEYS[2], math.max(t, ARGV[1]));" +
                            "return 1;" +
                        "else " +
                            "return 0;" +
                        "end;";
        if(tryLockScriptSha == null || "".equals(tryLockScriptSha)){
            tryLockScriptSha = JedisTemplate.operate().scriptLoad(tryLockScript);
        }

        unLockScript = "local count = redis.call('HGET',KEYS[1],KEYS[2]);" +
                        "if count then " +
                            "if (tonumber(count) > 1) then " +
                                "count = tonumber(count) - 1;" +
                                "redis.call('HSET',KEYS[1],KEYS[2],count);" +
                            "else " +
                                "redis.call('HDEL',KEYS[1],KEYS[2]);" +
                            "end;" +
                        "end;" +
                        "return;";
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
                res = (Long) JedisTemplate.operate().evalsha(tryLockScriptSha, 3, RedisReadWriteLock.getWriteLockKey(name), RedisReadWriteLock.getReadLockKey(name), RedisReadWriteLock.getThreadUid(), leastTimeLong.toString());
            }else {
                res = (Long) JedisTemplate.operate().eval(tryLockScript, 3, RedisReadWriteLock.getWriteLockKey(name), RedisReadWriteLock.getReadLockKey(name), RedisReadWriteLock.getThreadUid(), leastTimeLong.toString());
                tryLockScriptSha = JedisTemplate.operate().scriptLoad(tryLockScript);
            }
            if(res.equals(1L)){
                //successGetReadLock
                log.debug("success get read lock,  readLock={}", RedisReadWriteLock.getReadLockKey(name));
                break;
            }else {
                //need to wait write lock to be released
                log.debug("wait write lock release,  writeLock={}", RedisReadWriteLock.getWriteLockKey(name));
                try {
                    TimeUnit.MILLISECONDS.sleep(50);
                } catch (InterruptedException e) {
                    log.error("wait write lock release exception", e);
                }
            }
        }
        return true;
    }

    @Override
    public void unlock(String name){
        if(unLockScriptSha != null && !"" .equals(unLockScriptSha)){
            JedisTemplate.operate().evalsha(unLockScriptSha, 2, RedisReadWriteLock.getReadLockKey(name), RedisReadWriteLock.getThreadUid());
        }else {
            JedisTemplate.operate().eval(unLockScript, 2, RedisReadWriteLock.getReadLockKey(name), RedisReadWriteLock.getThreadUid());
            unLockScriptSha = JedisTemplate.operate().scriptLoad(unLockScript);
        }
        log.debug("success unlock read lock, readLock={}", RedisReadWriteLock.getReadLockKey(name));
    }

}
