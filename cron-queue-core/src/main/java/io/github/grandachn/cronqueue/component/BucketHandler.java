package io.github.grandachn.cronqueue.component;

import io.github.grandachn.cronqueue.constant.QueueConstant;
import io.github.grandachn.cronqueue.job.AbstractJob;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 扫描延迟任务桶中的任务，将到时间的任务放到对应topic的准备队列中
 *
 * 优化思路，开启两个线程池，workerThreadGroup用于处理获取到的job，workingThreadGroup从bucket中取job
 * @Author by guanda
 * @Date 2019/3/12 15:16
 */
@Slf4j
public class BucketHandler {
    private static volatile AtomicInteger workingThreadNum = new AtomicInteger(8);

    private static volatile AtomicInteger maxWorkingThreadNum = new AtomicInteger(128);

    private static final int monitorIntervalTime = 30 * 1000;

    private static final int noPointSleepTime = 100;

    private static ExecutorService workingThreadGroup = new ThreadPoolExecutor(workingThreadNum.get(), maxWorkingThreadNum.get(),
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            new RenameThreadFactory("BucketHandlerworkingThreadGroup"));

    private static ExecutorService monitorThread = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            new RenameThreadFactory("monitorThread"));

    private static volatile boolean workingThreadGroupIsLive = true;

    private static volatile AtomicLong noPointTime = new AtomicLong(0);

    static void start() {

        //开启boss线程
        for (int i = 0; i < workingThreadNum.get(); i++) {
            final int threadNo = i;
            workingThreadGroup.execute(() -> {
                List<String> bucketKeys = new LinkedList<>();
                if(workingThreadNum.get() < QueueConstant.BUCKET_NUM){
                    for (int j = 0; j < QueueConstant.BUCKET_NUM; j++) {
                        if (j % (workingThreadNum.get() % QueueConstant.BUCKET_NUM) == threadNo % QueueConstant.BUCKET_NUM) {
                            bucketKeys.add(QueueConstant.BUCKET_KEY_PREFIX + j);
                        }
                    }
                }else {
                    bucketKeys.add(QueueConstant.BUCKET_KEY_PREFIX + (threadNo % QueueConstant.BUCKET_NUM));
                }

                log.info("workingThreadGroup thread_" + threadNo + " is start：" + bucketKeys);
                while(workingThreadGroupIsLive){
                    List<ScoredSortedItem> scoredSortedItems = Bucket.getFirstFromBucketsPipeline(bucketKeys);
                    if (scoredSortedItems != null && scoredSortedItems.size() > 0) {
                        int noReadyNum = 0;
                        for (final ScoredSortedItem scoredSortedItem : scoredSortedItems) {
                            //延迟时间没到
                            if (scoredSortedItem.getExecuteTime() > System.currentTimeMillis()) {
                                noReadyNum++;
                                continue;
                            }
                            AbstractJob jod = JobPool.getJodById(scoredSortedItem.getJodId());

                            //延迟任务元数据不存在
                            if (jod == null) {
                                noReadyNum++;
                                Bucket.deleteFormBucket(scoredSortedItem);
                                continue;
                            }

                            //再次确认延时时间是否到了
                            if (jod.getExecuteTime() > System.currentTimeMillis()) {
                                //删除旧的
                                Bucket.deleteFormBucket(scoredSortedItem);
                                //更新一下Bucket中的数据
                                Bucket.addToBucket(Bucket.getDelayBucketKey(scoredSortedItem.getJodId()), new ScoredSortedItem(jod.getId(), jod.getExecuteTime()));
                            } else if (Bucket.deleteFormBucket(scoredSortedItem)){
                                //只有成功删除的线程才能将其放到ReadyQueue
                                ReadyQueue.pushToReadyQueue(QueueConstant.READY_QUEUE_TOPIC_PREFIX + jod.getTopic(), jod.getId());
                            } else{
                                log.debug("scoredSortedItem is process by other thread: {}", scoredSortedItem);
                            }

                        }
                        if(noReadyNum == scoredSortedItems.size()){
                            try {
                                TimeUnit.MILLISECONDS.sleep(noPointSleepTime);
                                noPointTime.getAndAdd(1);
                            } catch (InterruptedException e) {
                                log.error("workingThreadGroup error ",e);
                            }
                        }
                    }else {
                        //Bucket中没有job
                        try {
                            TimeUnit.MILLISECONDS.sleep(noPointSleepTime);
                            noPointTime.getAndAdd(1);
                        } catch (InterruptedException e) {
                            log.error("workingThreadGroup error ",e);
                        }
                    }
                }
            });
        }
    }

    static void stop(){
        workingThreadGroupIsLive = false;
        workingThreadGroup.shutdown();
        while (!workingThreadGroup.isTerminated()) {
            log.info("waitting for workingThreadGroup shutdown");
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                log.error("stop workingThreadGroup error", e);
            }
        }
        log.info("workingThreadGroup is shutdown");

    }

    static void restart(){
        long time = System.currentTimeMillis();
        stop();
        workingThreadGroup = new ThreadPoolExecutor(workingThreadNum.get(), maxWorkingThreadNum.get(),
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new RenameThreadFactory("BucketHandlerworkingThreadGroup"));
        workingThreadGroupIsLive = true;
        start();
        log.info("restart cost time " + (System.currentTimeMillis() - time) + "ms");
    }

    //后台线程，监控是否需要动态调整工作线程
    static void minotor(){
        monitorThread.execute(() -> {
            //冷启动，让出线程
            try {
                TimeUnit.SECONDS.sleep(10);
                noPointTime.set(0);
            } catch (InterruptedException e) {
                log.error("monitorThread error", e);
            }
            for(;;){
                try {
                    TimeUnit.MILLISECONDS.sleep(monitorIntervalTime);
                } catch (InterruptedException e) {
                    log.error("monitorThread error", e);
                }
                //如果1秒钟超过16次noPointTime，对应workThead sleep 100ms
                log.info("minotor noPointTime: {}, working thread:{}", noPointTime.get(), workingThreadNum.get());
                if(noPointTime.get() > (8 * monitorIntervalTime * noPointSleepTime / 1000 / 100) && workingThreadNum.get() > 1){
                    workingThreadNum.set(workingThreadNum.get() / 2);
                    log.info("minotor noPointTime: {}", noPointTime.get());
                    restart();
                    log.info("workingThreadNum change to: {}", workingThreadNum.get());
                    noPointTime.set(0);
                    continue;
                }
                if(noPointTime.get() < (4 * monitorIntervalTime * noPointSleepTime / 1000 / 100) && workingThreadNum.get() < maxWorkingThreadNum.get()){
                    workingThreadNum.set(workingThreadNum.get() * 2);
                    log.info("minotor noPointTime: {}", noPointTime.get());
                    restart();
                    log.info("workingThreadNum change to: {}", workingThreadNum.get());
                    noPointTime.set(0);
                }
                noPointTime.set(0);
            }
        });
    }

}
