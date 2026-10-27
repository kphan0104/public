package org.example.testsproducer.adapter.in.rest;

import org.example.testsproducer.application.exception.InvalidVariablesException;
import org.example.testsproducer.domain.model.MessageVariable;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BatchVariablesParserTest {

    private final BatchVariablesParser parser = new BatchVariablesParser(new ObjectMapper());

    @Test
    void parsesAllThreeTypesFromTheSwaggerExample() {
        var variables = parser.parse(PublishBatchController.VARIABLES_EXAMPLE);
        assertThat(variables).hasSize(3);
        var random = new Random(42);
        assertThat(variables.get("USER").generate(random)).matches("[a-z]{8}");
        assertThat(Integer.parseInt(variables.get("AMOUNT").generate(random))).isBetween(1, 10000);
        assertThat(variables.get("STATUS").generate(random)).isIn("OK", "ERROR", "PENDING");
        assertThat(parser.parse(null)).isEmpty();
        assertThat(parser.parse(" ")).isEmpty();
        assertThat(parser.parse("{}")).isEmpty();
    }

    @Test
    void rejectsInvalidDefinitionsIncludingUnusedRules() {
        for (String definition : new String[]{
                "not-json", "null", "[]", "{} {}",
                "{\"BAD-NAME\":{\"type\":\"integer\",\"min\":0,\"max\":1}}",
                "{\"X\":null}",
                "{\"X\":{\"type\":\"unknown\"}}",
                "{\"X\":{\"type\":\"string\",\"length\":0,\"characters\":\"a-z\"}}",
                "{\"X\":{\"type\":\"string\",\"length\":8,\"characters\":\"\"}}",
                "{\"X\":{\"type\":\"string\",\"length\":8,\"characters\":\"z-a\"}}",
                "{\"X\":{\"type\":\"integer\",\"min\":2,\"max\":1}}",
                "{\"X\":{\"type\":\"integer\",\"min\":1.5,\"max\":2}}",
                "{\"X\":{\"type\":\"integer\",\"min\":1,\"max\":2147483648}}",
                "{\"X\":{\"type\":\"integer\",\"min\":1,\"max\":2,\"typo\":0}}",
                "{\"X\":{\"type\":\"choice\",\"values\":[]}}",
                "{\"X\":{\"type\":\"choice\",\"values\":[1]}}"
        }) {
            assertThatThrownBy(() -> parser.parse(definition))
                    .as(definition).isInstanceOf(InvalidVariablesException.class);
        }
    }

    @Test
    void supportsInclusiveIntegerBoundsAndTheFullIntegerRange() {
        var random = new Random(42);
        assertThat(new MessageVariable.IntegerVariable(7, 7).generate(random)).isEqualTo("7");
        assertThat(new MessageVariable.IntegerVariable(Integer.MAX_VALUE, Integer.MAX_VALUE)
                .generate(random)).isEqualTo("2147483647");
        assertThat(new MessageVariable.IntegerVariable(Integer.MIN_VALUE, Integer.MIN_VALUE)
                .generate(random)).isEqualTo("-2147483648");
        assertThat(Integer.parseInt(new MessageVariable.IntegerVariable(Integer.MIN_VALUE, Integer.MAX_VALUE)
                .generate(random))).isBetween(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
}
