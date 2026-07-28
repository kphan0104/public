package org.example.testsproducer.adapter.in.rest;

public class InvalidOriginalMessageException extends RuntimeException {

    public InvalidOriginalMessageException(String message) {
        super(message);
    }

    public InvalidOriginalMessageException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
