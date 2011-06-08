package org.mvel2.tests.perftests;

import org.mvel2.MVEL;
import org.mvel2.compiler.ExecutableAccessor;
import org.mvel2.util.FastList;

import java.util.List;


public class InlineCollectionsPerformance {
  private static final int COUNT = 200000;


  public static void main(String[] args) {
    long time;
    for (int i = 0; i < 5; i++) {
//            time = System.currentTimeMillis();
//            testJavaList();
//            System.out.println("Java    : " + (System.currentTimeMillis() - time));
//

      time = System.currentTimeMillis();
      testOGNLList();
      System.out.println("OGNL2.7 : " + (System.currentTimeMillis() - time));

      time = System.currentTimeMillis();
      testMVELList();
      System.out.println("MVEL    : " + (System.currentTimeMillis() - time));
      System.out.println();
    }

  }

//    public static void testMVELList() {
//
//        ExecutableAccessor s = (ExecutableAccessor) MVEL.compileExpression("['Foo244':'Bar','Foo244':'Bar','Foo244':'Bar','Foo244':'Bar','Foo244':'Bar']");
//        Map list;
//        s.getNode().getReducedValueAccelerated(null, null, null);
//        for (int i = 0; i < COUNT; i++) {
//            list = (Map) s.getNode().getAccessor().getValue(null, null, null);
//
//        }
//    }

//    public static void testMVELList() {
//
//        ExecutableAccessor s = (ExecutableAccessor) MVEL.compileExpression("{'Foo244','Bar','Foo244','Bar','Foo244','Bar','Foo244','Bar','Foo244','Bar'}");
//        Object[] list;
//        s.getNode().getReducedValueAccelerated(null, null, null);
//        for (int i = 0; i < COUNT; i++) {
//            list = (Object[]) s.getNode().getAccessor().getValue(null, null, null);
//
//            //     assert "Foo244".equals(list.get(0)) && "Foo244".equals(list.get(2)) && list.size() == 10;
//        }
//    }

  public static void testMVELList() {

    ExecutableAccessor s = (ExecutableAccessor) MVEL.compileExpression("['Foo244','Bar','Foo244','Bar','Foo244','Bar','Foo244','Bar','Foo244','Bar']");
    List list;
    s.getNode().getReducedValueAccelerated(null, null, null);
    for (int i = 0; i < COUNT; i++) {
      list = (List) s.getNode().getAccessor().getValue(null, null, null);

      assert "Foo244".equals(list.get(0)) && "Foo244".equals(list.get(2)) && list.size() == 10;
    }
  }

  public static void testOGNLList() {

//        OgnlContext context = (OgnlContext) Ognl.createDefaultContext(null);
//        Node node;
//        try {
//            node = Ognl.compileExpression(context, null, "{'Foo244','Bar','Foo244','Bar','Foo244','Bar','Foo244','Bar','Foo244','Bar'}");
//        }
//        catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//        List list;
//        for (int i = 0; i < COUNT; i++) {
//            list = (List) node.getAccessor().get(null, null);
//
//            assert "Foo244".equals(list.get(0)) && "Foo244".equals(list.get(2)) && list.size() == 10;
//        }
  }

  public static void testJavaList() {
    FastList list;
    for (int i = 0; i < COUNT; i++) {
      list = new FastList(10);

      list.add("Foo244");
      list.add("Bar");

      list.add("Foo244");
      list.add("Bar");

      list.add("Foo244");
      list.add("Bar");

      list.add("Foo244");
      list.add("Bar");

      list.add("Foo244");
      list.add("Bar");

      assert "Foo244".equals(list.get(0)) && "Bar".equals(list.get(1)) && list.size() == 10;
    }

  }
}
