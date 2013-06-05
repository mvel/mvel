package org.mvel2.tests.classes;

import junit.framework.TestCase;
import org.mvel2.MVEL;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * @author Mike Brock
 */
public class ClassTests extends TestCase {
  private final String dir = "src/test/java/" + getClass().getPackage().getName().replaceAll("\\.", "/");

  public void testScript() throws IOException {
    final Object o = MVEL.evalFile(new File(dir + "/demo.mvel"), new HashMap<String, Object>());
  }

}
