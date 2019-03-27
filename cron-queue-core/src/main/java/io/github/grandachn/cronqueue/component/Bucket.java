package io.github.grandachn.cronqueue.component;

import io.github.grandachn.cronqueue.redis.JedisTemplate;
import io.github.grandachn.cronqueue.serialize.SerializeUtil;
import lombok.extern.log4j.Log4j;

import java.util.Set;

import static io.github.grandachn.cronqueue.constant.QueueConstant.BUCKET_KEY_PREFIX;
import static io.github.grandachn.cronqueue.constant.QueueConstant.BUCKET_NUM;

/**
 * 一组以时间为维度的有序队列，用来存放所有需要延迟的DelayJob（只存放DelayJob Id）
 * @Author by guanda
 * @Date 2019/3/12 11:56
 */
@Log4j
public class Bucket {

    /**
     * 添加 Job 到 延迟任务桶中
     * @param scoredSortedItem
     */
    public static boolean addToBucket(ScoredSortedItem scoredSortedItem) {
        try {
            String key = getDelayBucketKey(scoredSortedItem.getJodId());
            return JedisTemplate.operate().zadd(key, scoredSortedItem.getExecuteTime(), SerializeUtil.serialize(scoredSortedItem)) > 0;
        }catch (Exception e){
            log.error("将scoredSortedItem写入延迟任务桶中失败: " + scoredSortedItem, e);
        }
        return false;
    }

    /**
     * 添加 Job 到 延迟任务桶中
     * @param bucketKey
     * @param scoredSortedItem
     */
    public static boolean addToBucket(String bucketKey, ScoredSortedItem scoredSortedItem) {
        try {
            return JedisTemplate.operate().zadd(bucketKey, scoredSortedItem.getExecuteTime(), SerializeUtil.serialize(scoredSortedItem)) > 0;
        }catch (Exception e){
            log.error("将scoredSortedItem写入延迟任务桶中失败: " + scoredSortedItem, e);
        }
        return false;
    }

    /**
     * 从延迟任务桶中获取延迟时间最小的ScoredSortedItem
     * @param bucketKey
     * @return
     */
    static ScoredSortedItem getFirstFromBucket(String bucketKey) {
        Set set = JedisTemplate.operate().zrange(bucketKey, 0L, 1L);
        if (set.size() == 0) {
            return null;
        }
        return (ScoredSortedItem) SerializeUtil.deserialize(set.toArray()[0].toString());
    }

    /**
     * 从延迟任务桶中删除 jodId
     * @param scoredSortedItem
     */
    public static boolean deleteFormBucket(ScoredSortedItem scoredSortedItem) {
        String key = getDelayBucketKey(scoredSortedItem.getJodId());
        return JedisTemplate.operate().zrem(key, SerializeUtil.serialize(scoredSortedItem)) > 0;
    }

    /**
     * 从延迟任务桶中删除 jodId
     * @param bucketKey
     * @param scoredSortedItem
     */
    static boolean deleteFormBucket(String bucketKey, ScoredSortedItem scoredSortedItem) {
        return JedisTemplate.operate().zrem(bucketKey, SerializeUtil.serialize(scoredSortedItem)) > 0;
    }

    private static String getDelayBucketKey(String jodId) {
        return BUCKET_KEY_PREFIX + ( Math.abs(jodId.hashCode()) % BUCKET_NUM);
    }
}
