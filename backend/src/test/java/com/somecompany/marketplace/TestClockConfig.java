package com.somecompany.marketplace;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Fixes "today" at 2026-11-25 (UTC) for integration tests. */
@TestConfiguration
public class TestClockConfig {

    public static final Instant START = Instant.parse("2026-11-25T09:00:00Z");

    @Bean
    @Primary
    public MutableClock testClock() {
        return new MutableClock(START);
    }

    /**
     * A clock that moves forward 1 ms every time it is read, so log entries written in one upload
     * have distinct, increasing LogTime values, while the date stays fixed.
     */
    public static class MutableClock extends Clock {

        private Instant now;

        public MutableClock(Instant start) {
            this.now = start;
        }

        public synchronized void set(Instant instant) {
            this.now = instant;
        }

        public synchronized void advance(Duration duration) {
            this.now = now.plus(duration);
        }

        @Override
        public synchronized Instant instant() {
            Instant current = now;
            now = now.plusMillis(1);
            return current;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
