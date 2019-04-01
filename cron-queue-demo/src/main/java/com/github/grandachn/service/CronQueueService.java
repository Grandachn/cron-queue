package com.github.grandachn.service;

import io.github.grandachn.cronqueue.component.CronQueue;
import io.github.grandachn.cronqueue.job.CronJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Date;

/**
 * @Author by guanda
 * @Date 2019/4/1 17:20
 */
@Service
public class CronQueueService {
//    @Autowired
//    private CronQueueContext cronQueueContext;

    @Autowired
    private CronQueue cronQueue;

    @PostConstruct
    public void start() throws InterruptedException {
        Thread thread = new Thread(() -> {
            System.out.println("start");
        cronQueue.push(CronJob.builder().id("123").topic("cronQueueTest").message("hello").cronPattern("0/5 * * * * ? ").ttrTime(2 * 1000).build());
            while (true) {
            CronJob cronJob1 = (CronJob) cronQueue.pop("cronQueueTest");
            if (cronJob1 == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("sleep");
                continue;
            }
            System.out.println(cronJob1.getMessage() + "---" + new Date());
            cronQueue.finish(cronJob1);
            }
        });
        thread.start();
    }
}
