package net.onelitefeather.titan.entity;

public class EntityEncodeException extends RuntimeException {
    public EntityEncodeException() {
    }

    public EntityEncodeException(Throwable cause) {
        super(cause);
    }

    public EntityEncodeException(String message) {
        super(message);
    }

    public EntityEncodeException(String message, Throwable cause) {
        super(message, cause);
    }

    public EntityEncodeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
