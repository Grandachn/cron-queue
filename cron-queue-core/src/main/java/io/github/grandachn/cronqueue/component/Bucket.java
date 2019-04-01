package io.github.grandachn.cronqueue.component;

import io.github.grandachn.cronqueue.constant.QueueConstant;
import io.github.grandachn.cronqueue.redis.JedisConnectPoll;
import io.github.grandachn.cronqueue.redis.JedisTemplate;
import io.github.grandachn.cronqueue.serialize.SerializeUtil;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * 一组以时间为维度的有序队列，用来存放所有需要延迟的DelayJob（只存放DelayJob Id）
 * @Author by guanda
 * @Date 2019/3/12 11:56
 */
@Slf4j
public class Bucket {

    /**
     * 添加 CommonJob 到 任务桶中
     * @param scoredSortedItem 桶中元素
     */
    public static boolean addToBucket(ScoredSortedItem scoredSortedItem) {
        try {
            String key = getDelayBucketKey(scoredSortedItem.getJodId());
            return JedisTemplate.operate().zadd(key, scoredSortedItem.getExecuteTime(), SerializeUtil.serialize(scoredSortedItem)) > 0;
        }catch (Exception e){
            log.error("将scoredSortedItem写入延迟任务桶中失败: {}", scoredSortedItem, e);
        }
        return false;
    }

    /**
     * 添加 CommonJob 到 任务桶中
     * @param bucketKey 桶键
     * @param scoredSortedItem 桶中元素
     */
    public static boolean addToBucket(String bucketKey, ScoredSortedItem scoredSortedItem) {
        try {
            return JedisTemplate.operate().zadd(bucketKey, scoredSortedItem.getExecuteTime(), SerializeUtil.serialize(scoredSortedItem)) > 0;
        }catch (Exception e){
            log.error("将scoredSortedItem写入延迟任务桶中失败: {}" , scoredSortedItem, e);
        }
        return false;
    }

    /**
     * 从任务桶中获取延迟时间最小的ScoredSortedItem
     * @param bucketKey 桶键
     * @return 桶中元素
     */
    static ScoredSortedItem getFirstFromBucket(String bucketKey) {
        Set set = JedisTemplate.operate().zrange(bucketKey, 0L, 1L);
        if (set.size() == 0) {
            return null;
        }
        return (ScoredSortedItem) SerializeUtil.deserialize(set.toArray()[0].toString());
    }

    /**
     * 从任务桶中获取延迟时间最小的ScoredSortedItem
     * @param bucketKeys 桶键
     * @return 桶中元素
     */
    static List<ScoredSortedItem> getFirstFromBucketsPipeline (List<String> bucketKeys) {
        List<ScoredSortedItem> scoredSortedItems = new LinkedList<>();
        Jedis jedis = JedisConnectPoll.getJedis();
        Pipeline pipe =  jedis.pipelined();
        pipe.clear();
        List<Response<Set<String>>> responses = new LinkedList<>();
        for (String bucketKey : bucketKeys){
            responses.add(pipe.zrange(bucketKey, 0L, 1L));
        }
        pipe.sync();
        for(Response<Set<String>> response : responses){
            Set<String> set = response.get();
            if (set.size() > 0) {
                scoredSortedItems.add((ScoredSortedItem) SerializeUtil.deserialize(set.toArray()[0].toString()));
            }
        }
        pipe.close();
        jedis.close();
        return scoredSortedItems;
    }

    /**
     * 从任务桶中删除 jodId
     * @param scoredSortedItem 桶中元素
     */
    public static boolean deleteFormBucket(ScoredSortedItem scoredSortedItem) {
        String key = getDelayBucketKey(scoredSortedItem.getJodId());
        return JedisTemplate.operate().zrem(key, SerializeUtil.serialize(scoredSortedItem)) > 0;
    }

    /**
     * 从任务桶中删除 jodId
     * @param bucketKey 桶键
     * @param scoredSortedItem 桶中元素
     */
    static boolean deleteFormBucket(String bucketKey, ScoredSortedItem scoredSortedItem) {
        return JedisTemplate.operate().zrem(bucketKey, SerializeUtil.serialize(scoredSortedItem)) > 0;
    }

    public static String getDelayBucketKey(String jodId) {
        return QueueConstant.BUCKET_KEY_PREFIX + ( Math.abs(jodId.hashCode()) % QueueConstant.BUCKET_NUM);
    }
}
