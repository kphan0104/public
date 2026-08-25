package org.example.testsproducer.adapter.out.filesystem;

import org.example.testsproducer.application.port.out.OriginalMessageStorePort;
import org.example.testsproducer.config.SwaggerProperties;

import java.nio.file.Files;
import java.nio.file.Path;

public final class FileSystemOriginalMessageStoreAdapter
        implements OriginalMessageStorePort {

    private final Path directory;

    public FileSystemOriginalMessageStoreAdapter(
            SwaggerProperties properties
    ) {
        directory = properties.originalMessagesDirectory()
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public boolean save(String flow, String originalMessage) {
        Path messageFile = directory.resolve(flow + ".msg");
        boolean created = !Files.exists(messageFile);
        AtomicFileWriter.write(messageFile, originalMessage);
        return created;
    }
}
