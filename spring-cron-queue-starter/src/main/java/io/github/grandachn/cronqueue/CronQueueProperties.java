package io.github.grandachn.cronqueue;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Author by guanda
 * @Date 2019/4/1 16:54
 */
@ConfigurationProperties(prefix = "cronqueue")
public class CronQueueProperties {
    private boolean asServer;

    public boolean isAsServer() {
        return asServer;
    }

    public void setAsServer(boolean asServer) {
        this.asServer = asServer;
    }
}
