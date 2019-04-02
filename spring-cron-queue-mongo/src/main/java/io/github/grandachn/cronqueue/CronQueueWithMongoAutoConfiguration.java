package io.github.grandachn.cronqueue;

import com.mongodb.MongoClient;
import io.github.grandachn.cronqueue.component.CronQueue;
import io.github.grandachn.cronqueue.component.CronQueueContext;
import io.github.grandachn.cronqueue.persistence.PersistenceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.github.grandachn.cronqueue.persistence.MongoDbPersistencer;

/**
 * @Author by guanda
 * @Date 2019/4/1 17:02
 */
@Configuration
@ConditionalOnMissingBean(CronQueueContext.class)
public class CronQueueWithMongoAutoConfiguration {
    private final CronQueueProperties cronQueueProperties;

    @Autowired
    public CronQueueWithMongoAutoConfiguration(CronQueueProperties cronQueueProperties) {
        this.cronQueueProperties = cronQueueProperties;
    }

    @Bean
    @ConditionalOnMissingBean(CronQueueContext.class)
    @ConditionalOnClass(MongoClient.class)
    @Autowired
    public CronQueueContext cronQueueContextWithMongo(MongoClient mongoClient){
        System.out.println("cronQueueContextWithMongo start");
        CronQueueContext cronQueueContext = CronQueueContext.getContext();
        PersistenceUtil.setPersistencer(new MongoDbPersistencer(mongoClient, false));
        cronQueueContext.setPersitence(cronQueueProperties.isNeedPersistence());
        if(cronQueueProperties.isAsServer()){
            cronQueueContext.startServer();
        }
        return cronQueueContext;
    }

    @Bean
    @ConditionalOnMissingBean(CronQueue.class)
    public CronQueue cronQueue(){
        CronQueueContext cronQueueContext = CronQueueContext.getContext();
        return cronQueueContext.getCronQueue();
    }
}
