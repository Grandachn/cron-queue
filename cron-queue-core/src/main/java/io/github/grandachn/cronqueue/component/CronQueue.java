package io.github.grandachn.cronqueue.component;

import io.github.grandachn.cronqueue.job.Job;
import lombok.extern.log4j.Log4j;

import static io.github.grandachn.cronqueue.constant.QueueConstant.*;

/**
 *  延迟消息队列
 * @Author by guanda
 * @Date 2019/3/15 16:43
 */
@Log4j
public class CronQueue {

    private String getDelayBucketKey(String delayJodId) {
        return DELAY_BUCKET_KEY_PREFIX  + ( Math.abs(delayJodId.hashCode()) % DELAY_BUCKET_NUM );
    }

    /**
     * 从ReadyQueue获取准备好的延迟任务
     * @param topic 主题
     * @return 任务
     */
    public Job pop(String topic) {
        topic = READY_QUEUE_TOPIC_PREFIX + topic;
        String delayJodId = ReadyQueue.popFormReadyQueue(topic);
        if (delayJodId != null) {
            Job job = JobPool.getDelayJodById(delayJodId);
            if (job != null) {
                long delayTime = job.getExecuteTime();
                //获取消费超时时间，重新放到延迟任务桶中
                long reDelayTime = System.currentTimeMillis() + job.getTtrTime() * 1000L;
                job.setExecuteTime(reDelayTime);
                JobPool.addDelayJod(job);

                ScoredSortedItem item = new ScoredSortedItem(job.getId(), reDelayTime);
                Bucket.addToBucket(getDelayBucketKey(job.getId()), item);

                //返回的时候设置回
                job.setExecuteTime(delayTime);
                return job;
            }
        }
        return null;
    }

    /**
     * 添加延迟任务到延迟队列
     * @param job 任务
     */
    public void push(Job job) {
        Job jobOld = JobPool.getDelayJodById(job.getId());
        if(jobOld != null){
            Bucket.deleteFormBucket(getDelayBucketKey(job.getId()), ScoredSortedItem.builder()
                    .delayJodId(job.getId())
                    .executeTime(job.getExecuteTime())
                    .build());
        }

        JobPool.addDelayJod(job);
        ScoredSortedItem item = new ScoredSortedItem(job.getId(), job.getExecuteTime());
        Bucket.addToBucket(getDelayBucketKey(job.getId()), item);
    }


    /**
     * 任务完成需要显示调用此方法结束调用超时补偿
     * @param delayJodId 任务id
     */
    public void finish(String delayJodId) {
        Job delayJod = JobPool.getDelayJodById(delayJodId);

        if (delayJod == null){
            return;
        }

        //未超过设定的repeatSum
        if (delayJod.getExecuteCount() < delayJod.getRepeatSum() - 1){
            int count = delayJod.getExecuteCount();
            count++;
            delayJod.setExecuteCount(count);
            delayJod.setExecuteTime(System.currentTimeMillis() + delayJod.getRepeatInterval());
            //写回delayJobPool
            JobPool.addDelayJod(delayJod);
            //写回delayBucket中等待
            ScoredSortedItem item = new ScoredSortedItem(delayJod.getId(), delayJod.getExecuteTime());
            Bucket.addToBucket(getDelayBucketKey(delayJod.getId()), item);
            return;
        }

        //正常结束
        JobPool.deleteDelayJodById(delayJodId);
        ScoredSortedItem item = new ScoredSortedItem(delayJod.getId(), delayJod.getExecuteTime());
        Bucket.deleteFormBucket(getDelayBucketKey(delayJod.getId()), item);
    }


    /**
     * 不判断重复次数直接结束该delayJod
     * @param delayJodId 任务id
     */
    public void stop(String delayJodId) {
        Job delayJod = JobPool.getDelayJodById(delayJodId);

        if (delayJod == null){
            return;
        }
        //正常结束
        JobPool.deleteDelayJodById(delayJodId);
        ScoredSortedItem item = new ScoredSortedItem(delayJod.getId(), delayJod.getExecuteTime());
        Bucket.deleteFormBucket(getDelayBucketKey(delayJod.getId()), item);
    }
}

