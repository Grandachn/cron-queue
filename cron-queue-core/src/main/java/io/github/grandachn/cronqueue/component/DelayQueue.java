package io.github.grandachn.cronqueue.component;

import lombok.extern.log4j.Log4j;

/**
 *  延迟消息队列
 * @Author by guanda
 * @Date 2019/3/15 16:43
 */
@Log4j
public class DelayQueue {
    public static final String DELAY_BUCKET_KEY_PREFIX = "delayBucket_";
    public static final String READY_QUEUE_TOPIC_PREFIX = "readyQueue_topic_";
    public static final int  DELAY_BUCKET_NUM = 10;

    private String getDelayBucketKey(String delayJodId) {
        return DELAY_BUCKET_KEY_PREFIX  + ( Math.abs(delayJodId.hashCode()) % DELAY_BUCKET_NUM );
    }

    /**
     * 从ReadyQueue获取准备好的延迟任务
     * @param topic
     * @return
     */
    public DelayJob pop(String topic) {
        topic = READY_QUEUE_TOPIC_PREFIX + topic;
        String delayJodId = ReadyQueue.popFormReadyQueue(topic);
        if (delayJodId != null) {
            DelayJob delayJob = DelayJobPool.getDelayJodById(delayJodId);
            if (delayJob != null) {
                long delayTime = delayJob.getDelayTime();
                //获取消费超时时间，重新放到延迟任务桶中
                long reDelayTime = System.currentTimeMillis() + delayJob.getTtrTime() * 1000L;
                delayJob.setDelayTime(reDelayTime);
                DelayJobPool.addDelayJod(delayJob);

                ScoredSortedItem item = new ScoredSortedItem(delayJob.getId(), reDelayTime);
                DelayBucket.addToBucket(getDelayBucketKey(delayJob.getId()), item);

                //返回的时候设置回
                delayJob.setDelayTime(delayTime);
                return delayJob;
            }
        }
        return null;
    }

    /**
     * 添加延迟任务到延迟队列
     * @param delayJob
     */
    public void push(DelayJob delayJob) {
        DelayJob delayJobOld = DelayJobPool.getDelayJodById(delayJob.getId());
        if(delayJobOld != null){
            DelayBucket.deleteFormBucket(getDelayBucketKey(delayJob.getId()), ScoredSortedItem.builder()
                    .delayJodId(delayJob.getId())
                    .delayTime(delayJob.getDelayTime())
                    .build());
        }

        DelayJobPool.addDelayJod(delayJob);
        ScoredSortedItem item = new ScoredSortedItem(delayJob.getId(), delayJob.getDelayTime());
        DelayBucket.addToBucket(getDelayBucketKey(delayJob.getId()), item);
    }


    /**
     * 任务完成需要显示调用此方法结束调用超时补偿
     * @param delayJodId
     */
    public void finish(String delayJodId) {
        DelayJob delayJod = DelayJobPool.getDelayJodById(delayJodId);

        if (delayJod == null){
            return;
        }

        //未超过设定的repeatSum
        if (delayJod.getExecuteCount() < delayJod.getRepeatSum() - 1){
            int count = delayJod.getExecuteCount();
            count++;
            delayJod.setExecuteCount(count);
            delayJod.setDelayTime(System.currentTimeMillis() + delayJod.getRepeatInterval());
            //写回delayJobPool
            DelayJobPool.addDelayJod(delayJod);
            //写回delayBucket中等待
            ScoredSortedItem item = new ScoredSortedItem(delayJod.getId(), delayJod.getDelayTime());
            DelayBucket.addToBucket(getDelayBucketKey(delayJod.getId()), item);
            return;
        }

        //正常结束
        DelayJobPool.deleteDelayJodById(delayJodId);
        ScoredSortedItem item = new ScoredSortedItem(delayJod.getId(), delayJod.getDelayTime());
        DelayBucket.deleteFormBucket(getDelayBucketKey(delayJod.getId()), item);
    }


    /**
     * 不判断重复次数直接结束该delayJod
     * @param delayJodId
     */
    public void stop(String delayJodId) {
        DelayJob delayJod = DelayJobPool.getDelayJodById(delayJodId);

        if (delayJod == null){
            return;
        }
        //正常结束
        DelayJobPool.deleteDelayJodById(delayJodId);
        ScoredSortedItem item = new ScoredSortedItem(delayJod.getId(), delayJod.getDelayTime());
        DelayBucket.deleteFormBucket(getDelayBucketKey(delayJod.getId()), item);
    }
}

