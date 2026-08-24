package org.example.testsproducer.application.port.in;

public interface PublishRawMessageUseCase {

    PublishRawMessageResult publish(PublishRawMessageCommand command);
}
