package io.github.grandachn.cronqueue.component;

import com.alibaba.fastjson.JSON;
import io.github.grandachn.cronqueue.CronQueueContext;
import io.github.grandachn.cronqueue.job.AbstractJob;
import io.github.grandachn.cronqueue.persistence.PersistenceUtil;
import io.github.grandachn.cronqueue.redis.JedisTemplate;
import io.github.grandachn.cronqueue.serialize.SerializeUtil;
import lombok.extern.log4j.Log4j;

import static io.github.grandachn.cronqueue.constant.QueueConstant.QUEUE_JOB_POOL;

/**
 * 延迟任务池(维护延迟任务元信息)
 * @Author by guanda
 * @Date 2019/3/12 11:30
 */
@Log4j
public class JobPool {

    /**
     * 获取DelayJob
     * @param id 任务id
     * @return 任务
     */
    static AbstractJob getJodById(String id) {
        String jobJson = JedisTemplate.operate().hget(QUEUE_JOB_POOL, id);
        if (jobJson != null && !"".equals(jobJson)){
            return (AbstractJob)SerializeUtil.deserialize(jobJson);
        }
        //redis获取失败，如果开启持久化，则从持久化数据中取
        if(CronQueueContext.getContext().isPersitence()){
            try {
                Class clz = Class.forName(JSON.parseObject(PersistenceUtil.get(id)).getString("class"));
                return (AbstractJob)SerializeUtil.deserialize(PersistenceUtil.get(id), clz);
            } catch (ClassNotFoundException e) {
                log.error("持久化数据反序列化失败: ", e);
            }
        }
        return null;
    }

    /**
     * 添加 Job
     * @param job 任务
     */
    public static boolean addJod(AbstractJob job) {
        if(CronQueueContext.getContext().isPersitence()){
            if(PersistenceUtil.insertOrUpdate(job)){
                log.error("将job持久化失败: " + job);
                return false;
            }
        }

        try {
            JedisTemplate.operate().hset(QUEUE_JOB_POOL, job.getId(), SerializeUtil.serialize(job));
            return true;
        }catch (Exception e){
            log.error("将job写入JobPool失败: " + job, e);
        }
        return false;
    }


    /**
     * 删除DelayJob
     * @param job 任务
     * @return 任务
     */
    public static boolean deleteJod(AbstractJob job) {
        //如果持久化删除失败，则不清除redis数据，用于后期比对
        if(CronQueueContext.getContext().isPersitence()){
            if(PersistenceUtil.delete(job)){
                log.error("job持久化删除失败: " + job);
                return false;
            }
        }

        try {
            JedisTemplate.operate().hdel(QUEUE_JOB_POOL, job.getId());
            return true;
        }catch (Exception e){
            log.error("JobPool删除job失败， jobId： " + job.getId() , e);
        }
        return false;
    }

}
