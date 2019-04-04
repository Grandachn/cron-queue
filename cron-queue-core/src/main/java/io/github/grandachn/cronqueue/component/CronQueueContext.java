package io.github.grandachn.cronqueue.component;

import io.github.grandachn.cronqueue.persistence.DefaultPersistencer;
import io.github.grandachn.cronqueue.persistence.PersistenceUtil;
import io.github.grandachn.cronqueue.persistence.Persistencer;
import io.github.grandachn.cronqueue.serialize.FastJsonSerializer;
import io.github.grandachn.cronqueue.serialize.SerializeUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author by guanda
 * @Date 2019/3/27 13:39
 */
@Slf4j
@Data
public class CronQueueContext {
    private static volatile CronQueueContext cronQueueContext;
    private static volatile CronQueue cronQueue;
    private boolean isPersitence;

    private CronQueueContext(){
        SerializeUtil.setSerializer(new FastJsonSerializer());
        PersistenceUtil.setPersistencer(new DefaultPersistencer());
        setPersitence(false);
    }

    public void init(){
        try {
            PersistenceUtil.setPersistencer((Persistencer) Class.forName("io.github.grandachn.persistence.MongoDBPersistencer").getConstructor(Boolean.class).newInstance(false));
        } catch (Exception e) {
            log.error("CronQueueContext init error: ", e);
        }
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

    public void setPersitencer(Persistencer persitencer){
        PersistenceUtil.setPersistencer(persitencer);
    }
}
