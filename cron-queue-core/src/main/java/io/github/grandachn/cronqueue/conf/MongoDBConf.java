package io.github.grandachn.cronqueue.conf;

import io.github.grandachn.cronqueue.util.ResourceUtils;

/**
 * @Author by guanda
 * @Date 2019/4/1 15:35
 */
public class MongoDBConf {
    public static final String ADDRESS = ResourceUtils.getString("mongo.address");
    public static final int PORT = ResourceUtils.getInt("mongo.port");
    public static final String DATABASE = ResourceUtils.getString("mongo.database", "cronqueue");
    public static final String COLLECTION = ResourceUtils.getString("mongo.collection", "JobPool");
    public static final String USER = ResourceUtils.getString("mongo.user", "");
    public static final String PASSWORD = ResourceUtils.getString("mongo.password", "");
}
