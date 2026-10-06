package org.mvel3.compat.mvel2;

import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractTestAssertionsTest {

    @Test
    void convertsExpectedDoubleToActualInteger() {
        assertThatCode(() -> AbstractTest.assertNumEquals(10.0625, 10)).doesNotThrowAnyException();
    }

    @Test
    void permitsSmallDifferencesForTheSameNumericType() {
        assertThatCode(() -> AbstractTest.assertNumEquals(1.00009, 1.0)).doesNotThrowAnyException();
    }

    @Test
    void rejectsLargerDifferencesForTheSameNumericType() {
        assertThatThrownBy(() -> AbstractTest.assertNumEquals(1.001, 1.0))
                .isInstanceOf(AssertionFailedError.class);
    }

    @Test
    void permitsIntegerRoundingAfterConversionToDouble() {
        assertThatCode(() -> AbstractTest.assertNumEquals(10, 10.9)).doesNotThrowAnyException();
    }

    @Test
    void rejectsRoundingWhenDisabled() {
        assertThatThrownBy(() -> AbstractTest.assertNumEquals(10, 10.9, false))
                .isInstanceOf(AssertionFailedError.class);
    }

    @Test
    void rejectsDifferentIntegersAfterConversion() {
        assertThatThrownBy(() -> AbstractTest.assertNumEquals(10.9, 11))
                .isInstanceOf(AssertionFailedError.class);
    }

    @Test
    void rejectsDoubleAboveTheIntegerUpperBound() {
        assertThatThrownBy(() -> AbstractTest.assertNumEquals((double) Integer.MAX_VALUE + 1, 0))
                .isInstanceOf(RuntimeException.class);
    }
}
