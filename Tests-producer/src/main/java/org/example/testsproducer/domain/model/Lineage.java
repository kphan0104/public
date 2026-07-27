package org.example.testsproducer.domain.model;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

public final class Lineage {

    private final NavigableMap<Integer, Stage> stages;

    public Lineage(Map<Integer, Stage> stages) {
        Objects.requireNonNull(stages, "stages");
        if (stages.isEmpty()) {
            throw new IllegalArgumentException(
                    "Le lineage doit contenir au moins un stage"
            );
        }

        TreeMap<Integer, Stage> stageCopies = new TreeMap<>();
        stages.forEach((number, stage) -> {
            if (number == null || number < 1) {
                throw new IllegalArgumentException(
                        "Le numéro de stage doit être supérieur ou égal à 1"
                );
            }
            stageCopies.put(number, Objects.requireNonNull(stage, "stage"));
        });
        this.stages = Collections.unmodifiableNavigableMap(stageCopies);
    }

    public static Lineage startWith(Stage stage1) {
        return new Lineage(Map.of(1, stage1));
    }

    public int lastStage() {
        return stages.lastKey();
    }

    public Stage lastStageValue() {
        return stages.lastEntry().getValue();
    }

    public Map<Integer, Stage> stages() {
        return stages;
    }

    public Lineage withStage(int stageNumber, Stage stage) {
        TreeMap<Integer, Stage> updatedStages = new TreeMap<>(stages);
        updatedStages.put(stageNumber, stage);
        return new Lineage(updatedStages);
    }

    public Lineage withLastStageEventSize(int eventSize) {
        return withStage(lastStage(), lastStageValue().withEventSize(eventSize));
    }
}
