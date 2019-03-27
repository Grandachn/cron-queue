package io.github.grandachn.cronqueue.persistence;

import io.github.grandachn.cronqueue.job.AbstractJob;

/**
 * @Author by guanda
 * @Date 2019/3/27 16:06
 */
public interface Persistencer {
    boolean insertOrUpdate(AbstractJob job);
    boolean delete(AbstractJob job);
    String get(String jobId);
}
