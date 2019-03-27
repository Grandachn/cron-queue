package io.github.grandachn.cronqueue;

import io.github.grandachn.cronqueue.component.CronQueue;
import io.github.grandachn.cronqueue.job.CommonJob;
import io.github.grandachn.cronqueue.job.RepeateJob;
import lombok.extern.log4j.Log4j;

/**
 * @Author by guanda
 * @Date 2019/3/15 16:00
 */
@Log4j
public class Main {
    public static void main(String[] args) {
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
        cronQueueContext.setPersitence(true);
        cronQueueContext.startServer();
        CronQueue.push(RepeateJob.builder().id("abb").topic("cronQueueTest").message("hello").executeTime(System.currentTimeMillis()).repeatInterval(2000).repeatSum(10).build());
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    RepeateJob repeateJob = (RepeateJob) CronQueue.pop("cronQueueTest");
                    if(repeateJob == null){
                        try {
                            Thread.sleep(100);
                            System.out.println("sleep");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                    log.info(repeateJob.getMessage() + "---" + repeateJob.getExecuteCount());
                    CronQueue.finish(repeateJob);
                }
            }
        });
        thread.start();

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
