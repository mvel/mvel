package org.mvel2.tests.core;

import org.mvel2.MVEL;
import org.mvel2.ParserConfiguration;
import org.mvel2.ParserContext;
import org.mvel2.compiler.CompiledExpression;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.compiler.ExpressionCompiler;
import org.mvel2.integration.ResolverTools;
import org.mvel2.integration.impl.ClassImportResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.optimizers.OptimizerFactory;
import org.mvel2.tests.core.res.Bar;
import org.mvel2.tests.core.res.Base;
import org.mvel2.tests.core.res.Foo;
import org.mvel2.tests.core.res.MyEnum;
import org.mvel2.tests.core.res.Thing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mvel2.MVEL.compileExpression;
import static org.mvel2.MVEL.executeExpression;

public class WithTests extends AbstractTest {
  public void testWith() {
    String ex = "with (foo) {aValue = 'One',bValue='Two'}; foo.aValue + foo.bValue;";

    Map vars = createTestMap();
    Object o = MVEL.eval(ex, vars);
    assertEquals("OneTwo", o);

    assertEquals("OneTwo", test("with (foo) {aValue = 'One',bValue='Two'}; foo.aValue + foo.bValue;"));
  }

  public void testWith2() {
    assertEquals("OneTwoOne", test(
        "var y; with (foo) { \n" +
            "aValue = (y = 'One'), // this is a comment \n" +
            "bValue='Two'  // this is also a comment \n" +
            "}; \n" +
            "foo.aValue + foo.bValue + y;"));
  }

  public void testWith3() {
    String ex = "with (foo) {aValue = 'One',bValue='Two'}; with (foo) {aValue += 'One', bValue += 'Two'}; foo.aValue + foo.bValue;";

    Map vars = createTestMap();

    assertEquals("OneOneTwoTwo", MVEL.eval(ex, vars));


    assertEquals("OneOneTwoTwo", test("with (foo) {aValue = 'One',bValue='Two'}; with (foo) {aValue += 'One', bValue += 'Two'}; foo.aValue + foo.bValue;"));
  }


  public void testWith4() {
    assertEquals(10, test("with (foo) {countTest += 5 }; with (foo) { countTest *= 2 }; foo.countTest"));
  }

  public void testWith5() {
    String expr = "with (foo) { countTest += 5, \n" +
        "// foobar!\n" +
        "aValue = 'Hello',\n" +
        "/** Comment! **/\n" +
        "bValue = 'Goodbye'\n }; with (foo) { countTest *= 2 }; foo";

    Map vars = createTestMap();

    assertEquals(true, MVEL.eval(expr, vars) instanceof Foo);

    Foo foo = (Foo) test(expr);

    assertEquals(10, foo.getCountTest());
    assertEquals("Hello", foo.aValue);
    assertEquals("Goodbye", foo.bValue);
  }

  public void testInlineWith() {
    CompiledExpression expr = new ExpressionCompiler("foo.{name='poopy', aValue='bar'}").compile();
    Foo f = (Foo) executeExpression(expr, createTestMap());
    assertEquals("poopy", f.getName());
    assertEquals("bar", f.aValue);
  }

  public void testInlineWith2() {
    CompiledExpression expr = new ExpressionCompiler("foo.{name = 'poopy', aValue = 'bar', bar.{name = 'foobie'}}").compile();

    Foo f = (Foo) executeExpression(expr, createTestMap());

    assertEquals("poopy", f.getName());
    assertEquals("bar", f.aValue);
    assertEquals("foobie", f.getBar().getName());
  }

  public void testInlineWith3() {
    CompiledExpression expr = new ExpressionCompiler("foo.{name = 'poopy', aValue = 'bar', bar.{name = 'foobie'}, toUC('doopy')}").compile();

    Foo f = (Foo) executeExpression(expr, createTestMap());

    assertEquals("poopy", f.getName());
    assertEquals("bar", f.aValue);
    assertEquals("foobie", f.getBar().getName());
    assertEquals("doopy", f.register);
  }

  public void testInlineWith3a() {
    CompiledExpression expr = new ExpressionCompiler("foo.{name='poopy',aValue='bar',bar.{name='foobie'},toUC('doopy')}").compile();

    Foo f = (Foo) executeExpression(expr, createTestMap());

    assertEquals("poopy", f.getName());
    assertEquals("bar", f.aValue);
    assertEquals("foobie", f.getBar().getName());
    assertEquals("doopy", f.register);
  }

  public void testInlineWith4() {
    OptimizerFactory.setDefaultOptimizer("ASM");
    ParserContext pCtx = new ParserContext();
    pCtx.addImport(Foo.class);

    ExpressionCompiler expr = new ExpressionCompiler("new Foo().{ name = 'bar' }", pCtx);
    CompiledExpression c = expr.compile();

    Foo f = (Foo) executeExpression(c);

    assertEquals("bar", f.getName());

    f = (Foo) executeExpression(c);

    assertEquals("bar", f.getName());
  }

  public void testInlineWith5() {
    OptimizerFactory.setDefaultOptimizer("ASM");

    ParserContext pCtx = new ParserContext();
    pCtx.setStrongTyping(true);

    pCtx.addInput("foo", Foo.class);

    CompiledExpression expr = new ExpressionCompiler("foo.{name='poopy', aValue='bar'}", pCtx).compile();
    Foo f = (Foo) executeExpression(expr, createTestMap());
    assertEquals("poopy", f.getName());
    assertEquals("bar", f.aValue);
  }

  public void testInlineWithImpliedThis() {
    Base b = new Base();
    ExpressionCompiler expr = new ExpressionCompiler(".{ data = 'foo' }");
    CompiledExpression compiled = expr.compile();

    executeExpression(compiled, b);

    assertEquals(b.data, "foo");
  }

  public void testSingleMethodCall() {
    Base b = new Base();

    Map map = new HashMap();
    map.put("base", b);

    MVEL.eval("base.{ populate() }", map);

    assertEquals("sarah", b.barfoo);
  }

  public void testWithMultipleMethodCalls() {
    ParserContext ctx = ParserContext.create().stronglyTyped().withInput("foo", Foo.class);

    MVEL.compileExpression("with (foo) { setName('foo'), setBar(null) }", ctx);
  }

  public void testNewUsingWith() {
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addImport(Foo.class);
    ctx.addImport(Bar.class);

    Serializable s = compileExpression("[ 'foo' : (with ( new Foo() )" +
        " { bar = with ( new Bar() ) { name = 'ziggy' } }) ]",
        ctx);

    OptimizerFactory.setDefaultOptimizer("reflective");
    assertEquals("ziggy",
        (((Foo) ((Map) executeExpression(s)).get("foo")).getBar().getName()));
  }

  public void testSataticClassImportViaFactoryAndWithModification() {
    OptimizerFactory.setDefaultOptimizer("ASM");
    MapVariableResolverFactory mvf = new MapVariableResolverFactory(createTestMap());
    ClassImportResolverFactory classes = new ClassImportResolverFactory(null, null, false);
    classes.addClass(Person.class);

    ResolverTools.appendFactory(mvf,
        classes);

    assertEquals(21,
        executeExpression(
            compileExpression("p = new Person('tom'); p.age = 20; " +
                "with( p ) { age = p.age + 1 }; return p.age;",
                classes.getImportedClasses()),
            mvf));
  }


  public void testExecuteCoercionTwice() {
    OptimizerFactory.setDefaultOptimizer("reflective");

    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("foo",
        new Foo());
    vars.put("$value",
        new Long(5));

    ParserContext ctx = new ParserContext();
    ctx.setSourceFile("test.mv");
    ctx.setDebugSymbols(true);

    ExpressionCompiler compiler = new ExpressionCompiler("with (foo) { countTest = $value };", ctx);
    CompiledExpression compiled = compiler.compile();

    executeExpression(compiled, null, vars);
    executeExpression(compiled, null, vars);
  }

  public void testExecuteCoercionTwice2() {
    OptimizerFactory.setDefaultOptimizer("ASM");

    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("foo",
        new Foo());
    vars.put("$value",
        new Long(5));

    ParserContext ctx = new ParserContext();
    ctx.setSourceFile("test.mv");
    ctx.setDebugSymbols(true);

    ExpressionCompiler compiler = new ExpressionCompiler("with (foo) { countTest = $value };", ctx);
    CompiledExpression compiled = compiler.compile();

    executeExpression(compiled,
        null,
        vars);
    executeExpression(compiled,
        null,
        vars);
  }


  public void testNestedWithInList() {
    Recipient recipient1 = new Recipient();
    recipient1.setName("userName1");
    recipient1.setEmail("user1@domain.com");

    Recipient recipient2 = new Recipient();
    recipient2.setName("userName2");
    recipient2.setEmail("user2@domain.com");

    List list = new ArrayList();
    list.add(recipient1);
    list.add(recipient2);

    String text = "array = [" + "(with ( new Recipient() ) {name = 'userName1', email = 'user1@domain.com' }),"
        + "(with ( new Recipient() ) {name = 'userName2', email = 'user2@domain.com' })];\n";

    ParserContext context = new ParserContext();
    context.addImport(Recipient.class);

    ExpressionCompiler compiler = new ExpressionCompiler(text, context);
    Serializable execution = compiler.compile();
    List result = (List) executeExpression(execution,
        new HashMap());
    assertEquals(list,
        result);
  }

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
    ParserContext context;
    context = new ParserContext();
    context.addImport(Recipient.class);
    context.addImport(Recipients.class);
    context.addImport(EmailMessage.class);

    OptimizerFactory.setDefaultOptimizer("ASM");

    ExpressionCompiler compiler = new ExpressionCompiler(text, context);
    Serializable execution = compiler.compile();

    assertEquals(msg,
        executeExpression(execution));
    assertEquals(msg,
        executeExpression(execution));
    assertEquals(msg,
        executeExpression(execution));

    OptimizerFactory.setDefaultOptimizer("reflective");

    context = new ParserContext(context.getParserConfiguration());
    compiler = new ExpressionCompiler(text, context);
    execution = compiler.compile();

    assertEquals(msg,
        executeExpression(execution));
    assertEquals(msg,
        executeExpression(execution));
    assertEquals(msg,
        executeExpression(execution));
  }

  public void testInlineWithWithLiteral() {
    String expr = "'foo'.{ toString() }";
    assertEquals("foo", MVEL.eval(expr));

    Serializable s = MVEL.compileExpression(expr);

    assertEquals("foo", MVEL.executeExpression(s));
  }

  public void testInlineWithWithLiteral2() {
    String expr = "'foo'.{ toString() . toString() . toString() }";
    assertEquals("foo", MVEL.eval(expr));

    Serializable s = MVEL.compileExpression(expr);

    assertEquals("foo", MVEL.executeExpression(s));
  }

  public void testWithAndEnumInPackageImport() {
    ParserConfiguration pconf = new ParserConfiguration();
    pconf.addPackageImport(MyEnum.class.getPackage().getName());

    ParserContext pCtx = new ParserContext(pconf);
    pCtx.setStrongTyping(true);

    pCtx.addInput("thing", Thing.class);

    Thing thing = new Thing("xxx");
    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("thing", thing);

    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression("with( thing ) { myEnum = MyEnum.FULL_DOCUMENTATION }", pCtx);
    MVEL.executeExpression(stmt, null, vars);
    assertEquals(MyEnum.FULL_DOCUMENTATION, thing.getMyEnum());
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
    private List<Recipient> list = Collections.EMPTY_LIST;

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
