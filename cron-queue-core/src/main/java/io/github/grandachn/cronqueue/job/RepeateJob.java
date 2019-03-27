package io.github.grandachn.cronqueue.job;

import io.github.grandachn.cronqueue.CronQueueContext;
import io.github.grandachn.cronqueue.component.Bucket;
import io.github.grandachn.cronqueue.component.JobPool;
import io.github.grandachn.cronqueue.component.ScoredSortedItem;
import io.github.grandachn.cronqueue.persistence.PersistenceUtil;
import lombok.*;

import java.io.Serializable;

/**
 * 重复执行的任务
 * @Author by guanda
 * @Date 2019/3/27 10:39
 */
@Data
@NoArgsConstructor
public class RepeateJob extends AbstractJob implements Serializable{
    /**
     * 任务重复执行次数
     */
    private int repeatSum = 0;

    /**
     * 任务重复间隔时间
     */
    private long repeatInterval;

    /**
     * 任务已经执行次数
     */
    private int executeCount = 0;

    @Builder
    public RepeateJob(String id, String topic, long executeTime, long ttrTime, String message, int repeatSum, long repeatInterval, int executeCount) {
        super(id, topic, executeTime, ttrTime, message);
        this.repeatSum = repeatSum;
        this.repeatInterval = repeatInterval;
        this.executeCount = executeCount;
    }

    @Override
    public String toString() {
        return "RepeateJob{" +
                "repeatSum=" + repeatSum +
                ", repeatInterval=" + repeatInterval +
                ", executeCount=" + executeCount +
                ", id='" + id + '\'' +
                ", topic='" + topic + '\'' +
                ", executeTime=" + executeTime +
                ", ttrTime=" + ttrTime +
                ", message='" + message + '\'' +
                '}';
    }

    @Override
    public void finish() {
        int count = this.getExecuteCount();
        count++;
        this.setExecuteCount(count);

        if (this.getExecuteCount() < this.getRepeatSum()){
            this.setExecuteTime(System.currentTimeMillis() + this.getRepeatInterval());
            //写回delayJobPool
            JobPool.addJod(this);
            //写回delayBucket中等待
            ScoredSortedItem item = new ScoredSortedItem(this.getId(), this.getExecuteTime());
            Bucket.addToBucket(item);
        }else{
            //落盘的时候需要
            if(CronQueueContext.getContext().isPersitence()){
                JobPool.addJod(this);
            }

            //正常结束
            JobPool.deleteJod(this);
            ScoredSortedItem item = new ScoredSortedItem(this.getId(), this.getExecuteTime());
            Bucket.deleteFormBucket(item);
        }
    }
}
