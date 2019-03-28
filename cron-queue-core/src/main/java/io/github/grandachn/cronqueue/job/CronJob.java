package io.github.grandachn.cronqueue.job;

import io.github.grandachn.cronqueue.component.Bucket;
import io.github.grandachn.cronqueue.component.JobPool;
import io.github.grandachn.cronqueue.component.ScoredSortedItem;
import io.github.grandachn.cronqueue.util.CronUtils;
import lombok.Builder;
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


    @Builder
    public CronJob(String id, String topic, long executeTime, long ttrTime, String message, String cronPattern) {
        super(id, topic, executeTime, ttrTime, message);
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
        JobPool.addJod(this);
        //写回Bucket中等待
        item = new ScoredSortedItem(this.getId(), this.getExecuteTime());
        Bucket.addToBucket(item);
    }

}
