package io.github.grandachn.cronqueue.component;

import io.github.grandachn.cronqueue.job.AbstractJob;
import io.github.grandachn.cronqueue.redis.DistributedRedisLock;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

import static io.github.grandachn.cronqueue.constant.QueueConstant.*;

/**
 * 扫描延迟任务桶中的任务，将到时间的任务放到对应topic的准备队列中
 *
 * 优化思路，开启两个线程池，workerThreadGroup用于处理获取到的job，bossThreadGroup从bucket中取job
 * @Author by guanda
 * @Date 2019/3/12 15:16
 */
@Slf4j
public class BucketHandler {
    private static final int bossThreadNum = 2;

    private static ExecutorService bossThreadGroup = Executors.newFixedThreadPool(bossThreadNum, new RenameThreadFactory("BucketHandlerBossThreadGroup"));

    private static ExecutorService workerThreadGroup =  new ThreadPoolExecutor(4, BUCKET_NUM,
            5L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new RenameThreadFactory("BucketHandlerWorkerThreadGroup"));

    private static ExecutorService executorService = Executors.newFixedThreadPool(BUCKET_NUM, new RenameThreadFactory("DelayBucketHandlerThread"));

    public static void start() {

        //开启boss线程
        for (int i = 0; i < bossThreadNum; i++) {
            final int threadNo = i;
            bossThreadGroup.execute(() -> {
                List<String> bucketKeys = new LinkedList<>();
                for (int j = 0; j < BUCKET_NUM; j++) {
                    if (BUCKET_NUM % (threadNo + 1) == threadNo) {
                        bucketKeys.add(BUCKET_KEY_PREFIX + j);
                    }
                }
                for(;;){
                    List<ScoredSortedItem> scoredSortedItems = Bucket.getFirstFromBucketsPipeline(bucketKeys);
                    if (scoredSortedItems != null && scoredSortedItems.size() > 0) {
                        CountDownLatch countDownLatch = new CountDownLatch(scoredSortedItems.size());
                        for (final ScoredSortedItem scoredSortedItem : scoredSortedItems) {
                            //提交给workerThreadGroup
                            //加分布式锁，避免分布式强占
                            if(DistributedRedisLock.tryAcquire("bucket_handler_" + scoredSortedItem.getJodId())){
                                workerThreadGroup.submit(() -> {
                                    try{
                                        //延迟时间没到
                                        if (scoredSortedItem.getExecuteTime() > System.currentTimeMillis()) {
                                            return;
                                        }
                                        AbstractJob jod = JobPool.getJodById(scoredSortedItem.getJodId());

                                        //延迟任务元数据不存在
                                        if (jod == null) {
                                            Bucket.deleteFormBucket(scoredSortedItem);
                                            return;
                                        }

                                        //再次确认延时时间是否到了
                                        if (jod.getExecuteTime() > System.currentTimeMillis()) {
                                            //删除旧的
                                            Bucket.deleteFormBucket(scoredSortedItem);
                                            //更新一下Bucket中的数据
                                            Bucket.addToBucket(Bucket.getDelayBucketKey(scoredSortedItem.getJodId()), new ScoredSortedItem(jod.getId(), jod.getExecuteTime()));
                                        } else if (Bucket.deleteFormBucket(scoredSortedItem)){
                                            //只有成功删除的线程才能将其放到ReadyQueue
                                            ReadyQueue.pushToReadyQueue(READY_QUEUE_TOPIC_PREFIX + jod.getTopic(), jod.getId());
                                        } else{
                                            System.out.println("get fail " + scoredSortedItem);
                                        }
                                    } finally {
                                        countDownLatch.countDown();
                                        //释放分布式锁
                                        DistributedRedisLock.release("bucket_handler_" + scoredSortedItem.getJodId());
                                    }
                                });
                            }else {
                                countDownLatch.countDown();
                            }
                        }
                        try {
                            countDownLatch.await();
                        } catch (InterruptedException e) {
                            log.error("bossThreadGroup error ",e);
                        }
                    }else {
                        //Bucket中没有job
                        try {
                            TimeUnit.MILLISECONDS.sleep(50);
                        } catch (InterruptedException e) {
                            log.error("bossThreadGroup error ",e);
                        }
                    }
                }
            });
        }

//
//        for (int i = 0; i < BUCKET_NUM; i++) {
//            final int finalI = i;
//            Runnable task = new Runnable() {
//                @Override
//                public void run() {
//                    log.info(BUCKET_KEY_PREFIX + finalI + " handler thread is start");
//                    for (; ; ) {
//                        try {
//                            String bucketKey = BUCKET_KEY_PREFIX + finalI;
//                            ScoredSortedItem item = Bucket.getFirstFromBucket(bucketKey);
//                            //没有任务
//                            if (item == null) {
//                                TimeUnit.MILLISECONDS.sleep(100);
//                                continue;
//                            }
//                            //延迟时间没到
//                            if (item.getExecuteTime() > System.currentTimeMillis()) {
//                                TimeUnit.MILLISECONDS.sleep(100);
//                                continue;
//                            }
//
//                            AbstractJob jod = JobPool.getJodById(item.getJodId());
//
//                            //延迟任务元数据不存在
//                            if (jod == null) {
//                                Bucket.deleteFormBucket(bucketKey, item);
//                                continue;
//                            }
//
//                            //再次确认延时时间是否到了
//                            if (jod.getExecuteTime() > System.currentTimeMillis()) {
//                                //删除旧的
//                                Bucket.deleteFormBucket(bucketKey, item);
//                                //更新一下Bucket中的数据
//                                Bucket.addToBucket(bucketKey, new ScoredSortedItem(jod.getId(), jod.getExecuteTime()));
//                            } else if (Bucket.deleteFormBucket(bucketKey, item)) {
//                                //只有成功删除的线程才能将其放到ReadyQueue
//                                ReadyQueue.pushToReadyQueue(READY_QUEUE_TOPIC_PREFIX + jod.getTopic(), jod.getId());
//                            }
//
//                        } catch (Exception e) {
//                            log.error("扫描bucket出错：", e);
//                        }
//                    }
//                }
//            };
//
//            executorService.submit(task);
//        }
    }
}
