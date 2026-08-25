package org.example.testsproducer.adapter.out.filesystem;

import org.example.testsproducer.application.port.out.FlowTopicsStorePort;
import org.example.testsproducer.config.AdminProperties;

import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

public final class FileSystemFlowTopicsStoreAdapter
        implements FlowTopicsStorePort {

    private final Path flowTopicsFile;

    public FileSystemFlowTopicsStoreAdapter(AdminProperties properties) {
        flowTopicsFile = properties.flowTopicsFile();
    }

    @Override
    public void save(Map<String, String> topics) {
        StringBuilder yaml = new StringBuilder()
                .append("tests-producer:\n")
                .append("  flows:\n")
                .append("    topics:\n");
        new TreeMap<>(topics).forEach((flow, topic) -> yaml
                .append("      ")
                .append(flow)
                .append(": ")
                .append(topic)
                .append('\n'));
        AtomicFileWriter.write(flowTopicsFile, yaml.toString());
    }
}
