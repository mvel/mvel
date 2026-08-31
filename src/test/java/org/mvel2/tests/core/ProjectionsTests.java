package org.mvel2.tests.core;

import org.mvel2.MVEL;
import org.mvel2.tests.core.res.Base;
import org.mvel2.util.Make;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ProjectionsTests extends AbstractTest {
  public void testProjectionSupport() {
    assertEquals(true, test("(name in things)contains'Bob'"));
  }

  public void testProjectionSupport1() {
    assertEquals(true, test("(name in things) contains 'Bob'"));
  }

  public void testProjectionSupport2() {
    String ex = "(name in things).size()";
    Map vars = createTestMap();

    assertEquals(3, MVEL.eval(ex, new Base(), vars));

    assertEquals(3, test("(name in things).size()"));
  }

  public void testProjectionSupport3() {
    String ex = "(toUpperCase() in ['bar', 'foo'])[1]";
    Map vars = createTestMap();

    assertEquals("FOO", MVEL.eval(ex, new Base(), vars));

    assertEquals("FOO", test("(toUpperCase() in ['bar', 'foo'])[1]"));
  }

  public void testProjectionSupport4() {
    Collection col = (Collection) test("(toUpperCase() in ['zero', 'zen', 'bar', 'foo'] if ($ == 'bar'))");
    assertEquals(1, col.size());
    assertEquals("BAR", col.iterator().next());
  }

  public void testProjectionSupport5() {
    Collection col = (Collection) test("(toUpperCase() in ['zero', 'zen', 'bar', 'foo'] if ($.startsWith('z')))");
    assertEquals(2, col.size());
    Iterator iter = col.iterator();
    assertEquals("ZERO", iter.next());
    assertEquals("ZEN", iter.next());
  }
  
  public void testProjectionSupport6() {
    assertEquals(true, test("( name in things ) contains 'Bob'"));
  }

  public void testProjectionStandaloneWithSpaces1() {
    assertProjectionContainsBob("( name in things )");
  }

  public void testProjectionStandaloneWithSpaces2() {
    assertProjectionContainsBob("(name in things)");
  }

  public void testProjectionStandaloneWithSpaces3() {
    assertProjectionContainsBob("( name in things)");
  }

  public void testProjectionStandaloneWithSpaces4() {
    assertProjectionContainsBob("(name in things )");
  }

  public void testProjectionStandaloneWithSpaces5() {
    assertProjectionContainsBob("( name in things ) ");
  }

  public void testProjectionStandaloneWithSpaces6() {
    assertProjectionContainsBob("( name in things )\n");
  }

  public void testProjectionMultipleSpaces() {
    assertProjectionContainsBob("(  name  in  things  )");
  }

  public void testProjectionWithTabs() {
    assertProjectionContainsBob("(\tname\tin\tthings\t)");
  }

  public void testProjectionIfWithTrailingSpace() {
    Collection col = (Collection) test("( toUpperCase() in ['zero', 'zen', 'bar', 'foo'] if ($ == 'bar') )");
    assertEquals(1, col.size());
    assertEquals("BAR", col.iterator().next());
  }

  public void testProjectionCollectionNameStartingWithIn() {
    Map vars = new HashMap();
    vars.put("innings", Arrays.asList(new HashMap() {{ put("name", "First"); }},
                                      new HashMap() {{ put("name", "Second"); }}));
    Collection col = (Collection) MVEL.eval("( name in innings )", vars);
    assertEquals(2, col.size());
    assertTrue(col.contains("First"));
  }

  public void testProjectionCollectionNameStartingWithIf() {
    Map vars = new HashMap();
    vars.put("iffy_list", Arrays.asList("zero", "zen", "bar"));
    Collection col = (Collection) MVEL.eval("( toUpperCase() in iffy_list if ($.startsWith('z')) )", vars);
    assertEquals(2, col.size());
    assertTrue(col.contains("ZERO"));
    assertTrue(col.contains("ZEN"));
  }

  private void assertProjectionContainsBob(String expr) {
    Collection col = (Collection) test(expr);
    assertEquals(3, col.size());
    assertTrue(col.contains("Bob"));
  }

//
//  public void testNestedProjection() {
//    Map vars = createTestVars();
//    assertEquals(
//        Arrays.asList("George", "Michael", "Laura"),
//        MVEL.eval("familyMembers = (name in (familyMembers in users));", vars));
//  }

  public void testConcatProjection() {
    Map vars = createTestVars();
    assertEquals(
        Arrays.asList("George", "Michael", "Laura"), 
        MVEL.eval(
            "def concat(lists) {res = []; foreach (list : lists) {res += list} res;} " +
            "familyMembers = (name in concat((familyMembers in users)));", vars));
  }
  
  private Map createTestVars() {
    Collection users = new ArrayList();
    User user1 = new User("John");
    user1.getFamilyMembers().add(new User("George"));
    User user2 = new User("Bob");
    User user3 = new User("Cindy");
    user3.getFamilyMembers().add(new User("Michael"));
    user3.getFamilyMembers().add(new User("Laura"));
    users.add(user1);
    users.add(user2);
    users.add(user3);
    
    Map vars = new HashMap();
    vars.put("users", users);
    return vars;
  }

  public static final class User {
    private String name;
    private Collection familyMembers = new ArrayList();
    
    public User(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Collection getFamilyMembers() {
      return familyMembers;
    }

    public void setFamilyMembers(Collection familyMembers) {
      this.familyMembers = familyMembers;
    }
  }

}
