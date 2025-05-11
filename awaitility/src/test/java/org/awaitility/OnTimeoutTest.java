package org.awaitility;

import org.awaitility.classes.Asynch;
import org.awaitility.classes.FakeRepository;
import org.awaitility.classes.FakeRepositoryEqualsOne;
import org.awaitility.classes.FakeRepositoryImpl;
import org.awaitility.core.TimeoutInfo;
import org.awaitility.core.TimeoutReason;
import org.awaitility.pollinterval.FixedPollInterval;
import org.awaitility.pollinterval.PollInterval;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.*;

public class OnTimeoutTest {

    private static final String TEST_ALIAS = "This is a test";

    private static final Duration SAFETY_MARGIN = Duration.ofMillis(10);

    private static final Duration TEST_POLL_INTERVAL_DURATION = ONE_HUNDRED_MILLISECONDS;

    private static final PollInterval TEST_POLL_INTERVAL = new FixedPollInterval(TEST_POLL_INTERVAL_DURATION);

    private FakeRepository fakeRepository;

    @Before
    public void setup() {
        fakeRepository = new FakeRepositoryImpl();
        Awaitility.reset();
    }

    @Test(timeout = 2000)
    public void doesNotCallOnTimeoutCallbackWhenDoneWithoutViolatingConstraints() {
        AtomicReference<TimeoutInfo> capturedOnTimeoutContext = new AtomicReference<>();

        new Asynch(fakeRepository).perform();
        await().alias(TEST_ALIAS)
                .atLeast(TWO_HUNDRED_MILLISECONDS)
                .atMost(ONE_SECOND)
                .pollInterval(TEST_POLL_INTERVAL)
                .onTimeout(capturedOnTimeoutContext::set)
                .until(fakeRepositoryValueEqualsOne());

        assertThat(capturedOnTimeoutContext.get()).isNull();
    }

    @Test(timeout = 2000)
    public void callsOnTimeoutCallbackWhenDoneEarlierThanAtLeastConstraint() {
        AtomicReference<TimeoutInfo> capturedOnTimeoutContext = new AtomicReference<>();

        new Asynch(fakeRepository).perform();
        await().alias(TEST_ALIAS)
                .atLeast(ONE_SECOND)
                .pollInterval(TEST_POLL_INTERVAL)
                .onTimeout(capturedOnTimeoutContext::set)
                .until(fakeRepositoryValueEqualsOne());

        assertThat(capturedOnTimeoutContext.get()).isNotNull();
        assertCommonTimeoutContextFieldsSet(capturedOnTimeoutContext.get());
        assertIsConditionMetTooEarlyTimeout(capturedOnTimeoutContext.get());
    }

    @Test(timeout = 1000)
    public void callsOnTimeoutCallbackWhenDoneLaterThanAtMostConstraint() {
        AtomicReference<TimeoutInfo> capturedOnTimeoutContext = new AtomicReference<>();

        new Asynch(fakeRepository).perform();
        await().alias(TEST_ALIAS)
                .atMost(TWO_HUNDRED_MILLISECONDS)
                .pollInterval(TEST_POLL_INTERVAL)
                .onTimeout(capturedOnTimeoutContext::set)
                .ignoreExceptionsInstanceOf(AssertionError.class)
                .until(() -> {
                    throw new AssertionError("This will never be true");
                });

        assertThat(capturedOnTimeoutContext.get()).isNotNull();
        assertCommonTimeoutContextFieldsSet(capturedOnTimeoutContext.get());
        assertIsConditionNotMetTimeout(capturedOnTimeoutContext.get());
    }

    @Test(timeout = 1000)
    public void callsOnTimeoutCallbackWhenDoneLaterThanAtMostConstraintWithAtLeastConstraintPresent() {
        AtomicReference<TimeoutInfo> capturedOnTimeoutContext = new AtomicReference<>();

        new Asynch(fakeRepository).perform();
        await().alias(TEST_ALIAS)
                .atLeast(ONE_HUNDRED_MILLISECONDS)
                .atMost(TWO_HUNDRED_MILLISECONDS)
                .pollInterval(TEST_POLL_INTERVAL)
                .onTimeout(capturedOnTimeoutContext::set)
                .ignoreExceptionsInstanceOf(AssertionError.class)
                .until(() -> {
                    throw new AssertionError("This will never be true");
                });

        assertThat(capturedOnTimeoutContext.get()).isNotNull();
        assertCommonTimeoutContextFieldsSet(capturedOnTimeoutContext.get());
        assertIsConditionNotMetTimeout(capturedOnTimeoutContext.get());
    }

    @Test(timeout = 2000)
    public void callsOnTimeoutCallbackWhenConditionNotHeldForRequiredTime() {
        long startTime = System.currentTimeMillis();
        AtomicReference<TimeoutInfo> capturedOnTimeoutContext = new AtomicReference<>();

        await().alias(TEST_ALIAS)
                .during(FIVE_HUNDRED_MILLISECONDS)
                .atMost(ONE_SECOND)
                .pollDelay(ONE_MILLISECOND)
                .pollInterval(TEST_POLL_INTERVAL)
                .onTimeout(capturedOnTimeoutContext::set)
                .until(() -> (System.currentTimeMillis() - startTime) < 200L);

        assertThat(capturedOnTimeoutContext.get()).isNotNull();
        assertCommonTimeoutContextFieldsSet(capturedOnTimeoutContext.get());
        assertIsConditionNotHeldTimeout(capturedOnTimeoutContext.get());
    }

    private Callable<Boolean> fakeRepositoryValueEqualsOne() {
        return new FakeRepositoryEqualsOne(fakeRepository);
    }

    private void assertCommonTimeoutContextFieldsSet(TimeoutInfo context) {
        assertThat(context.getAlias()).isEqualTo(TEST_ALIAS);
        assertThat(context.getTimeoutMessage()).isNotNull();
    }

    private void assertIsConditionMetTooEarlyTimeout(TimeoutInfo context) {
        assertThat(context.isLateTimeout()).isFalse();
        assertThat(context.isEarlyTimeout()).isTrue();
        assertThat(context.getEvaluationDuration())
                .isGreaterThan(Duration.ofMillis(600).minus(TEST_POLL_INTERVAL_DURATION))
                .isLessThan(Duration.ofMillis(600).plus(TEST_POLL_INTERVAL_DURATION).plus(SAFETY_MARGIN));
        assertThat(context.getTimeoutReason()).isEqualTo(TimeoutReason.CONDITION_MET_TOO_EARLY);
    }

    private void assertIsConditionNotMetTimeout(TimeoutInfo context) {
        assertThat(context.isLateTimeout()).isTrue();
        assertThat(context.isEarlyTimeout()).isFalse();
        assertThat(context.getEvaluationDuration())
                .isGreaterThan(Duration.ofMillis(200))
                .isLessThan(Duration.ofMillis(200).plus(TEST_POLL_INTERVAL_DURATION).plus(SAFETY_MARGIN));
        assertThat(context.getTimeoutReason()).isEqualTo(TimeoutReason.CONDITION_NOT_MET);
    }

    private void assertIsConditionNotHeldTimeout(TimeoutInfo context) {
        assertThat(context.isLateTimeout()).isTrue();
        assertThat(context.isEarlyTimeout()).isFalse();
        assertThat(context.getEvaluationDuration())
                .isGreaterThan(Duration.ofMillis(1000))
                .isLessThan(Duration.ofMillis(1000).plus(TEST_POLL_INTERVAL_DURATION).plus(SAFETY_MARGIN));
        assertThat(context.getTimeoutReason()).isEqualTo(TimeoutReason.CONDITION_NOT_HELD);
    }
}
