package org.example.testsproducer.application.exception;

public final class LastConfiguredFlowDeletionException
        extends RuntimeException {

    public LastConfiguredFlowDeletionException() {
        super("Le dernier flux configuré ne peut pas être supprimé");
    }
}
