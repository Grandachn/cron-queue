package io.github.grandachn.cronqueue.component;

import com.alibaba.fastjson.JSON;
import io.github.grandachn.cronqueue.redis.JedisTemplate;
import lombok.extern.log4j.Log4j;

/**
 * 延迟任务池(维护延迟任务元信息)
 * @Author by guanda
 * @Date 2019/3/12 11:30
 */
@Log4j
public class DelayJobPool {
    private static final String DELAY_QUEUE_JOB_POOL = "delayQueueJobPool";

    /**
     * 获取DelayJob
     * @param id
     * @return
     */
    public static DelayJob getDelayJodById(String id) {
        return JSON.parseObject(JedisTemplate.operate().hget(DELAY_QUEUE_JOB_POOL, id), DelayJob.class);
    }

    /**
     * 添加 DelayJob
     * @param delayJob
     */
    public static boolean addDelayJod(DelayJob delayJob) {
        try {
            JedisTemplate.operate().hset(DELAY_QUEUE_JOB_POOL, delayJob.getId(), JSON.toJSONString(delayJob));
            return true;
        }catch (Exception e){
            log.error("将DelayJob写入DelayJobPool失败: " + delayJob, e);
        }
        return false;
    }


    /**
     * 删除DelayJob
     * @param id
     * @return
     */
    public static boolean deleteDelayJodById(String id) {
        try {
            JedisTemplate.operate().hdel(DELAY_QUEUE_JOB_POOL, id);
            return true;
        }catch (Exception e){
            log.error("DelayJobPool删除DelayJob失败， jobId： " + id , e);
        }
        return false;
    }

}
