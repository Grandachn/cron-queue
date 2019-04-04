package io.github.grandachn.cronqueue.conf;

import io.github.grandachn.cronqueue.util.ResourceUtils;

/**
 * @Author by guanda
 * @Date 2019/4/1 11:44
 */
public class RedisLockConf {
    public static final String LOCK_PREFIX = ResourceUtils.getString("redis.lock.prefix", "cronqueue_lock_");
    public static final String REENTRANT_LOCK_PREFIX = ResourceUtils.getString("redis.reentrant-lock.prefix", "cronqueue_reentrant_lock_");
    public static final String READ_LOCK_PREFIX = ResourceUtils.getString("redis.read-lock.prefix", "cronqueue_read_lock_");
    public static final String WRITE_LOCK_PREFIX = ResourceUtils.getString("redis.write-lock.prefix", "cronqueue_write_lock_");
    public static final String REENTRANT_WRITE_LOCK_PREFIX = ResourceUtils.getString("redis.reentrant-write-lock.prefix", "cronqueue_reentrant_write_lock_");
}
