package io.github.grandachn.cronqueue;

import io.github.grandachn.cronqueue.component.BucketHandler;
import io.github.grandachn.cronqueue.component.CronQueue;
import io.github.grandachn.cronqueue.component.CronQueueContext;
import io.github.grandachn.cronqueue.job.CronJob;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @Author by guanda
 * @Date 2019/3/15 16:00
 */
@Slf4j
public class Main {
    public static void main(String[] args) throws InterruptedException {
//        try(Jedis jedis = JedisConnectPoll.getJedis()){
//            jedis.set("grandaTest", "hello");
//            System.out.println(jedis.get("grandaTest"));
//            jedis.del("grandaTest");
//            System.out.println(jedis.get("grandaTest"));
//        }
//
//        System.out.println(JedisTemplate.operate().set("grandaTest", "123456"));
//        System.out.println(JedisTemplate.operate().get("grandaTest"));


        CronQueueContext cronQueueContext = CronQueueContext.getContext();
        cronQueueContext.setPersitence(false);
        cronQueueContext.startServer();
        for (int i = 0; i < 80; i++) {
            CronQueue.push(CronJob.builder().id("123" + i).topic("cronQueueTest").message("hello" + i).cronPattern("0/2 * * * * ? ").ttrTime(100 * 1000).build());
        }

        for (int i = 0; i < 8; i++) {
            Thread thread = new Thread(() -> {
                while (true) {
                    CronJob cronJob1 = (CronJob) CronQueue.pop("cronQueueTest");
                    if (cronJob1 == null) {
//
                        continue;
                    }
                    log.info(cronJob1.getMessage() + "---" + new Date());
                    CronQueue.finish(cronJob1);
                }
            });
            thread.start();
        }

//        TimeUnit.SECONDS.sleep(20);
//        cronQueueContext.stopServer();
//        TimeUnit.SECONDS.sleep(10);
//        cronQueueContext.restartServer();
//        TimeUnit.SECONDS.sleep(20);
//        CronQueue.stop(CronJob.builder().id("1231").topic("cronQueueTest").message("hello").cronPattern("0/10 * * * * ?").build());
//        System.out.println("stop");


//        CronQueueContext cronQueueContext = CronQueueContext.getContext();
//        cronQueueContext.setPersitence(true);
//        cronQueueContext.startServer();
//
//        final long start = System.currentTimeMillis();
//        CronQueue.push(CommonJob.builder().id("12").topic("cronQueueTest").message("hello").executeTime(start + 5000).build());
//
//
//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while(true){
//                    CommonJob repeateJob = (CommonJob) CronQueue.pop("cronQueueTest");
//                    if(repeateJob == null){
//                        try {
//                            Thread.sleep(100);
//                            System.out.println("sleep");
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        continue;
//                    }
//                    System.out.println(repeateJob.getMessage() + "---" + Long.valueOf(System.currentTimeMillis() - start) +"---" + Long.valueOf(repeateJob.getExecuteTime() - System.currentTimeMillis()));
//                    CronQueue.finish(repeateJob);
//                }
//            }
//        });
//        thread.start();

    }
}
