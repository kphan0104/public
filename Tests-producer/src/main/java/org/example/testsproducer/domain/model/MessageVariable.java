package org.example.testsproducer.domain.model;

import java.util.List;
import java.util.random.RandomGenerator;

public sealed interface MessageVariable {

    String generate(RandomGenerator random);

    record StringVariable(int length, String characters) implements MessageVariable {

        public StringVariable {
            if (length < 1 || length > 10_000) {
                throw new IllegalArgumentException("length doit être compris entre 1 et 10000");
            }
            if (characters == null || characters.isEmpty()) {
                throw new IllegalArgumentException("characters doit contenir au moins un caractère");
            }
            expandCharacters(characters);
        }

        @Override
        public String generate(RandomGenerator random) {
            int[] alphabet = expandCharacters(characters);
            StringBuilder value = new StringBuilder(length);
            for (int index = 0; index < length; index++) {
                value.appendCodePoint(alphabet[random.nextInt(alphabet.length)]);
            }
            return value.toString();
        }

        private static int[] expandCharacters(String characters) {
            var result = new java.util.ArrayList<Integer>();
            int[] points = characters.codePoints().toArray();
            for (int index = 0; index < points.length; index++) {
                if (index + 2 < points.length && points[index + 1] == '-') {
                    int start = points[index];
                    int end = points[index + 2];
                    if (end < start || end - start > 255) {
                        throw new IllegalArgumentException("Intervalle de caractères invalide : " + characters);
                    }
                    for (int point = start; point <= end; point++) {
                        result.add(point);
                    }
                    index += 2;
                } else {
                    result.add(points[index]);
                }
            }
            return result.stream().mapToInt(Integer::intValue).distinct().toArray();
        }
    }

    record IntegerVariable(int min, int max, int padding) implements MessageVariable {

        public IntegerVariable(int min, int max) {
            this(min, max, 0);
        }

        public IntegerVariable {
            if (min > max) {
                throw new IllegalArgumentException("min doit être inférieur ou égal à max");
            }
            if (padding < 0 || padding > 10_000) {
                throw new IllegalArgumentException("padding doit être compris entre 0 et 10000");
            }
        }

        @Override
        public String generate(RandomGenerator random) {
            String value = Long.toString(random.nextLong(min, (long) max + 1));
            boolean negative = value.startsWith("-");
            String digits = negative ? value.substring(1) : value;
            String padded = "0".repeat(Math.max(0, padding - digits.length())) + digits;
            return negative ? "-" + padded : padded;
        }
    }

    record ChoiceVariable(List<String> values) implements MessageVariable {

        public ChoiceVariable {
            if (values == null || values.isEmpty() || values.stream().anyMatch(java.util.Objects::isNull)) {
                throw new IllegalArgumentException("values doit être une liste non vide de chaînes de caractères");
            }
            values = List.copyOf(values);
        }

        @Override
        public String generate(RandomGenerator random) {
            return values.get(random.nextInt(values.size()));
        }
    }
}
