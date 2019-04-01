package io.github.grandachn.cronqueue.component;

import io.github.grandachn.cronqueue.persistence.MongoDBPersistencer;
import io.github.grandachn.cronqueue.persistence.Persistencer;
import io.github.grandachn.cronqueue.serialize.FastJsonSerializer;
import io.github.grandachn.cronqueue.serialize.SerializeUtil;
import io.github.grandachn.cronqueue.persistence.PersistenceUtil;
import lombok.Data;

/**
 * @Author by guanda
 * @Date 2019/3/27 13:39
 */
@Data
public class CronQueueContext {
    private static volatile CronQueueContext cronQueueContext;
    private static volatile CronQueue cronQueue;
    private boolean isPersitence;
    private static Persistencer persistencer;

    private CronQueueContext(Persistencer persistencer){
        CronQueueContext.persistencer = persistencer;
        SerializeUtil.setSerializer(new FastJsonSerializer());
        PersistenceUtil.setPersistencer(CronQueueContext.persistencer);
        setPersitence(false);
    }

    public static CronQueueContext getContext(){
        if(cronQueueContext == null){
            synchronized (CronQueueContext.class){
                if (cronQueueContext == null){
                    cronQueueContext = new CronQueueContext(persistencer);
                }
            }
        }
        return cronQueueContext;
    }

    public void startServer(){
        BucketHandler.start();
        BucketHandler.minotor();
    }

    public void stopServer(){
        BucketHandler.stop();
    }

    public void restartServer(){
        BucketHandler.restart();
    }

    public CronQueue getCronQueue(){
        if(cronQueue == null){
            synchronized (CronQueueContext.class){
                if (cronQueue == null){
                    cronQueue = new CronQueue();
                }
            }
        }
        return cronQueue;
    }
}
