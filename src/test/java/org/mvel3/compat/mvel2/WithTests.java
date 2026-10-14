package org.mvel3.compat.mvel2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mvel3.compat.mvel2.Mvel3TestSupport.ExpressionCompiler;
import org.mvel3.compat.mvel2.res.Bar;
import org.mvel3.compat.mvel2.res.Base;
import org.mvel3.compat.mvel2.res.Foo;
import org.mvel3.compat.mvel2.res.MyEnum;
import org.mvel3.compat.mvel2.res.Thing;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.eval;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;

/**
 * Imported from org.mvel2.tests.core.WithTests (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged. Failures are intentionally visible; known behavioural differences
 * are annotated with {@link KnownCompatibility}.
 *
 * <p>Not imported:
 * <ul>
 *   <li>{@code testInlineWithWithLiteralASM}: sets the ASM optimizer and calls
 *       {@code testInlineWithWithLiteral()}; the optimizer mode has no MVEL3 equivalent.</li>
 * </ul>
 */
@Tag("mvel2-compatibility")
public class WithTests extends AbstractTest {

  // Category not isolated: fails with parse errors at the `}` of the `with` block body. The expression
  // also uses single-quoted strings (single-quote-string). The with-block-syntax parse failure was observed
  // first; single-quote-string was not tested separately.
  @Test
  @KnownCompatibility("with-block-syntax")
  public void testWith() {
    String ex = "with (foo) {aValue = 'One',bValue='Two'}; foo.aValue + foo.bValue;";

    Map vars = createTestMap();
    Object o = eval(ex, vars);
    assertEquals("OneTwo", o);

    assertEquals("OneTwo", test("with (foo) {aValue = 'One',bValue='Two'}; foo.aValue + foo.bValue;"));
  }

  @Test
  @KnownCompatibility("with-block-syntax")
  public void testWith2() {
    assertEquals("OneTwoOne", test(
        "var y; with (foo) { \n" +
            "aValue = (y = 'One'), // this is a comment \n" +
            "bValue='Two'  // this is also a comment \n" +
            "}; \n" +
            "foo.aValue + foo.bValue + y;"));
  }

  // Category not isolated: fails with parse errors at the `with` block body. The expression
  // also uses single-quoted strings (single-quote-string). The with-block-syntax parse failure was observed
  // first; single-quote-string was not tested separately.
  @Test
  @KnownCompatibility("with-block-syntax")
  public void testWith3() {
    String ex = "with (foo) {aValue = 'One',bValue='Two'}; with (foo) {aValue += 'One', bValue += 'Two'}; foo.aValue + foo.bValue;";

    Map vars = createTestMap();

    assertEquals("OneOneTwoTwo", eval(ex, vars));


    assertEquals("OneOneTwoTwo", test("with (foo) {aValue = 'One',bValue='Two'}; with (foo) {aValue += 'One', bValue += 'Two'}; foo.aValue + foo.bValue;"));
  }

  @Test
  @KnownCompatibility("with-block-syntax")
  public void testWith4() {
    assertEquals(10, test("with (foo) {countTest += 5 }; with (foo) { countTest *= 2 }; foo.countTest"));
  }

  // Category not isolated: fails with parse errors at the `with` block body. The expression
  // also uses single-quoted strings (single-quote-string). The with-block-syntax parse failure was observed
  // first; single-quote-string was not tested separately.
  @Test
  @KnownCompatibility("with-block-syntax")
  public void testWith5() {
    String expr = "with (foo) { countTest += 5, \n" +
        "// foobar!\n" +
        "aValue = 'Hello',\n" +
        "/** Comment! **/\n" +
        "bValue = 'Goodbye'\n }; with (foo) { countTest *= 2 }; foo";

    Map vars = createTestMap();

    assertEquals(true, eval(expr, vars) instanceof Foo);

    Foo foo = (Foo) test(expr);

    assertEquals(10, foo.getCountTest());
    assertEquals("Hello", foo.aValue);
    assertEquals("Goodbye", foo.bValue);
  }

  // Category not isolated: the inline `with` form `foo.{name='poopy', aValue='bar'}` compiles but acts
  // as a no-op; the assertions fail with expected:<[poopy]> but was:<[dog]>. The expression also uses
  // single-quoted strings (single-quote-string); whether removing them changes the outcome was not tested.
  @Test
  @KnownCompatibility("with-block-syntax")
  public void testInlineWith() {
    Serializable expr = new ExpressionCompiler("foo.{name='poopy', aValue='bar'}").compile();
    Foo f = (Foo) executeExpression(expr, createTestMap());
    assertEquals("poopy", f.getName());
    assertEquals("bar", f.aValue);
  }

  // Category not isolated: the inline `with` form `foo.{name = 'poopy', ...}` compiles but acts as a
  // no-op; the assertions fail with expected:<[poopy]> but was:<[dog]>. The expression also uses
  // single-quoted strings (single-quote-string); whether removing them changes the outcome was not tested.
  @Test
  @KnownCompatibility("with-block-syntax")
  public void testInlineWith2() {
    Serializable expr = new ExpressionCompiler("foo.{name = 'poopy', aValue = 'bar', bar.{name = 'foobie'}}").compile();

    Foo f = (Foo) executeExpression(expr, createTestMap());

    assertEquals("poopy", f.getName());
    assertEquals("bar", f.aValue);
    assertEquals("foobie", f.getBar().getName());
  }

  // Category not isolated: the inline `with` form `foo.{name = 'poopy', ...}` compiles but acts as a
  // no-op; assertions fail with wrong values. The expression also uses single-quoted strings
  // (single-quote-string); whether removing them changes the outcome was not tested.
  @Test
  @KnownCompatibility("with-block-syntax")
  public void testInlineWith3() {
    Serializable expr = new ExpressionCompiler("foo.{name = 'poopy', aValue = 'bar', bar.{name = 'foobie'}, toUC('doopy')}").compile();

    Foo f = (Foo) executeExpression(expr, createTestMap());

    assertEquals("poopy", f.getName());
    assertEquals("bar", f.aValue);
    assertEquals("foobie", f.getBar().getName());
    assertEquals("doopy", f.register);
  }

  // Category not isolated: same as testInlineWith3 but without spaces around `=`. The inline `with` form
  // compiles but acts as a no-op; assertions fail with wrong values. The expression also uses single-quoted
  // strings (single-quote-string); whether removing them changes the outcome was not tested.
  @Test
  @KnownCompatibility("with-block-syntax")
  public void testInlineWith3a() {
    Serializable expr = new ExpressionCompiler("foo.{name='poopy',aValue='bar',bar.{name='foobie'},toUC('doopy')}").compile();

    Foo f = (Foo) executeExpression(expr, createTestMap());

    assertEquals("poopy", f.getName());
    assertEquals("bar", f.aValue);
    assertEquals("foobie", f.getBar().getName());
    assertEquals("doopy", f.register);
  }

  // Category not isolated: the inline `with` form `new Foo().{ name = 'bar' }` compiles but acts as a
  // no-op; the assertion fails with expected:<[bar]> but was:<[dog]>. The expression also uses
  // single-quoted strings (single-quote-string); whether removing them changes the outcome was not tested.
  // MVEL2 optimizer selection (ASM) has no MVEL3 equivalent.
  @Test
  @KnownCompatibility("with-block-syntax")
  public void testInlineWith4() {
    // MVEL2 optimizer selection (OptimizerFactory.setDefaultOptimizer("ASM")) has no MVEL3 equivalent.
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addImport(Foo.class);

    Serializable c = new ExpressionCompiler("new Foo().{ name = 'bar' }", ctx).compile();

    Foo f = (Foo) executeExpression(c);

    assertEquals("bar", f.getName());

    f = (Foo) executeExpression(c);

    assertEquals("bar", f.getName());
  }

  // Category not isolated: the inline `with` form `foo.{name='poopy', aValue='bar'}` compiles but acts
  // as a no-op; assertions fail with wrong values. The expression also uses single-quoted strings
  // (single-quote-string); whether removing them changes the outcome was not tested. MVEL2 strong-typing
  // and ASM optimizer have no MVEL3 equivalent.
  @Test
  @KnownCompatibility("with-block-syntax")
  public void testInlineWith5() {
    // MVEL2 ParserContext.strongTyping() and OptimizerFactory ("ASM") have no MVEL3 equivalent.
    // Strong typing is emulated by declaring the input type.
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addInput("foo", Foo.class);

    Serializable expr = new ExpressionCompiler("foo.{name='poopy', aValue='bar'}", ctx).compile();
    Foo f = (Foo) executeExpression(expr, createTestMap());
    assertEquals("poopy", f.getName());
    assertEquals("bar", f.aValue);
  }

  @Test
  @KnownCompatibility("with-block-syntax")
  public void testInlineWithImpliedThis() {
    Base b = new Base();
    Serializable compiled = new ExpressionCompiler(".{ data = 'foo' }").compile();

    executeExpression(compiled, b);

    assertEquals(b.data, "foo");
  }

  @Test
  @KnownCompatibility("with-block-syntax")
  public void testSingleMethodCall() {
    Base b = new Base();

    Map map = new HashMap();
    map.put("base", b);

    eval("base.{ populate() }", map);

    assertEquals("sarah", b.barfoo);
  }

  @Test
  @KnownCompatibility("with-block-syntax")
  public void testWithMultipleMethodCalls() {
    // MVEL2 ParserContext.create().stronglyTyped() has no MVEL3 equivalent.
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addInput("foo", Foo.class);

    compileExpression("with (foo) { setName('foo'), setBar(null) }", ctx);
  }

  // Category not isolated: fails with parse errors at the `with` block body. The expression uses nested
  // `with` inside a map literal (nested-inline-collection / with-block-syntax). The with-block-syntax
  // parse failure was observed first; nested-inline-collection was not tested separately.
  @Test
  @KnownCompatibility("with-block-syntax")
  public void testNewUsingWith() {
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addImport(Foo.class);
    ctx.addImport(Bar.class);

    // MVEL2 ParserContext strong typing and reflective optimizer have no MVEL3 equivalent.
    Serializable s = compileExpression("[ 'foo' : (with ( new Foo() )" +
        " { bar = with ( new Bar() ) { name = 'ziggy' } }) ]",
        ctx);

    assertEquals("ziggy",
        (((Foo) ((Map<?, ?>) executeExpression(s)).get("foo")).getBar().getName()));
  }

  // Category not isolated: fails with parse errors at the `with` block body.
  // MVEL2 ParserContext debug symbols and reflective optimizer have no MVEL3 equivalent.
  // Whether the additional context causes different behaviour was not tested.
  @Test
  @KnownCompatibility("with-block-syntax")
  public void testExecuteCoercionTwice() {
    // MVEL2 optimizer selection (reflective) and debug symbols have no MVEL3 equivalent.
    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("foo", new Foo());
    vars.put("$value", new Long(5));

    Serializable compiled = new ExpressionCompiler("with (foo) { countTest = $value };").compile();

    executeExpression(compiled, null, vars);
    executeExpression(compiled, null, vars);
  }

  // Category not isolated: fails with parse errors at the `with` block body.
  // MVEL2 ParserContext debug symbols and ASM optimizer have no MVEL3 equivalent.
  // Whether the additional context causes different behaviour was not tested.
  @Test
  @KnownCompatibility("with-block-syntax")
  public void testExecuteCoercionTwice2() {
    // MVEL2 optimizer selection (ASM) and debug symbols have no MVEL3 equivalent.
    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("foo", new Foo());
    vars.put("$value", new Long(5));

    Serializable compiled = new ExpressionCompiler("with (foo) { countTest = $value };").compile();

    executeExpression(compiled, null, vars);
    executeExpression(compiled, null, vars);
  }

  // Category not isolated: fails with parse errors at the inline `with` body. The expression also uses
  // single-quoted strings (single-quote-string). The with-block-syntax parse failure was observed first;
  // single-quote-string was not tested separately.
  @Test
  @KnownCompatibility("with-block-syntax")
  public void testNestedWithInList() {
    Recipient recipient1 = new Recipient();
    recipient1.setName("userName1");
    recipient1.setEmail("user1@domain.com");

    Recipient recipient2 = new Recipient();
    recipient2.setName("userName2");
    recipient2.setEmail("user2@domain.com");

    List<Recipient> list = new ArrayList<Recipient>();
    list.add(recipient1);
    list.add(recipient2);

    String text = "array = [" + "(with ( new Recipient() ) {name = 'userName1', email = 'user1@domain.com' }),"
        + "(with ( new Recipient() ) {name = 'userName2', email = 'user2@domain.com' })];\n";

    TestCompilerContext context = new TestCompilerContext();
    context.addImport(Recipient.class);

    Serializable execution = new ExpressionCompiler(text, context).compile();
    List<?> result = (List<?>) executeExpression(execution,
        new HashMap<String, Object>());
    assertEquals(list, result);
  }

  // Category not isolated: fails with parse errors at the inline `with` body. The expression also uses
  // single-quoted strings (single-quote-string) and nested `with` blocks (with-block-syntax). The
  // with-block-syntax parse failure was observed first; the others were not tested separately.
  // MVEL2 ASM and reflective optimizers have no MVEL3 equivalent.
  @Test
  @KnownCompatibility("with-block-syntax")
  public void testNestedWithInComplexGraph3() {
    Recipients recipients = new Recipients();

    Recipient recipient1 = new Recipient();
    recipient1.setName("user1");
    recipient1.setEmail("user1@domain.com");
    recipients.addRecipient(recipient1);

    Recipient recipient2 = new Recipient();
    recipient2.setName("user2");
    recipient2.setEmail("user2@domain.com");
    recipients.addRecipient(recipient2);

    EmailMessage msg = new EmailMessage();
    msg.setRecipients(recipients);
    msg.setFrom("from@domain.com");

    String text = "";
    text += "new EmailMessage().{ ";
    text += "     recipients = new Recipients().{ ";
    text += "         recipients = [ new Recipient().{ name = 'user1', email = 'user1@domain.com' }, ";
    text += "                        new Recipient().{ name = 'user2', email = 'user2@domain.com' } ] ";
    text += "     }, ";
    text += "     from = 'from@domain.com' }";
    TestCompilerContext context = new TestCompilerContext();
    context.addImport(Recipient.class);
    context.addImport(Recipients.class);
    context.addImport(EmailMessage.class);

    // MVEL2 ASM and reflective optimizers have no MVEL3 equivalent.
    Serializable execution = new ExpressionCompiler(text, context).compile();

    assertEquals(msg, executeExpression(execution));
    assertEquals(msg, executeExpression(execution));
    assertEquals(msg, executeExpression(execution));
  }

  // Category not isolated: fails with parse errors at the inline `with` body. The expression also uses
  // single-quoted strings (single-quote-string). The with-block-syntax parse failure was observed first;
  // single-quote-string was not tested separately.
  @Test
  @KnownCompatibility("with-block-syntax")
  public void testInlineWithWithLiteral() {
    String expr = "'foo'.{ toString() }";
    assertEquals("foo", eval(expr));

    Serializable s = compileExpression(expr);

    assertEquals("foo", executeExpression(s));
  }

  // Category not isolated: fails with parse errors at the inline `with` body. The expression also uses
  // single-quoted strings (single-quote-string). The with-block-syntax parse failure was observed first;
  // single-quote-string was not tested separately.
  @Test
  @KnownCompatibility("with-block-syntax")
  public void testInlineWithWithLiteral2() {
    String expr = "'foo'.{ toString() . toString() . toString() }";
    assertEquals("foo", eval(expr));

    Serializable s = compileExpression(expr);

    assertEquals("foo", executeExpression(s));
  }

  // Category not isolated: fails with parse errors at the `with` block body.
  // MVEL2's ParserConfiguration.addPackageImport and strong typing have no MVEL3 equivalent;
  // the import is replaced by addImport. Whether removing package import affects the parse error
  // was not tested.
  @Test
  @KnownCompatibility("with-block-syntax")
  public void testWithAndEnumInPackageImport() {
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addImport(MyEnum.class);
    ctx.addInput("thing", Thing.class);

    Thing thing = new Thing("xxx");
    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("thing", thing);

    Serializable stmt = compileExpression("with( thing ) { myEnum = MyEnum.FULL_DOCUMENTATION }", ctx);
    executeExpression(stmt, null, vars);
    assertEquals(MyEnum.FULL_DOCUMENTATION, thing.getMyEnum());
  }

  @Test
  @KnownCompatibility("with-block-syntax")
  public void testSataticClassImportViaFactoryAndWithModification() {
    // MVEL2's ClassImportResolverFactory / ResolverTools / MapVariableResolverFactory chain and
    // OptimizerFactory ("ASM") have no MVEL3 equivalent.
    // The import is passed via TestCompilerContext instead.
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addImport(Person.class);

    assertEquals(21,
        executeExpression(
            compileExpression("p = new Person('tom'); p.age = 20; " +
                "with( p ) { age = p.age + 1 }; return p.age;",
                ctx),
            createTestMap()));
  }

  /** Simple person fixture used by {@code testSataticClassImportViaFactoryAndWithModification}. */
  public static class Person {
    private String name;
    private int age;

    public Person() {
    }

    public Person(String name) {
      this.name = name;
    }

    public Person(String name, int age) {
      this.name = name;
      this.age = age;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public int getAge() {
      return age;
    }

    public void setAge(int age) {
      this.age = age;
    }
  }

  public static class Recipient {
    private String name;
    private String email;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((email == null) ? 0 : email.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      final Recipient other = (Recipient) obj;
      if (email == null) {
        if (other.email != null) return false;
      }
      else if (!email.equals(other.email)) return false;
      if (name == null) {
        if (other.name != null) return false;
      }
      else if (!name.equals(other.name)) return false;
      return true;
    }
  }

  public static class Recipients {
    private List<Recipient> list = Collections.emptyList();

    public void setRecipients(List<Recipient> recipients) {
      this.list = recipients;
    }

    public boolean addRecipient(Recipient recipient) {
      if (list == Collections.EMPTY_LIST) {
        this.list = new ArrayList<Recipient>();
      }
      if (!this.list.contains(recipient)) {
        this.list.add(recipient);
        return true;
      }
      return false;
    }

    public boolean removeRecipient(Recipient recipient) {
      return this.list.remove(recipient);
    }

    public List<Recipient> getRecipients() {
      return this.list;
    }

    public Recipient[] toArray() {
      return list.toArray(new Recipient[list.size()]);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((list == null) ? 0 : list.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      final Recipients other = (Recipients) obj;
      if (list == null) {
        if (other.list != null) return false;
      }
      return list.equals(other.list);
    }
  }

  public static class EmailMessage {
    private Recipients recipients;
    private String from;

    public EmailMessage() {
    }

    public Recipients getRecipients() {
      return recipients;
    }

    public void setRecipients(Recipients recipients) {
      this.recipients = recipients;
    }

    public String getFrom() {
      return from;
    }

    public void setFrom(String from) {
      this.from = from;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((from == null) ? 0 : from.hashCode());
      result = prime * result + ((recipients == null) ? 0 : recipients.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      final EmailMessage other = (EmailMessage) obj;
      if (from == null) {
        if (other.from != null) return false;
      }
      else if (!from.equals(other.from)) return false;
      if (recipients == null) {
        if (other.recipients != null) return false;
      }
      else if (!recipients.equals(other.recipients)) return false;
      return true;
    }
  }
}
