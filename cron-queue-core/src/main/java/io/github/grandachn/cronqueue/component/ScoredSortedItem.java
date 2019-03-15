package io.github.grandachn.cronqueue.component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author by guanda
 * @Date 2019/3/12 13:34
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScoredSortedItem implements Serializable{
    /**
     * 延迟任务的唯一标识
     */
    private String delayJodId;

    /**
     * 任务的执行时间
     */
    private long delayTime;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        ScoredSortedItem that = (ScoredSortedItem) o;

        if (delayTime != that.delayTime) return false;
        return delayJodId != null ? delayJodId.equals(that.delayJodId) : that.delayJodId == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (delayTime ^ (delayTime >>> 32));
        result = 31 * result + (delayJodId != null ? delayJodId.hashCode() : 0);
        return result;
    }
}
