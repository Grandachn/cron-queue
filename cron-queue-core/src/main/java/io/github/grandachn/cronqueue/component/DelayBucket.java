package io.github.grandachn.cronqueue.component;

import com.alibaba.fastjson.JSON;
import io.github.grandachn.cronqueue.redis.JedisTemplate;
import lombok.extern.log4j.Log4j;

import java.util.Set;

/**
 * 一组以时间为维度的有序队列，用来存放所有需要延迟的DelayJob（只存放DelayJob Id）
 * @Author by guanda
 * @Date 2019/3/12 11:56
 */
@Log4j
public class DelayBucket {

    /**
     * 添加 DelayJob 到 延迟任务桶中
     * @param key
     * @param scoredSortedItem
     */
    public static boolean addToBucket(String key, ScoredSortedItem scoredSortedItem) {
        try {
            return JedisTemplate.operate().zadd(key, scoredSortedItem.getDelayTime(), JSON.toJSONString(scoredSortedItem)) > 0;
        }catch (Exception e){
            log.error("将scoredSortedItem写入延迟任务桶中失败: " + scoredSortedItem, e);
        }
        return false;
    }

    /**
     * 从延迟任务桶中获取延迟时间最小的ScoredSortedItem
     * @param key
     * @return
     */
    public static ScoredSortedItem getFirstFromBucket(String key) {
        Set set = JedisTemplate.operate().zrange(key, 0L, 1L);
        if (set.size() == 0) {
            return null;
        }
        return (ScoredSortedItem) set.toArray()[0];
    }

    /**
     * 从延迟任务桶中删除 jodId
     * @param key
     * @param scoredSortedItem
     */
    public static boolean deleteFormBucket(String key, ScoredSortedItem scoredSortedItem) {
        return JedisTemplate.operate().zrem(key, JSON.toJSONString(scoredSortedItem)) > 0;
    }
}
