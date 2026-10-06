package org.mvel3.compat.mvel2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import org.mvel3.compat.mvel2.res.Base;
import org.mvel3.compat.mvel2.res.DerivedClass;
import org.mvel3.compat.mvel2.res.Foo;
import org.mvel3.compat.mvel2.res.TestInterface;

import static java.lang.System.currentTimeMillis;

/** Fixtures and assertions imported from the same mvel2 commit as ArithmeticTests. */
public abstract class AbstractTest extends Assert {

  protected static Map createTestMap() {
    Map map = new HashMap();
    Base base = new Base();
    map.put("this", base);
    map.put("fun", base.fun);
    map.put("sentence", base.sentence);
    map.put("list", base.list);
    map.put("things", base.things);
    map.put("funMap", base.funMap);
    map.put("fooMap", base.fooMap);
    map.put("data", base.data);

    map.put("foo", new Foo());
    map.put("a", null);
    map.put("b", null);
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


  protected Object test(final String expression) {
    // MVEL2's optimizer/thread/serialization matrix is not a MVEL3 execution mode.
    return Mvel3TestSupport.eval(expression, createTestMap());
  }

  protected Object _test(final String expression) {
    return test(expression);
  }

  protected static Object testCompiledSimple(final String expression, final Map variables) {
    final Serializable compiled = Mvel3TestSupport.compileExpression(expression);
    return Mvel3TestSupport.executeExpression(compiled, variables);
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
      obj = convertNumber(obj, obj2.getClass());

      if (!obj.equals(obj2)) {
        if (permitRoundingVariance) {
          obj = convertNumber(obj, Integer.class);
          obj2 = convertNumber(obj2, Integer.class);

          assertEquals(obj, obj2);
        }
        else {
          throw new AssertionFailedError("expected <" + String.valueOf(obj) + "> but was <" + String.valueOf(obj) + ">");
        }
      }

    }
  }

  // Preserve the Integer/Double conversions used by this suite's assertNumEquals calls.
  private static Number convertNumber(final Object value, final Class<?> targetType) {
    if (targetType.isInstance(value)) {
      return (Number) value;
    }
    if (value instanceof Double number && targetType == Integer.class) {
      if (number > Integer.MAX_VALUE) {
        throw new IllegalArgumentException("Double exceeds the maximum precision of Integer: " + number);
      }
      return number.intValue();
    }
    if (value instanceof Integer number && targetType == Double.class) {
      return number.doubleValue();
    }
    throw new IllegalArgumentException("Unsupported numeric comparison conversion: "
        + value.getClass().getName() + " to " + targetType.getName());
  }

  public static void assertEqualsByComparingTo(Object expected, Object actual) {
    if (expected == null || actual == null) {
      throw new AssertionError("null value");
    }
    if (!(expected instanceof Comparable && actual instanceof Comparable)) {
      throw new AssertionError("values not comparable: " +
                                       expected.getClass().getName() + " and " + actual.getClass().getName());
    }

    int compare = ((Comparable) expected).compareTo(actual);
    if (compare != 0) {
      fail("expected <" + String.valueOf(expected) + "> but was <" + String.valueOf(actual) + ">");
    }
  }
}
