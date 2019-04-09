package io.github.grandachn.cronqueue.redis.lock;

import io.github.grandachn.cronqueue.conf.RedisLockConf;
import io.github.grandachn.cronqueue.redis.JedisConnectPoll;
import io.github.grandachn.cronqueue.redis.JedisTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 基于redis的普通锁
 * @Author by guanda
 * @Date 2019/4/4 17:07
 */
@Slf4j
public class RedisLock implements Lock{
    private static volatile RedisLock redisLock;

    private static String tryLockScript;

    private static String unLockScript;

    private static String tryLockScriptSha;

    private static String unLockScriptSha;

    private RedisLock(){
        // lock reentrant-lock uuid leaseTime
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

        //lock reentrant-lock uuid
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

    public static RedisLock getRedisLock(){
        if(redisLock == null){
            synchronized (RedisReadLock.class){
                if (redisLock == null){
                    redisLock = new RedisLock();
                }
            }
        }
        return redisLock;
    }

    @Override
    public void lock(String name) {
        tryLock(name, Long.MAX_VALUE, 30, TimeUnit.SECONDS);
    }

    @Override
    public void lock(String name, long leaseTime, TimeUnit unit) {
        tryLock(name, Long.MAX_VALUE, leaseTime, unit);
    }

    @Override
    public boolean tryLock(String name, long waitTime, long leaseTime, TimeUnit unit) {
        Long waitUntilTime = unit.toMillis(waitTime) + System.currentTimeMillis();
        Long leastTimeLong = unit.toMillis(leaseTime);
        if(waitUntilTime < 0){
            waitUntilTime = Long.MAX_VALUE;
        }

        for(;;){
            if(System.currentTimeMillis() > waitUntilTime){
                return false;
            }

            Long res;
            if(tryLockScriptSha != null && !"" .equals(tryLockScriptSha)){
                res = (Long) JedisTemplate.operate().evalsha(tryLockScriptSha, 2, getLockKey(name), getReentrantLockKey(name), getThreadUid(), leastTimeLong.toString());
            }else {
                res = (Long) JedisTemplate.operate().eval(tryLockScript, 2, getLockKey(name), getReentrantLockKey(name), getThreadUid(), leastTimeLong.toString());
                tryLockScriptSha = JedisTemplate.operate().scriptLoad(tryLockScript);
            }

            if (res.equals(0L)) {
                log.debug("wait lock release,  lock = {}", getLockKey(name));
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    log.error("wait write lock release exception", e);
                }
            }else if(res.equals(1L)){
                //successLock
                log.debug("success get lock,  lock = {}", getLockKey(name));
                break;
            }else {
                log.debug("success in reentrant lock,  reentrantLock = {}, count now = {}",getReentrantLockKey(name), res);
                break;
            }
        }
        return true;
    }

    @Override
    public void unlock(String name) {
        Long res;

        if(unLockScriptSha != null && !"" .equals(unLockScriptSha)){
            res = (Long) JedisTemplate.operate().evalsha(unLockScriptSha, 2, getLockKey(name), getReentrantLockKey(name), getThreadUid());
        }else {
            res = (Long) JedisTemplate.operate().eval(unLockScript, 2, getLockKey(name), getReentrantLockKey(name), getThreadUid());
            unLockScriptSha = JedisTemplate.operate().scriptLoad(unLockScriptSha);
        }

        if(res.equals(0L)){
            log.debug("success unlock lock,  Lock = {}", getLockKey(name));
        }else if(res.equals(-1L)){
            log.debug("fail unlock, thread not get the lock,  Lock = {}, thread = {}", getLockKey(name), getThreadUid());
        }else {
            log.debug("success unlock reentrant lock,  reentrantLock = {}, count left = {}", getReentrantLockKey(name), res);
        }
    }

    private String getLockKey(String name){
        return RedisLockConf.LOCK_PREFIX + name;
    }

    private String getReentrantLockKey(String name){
        return RedisLockConf.REENTRANT_LOCK_PREFIX + name;
    }

    private String getThreadUid(){
        return JedisConnectPoll.JEDIS_CONNECT_POLL_UUID.toString() + ":" + Thread.currentThread().getId();
    }
}
