package io.github.grandachn.cronqueue.component;

import io.github.grandachn.cronqueue.conf.QueueConf;
import io.github.grandachn.cronqueue.job.AbstractJob;
import io.github.grandachn.cronqueue.redis.lock.RedisReadWriteLock;
import lombok.extern.slf4j.Slf4j;

/**
 *  延迟消息队列
 * @Author by guanda
 * @Date 2019/3/15 16:43
 */
@Slf4j
public class CronQueue {

    /**
     * 从ReadyQueue获取准备好的延迟任务
     * @param topic 主题
     * @return 任务
     */
    public AbstractJob pop(String topic) {
        topic = QueueConf.READY_QUEUE_TOPIC_PREFIX + topic;
        String jodId = ReadyQueue.popFormReadyQueue(topic);
        if (jodId != null) {
            AbstractJob job = JobPool.getJodById(jodId);
            if (job != null) {
                long execTime = job.getExecuteTime();
                //获取消费超时时间，重新放到延迟任务桶中
                long reDelayTime = System.currentTimeMillis() + job.getTtrTime();
                job.setExecuteTime(reDelayTime);
                JobPool.addJod(job);

                ScoredSortedItem item = new ScoredSortedItem(job.getId(), reDelayTime);
                Bucket.addToBucket(item);

                //返回的时候设置回
                job.setExecuteTime(execTime);
                return job;
            }
        }
        return null;
    }

    /**
     * 添加延迟任务到延迟队列
     * @param job 任务
     */
    public void push(AbstractJob job) {
        AbstractJob jobOld = JobPool.getJodById(job.getId());
        if(jobOld != null){
            Bucket.deleteFormBucket(ScoredSortedItem.builder()
                    .jodId(job.getId())
                    .executeTime(job.getExecuteTime())
                    .build());
        }

        JobPool.addJod(job);
        ScoredSortedItem item = new ScoredSortedItem(job.getId(), job.getExecuteTime());
        Bucket.addToBucket(item);
    }


    /**
     * 任务完成需要显示调用此方法结束调用超时补偿
     * @param job 任务
     */
    public void finish(AbstractJob job) {
        AbstractJob jod = JobPool.getJodById(job.getId());
        if (jod == null){
            return;
        }
        jod.finish();
    }


    /**
     * 直接结束该jod
     * @param job 任务
     */
    public void stop(AbstractJob job) {
        AbstractJob jod = JobPool.getJodById(job.getId());

        if (jod == null){
            return;
        }
        //正常结束
        //写锁
        RedisReadWriteLock.writeLock().lock(job.getId());
//        DistributedRedisLock.acquireWriteLock(job.getId());
        JobPool.deleteJod(job);
        ScoredSortedItem item = new ScoredSortedItem(jod.getId(), jod.getExecuteTime());
        Bucket.deleteFormBucket(item);
        RedisReadWriteLock.writeLock().unlock(job.getId());
//        DistributedRedisLock.releaseWriteLock(job.getId());
    }
}

