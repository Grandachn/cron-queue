package io.github.grandachn.redis.lock;

import io.github.grandachn.cronqueue.redis.lock.RedisReadWriteLock;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @Author by guanda
 * @Date 2019/4/4 16:40
 */
@Slf4j
public class LockTest {
    @Test
    public void RedisReadWriteLockTest(){
        final int[] num = {0};
        for (int i = 0; i < 10 ; i++) {
            Thread thread = new Thread(() -> {
                RedisReadWriteLock.writeLock().tryLock("ccc", 30,300, TimeUnit.SECONDS);
                num[0]++;
                log.info("【写】：" + num[0]);
                RedisReadWriteLock.writeLock().unlock("ccc");
            });
            thread.start();
        }
        for (int i = 0; i < 100 ; i++) {
            Thread thread = new Thread(() -> {
                RedisReadWriteLock.readLock().tryLock("ccc", 30,300, TimeUnit.SECONDS);

                log.info("读：" + num[0]);

                RedisReadWriteLock.readLock().unlock("ccc");
            });
            thread.start();
            if(i % 3 == 0){
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
