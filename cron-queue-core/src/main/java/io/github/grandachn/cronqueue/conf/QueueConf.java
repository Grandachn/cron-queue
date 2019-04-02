package io.github.grandachn.cronqueue.conf;

import io.github.grandachn.cronqueue.util.ResourceUtils;

public class QueueConf {
    public static final String BUCKET_KEY_PREFIX = ResourceUtils.getString("queue.bucket-prefix", "cronqueue_bucket_");
    public static final String READY_QUEUE_TOPIC_PREFIX = ResourceUtils.getString("queue.ready-prefix", "readyqueue_topic_");
    public static final int BUCKET_NUM = ResourceUtils.getInt("queue.bucket-num", 8);
    public static final String QUEUE_JOB_POOL = ResourceUtils.getString("queue.job-pool-name", "cronqueue_job_pool");
}
