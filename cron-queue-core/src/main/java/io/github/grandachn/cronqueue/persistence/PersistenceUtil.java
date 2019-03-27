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

    public static void main(String[] args) {
        PersistenceUtil.setPersistencer(new MongoDBPersistencer(false));
        SerializeUtil.setSerializer(new FastJsonSerializer());
        RepeateJob repeateJob = RepeateJob.builder().id("1").topic("cronQueueTest").message("hello").executeTime(System.currentTimeMillis()).repeatInterval(1000).repeatSum(10).build();
        System.out.println(PersistenceUtil.insertOrUpdate(repeateJob));
        System.out.println(PersistenceUtil.delete(repeateJob));
        System.out.println(PersistenceUtil.insertOrUpdate(repeateJob));
        System.out.println(PersistenceUtil.get("1"));
        try {
            Class clz = Class.forName(JSON.parseObject(PersistenceUtil.get("1")).getString("class"));
            System.out.println((RepeateJob)SerializeUtil.deserialize(PersistenceUtil.get("1"), clz));
        } catch (ClassNotFoundException e) {
        }
    }
}
