package io.github.grandachn.cronqueue;

import io.github.grandachn.cronqueue.component.CronQueue;
import io.github.grandachn.cronqueue.component.CronQueueContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author by guanda
 * @Date 2019/4/1 17:02
 */
@Slf4j
@Configuration
@ConditionalOnMissingBean(CronQueueContext.class)
@EnableConfigurationProperties(CronQueueProperties.class)
public class CronQueueAutoConfiguration {
    private final CronQueueProperties cronQueueProperties;

    @Autowired
    public CronQueueAutoConfiguration(CronQueueProperties cronQueueProperties) {
        this.cronQueueProperties = cronQueueProperties;
    }


    @Bean
    @ConditionalOnMissingBean(CronQueueContext.class)
    @ConditionalOnMissingClass({"com.mongodb.MongoClient"})
    public CronQueueContext cronQueueContext(){
        log.info("cronQueueContext start");
        CronQueueContext cronQueueContext = CronQueueContext.getContext();
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
