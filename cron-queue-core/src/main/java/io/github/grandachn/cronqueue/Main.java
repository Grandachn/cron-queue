package io.github.grandachn.cronqueue;

import io.github.grandachn.cronqueue.redis.JedisConnectPoll;
import io.github.grandachn.cronqueue.redis.JedisTemplate;
import redis.clients.jedis.Jedis;

/**
 * @Author by guanda
 * @Date 2019/3/15 16:00
 */
public class Main {
    public static void main(String[] args) {
        try(Jedis jedis = JedisConnectPoll.getJedis()){
            jedis.set("grandaTest", "hello");
            System.out.println(jedis.get("grandaTest"));
            jedis.del("grandaTest");
            System.out.println(jedis.get("grandaTest"));
        }

        System.out.println(JedisTemplate.operate().set("grandaTest", "123456"));
        System.out.println(JedisTemplate.operate().get("grandaTest"));
    }
}
