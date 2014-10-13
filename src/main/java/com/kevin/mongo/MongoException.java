package com.kevin.mongo;

/**
 * (description)
 *
 */
public class MongoException extends RuntimeException {

    public MongoException() {
    }

    public MongoException(String message) {
        super(message);
    }

    public MongoException(String message, Throwable cause) {
        super(message, cause);
    }

    public MongoException(Throwable cause) {
        super(cause);
    }
}
