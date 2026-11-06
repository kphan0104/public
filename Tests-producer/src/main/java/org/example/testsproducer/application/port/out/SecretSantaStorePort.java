package org.example.testsproducer.application.port.out;

import org.example.testsproducer.domain.model.SecretSantaDraw;
import java.util.function.Supplier;

public interface SecretSantaStorePort {
    SecretSantaDraw loadOrCreate(Supplier<SecretSantaDraw> create);
}
