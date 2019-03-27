package io.github.grandachn.cronqueue.serialize;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * @Author by guanda
 * @Date 2019/3/27 11:15
 */
public class FastJsonSerializer implements Serializer{
    static {
        ParserConfig.getGlobalInstance().addAccept("io.github.grandachn.cronqueue.");
    }

    @Override
    public String serialize(Object o) {
        return JSON.toJSONString(o, SerializerFeature.WriteClassName);
    }

    @Override
    public Object deserialize(String s) {
        return JSON.parse(s);
    }

    @Override
    public Object deserialize(String s, Class clz) {
        return JSON.parseObject(s, clz);
    }
}
