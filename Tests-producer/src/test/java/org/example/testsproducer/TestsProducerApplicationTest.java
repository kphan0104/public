package org.example.testsproducer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.kafka.producer.key-serializer="
                + "org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer="
                + "org.apache.kafka.common.serialization.ByteArraySerializer",
        "tests-producer.publication.acknowledgement-timeout=10s",
        "tests-producer.publication.max-message-bytes=1000000"
})
class TestsProducerApplicationTest {

    @Test
    void contextLoads() {
    }
}
