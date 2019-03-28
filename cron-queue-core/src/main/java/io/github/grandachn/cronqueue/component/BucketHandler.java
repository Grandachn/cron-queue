package io.github.grandachn.cronqueue.component;

import io.github.grandachn.cronqueue.job.AbstractJob;
import lombok.extern.log4j.Log4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.github.grandachn.cronqueue.constant.QueueConstant.*;

/**
 * 扫描延迟任务桶中的任务，将到时间的任务放到对应topic的准备队列中
 * @Author by guanda
 * @Date 2019/3/12 15:16
 */
@Log4j
public class BucketHandler {

    private static ExecutorService executorService = Executors.newFixedThreadPool(BUCKET_NUM, new RenameThreadFactory("DelayBucketHandlerThread"));

    public static void start(){
        for(int i = 0; i < BUCKET_NUM; i++){
            final int finalI = i;
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    log.info(BUCKET_KEY_PREFIX + finalI + " handler thread is start");
                    for(;;) {
                        try {
                            String bucketKey = BUCKET_KEY_PREFIX + finalI;
                            ScoredSortedItem item = Bucket.getFirstFromBucket(bucketKey);
                            //没有任务
                            if (item == null) {
                                TimeUnit.MILLISECONDS.sleep(100);
                                continue;
                            }
                            //延迟时间没到
                            if (item.getExecuteTime() > System.currentTimeMillis()) {
                                TimeUnit.MILLISECONDS.sleep(100);
                                continue;
                            }

                            AbstractJob jod = JobPool.getJodById(item.getJodId());

                            //延迟任务元数据不存在
                            if (jod == null) {
                                Bucket.deleteFormBucket(bucketKey,item);
                                continue;
                            }

                            //再次确认延时时间是否到了
                            if (jod.getExecuteTime() > System.currentTimeMillis()) {
                                //删除旧的
                                Bucket.deleteFormBucket(bucketKey,item);
                                //更新一下Bucket中的数据
                                Bucket.addToBucket(bucketKey, new ScoredSortedItem(jod.getId(), jod.getExecuteTime()));
                            } else if (Bucket.deleteFormBucket(bucketKey,item)){
                                //只有成功删除的线程才能将其放到ReadyQueue
                                ReadyQueue.pushToReadyQueue(READY_QUEUE_TOPIC_PREFIX + jod.getTopic(),jod.getId());
                            }

                        }catch (Exception e) {
                            log.error("扫描bucket出错：",e);
                        }
                    }
                }
            };

            executorService.submit(task);
        }
    }

}
