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
 * 延迟任务
 * @Author by guanda
 * @Date 2019/3/12 11:24
 */
public class Job extends AbstractJob implements Serializable {

    @Builder
    public Job(String id, String topic, long executeTime, long ttrTime, String message) {
        super(id, topic, executeTime, ttrTime, message);
    }

    @Override
    public void finish(){
        //正常结束
        JobPool.deleteJod(this);
        ScoredSortedItem item = new ScoredSortedItem(this.getId(), this.getExecuteTime());
        Bucket.deleteFormBucket(item);
    }
}
