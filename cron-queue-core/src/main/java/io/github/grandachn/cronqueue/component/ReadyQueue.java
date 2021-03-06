package io.github.grandachn.cronqueue.component;

import io.github.grandachn.cronqueue.redis.JedisTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 存放可以消费的jod
 * @Author by guanda
 * @Date 2019/3/12 14:15
 */
@Slf4j
public class ReadyQueue {
    /**
     * 添加jodid到准备队列
     * @param topic 主题
     * @param delayJodId 任务id
     */
    static void pushToReadyQueue(String topic, String delayJodId) {
        JedisTemplate.operate().lpush(topic, delayJodId);
    }

    /**
     * 从准备队列中获取jodid
     * @param topic 主题
     * @return json序列化后的job
     */
    static String popFormReadyQueue(String topic) {
        List<String> jobIdList = new ArrayList<>();

        while (jobIdList == null || jobIdList.size() == 0){
            try {
                jobIdList = JedisTemplate.operate().brpop(8, topic);
            } catch (Exception e) {
                log.error("阻塞等待队列异常，" ,e);
            }
        }
        return jobIdList.get(1) ;
    }
}

