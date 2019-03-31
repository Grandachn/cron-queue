package io.github.grandachn.cronqueue.component;

import io.github.grandachn.cronqueue.persistence.MongoDBPersistencer;
import io.github.grandachn.cronqueue.persistence.PersistenceUtil;
import io.github.grandachn.cronqueue.serialize.FastJsonSerializer;
import io.github.grandachn.cronqueue.serialize.SerializeUtil;
import lombok.Data;

/**
 * @Author by guanda
 * @Date 2019/3/27 13:39
 */
@Data
public class CronQueueContext {
    private static volatile CronQueueContext cronQueueContext;
    private boolean isPersitence;

    private CronQueueContext(){
        init();
    }

    private void init(){
        SerializeUtil.setSerializer(new FastJsonSerializer());
        PersistenceUtil.setPersistencer(new MongoDBPersistencer(false));
        setPersitence(false);
    }

    public static CronQueueContext getContext(){
        if(cronQueueContext == null){
            synchronized (CronQueueContext.class){
                if (cronQueueContext == null){
                    cronQueueContext = new CronQueueContext();
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
}
