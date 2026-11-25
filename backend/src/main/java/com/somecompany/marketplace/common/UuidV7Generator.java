package com.somecompany.marketplace.common;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import java.util.UUID;

/** Time-ordered UUIDv7 ids (RFC 9562) using java-uuid-generator. */
public class UuidV7Generator implements IdGenerator {

    private final TimeBasedEpochGenerator generator = Generators.timeBasedEpochGenerator();

    @Override
    public UUID next() {
        return generator.generate();
    }
}
