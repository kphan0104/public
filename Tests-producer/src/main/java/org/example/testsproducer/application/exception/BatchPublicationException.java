package org.example.testsproducer.application.exception;

public class BatchPublicationException extends RuntimeException {

    private final int requestedCount;
    private final int publishedCount;

    public BatchPublicationException(RuntimeException cause, int requestedCount, int publishedCount) {
        super(cause.getMessage(), cause);
        this.requestedCount = requestedCount;
        this.publishedCount = publishedCount;
    }

    public int requestedCount() {
        return requestedCount;
    }

    public int publishedCount() {
        return publishedCount;
    }
}
