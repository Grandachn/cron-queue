package io.github.grandachn.cronqueue.component;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 给线程命名
 * @Author by guanda
 * @Date 2019/3/12 16:50
 */
public class RenameThreadFactory implements ThreadFactory {
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    RenameThreadFactory(String namePrefix) {
        this.namePrefix = namePrefix+"-";
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread( r,namePrefix + threadNumber.getAndIncrement());
        if (t.isDaemon()) {
            t.setDaemon(true);
        }
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
    }
}
