package org.mvel.tests.main;

import junit.framework.TestCase;
import org.mvel.ExpressionCompiler;
import org.mvel.MVEL;
import org.mvel.ParserContext;
import org.mvel.debug.DebugTools;
import org.mvel.optimizers.OptimizerFactory;
import org.mvel.tests.main.res.*;
import org.mvel.util.StringAppender;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class AbstractTest extends TestCase {
    protected Foo foo = new Foo();
    protected Map<String, Object> map = new HashMap<String, Object>();
    protected Base base = new Base();
    protected DerivedClass derived = new DerivedClass();

    public void testNothing() {
        // to satify Eclipse and Surefire.
    }

    public AbstractTest() {
        foo.setBar(new Bar());
        map.put("foo", foo);
        map.put("a", null);
        map.put("b", null);
        map.put("c", "cat");
        map.put("BWAH", "");

        map.put("misc", new MiscTestClass());

        map.put("pi", "3.14");
        map.put("hour", "60");
        map.put("zero", 0);

        map.put("order", new Order());
        map.put("$id", 20);

        map.put("testImpl",
                new TestInterface() {

                    public String getName() {
                        return "FOOBAR!";
                    }


                    public boolean isFoo() {
                        return true;
                    }
                });

        map.put("derived", derived);
    }

    protected Object test(String ex) {
        return test(ex, this.base, this.map);
    }

    protected Object test(String ex, Object base, Map map) {
        ExpressionCompiler compiler = new ExpressionCompiler(ex);
        StringAppender failErrors = null;

        Serializable compiled = compiler.compile();
        Object first = null, second = null, third = null, fourth = null, fifth = null, sixth = null, seventh = null;

     //  System.out.println(DebugTools.decompile((Serializable) compiled));

        if (!Boolean.getBoolean("mvel.disable.jit")) {

            OptimizerFactory.setDefaultOptimizer("ASM");

            try {
                first = MVEL.executeExpression(compiled, base, map);

            }
            catch (Exception e) {
                failErrors = new StringAppender();
                failErrors.append("\nFIRST TEST: { " + ex + " }: EXCEPTION REPORT: \n\n");

                CharArrayWriter writer = new CharArrayWriter();
                e.printStackTrace(new PrintWriter(writer));

                failErrors.append(writer.toCharArray());
            }

            try {
                second = MVEL.executeExpression(compiled, base, map);
            }
            catch (Exception e) {
                if (failErrors == null) failErrors = new StringAppender();
                failErrors.append("\nSECOND TEST: { " + ex + " }: EXCEPTION REPORT: \n\n");

                CharArrayWriter writer = new CharArrayWriter();
                e.printStackTrace(new PrintWriter(writer));

                failErrors.append(writer.toCharArray());
            }

        }

        try {
            third = MVEL.eval(ex, base, map);
        }
        catch (Exception e) {
            if (failErrors == null) failErrors = new StringAppender();
            failErrors.append("\nTHIRD TEST: { " + ex + " }: EXCEPTION REPORT: \n\n");

            CharArrayWriter writer = new CharArrayWriter();
            e.printStackTrace(new PrintWriter(writer));

            failErrors.append(writer.toCharArray());
        }


        if (first != null && !first.getClass().isArray()) {
            if (!first.equals(second)) {
                throw new AssertionError("Different result from test 1 and 2 (Compiled Re-Run / JIT) [first: "
                        + String.valueOf(first) + "; second: " + String.valueOf(second) + "]");
            }

            if (!first.equals(third)) {
                throw new AssertionError("Different result from test 1 and 3 (Compiled to Interpreted) [first: " +
                        String.valueOf(first) + " (" + first.getClass().getName() + "); third: " + String.valueOf(third) + " (" + (second != null ? first.getClass().getName() : "null") + ")]");
            }
        }

        OptimizerFactory.setDefaultOptimizer("reflective");
        compiled = MVEL.compileExpression(ex);

        try {
            fourth = MVEL.executeExpression(compiled, base, map);
        }
        catch (Exception e) {
            if (failErrors == null) failErrors = new StringAppender();
            failErrors.append("\nFOURTH TEST: { " + ex + " }: EXCEPTION REPORT: \n\n");

            CharArrayWriter writer = new CharArrayWriter();
            e.printStackTrace(new PrintWriter(writer));

            failErrors.append(writer.toCharArray());
        }

        try {
            fifth = MVEL.executeExpression(compiled, base, map);
        }
        catch (Exception e) {
            if (failErrors == null) failErrors = new StringAppender();
            failErrors.append("\nFIFTH TEST: { " + ex + " }: EXCEPTION REPORT: \n\n");

            CharArrayWriter writer = new CharArrayWriter();
            e.printStackTrace(new PrintWriter(writer));

            failErrors.append(writer.toCharArray());
        }

        if (fourth != null && !fourth.getClass().isArray()) {
            if (!fourth.equals(fifth)) {
                throw new AssertionError("Different result from test 4 and 5 (Compiled Re-Run / Reflective) [first: "
                        + String.valueOf(first) + "; second: " + String.valueOf(second) + "]");
            }
        }

        ParserContext ctx = new ParserContext();
        ctx.setSourceFile("unittest");
        ExpressionCompiler debuggingCompiler = new ExpressionCompiler(ex);
        debuggingCompiler.setDebugSymbols(true);

        Serializable compiledD = debuggingCompiler.compile(ctx);

        try {
            sixth = MVEL.executeExpression(compiledD, base, map);
        }
        catch (Exception e) {
            if (failErrors == null) failErrors = new StringAppender();
            failErrors.append("\nSIXTH TEST: { " + ex + " }: EXCEPTION REPORT: \n\n");

            CharArrayWriter writer = new CharArrayWriter();
            e.printStackTrace(new PrintWriter(writer));

            failErrors.append(writer.toCharArray());
        }


        if (sixth != null && !sixth.getClass().isArray()) {
            if (!fifth.equals(sixth)) {
                System.out.println("Payload 1 -- No Symbols: ");
                System.out.println(DebugTools.decompile(compiled));
                System.out.println();

                System.out.println("Payload 2 -- With Symbols: ");
                System.out.println(DebugTools.decompile(compiledD));
                System.out.println();

                throw new AssertionError("Different result from test 5 and 6 (Compiled to Compiled+DebuggingSymbols) [first: "
                        + String.valueOf(fifth) + "; second: " + String.valueOf(sixth) + "]");
            }
        }

        try {
            seventh = MVEL.executeExpression(compiledD, base, map);
        }
        catch (Exception e) {
            if (failErrors == null) failErrors = new StringAppender();
            failErrors.append("\nSEVENTH TEST: { " + ex + " }: EXCEPTION REPORT: \n\n");

            CharArrayWriter writer = new CharArrayWriter();
            e.printStackTrace(new PrintWriter(writer));

            failErrors.append(writer.toCharArray());
        }

        if (seventh != null && !seventh.getClass().isArray()) {
            if (!seventh.equals(sixth)) {
                throw new AssertionError("Different result from test 4 and 5 (Compiled Re-Run / Reflective) [first: "
                        + String.valueOf(first) + "; second: " + String.valueOf(second) + "]");
            }
        }

        if (failErrors != null) {
            throw new AssertionError("Detailed Failure Report:\n" + failErrors.toString());

        }


        return fourth;
    }

    public static class MiscTestClass {
        int exec = 0;

        @SuppressWarnings({"unchecked", "UnnecessaryBoxing"})
        public List toList(Object object1, String string, int integer, Map map, List list) {
            exec++;
            List l = new ArrayList();
            l.add(object1);
            l.add(string);
            l.add(new Integer(integer));
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

        public Person() {

        }

        public Person(String name) {
            this.name = name;
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
            this.items.add( item );
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
}
