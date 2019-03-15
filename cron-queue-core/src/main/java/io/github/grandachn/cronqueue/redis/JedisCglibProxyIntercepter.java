package io.github.grandachn.cronqueue.redis;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import redis.clients.jedis.Jedis;

import java.lang.reflect.Method;

/**
 * @Author by guanda
 * @Date 2019/3/15 16:07
 */
public class JedisCglibProxyIntercepter implements MethodInterceptor {
    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        try(Jedis jedis = JedisConnectPoll.getJedis()){
            return method.invoke(jedis, objects);
        }
    }
}
