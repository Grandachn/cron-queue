package io.github.grandachn.cronqueue.component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * 桶中元素，保存任务id和任务下次执行时间
 * @Author by guanda
 * @Date 2019/3/12 13:34
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class ScoredSortedItem implements Serializable{
    /**
     * 延迟任务的唯一标识
     */
    private String delayJodId;

    /**
     * 任务的执行时间
     */
    private long executeTime;

}
