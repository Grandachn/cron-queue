package io.github.grandachn.cronqueue.redis;

import net.sf.cglib.proxy.Enhancer;
import redis.clients.jedis.Jedis;

/**
 * @Author by guanda
 * @Date 2019/3/15 15:55
 */
public class JedisTemplate {
    public static Jedis operate(){
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(Jedis.class);
        enhancer.setCallback(new JedisCglibProxyIntercepter());
        return (Jedis) enhancer.create();
    }
}
