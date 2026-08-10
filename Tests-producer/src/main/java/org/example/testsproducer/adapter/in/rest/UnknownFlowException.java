package org.example.testsproducer.adapter.in.rest;

final class UnknownFlowException extends RuntimeException {

    UnknownFlowException(String flow) {
        super("Le flux '" + flow + "' n'existe pas dans flow-topics.yml");
    }
}
