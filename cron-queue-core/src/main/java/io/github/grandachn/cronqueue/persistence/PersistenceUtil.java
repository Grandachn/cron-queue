package io.github.grandachn.cronqueue.persistence;

import com.alibaba.fastjson.JSON;
import io.github.grandachn.cronqueue.job.AbstractJob;
import io.github.grandachn.cronqueue.job.RepeateJob;
import io.github.grandachn.cronqueue.serialize.FastJsonSerializer;
import io.github.grandachn.cronqueue.serialize.SerializeUtil;

/**
 * @Author by guanda
 * @Date 2019/3/27 16:04
 */
public class PersistenceUtil {
    private static Persistencer persistencer;

    public static void setPersistencer(Persistencer persistencer) {
        PersistenceUtil.persistencer = persistencer;
    }

    public static boolean insertOrUpdate(AbstractJob job){
        return persistencer.insertOrUpdate(job);
    }

    public static String get(String jobId){
        return persistencer.get(jobId);
    }

    public static boolean delete(AbstractJob job){
        return persistencer.delete(job);
    }
}
