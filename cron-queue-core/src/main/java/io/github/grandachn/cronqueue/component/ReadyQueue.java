package io.github.grandachn.cronqueue.component;

import io.github.grandachn.cronqueue.redis.JedisTemplate;
import lombok.extern.log4j.Log4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 存放可以消费的jod
 * @Author by guanda
 * @Date 2019/3/12 14:15
 */
@Log4j
public class ReadyQueue {
    /**
     * 添加jodid到准备队列
     * @param topic
     * @param delayJodId
     */
    public static void pushToReadyQueue(String topic, String delayJodId) {
        JedisTemplate.operate().lpush(topic, delayJodId);
    }

    /**
     * 从准备队列中获取jodid
     * @param topic
     * @return
     */
    public static String popFormReadyQueue(String topic) {
        List<String> jobIdList = new ArrayList<>();

        while (jobIdList.size() == 0){
            try {
                jobIdList = JedisTemplate.operate().brpop(5, topic);
            } catch (Exception e) {
                log.error("阻塞等待队列异常，" ,e);
            }
        }
        return jobIdList.get(0) ;
    }
}

