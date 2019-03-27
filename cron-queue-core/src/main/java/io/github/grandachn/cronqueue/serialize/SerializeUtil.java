package io.github.grandachn.cronqueue.serialize;

/**
 * @Author by guanda
 * @Date 2019/3/27 11:17
 */
public class SerializeUtil {
    private static Serializer serializer;

    public static void setSerializer(Serializer serializer) {
        SerializeUtil.serializer = serializer;
    }

    public static String serialize(Object o) {
        return serializer.serialize(o);
    }

    public static Object deserialize(String s) {
        return serializer.deserialize(s);
    }

    public static Object deserialize(String s, Class clz) {
        return serializer.deserialize(s, clz);
    }
}
