package org.example.testsproducer.application.service;

import org.example.testsproducer.application.exception.BatchPublicationException;
import org.example.testsproducer.application.exception.EventPublicationException;
import org.example.testsproducer.application.exception.InvalidVariablesException;
import org.example.testsproducer.application.exception.MessageTooLargeException;
import org.example.testsproducer.application.port.in.PublishBatchCommand;
import org.example.testsproducer.application.port.in.PublishBatchResult;
import org.example.testsproducer.application.port.in.PublishBatchUseCase;
import org.example.testsproducer.application.port.in.PublishEventCommand;
import org.example.testsproducer.application.port.in.PublishEventUseCase;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

public final class PublishBatchService implements PublishBatchUseCase {

    private static final Pattern TOKEN = Pattern.compile("\\$\\{([^{}]*)}");

    private final PublishEventUseCase publishEvent;

    public PublishBatchService(PublishEventUseCase publishEvent) {
        this.publishEvent = publishEvent;
    }

    @Override
    public PublishBatchResult publish(PublishBatchCommand command) {
        var placeholders = TOKEN.matcher(command.originalMessage());
        while (placeholders.find()) {
            String name = placeholders.group(1);
            if (!command.variables().containsKey(name)) {
                throw new InvalidVariablesException("Aucune règle définie pour ${" + name + "}");
            }
        }

        for (int index = 0; index < command.count(); index++) {
            Map<String, String> values = new HashMap<>();
            command.variables().forEach((name, rule) ->
                    values.put(name, rule.generate(ThreadLocalRandom.current())));
            String message = TOKEN.matcher(command.originalMessage()).replaceAll(
                    match -> java.util.regex.Matcher.quoteReplacement(values.get(match.group(1)))
            );
            try {
                publishEvent.publish(new PublishEventCommand(
                        command.topic(),
                        command.flowName(),
                        message,
                        command.eventTemplate()
                ));
            } catch (EventPublicationException | MessageTooLargeException exception) {
                throw new BatchPublicationException(exception, command.count(), index);
            }
        }
        return new PublishBatchResult(command.topic(), command.count(), command.count());
    }
}
