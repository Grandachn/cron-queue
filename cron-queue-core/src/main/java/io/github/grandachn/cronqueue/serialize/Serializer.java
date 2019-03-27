package io.github.grandachn.cronqueue.serialize;

/**
 * @Author by guanda
 * @Date 2019/3/27 11:13
 */
public interface Serializer {
    String serialize(Object o);
    Object deserialize(String s);
    Object deserialize(String s, Class clz);
}
