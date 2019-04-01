package io.github.grandachn.cronqueue.conf;

import io.github.grandachn.cronqueue.util.ResourceUtils;

/**
 * @Author by guanda
 * @Date 2019/4/1 11:44
 */
public class RedissonConf {
    public static final String ADDRESS = ResourceUtils.getString("redisson.address");
    public static final int PORT = ResourceUtils.getInt("redisson.port");
    public static final String PASSWORD =  ResourceUtils.getString("redisson.password", "");
}
