package com.somecompany.marketplace.common;

import java.util.UUID;

/** Produces primary keys. Abstracted so tests can control ids if they need to. */
public interface IdGenerator {
    UUID next();
}
