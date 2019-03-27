package io.github.grandachn.cronqueue.job;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author by guanda
 * @Date 2019/3/27 14:00
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractJob {
    /**
     * 延迟任务的唯一标识，用于检索任务
     */
    protected String id;

    /**
     * 任务类型（具体业务类型）
     */
    protected String topic;

    /**
     * 任务的执行时间
     */
    protected long executeTime;

    /**
     * 任务的执行超时时间
     */
    protected long ttrTime;

    /**
     * 任务具体的消息内容，用于处理具体业务逻辑用
     */
    protected String message;

    /**
     * 结束调用
     */
    abstract public void finish();

}
