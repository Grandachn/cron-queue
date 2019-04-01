package io.github.grandachn.cronqueue;

import com.mongodb.MongoClient;
import io.github.grandachn.cronqueue.component.CronQueue;
import io.github.grandachn.cronqueue.component.CronQueueContext;
import io.github.grandachn.cronqueue.persistence.MongoDBPersistencer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author by guanda
 * @Date 2019/4/1 17:02
 */
@Configuration
@ConditionalOnMissingBean(CronQueueContext.class)
@EnableConfigurationProperties(CronQueueProperties.class)
public class CronQueueAutoConfiguration {
    private final CronQueueProperties cronQueueProperties;

    @Autowired
    private MongoClient mongoClient;

    @Autowired
    public CronQueueAutoConfiguration(CronQueueProperties cronQueueProperties) {
        this.cronQueueProperties = cronQueueProperties;
    }


    @Bean
    @ConditionalOnMissingBean(CronQueueContext.class)
    public CronQueueContext cronQueueContext(){
        System.out.println("cronQueueContext start");
        new MongoDBPersistencer()
        CronQueueContext cronQueueContext = CronQueueContext.getContext();
        //配置参数
        cronQueueContext.setPersitence(false);
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

    @Bean
    @ConditionalOnMissingBean(CronQueue.class)
    public MongoDBPersistencer mongoDBPersistencer(){
        MongoDBPersistencer.
        return cronQueueContext.getCronQueue();
    }
}
