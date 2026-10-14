package org.mvel3.compat.mvel3;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mvel3.Foo;
import org.mvel3.Base;
import org.mvel3.MVEL;
import org.mvel3.compat.mvel2.WithTests;
import org.mvel3.compat.mvel2.res.MyEnum;
import org.mvel3.compat.mvel2.res.Thing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;

/**
 * Porting of WithTests from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>MVEL3 supports a restricted form of {@code with}: the body must use
 * semicolons as separators and the last statement before {@code '}' must also
 * end with {@code ';'}.  The comma-separated form {@code with (obj) \{ p=v, q=w \}}
 * and the inline form {@code obj.\{ p=v \}} are not supported.
 *
 * <p>Tests are {@code @Disabled("with-block-syntax: not representable")} when:
 * <ul>
 *   <li>the original specifically exercises the inline form ({@code obj.\{..\}}), or</li>
 *   <li>the original captures a variable inside the {@code with} body and uses it
 *       outside ({@code aValue = (y = 'One')}), which MVEL3 fails to resolve, or</li>
 *   <li>the original nests {@code with} blocks inside another expression (e.g. a map
 *       literal or a cast).</li>
 * </ul>
 *
 * <p>Other adaptations:
 * <ul>
 *   <li><b>single-quote-string</b>: string literals use double quotes.</li>
 *   <li><b>block-syntax</b>: the result is returned explicitly with {@code return}.</li>
 *   <li>MVEL2 optimizer modes and {@code ParserContext} strong-typing have no MVEL3
 *       equivalent and are removed.</li>
 * </ul>
 */
public class WithAdaptedTest {

    private static final String NOT_REPRESENTABLE = "with-block-syntax: not representable";

    private static final Set<String> IMPORTS = Set.of(
            Foo.class.getCanonicalName(),
            Base.class.getCanonicalName());

    private static Object run(final String expression, final Map<String, Object> vars) {
        return new MVEL().executeExpression(expression, IMPORTS, vars);
    }

    private static Object run(final String expression) {
        return run(expression, new HashMap<>());
    }

    private static Map<String, Object> fooVars() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("foo", new Foo());
        return vars;
    }

    @Test
    public void testWith() {
        Map<String, Object> vars = fooVars();
        assertThat(run("with (foo) { aValue = \"One\"; bValue = \"Two\"; } return foo.aValue + foo.bValue;", vars))
                .isEqualTo("OneTwo");
    }

    // Original: with (foo) { aValue = (y = 'One'), bValue='Two' }; foo.aValue + foo.bValue + y;
    // The body assigns to an outer variable `y` via `aValue = (y = 'One')`. MVEL3 interprets
    // `y` as a property of `foo` and fails with "The node has neither a public field nor a getter
    // method for: foo.y".
    @Test
    @Disabled(NOT_REPRESENTABLE)
    public void testWith2() {
    }

    @Test
    public void testWith3() {
        Map<String, Object> vars = fooVars();
        assertThat(run("with (foo) { aValue = \"One\"; bValue = \"Two\"; } "
                       + "with (foo) { aValue += \"One\"; bValue += \"Two\"; } "
                       + "return foo.aValue + foo.bValue;", vars))
                .isEqualTo("OneOneTwoTwo");
    }

    @Test
    public void testWith4() {
        Map<String, Object> vars = fooVars();
        assertThat(run("with (foo) { countTest += 5; } with (foo) { countTest *= 2; } "
                       + "return foo.countTest;", vars))
                .isEqualTo(10);
    }

    @Test
    public void testWith5() {
        Map<String, Object> vars = fooVars();
        Object result = run("with (foo) { countTest += 5; aValue = \"Hello\"; bValue = \"Goodbye\"; } "
                            + "with (foo) { countTest *= 2; } "
                            + "return foo;", vars);
        assertThat(result).isInstanceOf(Foo.class);
        Foo foo = (Foo) result;
        assertThat(foo.getCountTest()).isEqualTo(10);
        assertThat(foo.aValue).isEqualTo("Hello");
        assertThat(foo.bValue).isEqualTo("Goodbye");
    }

    // Original: foo.{name='poopy', aValue='bar'}  — inline with, returns the receiver.
    // The inline form `obj.{..}` is a no-op in MVEL3.
    @Test
    @Disabled(NOT_REPRESENTABLE)
    public void testInlineWith() {
    }

    // Original: foo.{name = 'poopy', aValue = 'bar', bar.{name = 'foobie'}}
    // Nested inline with; not representable.
    @Test
    @Disabled(NOT_REPRESENTABLE)
    public void testInlineWith2() {
    }

    // Original: foo.{name = 'poopy', aValue = 'bar', bar.{name = 'foobie'}, toUC('doopy')}
    // Nested inline with with method call; not representable.
    @Test
    @Disabled(NOT_REPRESENTABLE)
    public void testInlineWith3() {
    }

    // Original: same as testInlineWith3 without spaces.
    @Test
    @Disabled(NOT_REPRESENTABLE)
    public void testInlineWith3a() {
    }

    @Test
    public void testInlineWith4() {
        // Original: new Foo().{ name = 'bar' }  — inline with on a new object.
        // Inline with is a no-op; use the block form with a declared variable.
        assertThat(run("Foo f = new Foo(); with (f) { setName(\"bar\"); } return f.getName();"))
                .isEqualTo("bar");
    }

    @Test
    public void testInlineWith5() {
        // Original: foo.{name='poopy', aValue='bar'}  — inline with.
        // Replaced with block with.
        Map<String, Object> vars = fooVars();
        run("with (foo) { setName(\"poopy\"); aValue = \"bar\"; } return null;", vars);
        Foo f = (Foo) vars.get("foo");
        assertThat(f.getName()).isEqualTo("poopy");
        assertThat(f.aValue).isEqualTo("bar");
    }

    // Original: .{ data = 'foo' }  (leading dot = implied this, evaluated against a Base root object).
    // The leading-dot implied-this syntax is not supported in MVEL3.
    @Test
    @Disabled(NOT_REPRESENTABLE)
    public void testInlineWithImpliedThis() {
    }

    @Test
    public void testSingleMethodCall() {
        Base b = new Base();
        Map<String, Object> vars = new HashMap<>();
        vars.put("base", b);
        run("with (base) { populate(); } return null;", vars);
        assertThat(b.barfoo).isEqualTo("sarah");
    }

    @Test
    public void testWithMultipleMethodCalls() {
        // Compile-only test in the original; checks that no exception is thrown.
        Map<String, Object> vars = fooVars();
        run("with (foo) { setName(\"foo\"); setBar(null); } return null;", vars);
    }

    // Original: [ 'foo' : (with ( new Foo() ) { bar = with ( new Bar() ) { name = 'ziggy' } }) ]
    // Nesting a `with` block inside a map literal is not supported in MVEL3 (parse error at `(`
    // after `with`).
    @Test
    @Disabled(NOT_REPRESENTABLE)
    public void testNewUsingWith() {
    }

    @Test
    public void testSataticClassImportViaFactoryAndWithModification() {
        // Original: p = new Person('tom'); p.age = 20; with( p ) { age = p.age + 1 }; return p.age;
        Set<String> imp = Set.of(WithTests.Person.class.getCanonicalName());
        Map<String, Object> vars = new HashMap<>();
        vars.put("p", new WithTests.Person("tom"));
        assertThat(new MVEL().executeExpression(
                "p.setAge(20); with (p) { age = p.age + 1; } return p.age;",
                imp, vars))
                .isEqualTo(21);
    }

    @Test
    public void testExecuteCoercionTwice() {
        // Original: with (foo) { countTest = $value }  evaluated twice with a Long value.
        // `countTest` is int; MVEL3 accepts `(int)(long) $value` for the coercion.
        Map<String, Object> vars = fooVars();
        vars.put("$value", 5L);
        run("with (foo) { countTest = (int)(long) $value; } return null;", vars);
        assertThat(((Foo) vars.get("foo")).getCountTest()).isEqualTo(5);
        run("with (foo) { countTest = (int)(long) $value; } return null;", vars);
        assertThat(((Foo) vars.get("foo")).getCountTest()).isEqualTo(5);
    }

    @Test
    public void testNestedWithInList() {
        // Original: [ (with (new Recipient()){name='userName1', email='user1@domain.com'}), ... ]
        // Nesting `with` inside a list literal is not supported; recipients are built with
        // block `with` and collected in a list.
        Set<String> imp = Set.of(WithTests.Recipient.class.getCanonicalName());
        Map<String, Object> vars = new HashMap<>();
        vars.put("r1", new WithTests.Recipient());
        vars.put("r2", new WithTests.Recipient());
        new MVEL().executeExpression(
                "with (r1) { name = \"userName1\"; email = \"user1@domain.com\"; } "
                + "with (r2) { name = \"userName2\"; email = \"user2@domain.com\"; } "
                + "return null;",
                imp, vars);
        WithTests.Recipient expected1 = new WithTests.Recipient();
        expected1.setName("userName1");
        expected1.setEmail("user1@domain.com");
        WithTests.Recipient expected2 = new WithTests.Recipient();
        expected2.setName("userName2");
        expected2.setEmail("user2@domain.com");
        assertThat(vars.get("r1")).isEqualTo(expected1);
        assertThat(vars.get("r2")).isEqualTo(expected2);
    }

    @Test
    public void testNestedWithInComplexGraph3() {
        // Original used deeply nested inline-with to build an EmailMessage object graph.
        // Inner `with` blocks are replaced by sequential block `with` statements.
        Set<String> imp = Set.of(
                WithTests.Recipient.class.getCanonicalName(),
                WithTests.Recipients.class.getCanonicalName(),
                WithTests.EmailMessage.class.getCanonicalName());
        Map<String, Object> vars = new HashMap<>();
        vars.put("msg",    new WithTests.EmailMessage());
        vars.put("recips", new WithTests.Recipients());
        vars.put("rc1",    new WithTests.Recipient());
        vars.put("rc2",    new WithTests.Recipient());
        new MVEL().executeExpression(
                "with (rc1) { name = \"user1\"; email = \"user1@domain.com\"; } "
                + "with (rc2) { name = \"user2\"; email = \"user2@domain.com\"; } "
                + "recips.addRecipient(rc1); recips.addRecipient(rc2); "
                + "with (msg) { recipients = recips; from = \"from@domain.com\"; } "
                + "return null;",
                imp, vars);

        WithTests.Recipients recipients = new WithTests.Recipients();
        WithTests.Recipient r1 = new WithTests.Recipient();
        r1.setName("user1"); r1.setEmail("user1@domain.com");
        recipients.addRecipient(r1);
        WithTests.Recipient r2 = new WithTests.Recipient();
        r2.setName("user2"); r2.setEmail("user2@domain.com");
        recipients.addRecipient(r2);
        WithTests.EmailMessage expected = new WithTests.EmailMessage();
        expected.setRecipients(recipients);
        expected.setFrom("from@domain.com");

        assertThat(vars.get("msg")).isEqualTo(expected);
    }

    // Original: 'foo'.{ toString() }  — inline with on a String literal.
    // The inline form is a no-op; not representable with block with.
    @Test
    @Disabled(NOT_REPRESENTABLE)
    public void testInlineWithWithLiteral() {
    }

    // Original: 'foo'.{ toString() . toString() . toString() }  — chained inline with on a literal.
    @Test
    @Disabled(NOT_REPRESENTABLE)
    public void testInlineWithWithLiteral2() {
    }

    @Test
    public void testWithAndEnumInPackageImport() {
        // Original: with( thing ) { myEnum = MyEnum.FULL_DOCUMENTATION }
        Set<String> imp = Set.of(
                MyEnum.class.getCanonicalName(),
                Thing.class.getCanonicalName());
        Thing thing = new Thing("xxx");
        Map<String, Object> vars = new HashMap<>();
        vars.put("thing", thing);
        new MVEL().executeExpression(
                "with (thing) { myEnum = MyEnum.FULL_DOCUMENTATION; } return null;",
                imp, vars);
        assertThat(thing.getMyEnum()).isEqualTo(MyEnum.FULL_DOCUMENTATION);
    }
}
