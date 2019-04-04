package io.github.grandachn.cronqueue.job;

import io.github.grandachn.cronqueue.component.JobPool;
import io.github.grandachn.cronqueue.component.ScoredSortedItem;
import io.github.grandachn.cronqueue.component.Bucket;
import io.github.grandachn.cronqueue.redis.lock.RedisReadWriteLock;
import io.github.grandachn.cronqueue.util.CronUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 定时任务
 * @Author by guanda
 * @Date 2019/3/27 10:39
 */
@Data
@NoArgsConstructor
public class CronJob extends AbstractJob implements Serializable{
    /**
     * cron表达式
     */
    private String cronPattern;


    public CronJob(String id, String topic, long ttrTime, String message, String cronPattern) {
        super(id, topic, 0, ttrTime, message);
        this.cronPattern = cronPattern;
        this.executeTime = CronUtils.getNextExecTime(cronPattern).getTime();
    }

    @Override
    public void finish() {
        //清理超时调用
        ScoredSortedItem item = new ScoredSortedItem(this.getId(), this.getExecuteTime());
        Bucket.deleteFormBucket(item);

        //这里很容易出问题，CronUtils.getNextExecTime(cronPattern).getTime()的时间戳有可能是比当前时间戳小很微小的一点
        //导致getTime()时间下的定时任务被重复执行
        long time = CronUtils.getNextExecTime(cronPattern).getTime();
        while(time <=  System.currentTimeMillis()){
            time = CronUtils.getNextExecTime(cronPattern).getTime();
        }

        this.setExecuteTime(time);
        //写回JobPool
        //加分布式读锁做检查，和stop()的写锁相对应
        RedisReadWriteLock.readLock().lock(this.getId());
//        DistributedRedisLock.acquireReadLock(this.getId());
        if(JobPool.getJodById(this.id) != null){
            JobPool.addJod(this);
            //写回Bucket中等待
            item = new ScoredSortedItem(this.getId(), this.getExecuteTime());
            Bucket.addToBucket(item);
        }
        RedisReadWriteLock.readLock().unlock(this.getId());
//        DistributedRedisLock.releaseReadLock(this.getId());
    }

    public static Builder builder(){
        return new Builder();
    }

    public static final class Builder {
        String id;
        String topic;
        long ttrTime;
        String message;
        private String cronPattern;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder topic(String topic) {
            this.topic = topic;
            return this;
        }

        public Builder ttrTime(long ttrTime) {
            this.ttrTime = ttrTime;
            return this;
        }

        public Builder cronPattern(String cronPattern) {
            this.cronPattern = cronPattern;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public CronJob build() {
            CronJob cronJob = new CronJob();
            cronJob.setId(id);
            cronJob.setTopic(topic);
            cronJob.setExecuteTime(CronUtils.getNextExecTime(cronPattern).getTime());
            cronJob.setTtrTime(ttrTime);
            cronJob.setCronPattern(cronPattern);
            cronJob.setMessage(message);
            return cronJob;
        }
    }

}
