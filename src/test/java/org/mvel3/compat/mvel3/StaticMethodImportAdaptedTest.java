/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel3.compat.mvel3;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mvel3.MVEL;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mvel3.compat.mvel3.StaticMethodImportAdaptedTest.CoreMatcher.is;
import static org.mvel3.compat.mvel3.StaticMethodImportAdaptedTest.IsEqual.equalTo;
import static org.mvel3.compat.mvel3.StaticMethodImportAdaptedTest.IsInstanceOf.instanceOf;
import static org.mvel3.compat.mvel3.StaticMethodImportAdaptedTest.MatcherAssert.assertThat;

/**
 * Porting of StaticMethodImportTests from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>Adaptations:
 * <ul>
 *   <li><b>inline-import</b>: In MVEL2, {@code import_static ...} was allowed inside
 *       expression strings. In MVEL3, imports are passed via the API imports set,
 *       and static methods/classes are called with class qualification.</li>
 *   <li><b>void-method-expression</b>: {@code assertThat(...)} returns {@code void},
 *       so it is followed by {@code return null;} in the block.</li>
 *   <li><b>class-literal</b>: In MVEL2, bare {@code String} was evaluated as {@code String.class}.
 *       In MVEL3, explicit {@code String.class} is required.</li>
 * </ul>
 */
public class StaticMethodImportAdaptedTest {

    private static final Set<String> IMPORTS = Set.of(
            MatcherAssert.class.getCanonicalName(),
            IsEqual.class.getCanonicalName(),
            IsInstanceOf.class.getCanonicalName(),
            CoreMatcher.class.getCanonicalName(),
            Is.class.getCanonicalName());

    private static Object eval(final String expression) {
        return new MVEL().executeExpression(expression, IMPORTS, Collections.emptyMap());
    }

    @Test
    public void testJava() {
        assertThat(150, is(150));
        eval("MatcherAssert.assertThat(\"xxx\", 150, CoreMatcher.is(150)); return null;");
        assertThat(150, is(equalTo(150)));
        eval("MatcherAssert.assertThat(\"xxx\", 150, CoreMatcher.is(IsEqual.equalTo(150))); return null;");

        try {
            assertThat(149, is(150));
            fail("should have thrown exception");
        } catch (AssertionError e) {

        }
        try {
            eval("MatcherAssert.assertThat(149, CoreMatcher.is(150)); return null;");
            fail("should have thrown exception");
        } catch (AssertionError e) {
            // expected AssertionError
        }

        try {
            assertThat(149, is(equalTo(150)));
            fail("should have thrown exception");
        } catch (AssertionError e) {

        }

        try {
            eval("MatcherAssert.assertThat(149, CoreMatcher.is(IsEqual.equalTo(150))); return null;");
            fail("should have thrown exception");
        } catch (AssertionError e) {
            // expected AssertionError
        }

        assertThat("yoda", is("yoda"));
        eval("MatcherAssert.assertThat(\"yoda\", CoreMatcher.is(\"yoda\")); return null;");

        assertThat("yoda", is(equalTo("yoda")));
        eval("MatcherAssert.assertThat(\"yoda\", CoreMatcher.is(IsEqual.equalTo(\"yoda\"))); return null;");

        try {
            assertThat("darth", is("yoda"));
            fail("should have thrown exception");
        } catch (AssertionError e) {

        }

        try {
            eval("MatcherAssert.assertThat(\"darth\", CoreMatcher.is(\"yoda\")); return null;");
            fail("should have thrown exception");
        } catch (AssertionError e) {
            // expected AssertionError
        }

        assertThat("yoda", is(instanceOf(String.class)));
        eval("MatcherAssert.assertThat(\"yoda\", CoreMatcher.is(IsInstanceOf.instanceOf(String.class))); return null;");

        assertThat("yoda", is(String.class));
        eval("MatcherAssert.assertThat(\"yoda\", CoreMatcher.is(String.class)); return null;");
    }

    public static class MatcherAssert {
        public static <T> void assertThat(T actual, Matcher<? super T> matcher) {
            if (!matcher.matches(actual)) {
                throw new AssertionError();
            }
        }

        public static <T> void assertThat(String reason, T actual, Matcher<? super T> matcher) {
            if (!matcher.matches(actual)) {
                throw new AssertionError(reason);
            }
        }
    }

    public static class CoreMatcher {

        public static <T> Matcher<T> is(Matcher<T> matcher) {
            return Is.<T>is(matcher);
        }

        public static <T> Matcher<T> is(T value) {
            return Is.<T>is(value);
        }

        public static <T> Matcher<T> is(java.lang.Class<T> type) {
            return Is.<T>is(type);
        }
    }

    public static class Is<T> implements Matcher<T> {
        private final Matcher<T> matcher;

        public Is(Matcher<T> matcher) {
            this.matcher = matcher;
        }

        public boolean matches(Object arg) {
            return matcher.matches(arg);
        }

        public static <T> Matcher<T> is(Matcher<T> matcher) {
            return new Is<T>(matcher);
        }

        public static <T> Matcher<T> is(T value) {
            return is(equalTo(value));
        }

        public static <T> Matcher<T> is(Class<T> type) {
            final Matcher<T> typeMatcher = instanceOf(type);
            return is(typeMatcher);
        }
    }

    public static class IsEqual<T> implements Matcher<T> {
        private final Object object;

        public IsEqual(T equalArg) {
            object = equalArg;
        }

        public boolean matches(Object arg) {
            return areEqual(arg, object);
        }

        public static <T> Matcher<T> equalTo(T operand) {
            return new IsEqual<T>(operand);
        }

        private static boolean areEqual(Object o1, Object o2) {
            if (o1 == null) {
                return o2 == null;
            } else if (o2 != null && isArray(o1)) {
                return isArray(o2) && areArraysEqual(o1, o2);
            } else {
                return o1.equals(o2);
            }
        }

        private static boolean areArraysEqual(Object o1, Object o2) {
            return areArrayLengthsEqual(o1, o2)
                    && areArrayElementsEqual(o1, o2);
        }

        private static boolean areArrayLengthsEqual(Object o1, Object o2) {
            return Array.getLength(o1) == Array.getLength(o2);
        }

        private static boolean areArrayElementsEqual(Object o1, Object o2) {
            for (int i = 0; i < Array.getLength(o1); i++) {
                if (!areEqual(Array.get(o1, i), Array.get(o2, i))) return false;
            }
            return true;
        }

        private static boolean isArray(Object o) {
            return o.getClass().isArray();
        }
    }

    public static class IsInstanceOf implements Matcher<Object> {
        private final Class<?> matchableClass;

        public IsInstanceOf(Class<?> expectedClass) {
            this.matchableClass = matchableClass(expectedClass);
        }

        public boolean matches(Object item) {
            if (null == item) {
                return false;
            }

            if (!matchableClass.isInstance(item)) {
                return false;
            }

            return true;
        }

        private static Class<?> matchableClass(Class<?> expectedClass) {
            if (boolean.class.equals(expectedClass)) return Boolean.class;
            if (byte.class.equals(expectedClass)) return Byte.class;
            if (char.class.equals(expectedClass)) return Character.class;
            if (double.class.equals(expectedClass)) return Double.class;
            if (float.class.equals(expectedClass)) return Float.class;
            if (int.class.equals(expectedClass)) return Integer.class;
            if (long.class.equals(expectedClass)) return Long.class;
            if (short.class.equals(expectedClass)) return Short.class;
            return expectedClass;
        }

        @SuppressWarnings("unchecked")
        public static <T> Matcher<T> instanceOf(Class<?> type) {
            return (Matcher<T>) new IsInstanceOf(type);
        }

        @SuppressWarnings("unchecked")
        public static <T> Matcher<T> any(Class<T> type) {
            return (Matcher<T>) new IsInstanceOf(type);
        }
    }

    public static interface Matcher<T> {
        boolean matches(Object item);
    }
}
