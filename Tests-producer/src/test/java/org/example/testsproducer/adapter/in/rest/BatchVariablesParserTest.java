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
                "{\"X\":{\"type\":\"integer\",\"min\":1,\"max\":2,\"padding\":-1}}",
                "{\"X\":{\"type\":\"integer\",\"min\":1,\"max\":2,\"padding\":10001}}",
                "{\"X\":{\"type\":\"integer\",\"min\":1,\"max\":2,\"padding\":2.5}}",
                "{\"X\":{\"type\":\"integer\",\"min\":1,\"max\":2,\"padding\":\"5\"}}",
                "{\"X\":{\"type\":\"integer\",\"min\":1,\"max\":2,\"padding\":null}}",
                "{\"X\":{\"type\":\"choice\",\"values\":[]}}",
                "{\"X\":{\"type\":\"choice\",\"values\":[1]}}"
        }) {
            assertThatThrownBy(() -> parser.parse(definition))
                    .as(definition).isInstanceOf(InvalidVariablesException.class);
        }
    }

    @Test
    void padsIntegerVariablesWithZerosWithoutTruncatingValues() {
        var variables = parser.parse("""
                {
                  "NUMBER": {"type": "integer", "min": 1, "max": 99999, "padding": 5},
                  "FIRST": {"type": "integer", "min": 1, "max": 1, "padding": 5},
                  "LAST": {"type": "integer", "min": 99999, "max": 99999, "padding": 5},
                  "LONGER": {"type": "integer", "min": 12345, "max": 12345, "padding": 2},
                  "NEGATIVE": {"type": "integer", "min": -1, "max": -1, "padding": 5},
                  "ZERO": {"type": "integer", "min": 0, "max": 0, "padding": 5},
                  "UNPADDED": {"type": "integer", "min": 1, "max": 1}
                }
                """);
        var random = new Random(42);
        assertThat("e" + variables.get("FIRST").generate(random)).isEqualTo("e00001");
        assertThat("e" + variables.get("LAST").generate(random)).isEqualTo("e99999");
        assertThat(variables.get("LONGER").generate(random)).isEqualTo("12345");
        assertThat(variables.get("NEGATIVE").generate(random)).isEqualTo("-00001");
        assertThat(variables.get("ZERO").generate(random)).isEqualTo("00000");
        assertThat(variables.get("UNPADDED").generate(random)).isEqualTo("1");
        for (int index = 0; index < 1000; index++) {
            String value = variables.get("NUMBER").generate(random);
            assertThat(value).matches("[0-9]{5}");
            assertThat(Integer.parseInt(value)).isBetween(1, 99999);
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
