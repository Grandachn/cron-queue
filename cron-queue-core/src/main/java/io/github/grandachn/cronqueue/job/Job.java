package io.github.grandachn.cronqueue.job;


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
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Job implements Serializable {
    /**
     * 延迟任务的唯一标识，用于检索任务
     */
    private String id;

    /**
     * 任务类型（具体业务类型）
     */
    private String topic;

    /**
     * 任务的执行时间
     */
    private long executeTime;

    /**
     * 任务的执行超时时间
     */
    private long ttrTime;

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

    /**
     * 任务具体的消息内容，用于处理具体业务逻辑用
     */
    private String message;

}
