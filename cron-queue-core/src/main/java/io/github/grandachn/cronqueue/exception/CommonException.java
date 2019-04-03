package io.github.grandachn.cronqueue.exception;

/**
 * @Author by guanda
 * @Date 2019/4/1 14:58
 */
public class CommonException extends RuntimeException{
    public CommonException(String message) {
        super(message);
    }

    public CommonException(String message, Throwable cause) {
        super(message, cause);
    }

    public static CommonException build(Throwable cause, String message, Object... objs){
        for(Object obj : objs){
            message = message.replaceFirst("\\{\\}", obj.toString());
        }
        return new CommonException(message);
    }

    public static CommonException build(Throwable cause, Object... objs){
        StringBuilder stringBuilder = new StringBuilder();
        for(Object obj : objs){
            stringBuilder.append(obj);
        }
        return new CommonException(stringBuilder.toString(), cause);
    }

    public static CommonException build(Throwable cause, Object obj){
        return new CommonException(obj.toString(), cause);
    }

    public static CommonException build(Throwable cause, String s){
        return new CommonException(s, cause);
    }

    public static CommonException build(String s){
        return new CommonException(s);
    }
}
