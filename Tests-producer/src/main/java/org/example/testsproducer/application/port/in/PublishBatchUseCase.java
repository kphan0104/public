package org.example.testsproducer.application.port.in;

public interface PublishBatchUseCase {

    PublishBatchResult publish(PublishBatchCommand command);
}
