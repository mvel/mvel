package org.mvel2.tests.core;

import junit.framework.TestCase;
import org.mvel2.MVEL;
import org.mvel2.PropertyAccessor;
import org.mvel2.asm.MethodVisitor;

import static org.mvel2.asm.Opcodes.*;

import org.mvel2.integration.*;
import org.mvel2.optimizers.OptimizerFactory;
import org.mvel2.optimizers.impl.asm.ProducesBytecode;
import org.mvel2.tests.core.res.Bar;
import org.mvel2.tests.core.res.Base;
import org.mvel2.tests.core.res.Foo;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PropertyHandlerTests extends TestCase {
  Base base = new Base();

  public class TestPropertyHandler implements PropertyHandler, ProducesBytecode {
    public Object getProperty(String name, Object contextObj,
                              VariableResolverFactory variableFactory) {
      assertNotNull(contextObj);
      assertEquals("0", name);
      assertTrue(contextObj instanceof List);
      return "gotcalled";
    }

    public Object setProperty(String name, Object contextObj,
                              VariableResolverFactory variableFactory, Object value) {
      assertNotNull(contextObj);
      assertEquals("0", name);
      assertTrue(contextObj instanceof List);
      ((List) contextObj).set(0, "set");
      return null;
    }

    public void produceBytecodeGet(MethodVisitor mv, String propertyName, VariableResolverFactory factory) {
      mv.visitLdcInsn("gotcalled");
    }

    public void produceBytecodePut(MethodVisitor mv, String propertyName, VariableResolverFactory factory) {
      mv.visitTypeInsn(CHECKCAST, "java/util/List");
      mv.visitInsn(ICONST_0);
      mv.visitLdcInsn("set");
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "set", "(ILjava/lang/Object;)Ljava/lang/Object;");
      mv.visitInsn(POP);
      mv.visitInsn(ACONST_NULL);
    }
  }

  @Override
  protected void setUp() throws Exception {
    PropertyHandlerFactory.registerPropertyHandler(List.class, new TestPropertyHandler());
  }

  @Override
  protected void tearDown() throws Exception {
    GlobalListenerFactory.disposeAll();
    PropertyHandlerFactory.disposeAll();
  }

  public void testListPropertyHandler() {
    MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;

    assertEquals("gotcalled", PropertyAccessor.get("list[0]", base));

    PropertyAccessor.set(base, "list[0]", "hey you");
    assertEquals("set", base.list.get(0));

    MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = false;
  }

  public void testListPropertyHandler2() {
    MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;

    Serializable s = MVEL.compileSetExpression("list[0]");

    Base b;
    MVEL.executeSetExpression(s, new Base(), "hey you");
    MVEL.executeSetExpression(s, b = new Base(), "hey you");

    assertEquals("set", b.list.get(0));
  }

  public void testListPropertyHandler3() {
    MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;

    OptimizerFactory.setDefaultOptimizer("ASM");

    Serializable s = MVEL.compileSetExpression("list[0]");

    Base b;
    MVEL.executeSetExpression(s, new Base(), "hey you");
    MVEL.executeSetExpression(s, b = new Base(), "hey you");

    assertEquals("set", b.list.get(0));
  }

  public void testListPropertyHandler4() {
    MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;

    OptimizerFactory.setDefaultOptimizer("ASM");

    final String[] res = new String[1];

    GlobalListenerFactory.registerGetListener(new Listener() {
      public void onEvent(Object context, String contextName, VariableResolverFactory variableFactory, Object value) {
        System.out.println("Listener Fired:" + contextName);
        res[0] = contextName;
      }
    });

    Serializable s = MVEL.compileSetExpression("list[0]");

    Base b;
    MVEL.executeSetExpression(s, new Base(), "hey you");
    res[0] = null;

    MVEL.executeSetExpression(s, b = new Base(), "hey you");

    assertEquals("set", b.list.get(0));
    assertEquals("list", res[0]);
  }

  public void testNullPropertyHandler() {
    MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;

    OptimizerFactory.setDefaultOptimizer("ASM");

    PropertyHandlerFactory.setNullPropertyHandler(new PropertyHandler() {
      public Object getProperty(String name, Object contextObj, VariableResolverFactory variableFactory) {
        return "NULL";
      }

      public Object setProperty(String name, Object contextObj, VariableResolverFactory variableFactory, Object value) {
        return "NULL";
      }
    });

    Foo foo = new Foo();
    Bar bar = foo.getBar();
    foo.setBar(null);

    Map map = new HashMap();
    map.put("foo", foo);

    Serializable s = MVEL.compileExpression("foo.bar");

    assertEquals("NULL", MVEL.executeExpression(s, map));
    assertEquals("NULL", MVEL.executeExpression(s, map));
    foo.setBar(bar);
    assertEquals(bar, MVEL.executeExpression(s, map));
  }

  public void testNullPropertyHandler2() {
    MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;

    OptimizerFactory.setDefaultOptimizer("reflective");

    PropertyHandlerFactory.setNullPropertyHandler(new PropertyHandler() {
      public Object getProperty(String name, Object contextObj, VariableResolverFactory variableFactory) {
        return "NULL";
      }

      public Object setProperty(String name, Object contextObj, VariableResolverFactory variableFactory, Object value) {
        return "NULL";
      }
    });

    Foo foo = new Foo();
    Bar bar = foo.getBar();
    foo.setBar(null);

    Map map = new HashMap();
    map.put("foo", foo);

    Serializable s = MVEL.compileExpression("foo.bar");

    assertEquals("NULL", MVEL.executeExpression(s, map));
    assertEquals("NULL", MVEL.executeExpression(s, map));
    foo.setBar(bar);
    assertEquals(bar, MVEL.executeExpression(s, map));
  }


  public void testMapPropertyHandler() {
    MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;

    PropertyHandlerFactory.registerPropertyHandler(Map.class, new
        PropertyHandler() {
          public Object getProperty(String name, Object contextObj,
                                    VariableResolverFactory variableFactory) {
            assertNotNull(contextObj);
            assertEquals("'key'", name);
            assertTrue(contextObj instanceof Map);
            return "gotcalled";
          }

          public Object setProperty(String name, Object contextObj,
                                    VariableResolverFactory variableFactory, Object value) {
            assertNotNull(contextObj);
            assertEquals("'key'", name);
            assertTrue(contextObj instanceof Map);
            ((Map) contextObj).put("key", "set");
            return null;
          }
        });

    assertEquals("gotcalled", PropertyAccessor.get("funMap['key']", base));

    PropertyAccessor.set(base, "funMap['key']", "hey you");
    assertEquals("set", base.funMap.get("key"));

    MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = false;
  }

  public void testArrayPropertyHandler() {

    MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;

    PropertyHandlerFactory.registerPropertyHandler(Array.class, new
        PropertyHandler() {
          public Object getProperty(String name, Object contextObj,
                                    VariableResolverFactory variableFactory) {
            assertNotNull(contextObj);
            assertEquals("0", name);
            assertTrue(contextObj.getClass().isArray());
            return "gotcalled";
          }

          public Object setProperty(String name, Object contextObj,
                                    VariableResolverFactory variableFactory, Object value) {
            assertNotNull(contextObj);
            assertEquals("0", name);
            assertTrue(contextObj.getClass().isArray());
            Array.set(contextObj, 0, "set");
            return null;
          }
        });

    assertEquals("gotcalled", PropertyAccessor.get("stringArray[0]", base));

    PropertyAccessor.set(base, "stringArray[0]", "hey you");
    assertEquals("set", base.stringArray[0]);

    MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = false;
  }

  public void testSetListListener() {
    MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;

    class MyListener implements Listener {
      public int counter;

      public void onEvent(Object context, String contextName,
                          VariableResolverFactory variableFactory, Object value) {
        counter++;
      }
    }

    class MyBean {
      private List someList;

      public List getSomeList() {
        return someList;
      }
    }

    MyListener listener = new MyListener();
    GlobalListenerFactory.registerGetListener(listener);
    MVEL.getProperty("someList", new MyBean());
    MVEL.getProperty("someList", new MyBean());
    assertEquals(2, listener.counter);
  }

  public void _testListener() {
    MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;
    class MyListener implements Listener {
      public int count;

      public void onEvent(Object context, String contextName,
                          VariableResolverFactory variableFactory, Object value) {
        count++;
      }
    }

    MyListener listener = new MyListener();
    GlobalListenerFactory.registerGetListener(listener);

    PropertyHandlerFactory.setNullPropertyHandler(new PropertyHandler() {
      public Object getProperty(String name, Object contextObj,
                                VariableResolverFactory variableFactory) {
        List someList = new ArrayList();
        someList.add(new Foo());
        return someList;
      }

      public Object setProperty(String name, Object contextObj,
                                VariableResolverFactory variableFactory, Object value) {
        return null;
      }
    });

    PropertyHandlerFactory.registerPropertyHandler(List.class, new
        PropertyHandler() {
          public Object getProperty(String name, Object contextObj,
                                    VariableResolverFactory variableFactory) {
            List list = (List) contextObj;
            int index = Integer.valueOf(name);
            while (index >= list.size()) {
              list.add(new Foo());
            }

            return list.get(index);
          }

          public Object setProperty(String name, Object contextObj,
                                    VariableResolverFactory variableFactory, Object value) {
            return null;
          }
        });

    Foo foo = new Foo();

    final Serializable fooExpr0 =
        MVEL.compileSetExpression("collectionTest[0].name");
    final Serializable fooExpr1 =
        MVEL.compileSetExpression("collectionTest[1].name");
    MVEL.executeSetExpression(fooExpr0, foo, "John Galt");
    MVEL.executeSetExpression(fooExpr1, foo, "The Joker");
    assertEquals(2, listener.count);
    MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = false;
  }

  public class WorkObject {
    Map<String, Object> map = new HashMap<String, Object>();

    public boolean hasFieldName(String name) {
      return map.containsKey(name);
    }

    public Object getFieldValue(String name) {
      return map.get(name);
    }

    public void setFieldValue(String name, Object value, boolean bool) {
      map.put(name, value);
    }
  }

  public class WebPropertyHandler implements PropertyHandler {

    public Object getProperty(String arg0, Object arg1,
                              VariableResolverFactory arg2) {
      WorkObject wob = (WorkObject) arg1;
      if (wob.hasFieldName(arg0)) {
        return wob.getFieldValue(arg0);
      }
      else
        return null;
    }

    public Object setProperty(String arg0, Object arg1,
                              VariableResolverFactory arg2, Object arg3) {
      WorkObject wob = (WorkObject) arg1;
      wob.setFieldValue(arg0, arg3, true);
      return arg3;
    }
  }

  public void testPropertyHandlerSetting() {
    MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;
    PropertyHandlerFactory.registerPropertyHandler(WorkObject.class, new WebPropertyHandler());

    Map vars = new HashMap();
    WorkObject wo = new WorkObject();
    vars.put("wobj", wo);

    MVEL.setProperty(vars, "wobj.foo", "foobie");

    assertEquals("foobie", wo.getFieldValue("foo"));
  }

  public class A {}

  public class B extends A implements Cloneable {
  }

  public class C extends A implements Cloneable {
    public String prop = "Property";
  }

  public void testPropertyHandlerPreCompile() {
    MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;
    PropertyHandlerFactory.registerPropertyHandler(B.class, new PropertyHandler() {
      @Override
      public Object getProperty(String name, Object contextObj, VariableResolverFactory variableFactory) {
        return "Handled property";
      }
      @Override
      public Object setProperty(String name, Object contextObj, VariableResolverFactory variableFactory,
          Object value) {
        return null;
      }
    });

    Serializable compiled = MVEL.compileExpression("prop");
    assertEquals("Property", MVEL.executeExpression(compiled, new C()));
  }
}