package io.github.grandachn.cronqueue.component;

import io.github.grandachn.cronqueue.job.Job;
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

    private ExecutorService executorService = Executors.newFixedThreadPool(DELAY_BUCKET_NUM, new RenameThreadFactory("DelayBucketHandlerThread"));

    private void start(){
        for(int i = 0; i < DELAY_BUCKET_NUM; i++){
            final int finalI = i;
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    log.info(DELAY_BUCKET_KEY_PREFIX + finalI + " handler thread is start");
                    while (true) {
                        try {
                            String delayBucketKey = DELAY_BUCKET_KEY_PREFIX + finalI;
                            ScoredSortedItem item = Bucket.getFirstFromBucket(delayBucketKey);
                            //没有任务
                            if (item == null) {
                                TimeUnit.SECONDS.sleep(1L);
                                continue;
                            }
                            //延迟时间没到
                            if (item.getExecuteTime() > System.currentTimeMillis()) {
                                TimeUnit.SECONDS.sleep(1L);
                                continue;
                            }

                            Job delayJod = JobPool.getDelayJodById(item.getDelayJodId());

                            //延迟任务元数据不存在
                            if (delayJod == null) {
                                Bucket.deleteFormBucket(delayBucketKey,item);
                                continue;
                            }

                            //再次确认延时时间是否到了
                            if (delayJod.getExecuteTime() > System.currentTimeMillis()) {
                                //删除旧的
                                Bucket.deleteFormBucket(delayBucketKey,item);
                                //更新一下delayBucket中的数据
                                Bucket.addToBucket(delayBucketKey, new ScoredSortedItem(delayJod.getId(), delayJod.getExecuteTime()));
                            } else if (Bucket.deleteFormBucket(delayBucketKey,item)){
                                //只有成功删除的线程才能将其放到ReadyQueue
                                ReadyQueue.pushToReadyQueue(READY_QUEUE_TOPIC_PREFIX + delayJod.getTopic(),delayJod.getId());
                            }

                        }catch (Exception e) {
                            log.error("扫描delaybucket出错：",e);
                        }
                    }
                }
            };

            executorService.submit(task);
        }

    }


}
