package org.example.testsproducer.application.exception;

public final class UnknownConfiguredFlowException extends RuntimeException {

    public UnknownConfiguredFlowException(String flow) {
        super("Le flux '" + flow + "' n'existe pas");
    }
}
