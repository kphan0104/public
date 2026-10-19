package org.example.testsproducer.application.port.in;

import java.util.Map;

public interface FlowManagementUseCase {

    String topicFor(String flow);

    Map<String, String> topics();

    FlowUpsertResult upsertFlow(String flow, String topic);

    OriginalMessageUpsertResult upsertOriginalMessage(
            String flow,
            String originalMessage
    );
}
