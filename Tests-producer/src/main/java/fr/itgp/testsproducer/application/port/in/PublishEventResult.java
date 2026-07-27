package fr.itgp.testsproducer.application.port.in;

public record PublishEventResult(
        String topic,
        int partition,
        long offset,
        int eventSize,
        String timestamp
) {
}
