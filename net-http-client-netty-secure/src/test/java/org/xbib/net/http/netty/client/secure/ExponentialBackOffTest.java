package org.xbib.net.http.netty.client.secure;

import org.junit.jupiter.api.Test;
import org.xbib.net.http.client.BackOff;
import org.xbib.net.http.client.ExponentialBackOff;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests {@link ExponentialBackOff}.
 */
class ExponentialBackOffTest {

    @Test
    void testConstructor() {
        ExponentialBackOff backOffPolicy = new ExponentialBackOff();
        assertEquals(ExponentialBackOff.DEFAULT_INITIAL_INTERVAL_MILLIS,
                backOffPolicy.getInitialIntervalMillis());
        assertEquals(ExponentialBackOff.DEFAULT_INITIAL_INTERVAL_MILLIS,
                backOffPolicy.getCurrentIntervalMillis());
        assertEquals(ExponentialBackOff.DEFAULT_RANDOMIZATION_FACTOR,
                backOffPolicy.getRandomizationFactor(), 1);
        assertEquals(ExponentialBackOff.DEFAULT_MULTIPLIER, backOffPolicy.getMultiplier(), 1);
        assertEquals(
                ExponentialBackOff.DEFAULT_MAX_INTERVAL_MILLIS, backOffPolicy.getMaxIntervalMillis());
        assertEquals(ExponentialBackOff.DEFAULT_MAX_ELAPSED_TIME_MILLIS,
                backOffPolicy.getMaxElapsedTimeMillis());
    }

    @Test
    void testBuilder() {
        ExponentialBackOff backOffPolicy = new ExponentialBackOff.Builder().build();
        assertEquals(ExponentialBackOff.DEFAULT_INITIAL_INTERVAL_MILLIS,
                backOffPolicy.getInitialIntervalMillis());
        assertEquals(ExponentialBackOff.DEFAULT_INITIAL_INTERVAL_MILLIS,
                backOffPolicy.getCurrentIntervalMillis());
        assertEquals(ExponentialBackOff.DEFAULT_RANDOMIZATION_FACTOR,
                backOffPolicy.getRandomizationFactor(), 1);
        assertEquals(ExponentialBackOff.DEFAULT_MULTIPLIER, backOffPolicy.getMultiplier(), 1);
        assertEquals(ExponentialBackOff.DEFAULT_MAX_INTERVAL_MILLIS, backOffPolicy.getMaxIntervalMillis());
        assertEquals(ExponentialBackOff.DEFAULT_MAX_ELAPSED_TIME_MILLIS,
                backOffPolicy.getMaxElapsedTimeMillis());

        int testInitialInterval = 1;
        double testRandomizationFactor = 0.1;
        double testMultiplier = 5.0;
        int testMaxInterval = 10;
        int testMaxElapsedTime = 900000;

        backOffPolicy = new ExponentialBackOff.Builder()
                .setInitialIntervalMillis(testInitialInterval)
                .setRandomizationFactor(testRandomizationFactor)
                .setMultiplier(testMultiplier)
                .setMaxIntervalMillis(testMaxInterval)
                .setMaxElapsedTimeMillis(testMaxElapsedTime)
                .build();
        assertEquals(testInitialInterval, backOffPolicy.getInitialIntervalMillis());
        assertEquals(testInitialInterval, backOffPolicy.getCurrentIntervalMillis());
        assertEquals(testRandomizationFactor, backOffPolicy.getRandomizationFactor(), 1);
        assertEquals(testMultiplier, backOffPolicy.getMultiplier(), 1);
        assertEquals(testMaxInterval, backOffPolicy.getMaxIntervalMillis());
        assertEquals(testMaxElapsedTime, backOffPolicy.getMaxElapsedTimeMillis());
    }

    @Test
    void testBackOff() {
        int testInitialInterval = 500;
        double testRandomizationFactor = 0.1;
        double testMultiplier = 2.0;
        int testMaxInterval = 5000;
        int testMaxElapsedTime = 900000;

        ExponentialBackOff backOffPolicy = new ExponentialBackOff.Builder()
                .setInitialIntervalMillis(testInitialInterval)
                .setRandomizationFactor(testRandomizationFactor)
                .setMultiplier(testMultiplier)
                .setMaxIntervalMillis(testMaxInterval)
                .setMaxElapsedTimeMillis(testMaxElapsedTime)
                .build();
        int[] expectedResults = {500, 1000, 2000, 4000, 5000, 5000, 5000, 5000, 5000, 5000};
        for (int expected : expectedResults) {
            assertEquals(expected, backOffPolicy.getCurrentIntervalMillis());
            // Assert that the next back off falls in the expected range.
            int minInterval = (int) (expected - (testRandomizationFactor * expected));
            int maxInterval = (int) (expected + (testRandomizationFactor * expected));
            long actualInterval = backOffPolicy.nextBackOffMillis();
            assertTrue(minInterval <= actualInterval && actualInterval <= maxInterval);
        }
    }

    @Test
    void testGetRandomizedInterval() {
        // 33% chance of being 1.
        assertEquals(1, ExponentialBackOff.getRandomValueFromInterval(0.5, 0, 2));
        assertEquals(1, ExponentialBackOff.getRandomValueFromInterval(0.5, 0.33, 2));
        // 33% chance of being 2.
        assertEquals(2, ExponentialBackOff.getRandomValueFromInterval(0.5, 0.34, 2));
        assertEquals(2, ExponentialBackOff.getRandomValueFromInterval(0.5, 0.66, 2));
        // 33% chance of being 3.
        assertEquals(3, ExponentialBackOff.getRandomValueFromInterval(0.5, 0.67, 2));
        assertEquals(3, ExponentialBackOff.getRandomValueFromInterval(0.5, 0.99, 2));
    }

    @Test
    void testGetElapsedTimeMillis() {
        ExponentialBackOff backOffPolicy = new ExponentialBackOff.Builder().setNanoClock(new MyNanoClock()).build();
        long elapsedTimeMillis = backOffPolicy.getElapsedTimeMillis();
        assertEquals(1000, elapsedTimeMillis);
    }

    @Test
    void testMaxElapsedTime() {
        ExponentialBackOff backOffPolicy =
                new ExponentialBackOff.Builder().setNanoClock(new MyNanoClock(10000)).build();
        assertTrue(backOffPolicy.nextBackOffMillis() != BackOff.STOP);
        // Change the currentElapsedTimeMillis to be 0 ensuring that the elapsed time will be greater
        // than the max elapsed time.
        backOffPolicy.setStartTimeNanos(0);
        assertEquals(BackOff.STOP, backOffPolicy.nextBackOffMillis());
    }

    @Test
    void testBackOffOverflow() {
        int testInitialInterval = Integer.MAX_VALUE / 2;
        double testMultiplier = 2.1;
        int testMaxInterval = Integer.MAX_VALUE;
        ExponentialBackOff backOffPolicy = new ExponentialBackOff.Builder()
                .setInitialIntervalMillis(testInitialInterval)
                .setMultiplier(testMultiplier)
                .setMaxIntervalMillis(testMaxInterval)
                .build();
        backOffPolicy.nextBackOffMillis();
        // Assert that when an overflow is possible the current interval is set to the max interval.
        assertEquals(testMaxInterval, backOffPolicy.getCurrentIntervalMillis());
    }

    static class MyNanoClock implements ExponentialBackOff.NanoClock {

        private int i = 0;
        private long startSeconds;

        MyNanoClock() {
        }

        MyNanoClock(long startSeconds) {
            this.startSeconds = startSeconds;
        }

        public long nanoTime() {
            return (startSeconds + i++) * 1000000000;
        }
    }

}
