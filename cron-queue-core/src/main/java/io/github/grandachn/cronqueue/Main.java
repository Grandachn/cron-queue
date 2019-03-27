package io.github.grandachn.cronqueue;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.github.grandachn.cronqueue.component.CronQueue;
import io.github.grandachn.cronqueue.job.Job;
import io.github.grandachn.cronqueue.job.RepeateJob;
import io.github.grandachn.cronqueue.redis.JedisConnectPoll;
import io.github.grandachn.cronqueue.redis.JedisTemplate;
import redis.clients.jedis.Jedis;

/**
 * @Author by guanda
 * @Date 2019/3/15 16:00
 */
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

//        CronQueueContext cronQueueContext = new CronQueueContext();
//        CronQueue.push(RepeateJob.builder().id("1").topic("cronQueueTest").message("hello").executeTime(System.currentTimeMillis()).repeatInterval(1000).repeatSum(10).build());
//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while(true){
//                    RepeateJob repeateJob = (RepeateJob) CronQueue.pop("cronQueueTest");
//                    if(repeateJob == null){
//                        try {
//                            Thread.sleep(100);
//                            System.out.println("sleep");
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        continue;
//                    }
//                    System.out.println(repeateJob.getMessage() + "---" + repeateJob.getExecuteCount());
//                    CronQueue.finish(repeateJob);
//                }
//            }
//        });
//        thread.start();

        final long start = System.currentTimeMillis();
        CronQueue.push(Job.builder().id("1").topic("cronQueueTest").message("hello").executeTime(start + 5000).build());


        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    Job repeateJob = (Job) CronQueue.pop("cronQueueTest");
                    if(repeateJob == null){
                        try {
                            Thread.sleep(100);
                            System.out.println("sleep");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                    System.out.println(repeateJob.getMessage() + "---" + Long.valueOf(System.currentTimeMillis() - start) +"---" + Long.valueOf(repeateJob.getExecuteTime() - System.currentTimeMillis()));
                    CronQueue.finish(repeateJob);
                }
            }
        });
        thread.start();

    }
}
