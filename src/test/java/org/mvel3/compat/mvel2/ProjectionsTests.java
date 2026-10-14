package org.mvel3.compat.mvel2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mvel3.compat.mvel2.res.Base;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.eval;

/**
 * Imported from org.mvel2.tests.core.ProjectionsTests (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged. Failures are intentionally visible; known behavioural differences
 * are annotated with {@link KnownCompatibility}.
 */
@Tag("mvel2-compatibility")
public class ProjectionsTests extends AbstractTest {
  // Category not isolated: fails at the first parse error, `in` of the projection syntax. The expression also
  // uses single-quoted strings (single-quote-string) and the infix `contains` operator (contains-operator); neither was tested separately.
  @Test
  @KnownCompatibility("projection-operator")
  public void testProjectionSupport() {
    assertEquals(true, test("(name in things)contains'Bob'"));
  }

  // Category not isolated: fails at the first parse error, `in` of the projection syntax. The expression also
  // uses single-quoted strings (single-quote-string) and the infix `contains` operator (contains-operator); neither was tested separately.
  @Test
  @KnownCompatibility("projection-operator")
  public void testProjectionSupport1() {
    assertEquals(true, test("(name in things) contains 'Bob'"));
  }

  @Test
  @KnownCompatibility("projection-operator")
  public void testProjectionSupport2() {
    String ex = "(name in things).size()";
    Map vars = createTestMap();

    assertEquals(3, eval(ex, new Base(), vars));

    assertEquals(3, test("(name in things).size()"));
  }

  // Category not isolated: fails at the first parse error, `in` of the projection syntax. The expression also
  // uses single-quoted strings (single-quote-string); neither was tested separately.
  @Test
  @KnownCompatibility("projection-operator")
  public void testProjectionSupport3() {
    String ex = "(toUpperCase() in ['bar', 'foo'])[1]";
    Map vars = createTestMap();

    assertEquals("FOO", eval(ex, new Base(), vars));

    assertEquals("FOO", test("(toUpperCase() in ['bar', 'foo'])[1]"));
  }

  // Category not isolated: fails at the first parse error, `in` of the projection syntax. The expression also
  // uses single-quoted strings (single-quote-string); neither was tested separately.
  @Test
  @KnownCompatibility("projection-operator")
  public void testProjectionSupport4() {
    Collection col = (Collection) test("(toUpperCase() in ['zero', 'zen', 'bar', 'foo'] if ($ == 'bar'))");
    assertEquals(1, col.size());
    assertEquals("BAR", col.iterator().next());
  }

  // Category not isolated: fails at the first parse error, `in` of the projection syntax. The expression also
  // uses single-quoted strings (single-quote-string); neither was tested separately.
  @Test
  @KnownCompatibility("projection-operator")
  public void testProjectionSupport5() {
    Collection col = (Collection) test("(toUpperCase() in ['zero', 'zen', 'bar', 'foo'] if ($.startsWith('z')))");
    assertEquals(2, col.size());
    Iterator iter = col.iterator();
    assertEquals("ZERO", iter.next());
    assertEquals("ZEN", iter.next());
  }

  // Category not isolated: fails at the first parse error, `in` of the projection syntax. The expression also
  // uses single-quoted strings (single-quote-string) and the infix `contains` operator (contains-operator); neither was tested separately.
  @Test
  @KnownCompatibility("projection-operator")
  public void testProjectionSupport6() {
    assertEquals(true, test("( name in things ) contains 'Bob'"));
  }

  @Test
  @KnownCompatibility("projection-operator")
  public void testProjectionStandaloneWithSpaces1() {
    assertProjectionContainsBob("( name in things )");
  }

  @Test
  @KnownCompatibility("projection-operator")
  public void testProjectionStandaloneWithSpaces2() {
    assertProjectionContainsBob("(name in things)");
  }

  @Test
  @KnownCompatibility("projection-operator")
  public void testProjectionStandaloneWithSpaces3() {
    assertProjectionContainsBob("( name in things)");
  }

  @Test
  @KnownCompatibility("projection-operator")
  public void testProjectionStandaloneWithSpaces4() {
    assertProjectionContainsBob("(name in things )");
  }

  @Test
  @KnownCompatibility("projection-operator")
  public void testProjectionStandaloneWithSpaces5() {
    assertProjectionContainsBob("( name in things ) ");
  }

  @Test
  @KnownCompatibility("projection-operator")
  public void testProjectionStandaloneWithSpaces6() {
    assertProjectionContainsBob("( name in things )\n");
  }

  @Test
  @KnownCompatibility("projection-operator")
  public void testProjectionMultipleSpaces() {
    assertProjectionContainsBob("(  name  in  things  )");
  }

  @Test
  @KnownCompatibility("projection-operator")
  public void testProjectionWithTabs() {
    assertProjectionContainsBob("(\tname\tin\tthings\t)");
  }

  // Category not isolated: fails at the first parse error, `in` of the projection syntax. The expression also
  // uses single-quoted strings (single-quote-string); neither was tested separately.
  @Test
  @KnownCompatibility("projection-operator")
  public void testProjectionIfWithTrailingSpace() {
    Collection col = (Collection) test("( toUpperCase() in ['zero', 'zen', 'bar', 'foo'] if ($ == 'bar') )");
    assertEquals(1, col.size());
    assertEquals("BAR", col.iterator().next());
  }

  @Test
  @KnownCompatibility("projection-operator")
  public void testProjectionCollectionNameStartingWithIn() {
    Map vars = new HashMap();
    vars.put("innings", Arrays.asList(new HashMap() {{ put("name", "First"); }},
                                      new HashMap() {{ put("name", "Second"); }}));
    Collection col = (Collection) eval("( name in innings )", vars);
    assertEquals(2, col.size());
    assertTrue(col.contains("First"));
  }

  // Category not isolated: fails at the first parse error, `in` of the projection syntax. The expression also
  // uses single-quoted strings (single-quote-string) and the infix `contains` operator (contains-operator); neither was tested separately.
  @Test
  @KnownCompatibility("projection-operator")
  public void testProjectionCollectionNameStartingWithIf() {
    Map vars = new HashMap();
    vars.put("iffy_list", Arrays.asList("zero", "zen", "bar"));
    Collection col = (Collection) eval("( toUpperCase() in iffy_list if ($.startsWith('z')) )", vars);
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

  // Category not isolated: fails with "A reference was unexpectedly null" (see javaparser-null-node). The
  // expression combines the projection syntax, a `def` function and a MVEL2 `foreach` statement; which one
  // triggers the error was not verified.
  @Test
  @KnownCompatibility("projection-operator")
  public void testConcatProjection() {
    Map vars = createTestVars();
    assertEquals(
        Arrays.asList("George", "Michael", "Laura"), 
        eval(
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
