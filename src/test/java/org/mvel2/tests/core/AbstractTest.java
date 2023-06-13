package org.mvel2.tests.core;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.mvel2.DataConversion;
import org.mvel3.EvaluatorBuilder;
import org.mvel3.EvaluatorBuilder.ContextInfoBuilder;
import org.mvel3.EvaluatorBuilder.EvaluatorInfo;
import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.dynamic.DynamicOptimizer;
import org.mvel2.tests.core.res.DerivedClass;
import org.mvel2.tests.core.res.Foo;
import org.mvel2.tests.core.res.TestInterface;
import org.mvel3.Evaluator;
import org.mvel3.Type;
import org.mvel3.transpiler.MVELTranspiler;
import org.mvel3.transpiler.context.Declaration;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static org.mvel2.MVEL.compileExpression;
import static org.mvel2.MVEL.executeExpression;
import static org.mvel2.debug.DebugTools.decompile;

public abstract class AbstractTest extends TestCase {

  static {
    // Modify the dynamic optimizer to ensure it always engages the JIT
    DynamicOptimizer.tenuringThreshold = 1;
  }


  private boolean silentTests = Boolean.getBoolean("mvel.tests.silent");

  public void testNothing() {
    // to satify Eclipse and Surefire.
  }

  protected void setUp() throws Exception {

  }

  protected static Map createTestMap() {
    Map map = new HashMap();
    map.put("foo", new Foo());
    map.put("a", 5);
    map.put("b", 10);
    map.put("c", "cat");
    map.put("BWAH", "");

    map.put("misc", new MiscTestClass());

    map.put("pi", "3.14");
    map.put("hour", 60);
    map.put("zero", 0);

    map.put("array", new String[]{"", "blip"});

    map.put("order", new Order());
    map.put("$id", 20);

    map.put("five", 5);

    map.put("testImpl",
        new TestInterface() {

          public String getName() {
            return "FOOBAR!";
          }


          public boolean isFoo() {
            return true;
          }
        });

    map.put("derived", new DerivedClass());

    map.put("ipaddr", "10.1.1.2");

    map.put("dt1", new Date(currentTimeMillis() - 100000));
    map.put("dt2", new Date(currentTimeMillis()));
    return map;
  }


  protected void tearDown() throws Exception {
  }

  protected Object test(final String ex) {
    Thread[] threads;

    int threadCount;
    if (Boolean.getBoolean("mvel.tests.quick")) {
      threadCount = 1;
    }
    else if (getProperty("mvel.tests.threadcount") != null) {
      threadCount = parseInt(getProperty("mvel.tests.threadcount"));
    }
    else {
      threadCount = 5;
    }
    threads = new Thread[1];

    final Collection<Object> results = Collections.synchronizedCollection(new LinkedList<Object>());
    final Collection<Throwable> exceptions = Collections.synchronizedCollection(new LinkedList<Throwable>());
    long time = currentTimeMillis();

    for (int i = 0; i < threads.length; i++) {
      threads[i] = new Thread(new TestRunner(results, exceptions, ex));
    }

    if (!silentTests) {
      System.out.println("\n[test] begin test for:\n----------------------");
      System.out.println(ex);
      System.out.println("----------------------");
    }

    for (Thread thread1 : threads) {
      if (threads.length > 1) {
        System.out.println("Start Thread.");
        thread1.setPriority(Thread.MIN_PRIORITY);
        thread1.start();
      }
      else {
        thread1.run();
      }
    }

    if (threads.length > 1) {
      boolean threadsRunning = true;
      while (threadsRunning) {
        threadsRunning = false;
        for (Thread thread : threads) {
          if (thread.isAlive()) {
            System.out.println("Thread Alive.");
            threadsRunning = true;
            break;
          }
        }

        try {
          Thread.sleep(250);
        }
        catch (InterruptedException e) {
          break;
        }
      }
    }

    System.out.println("All threads have stopped.");
    System.out.println("Result Count: " + results.size());

    // analyze results

    if (!silentTests) {
      System.out.println("[test] finished in: " + (currentTimeMillis() - time) + "ms (execution count: " + (threads.length * 8) + " [mixed modes])");
      System.out.print("[test] analyzing results ... ");
    }

    Object last = null;
    if (!exceptions.isEmpty()) {
        Throwable firstException = exceptions.iterator().next();
        String message = firstException.getMessage().replaceAll("\n", " ");
        if (message.length() > 80) {
            message = message.substring(0, 80 - 3) + "...";
        }
        throw new RuntimeException(exceptions.size() + " out of " + threadCount
                + " threads terminated due to exception: " + message,
                firstException);
    }
    if (!results.isEmpty()) {
      last = results.iterator().next();
      if (last != null) {
        for (Object o : results) {
          if (o == null) {
            throw new AssertionError("differing result in multi-thread test (first array has: " + valueOf(last) + "; second has: " + valueOf(o) + ")");
          }
          else if (!o.equals(last)) {
            if (o.getClass().isArray()) {
//                            Object[] a1 = (Object[]) o;
//                            Object[] a2 = (Object[]) last;
//
//                            if (a1.length == a2.length) {
//                                for (int i = 0; i < a1.length; i++) {
//                                    if (a1[i] == null && a2[i] == null) {
//                                        continue;
//                                    }
//                                    else if (!a1[i].equals(a2[i])) {
//                                        throw new AssertionError("differing result in multi-thread test (first array has: " + valueOf(last) + "; second has: " + valueOf(o) + ")");
//                                    }
//                                }
//                            }
//                            else {
//                                throw new AssertionError("differing result in multi-thread test: array sizes differ.");
//                            }
            }
            else {
              throw new AssertionError("differing result in multi-thread test (last was: " + valueOf(last) + "; current is: " + valueOf(o) + ")");
            }
          }
          last = o;
        }
      }
    }


    if (!silentTests) {
      System.out.println("good!");
    }
    return last;
  }

  protected static class TestRunner implements Runnable {
    private final Collection<Object> results;
    private final Collection<Throwable> exceptions;
    private final String expression;

    public TestRunner(Collection<Object> results, Collection<Throwable> exceptions, String expression) {
      this.results = results;
        this.exceptions = exceptions;
        this.expression = expression;
    }

    public void run() {
      try {
        Object result = runSingleTest(expression);
        results.add(result);
      }
      catch (Throwable e) {
        exceptions.add(e);
        System.out.println("thread terminating due to exception");
        e.printStackTrace();
      }
    }
  }

  protected static Object runSingleTest(final String ex) {
    return _test(ex);
  }

  protected static Object runSingleTest(final String ex, Set<String> imports) {
    return _test(ex, imports);
  }

  protected static Object testCompiledSimple(String ex) {
    return MVEL.executeExpression(MVEL.compileExpression(ex));
  }

  protected static Object testCompiledSimple(String ex, Map map) {
    return new org.mvel3.MVEL().executeExpression(ex, Collections.emptySet(), (Map<String, Object>) map);
  }

  protected static Object testCompiledSimpleVoid(String ex, Map map) {
    return new org.mvel3.MVEL().executeExpression(ex, Collections.emptySet(), (Map<String, Object>) map, null);
  }

  protected static Object testCompiledSimple(String ex, Object base, Map map) {
    //org.mvel3.MVEL.get().compilePojoEvaluator(ex, Collections.emptySet(), base.getClass(), Collections.emptySet(), Object.class);
    return MVEL.executeExpression(MVEL.compileExpression(ex), base, map);
  }

  private static String maybeWrap(String providedExpr) {
    String actualExpr = providedExpr;
    if ( !providedExpr.contains(";")) { // @TODO this is aa very crude sniff. Personally I'd rather not sniff and instead have two different method calls (mdp)
      //actualExpr = "{ return " + providedExpr + ";}";
      actualExpr = "return " + providedExpr + ";";
    }
    return actualExpr;
  }

  protected static Object _test(String ex) {
    return _test(ex, Collections.<String>emptySet());
  }

  protected static Object _test(String ex, Set<String> imports) {
    //ex = maybeWrap(ex);
    Map<String, Object>                          vars     = createTestMap();
    Map<String, Type<?>>                            types    = org.mvel3.MVEL.getTypeMap(vars);
    Evaluator<Map<String, Object>, Void, Object> compiled = new org.mvel3.MVEL().compileMapEvaluator(ex, Object.class, imports, types);
    Object result = compiled.eval(createTestMap());
    System.out.println("result: " + result);
    return result;

    //createTestMap()

//    //ExpressionCompiler compiled = new ExpressionCompiler(ex);
//    StringAppender failErrors = new StringAppender();
//
//    Object first = null, second = null, third = null, fourth = null, fifth = null, sixth = null, seventh = null,
//        eighth = null;
//
//    //System.out.println(DebugTools.decompile((Serializable) compiled));
//
//    if (!Boolean.getBoolean("mvel2.disable.jit")) {
//
//      setDefaultOptimizer("ASM");
//
//      try {
//        first = compiled.eval(createTestMap());
//        //first = executeExpression(compiled, new Base(), createTestMap());
//      }
//      catch (Exception e) {
//        failErrors.append("\nFIRST TEST: { " + ex + " }: EXCEPTION REPORT: \n\n");
//
//        CharArrayWriter writer = new CharArrayWriter();
//        e.printStackTrace(new PrintWriter(writer));
//
//        failErrors.append(writer.toCharArray());
//      }
//
//      try {
//        second = compiled.eval(createTestMap());
//        //second = executeExpression(compiled, new Base(), createTestMap());
//      }
//      catch (Exception e) {
//        failErrors.append("\nSECOND TEST: { " + ex + " }: EXCEPTION REPORT: \n\n");
//
//        CharArrayWriter writer = new CharArrayWriter();
//        e.printStackTrace(new PrintWriter(writer));
//
//        failErrors.append(writer.toCharArray());
//      }
//
//    }
//
//    try {
//      third = compiled.eval(createTestMap());
//      //third = MVEL.eval(ex, new Base(), createTestMap());
//    }
//    catch (Exception e) {
//      failErrors.append("\nTHIRD TEST: { " + ex + " }: EXCEPTION REPORT: \n\n");
//
//      CharArrayWriter writer = new CharArrayWriter();
//      e.printStackTrace(new PrintWriter(writer));
//
//      failErrors.append(writer.toCharArray());
//    }
//
//    if (first != null && !first.getClass().isArray()) {
//      if (!first.equals(second)) {
//        System.out.println(failErrors.toString());
//
//        throw new AssertionError("Different result from test 1 and 2 (Compiled Re-Run / JIT) [first: "
//            + valueOf(first) + "; second: " + valueOf(second) + "]");
//      }
//
//      if (!first.equals(third)) {
//        if (failErrors != null) System.out.println(failErrors.toString());
//
//        throw new AssertionError("Different result from test 1 and 3 (Compiled to Interpreted) [first: " +
//            valueOf(first) + " (" + (first != null ? first.getClass().getName() : null) + "); third: " + valueOf(third) + " (" + (third != null ? third.getClass().getName() : "null") + ")]");
//      }
//    }
//
//    setDefaultOptimizer("reflective");
//    Serializable compiled2 = compileExpression(ex);
//
//    try {
//      fourth = executeExpression(compiled2, new Base(), createTestMap());
//    }
//    catch (Exception e) {
//      if (failErrors == null) failErrors = new StringAppender();
//      failErrors.append("\nFOURTH TEST: { " + ex + " }: EXCEPTION REPORT: \n\n");
//
//      CharArrayWriter writer = new CharArrayWriter();
//      e.printStackTrace(new PrintWriter(writer));
//
//      failErrors.append(writer.toCharArray());
//    }
//
//    try {
//      fifth = executeExpression(compiled2, new Base(), createTestMap());
//    }
//    catch (Exception e) {
//      e.printStackTrace();
//      if (failErrors == null) failErrors = new StringAppender();
//      failErrors.append("\nFIFTH TEST: { " + ex + " }: EXCEPTION REPORT: \n\n");
//
//      CharArrayWriter writer = new CharArrayWriter();
//      e.printStackTrace(new PrintWriter(writer));
//
//      failErrors.append(writer.toCharArray());
//    }
//
//    if (fourth != null && !fourth.getClass().isArray()) {
//      if (!fourth.equals(fifth)) {
//        throw new AssertionError("Different result from test 4 and 5 (Compiled Re-Run X2) [fourth: "
//            + valueOf(fourth) + "; fifth: " + valueOf(fifth) + "]");
//      }
//    }
//
//    ParserContext ctx = new ParserContext();
//    ctx.setSourceFile("unittest");
//    ctx.setDebugSymbols(true);
//
//    ExpressionCompiler debuggingCompiler = new ExpressionCompiler(ex, ctx);
//    //     debuggingCompiler.setDebugSymbols(true);
//
//    CompiledExpression compiledD = debuggingCompiler.compile();
//
//    try {
//      sixth = executeExpression(compiledD, new Base(), createTestMap());
//    }
//    catch (Exception e) {
//      if (failErrors == null) failErrors = new StringAppender();
//      failErrors.append("\nSIXTH TEST: { " + ex + " }: EXCEPTION REPORT: \n\n");
//
//      CharArrayWriter writer = new CharArrayWriter();
//      e.printStackTrace(new PrintWriter(writer));
//
//      failErrors.append(writer.toCharArray());
//    }
//
//
//    if (sixth != null && !sixth.getClass().isArray()) {
//      if (!fifth.equals(sixth)) {
//        System.out.println("Payload 1 -- No Symbols: ");
//        System.out.println(decompile(compiled));
//        System.out.println();
//
//        System.out.println("Payload 2 -- With Symbols: ");
//        System.out.println(decompile(compiledD));
//        System.out.println();
//
//        throw new AssertionError("Different result from test 5 and 6 (Compiled to Compiled+DebuggingSymbols) [first: "
//            + valueOf(fifth) + "; second: " + valueOf(sixth) + "]");
//      }
//    }
//
//    try {
//      seventh = executeExpression(compiledD, new Base(), createTestMap());
//    }
//    catch (Exception e) {
//      if (failErrors == null) failErrors = new StringAppender();
//      failErrors.append("\nSEVENTH TEST: { " + ex + " }: EXCEPTION REPORT: \n\n");
//
//      CharArrayWriter writer = new CharArrayWriter();
//      e.printStackTrace(new PrintWriter(writer));
//
//      failErrors.append(writer.toCharArray());
//    }
//
//    if (seventh != null && !seventh.getClass().isArray()) {
//      if (!seventh.equals(sixth)) {
//        throw new AssertionError("Different result from test 4 and 5 (Compiled Re-Run / Reflective) [first: "
//            + valueOf(first) + "; second: " + valueOf(second) + "]");
//      }
//    }
//
//    try {
//      Serializable xx = serializationTest(compiledD);
//      eighth = executeExpression(xx, new Base(), new MapVariableResolverFactory(createTestMap()));
//    }
//    catch (Exception e) {
//      if (failErrors == null) failErrors = new StringAppender();
//      failErrors.append("\nEIGHTH TEST (Serializability): { " + ex + " }: EXCEPTION REPORT: \n\n");
//
//      CharArrayWriter writer = new CharArrayWriter();
//      e.printStackTrace(new PrintWriter(writer));
//
//      failErrors.append(writer.toCharArray());
//    }
//
//    if (eighth != null && !eighth.getClass().isArray()) {
//      if (!eighth.equals(seventh)) {
//        throw new AssertionError("Different result from test 7 and 8 (Compiled Re-Run / Reflective) [first: "
//            + valueOf(first) + "; second: " + valueOf(second) + "]");
//      }
//    }
//
//
//    if (failErrors.length() > 0) {
//      System.out.println(decompile(compiledD));
//      throw new AssertionError("Detailed Failure Report:\n" + failErrors.toString());
//    }
//
//    return fourth;
  }

  public Object eval(String expression, Object rootObject, Map<String, Object> vars) {
    return eval(expression, rootObject, vars, new HashSet<>());
  }

  public Object eval(String expression, Object rootObject, Map<String, Object> vars, Set<String> imports) {
    if (imports == null) {
      imports = new HashSet<>();
    }

    EvaluatorBuilder<Object, Object, Object> builder = EvaluatorBuilder
                                                                            .create()
                                                                            .setImports(imports)
                                                                            .setExpression(expression)
                                                                            .setOutType(Type.type(Object.class));

    if (vars != null) {
      // @TODO why do these need to be separared, how can I fix the generics (mdp)
      ContextInfoBuilder<Object> contextBuilder = ContextInfoBuilder.create(Type.type(Map.class));
      contextBuilder.setVars(Declaration.from(org.mvel3.MVEL.getTypeMap(vars)));
      builder.setVariableInfo(contextBuilder);
    }

    Object evalContext = vars; // If vars are passsed us it, else default to the root object
    if (rootObject != null) {
      builder.setRootDeclaration(Type.type(rootObject.getClass()));

      if (vars != null) {
        vars.put("__this", rootObject); // there are vars, so the root object must map to a property of the context
      } else {
        ContextInfoBuilder<Object> contextBuilder = ContextInfoBuilder.create(Type.type(rootObject.getClass()));
        builder.setVariableInfo(contextBuilder);
        evalContext = rootObject; // no vars, so default to the root object
      }
    }

    EvaluatorInfo<Object, Object, Object> evalInfo = builder.build();

    return org.mvel3.MVEL.get().compile(evalInfo).eval(evalContext);
  }

  public Object eval(String expression, Map<String, String> map, VariableResolverFactory factory) {
    return true;
  }

  public Object eval(String expr, Object rootObject) {
    return eval(expr, rootObject, new HashMap<>());
  }

  public Object eval(String expr) {
    return eval(expr, null, new HashMap<>());
  }

  protected static Serializable serializationTest(Serializable s) throws Exception {
    File file = new File("./mvel_ser_test" + currentTimeMillis() + Math.round(Math.random() * 1000) + ".tmp");
    InputStream inputStream = null;
    ObjectInputStream objectIn = null;
    try {
      file.createNewFile();
      file.deleteOnExit();

      FileOutputStream fileStream = new FileOutputStream(file);
      ObjectOutputStream objectOut = new ObjectOutputStream(new BufferedOutputStream(fileStream));
      objectOut.writeObject(s);

      objectOut.flush();
      fileStream.flush();
      fileStream.close();

      inputStream = new BufferedInputStream(new FileInputStream(file));

      objectIn = new ObjectInputStream(inputStream);

      return (Serializable) objectIn.readObject();
    }
    finally {
      if (inputStream != null) inputStream.close();
      if (objectIn != null) objectIn.close();
      // file.delete();
    }

  }


  public static class MiscTestClass {
    int exec = 0;

    @SuppressWarnings({"unchecked", "UnnecessaryBoxing"})
    public List toList(Object object1, String string, int integer, Map map, List list) {
      exec++;
      List l = new ArrayList();
      l.add(object1);
      l.add(string);
      l.add(Integer.valueOf(integer));
      l.add(map);
      l.add(list);
      return l;
    }


    public int getExec() {
      return exec;
    }
  }

  public static class Bean {
    private Date myDate = new Date();

    public Date getToday() {
      return new Date();
    }

    public Date getNullDate() {
      return null;
    }

    public String getNullString() {
      return null;
    }

    public Date getMyDate() {
      return myDate;
    }

    public void setMyDate(Date myDate) {
      this.myDate = myDate;
    }
  }

  public static class Context {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
    private Bean bean;

    public Bean getBean() {
      return bean;
    }

    public void setBean(Bean bean) {
      this.bean = bean;
    }

    public String formatDate(Date date) {
      return date == null ? null : dateFormat.format(date);
    }

    public String formatString(String str) {
      return str == null ? "<NULL>" : str;
    }
  }

  public static class Person {
    private String name;
    private int age;
    private String likes;
    private List<Foo> footributes;
    private Map<String, Foo> maptributes;
    private Map<Object, Foo> objectKeyMaptributes;

    public Person() {

    }

    public Person(String name) {
      this.name = name;
    }

    public Person(String name, int age) {
      this.name = name;
      this.age = age;
    }

    public Person(String name, String likes, int age) {
      this.name = name;
      this.likes = likes;
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

    public List<Foo> getFootributes() {
      return footributes;
    }

    public void setFootributes(List<Foo> footributes) {
      this.footributes = footributes;
    }

    public Map<String, Foo> getMaptributes() {
      return maptributes;
    }

    public void setMaptributes(Map<String, Foo> maptributes) {
      this.maptributes = maptributes;
    }

    public Map<Object, Foo> getObjectKeyMaptributes() {
      return objectKeyMaptributes;
    }

    public void setObjectKeyMaptributes(Map<Object, Foo> objectKeyMaptributes) {
      this.objectKeyMaptributes = objectKeyMaptributes;
    }

    public String toString() {
      return "Person( name==" + name + " age==" + age + " likes==" + likes + " )";
    }
  }

  public static class Address {
    private String street;

    public Address(String street) {
      super();
      this.street = street;
    }

    public String getStreet() {
      return street;
    }

    public void setStreet(String street) {
      this.street = street;
    }
  }

  public static class Drools {
    public void insert(Object obj) {
    }
  }

  public static class Model {
    private List latestHeadlines;


    public List getLatestHeadlines() {
      return latestHeadlines;
    }

    public void setLatestHeadlines(List latestHeadlines) {
      this.latestHeadlines = latestHeadlines;
    }
  }

  public static class Message {
    public static final int HELLO = 0;
    public static final int GOODBYE = 1;

    private List items = new ArrayList();

    private String message;

    private int status;

    public String getMessage() {
      return this.message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public int getStatus() {
      return this.status;
    }

    public void setStatus(int status) {
      this.status = status;
    }

    public void addItem(Item item) {
      this.items.add(item);
    }

    public List getItems() {
      return items;
    }
  }

  public static class Item {
    private String name;

    public Item(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  public class ClassA {
    private Integer i;
    private double d;
    private String s;
    public Date date;
    private BigDecimal bigdec;
    private BigInteger bigint;

    public Integer getI() {
      return i;
    }

    public void setI(Integer i) {
      this.i = i;
    }

    public double getD() {
      return d;
    }

    public void setD(double d) {
      this.d = d;
    }

    public String getS() {
      return s;
    }

    public void setS(String s) {
      this.s = s;
    }

    public Date getDate() {
      return date;
    }

    public void setDate(Date date) {
      this.date = date;
    }

    public BigDecimal getBigdec() {
      return bigdec;
    }

    public void setBigdec(BigDecimal bigdec) {
      this.bigdec = bigdec;
    }

    public BigInteger getBigint() {
      return bigint;
    }

    public void setBigint(BigInteger bigint) {
      this.bigint = bigint;
    }
  }

  public class ClassB {
    private Integer i;
    private double d;
    private String s;
    public String date;
    private BigDecimal bigdec;
    private BigInteger bigint;

    public Integer getI() {
      return i;
    }

    public void setI(Integer i) {
      this.i = i;
    }

    public double getD() {
      return d;
    }

    public void setD(double d) {
      this.d = d;
    }

    public String getS() {
      return s;
    }

    public void setS(String s) {
      this.s = s;
    }

    public String getDate() {
      return date;
    }

    public void setDate(String date) {
      this.date = date;
    }

    public BigDecimal getBigdec() {
      return bigdec;
    }

    public void setBigdec(BigDecimal bigdec) {
      this.bigdec = bigdec;
    }

    public BigInteger getBigint() {
      return bigint;
    }

    public void setBigint(BigInteger bigint) {
      this.bigint = bigint;
    }
  }

  public static class Order {
    private int number = 20;


    public int getNumber() {
      return number;
    }

    public void setNumber(int number) {
      this.number = number;
    }
  }

  public static void assertNumEquals(Object obj, Object obj2) {
    assertNumEquals(obj, obj2, true);
  }

  public static void assertNumEquals(Object obj, Object obj2, boolean permitRoundingVariance) {
    if (obj == null || obj2 == null) throw new AssertionError("null value");


    if (obj.getClass().equals(obj2.getClass())) {
      if (obj instanceof Number) {
        double compare = ((Number) obj).doubleValue() - ((Number) obj2).doubleValue();
        if (!(compare <= 0.0001d && compare >= -0.0001d)) {
          throw new AssertionFailedError("expected <" + String.valueOf(obj) + "> but was <" + String.valueOf(obj) + ">");
        }
      }
      else {
        assertEquals(obj, obj2);
      }

    }
    else {
      obj = DataConversion.convert(obj, obj2.getClass());

      if (!obj.equals(obj2)) {
        if (permitRoundingVariance) {
          obj = DataConversion.convert(obj, Integer.class);
          obj2 = DataConversion.convert(obj2, Integer.class);

          assertEquals(obj, obj2);
        }
        else {
          throw new AssertionFailedError("expected <" + String.valueOf(obj) + "> but was <" + String.valueOf(obj) + ">");
        }
      }

    }
  }
}
