package fr.itgp.testsproducer.application.port.in;

public interface PublishEventUseCase {

    PublishEventResult publish(PublishEventCommand command);
}
