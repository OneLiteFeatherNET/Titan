package net.onelitefeather.titan.entity;

public class EntityDecodeException extends RuntimeException {
    public EntityDecodeException() {
    }

    public EntityDecodeException(Throwable cause) {
        super(cause);
    }

    public EntityDecodeException(String message) {
        super(message);
    }

    public EntityDecodeException(String message, Throwable cause) {
        super(message, cause);
    }

    public EntityDecodeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
