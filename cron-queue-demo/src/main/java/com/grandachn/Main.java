package com.grandachn;

import io.github.grandachn.cronqueue.component.CronQueue;
import io.github.grandachn.cronqueue.component.CronQueueContext;
import io.github.grandachn.cronqueue.job.CronJob;
import io.github.grandachn.cronqueue.persistence.MongoDbPersistencer;

import java.util.Date;

/**
 * @Author by guanda
 * @Date 2019/4/2 11:53
 */
public class Main {
    public static void main(String[] args) throws InterruptedException {
        CronQueueContext cronQueueContext = CronQueueContext.getContext();
        cronQueueContext.setPersitence(false);
        cronQueueContext.startServer();
        cronQueueContext.setPersitencer(new MongoDbPersistencer(false));
        CronQueue cronQueue = cronQueueContext.getCronQueue();
        cronQueue.push(CronJob.builder().id("123").topic("cronQueueTest").message("hello").cronPattern("0/5 * * * * ? ").ttrTime(2 * 1000).build());


        Thread thread = new Thread(() -> {
            while (true) {
                CronJob cronJob1 = (CronJob) cronQueue.pop("cronQueueTest");
                if (cronJob1 == null) {
                    continue;
                }
                System.out.println( cronJob1.getMessage() + "---" + new Date());
                cronQueue.finish(cronJob1);
            }
        });

        thread.start();
    }
}
