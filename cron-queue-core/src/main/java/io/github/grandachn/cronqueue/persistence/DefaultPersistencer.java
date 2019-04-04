package io.github.grandachn.cronqueue.persistence;

import io.github.grandachn.cronqueue.job.AbstractJob;

/**
 * @Author by guanda
 * @Date 2019/4/4 16:29
 */
public class DefaultPersistencer implements Persistencer{
    @Override
    public boolean insertOrUpdate(AbstractJob job) {
        return true;
    }

    @Override
    public boolean delete(AbstractJob job) {
        return true;
    }

    @Override
    public String get(String jobId) {
        return "";
    }
}
