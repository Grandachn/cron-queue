package io.github.grandachn.cronqueue.persistence;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import io.github.grandachn.cronqueue.conf.MongoDBConf;
import io.github.grandachn.cronqueue.job.AbstractJob;
import io.github.grandachn.cronqueue.persistence.Persistencer;
import io.github.grandachn.cronqueue.util.ReflectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;

import java.lang.reflect.Field;
import java.util.Date;

/**
 * 基于MongoDB的落盘持久化
 * @Author by guanda
 * @Date 2019/3/27 16:10
 */
@Slf4j
public class MongoDbPersistencer implements Persistencer {
    private MongoCollection<Document> mongoCollection;

    private boolean isHardDelete;

    public MongoDbPersistencer(MongoClient mongoClient, boolean isHardDelete){
        this.isHardDelete = isHardDelete;
        MongoDatabase mongoDatabase = mongoClient.getDatabase(MongoDBConf.DATABASE);
        mongoCollection = mongoDatabase.getCollection(MongoDBConf.COLLECTION);
    }

    public MongoDbPersistencer(boolean isHardDelete){
        this.isHardDelete = isHardDelete;
        initConn();
    }

    public void initConn() {
        MongoClient mongoClient;

        ServerAddress serverAddress = new ServerAddress(MongoDBConf.ADDRESS, MongoDBConf.PORT);

        if(!"".equals(MongoDBConf.USER)){
            MongoCredential credential = MongoCredential.createCredential(MongoDBConf.USER, MongoDBConf.DATABASE, MongoDBConf.PASSWORD.toCharArray());
            mongoClient = new MongoClient(serverAddress, credential, MongoClientOptions.builder().build());
        }else{
            mongoClient = new MongoClient(serverAddress);
        }

        MongoDatabase mongoDatabase = mongoClient.getDatabase(MongoDBConf.DATABASE);
        mongoCollection = mongoDatabase.getCollection(MongoDBConf.COLLECTION);
    }

    @Override
    public boolean insertOrUpdate(AbstractJob job) {
        Document document = jobToDocument(job);
        document.append("delete", false);
        if (document.containsKey("id")) {
            UpdateResult result = mongoCollection.replaceOne(Filters.eq("id", job.getId()), document, new ReplaceOptions().upsert(true));
            if(result.getModifiedCount() > 0 || !result.getUpsertedId().isNull()){
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean delete(AbstractJob job) {
        if(isHardDelete){
            DeleteResult deleteResult = mongoCollection.deleteOne(Filters.eq("id", job.getId()));
            return deleteResult.getDeletedCount() > 0;
        }else{
            Document document = jobToDocument(job);
            document.append("delete", true);
            if (document.containsKey("id")) {
                UpdateResult result = mongoCollection.replaceOne(Filters.eq("id", job.getId()), document, new ReplaceOptions().upsert(true));
                if(result.getModifiedCount() > 0 || !result.getUpsertedId().isNull()){
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public String get(String jobId) {
        FindIterable<Document> queryRst = mongoCollection.find(Filters.eq("id", jobId));
        MongoCursor<Document> cursor = queryRst.iterator();
        if (cursor.hasNext()){
            return cursor.next().toJson(JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build());
        }
        return "";
    }

    private Document jobToDocument(AbstractJob job){
        Document document = new Document();
        for(Field field: ReflectionUtils.getAllFields(job)){
            field.setAccessible(true);
            try {
                document.append(field.getName(), field.get(job));
            } catch (IllegalAccessException e) {
                log.error("Job对象转mongodb文档异常", e);
            }
        }
        //添加job的实际class
        document.append("class", job.getClass().getName());
        document.append("createTime", new Date());
        return document;
    }
}
