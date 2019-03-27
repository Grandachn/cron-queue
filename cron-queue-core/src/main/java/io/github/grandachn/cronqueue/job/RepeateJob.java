package io.github.grandachn.cronqueue.job;

import io.github.grandachn.cronqueue.component.Bucket;
import io.github.grandachn.cronqueue.component.JobPool;
import io.github.grandachn.cronqueue.component.ScoredSortedItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    public void finish() {
        if (this.getExecuteCount() < this.getRepeatSum() - 1){
            int count = this.getExecuteCount();
            count++;
            this.setExecuteCount(count);
            this.setExecuteTime(System.currentTimeMillis() + this.getRepeatInterval());
            //写回delayJobPool
            JobPool.addJod(this);
            //写回delayBucket中等待
            ScoredSortedItem item = new ScoredSortedItem(this.getId(), this.getExecuteTime());
            Bucket.addToBucket(item);
        }else{
            //正常结束
            JobPool.deleteJod(this);
            ScoredSortedItem item = new ScoredSortedItem(this.getId(), this.getExecuteTime());
            Bucket.deleteFormBucket(item);
        }
    }
}
