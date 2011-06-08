package org.mvel2.tests.core;

import junit.framework.TestCase;
import org.mvel2.MVEL;
import org.mvel2.util.FastList;
import org.mvel2.util.StringAppender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UtilsTests extends TestCase {

  public void testMain() {
    assertEquals("foobarfoobar", new StringAppender().append("foo").append('b').append('a').append('r').append("foobar").toString());
  }

  public void testMain2() {
    assertEquals("foo bar test 1 2 3foo bar test 1 2 3",
        new StringAppender(0).append("foo bar ").append("test").append(" 1 2 3")
            .append("foo bar").append(" ").append("test").append(" 1 2 3").toString());
  }

  public void testMain3() {
    assertEquals("C:/projects/webcat/exploded/resources/productimages/",
        new StringAppender(10).append("C:/projects/webcat/exploded/")
            .append("resources/productimages/").toString());
  }

  public void testFastList1() {
    FastList list = new FastList(3);
    list.add("One");
    list.add("Two");
    list.add("Three");
    list.add("Five");

    list.add(1, "Four");

    String[] zz1 = {"One", "Four", "Two", "Three", "Five"};
    int i = 0;
    for (Object o : list) {
      if (!zz1[i++].equals(o)) throw new AssertionError("problem with list!");
    }

    list.remove(2);

    String[] zz2 = {"One", "Four", "Three", "Five"};
    i = 0;
    for (Object o : list) {
      if (!zz2[i++].equals(o)) throw new AssertionError("problem with list!");
    }
  }

  public void testAddToFastList() throws Exception {
    FastList fl = new FastList(0);
    assertEquals(0, fl.size());

    // this throws an ArrayIndexOutOfBoundsException:0
    fl.add("value");
    assertEquals(1, fl.size());
  }

  public void testAddAllFastList() throws Exception {
    FastList fl1 = new FastList(1);
    fl1.add("value1");
    fl1.add("value2");
    assertEquals(2, fl1.size());

    FastList fl2 = new FastList(1);
    fl2.add("value3");
    fl2.add("value4");

    // the addAll results in a list of 2 instead of 4 that was expected
    fl1.addAll(fl2);

    assertEquals(4, fl1.size());
  }

  public void testAddAllFastList2() throws Exception {
    FastList<String> fl1 = new FastList<String>();
    fl1.add("value1");
    fl1.add("value2");

    FastList<String> fl2 = new FastList<String>();
    fl2.add("value3");
    fl2.add("value4");

    fl1.addAll(fl2);

    assertEquals("value1", fl1.get(0));
    assertEquals("value2", fl1.get(1));
    assertEquals("value3", fl1.get(2)); // this results in null
    assertEquals("value4", fl1.get(3)); // this results in null
  }

  public void testAddAll2() {
    FastList<String> flSource = new FastList<String>();
    flSource.add("value");

    FastList<String> flDest = new FastList<String>(flSource.size());
    flDest.addAll(flSource); // throws ArrayIndexOutOfBoundsException: 2
    assertEquals("value", flDest.get(0));
  }

  public void testFastListEval() throws Exception {
    Map<String, Object> map = new HashMap<String, Object>();

    // The following throws a PropertyAccessException:
    // unable to resolve property: could not access property
    MVEL.eval("list = []; list.add('value')", map);

    assertEquals(1, ((List) map.get("list")).size());
  }

}
