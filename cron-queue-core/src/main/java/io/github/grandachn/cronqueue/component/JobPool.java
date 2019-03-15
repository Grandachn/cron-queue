package io.github.grandachn.cronqueue.component;

import com.alibaba.fastjson.JSON;
import io.github.grandachn.cronqueue.job.Job;
import io.github.grandachn.cronqueue.redis.JedisTemplate;
import lombok.extern.log4j.Log4j;

import static io.github.grandachn.cronqueue.constant.QueueConstant.DELAY_QUEUE_JOB_POOL;

/**
 * 延迟任务池(维护延迟任务元信息)
 * @Author by guanda
 * @Date 2019/3/12 11:30
 */
@Log4j
class JobPool {


    /**
     * 获取DelayJob
     * @param id 任务id
     * @return 任务
     */
    static Job getDelayJodById(String id) {
        return JSON.parseObject(JedisTemplate.operate().hget(DELAY_QUEUE_JOB_POOL, id), Job.class);
    }

    /**
     * 添加 Job
     * @param job 任务
     */
    static boolean addDelayJod(Job job) {
        try {
            JedisTemplate.operate().hset(DELAY_QUEUE_JOB_POOL, job.getId(), JSON.toJSONString(job));
            return true;
        }catch (Exception e){
            log.error("将DelayJob写入DelayJobPool失败: " + job, e);
        }
        return false;
    }


    /**
     * 删除DelayJob
     * @param id 任务id
     * @return 任务
     */
    static boolean deleteDelayJodById(String id) {
        try {
            JedisTemplate.operate().hdel(DELAY_QUEUE_JOB_POOL, id);
            return true;
        }catch (Exception e){
            log.error("DelayJobPool删除DelayJob失败， jobId： " + id , e);
        }
        return false;
    }

}
