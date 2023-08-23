package org.mvel2.tests.classes;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.mvel2.MVEL;
import org.mvel2.tests.BaseMvelTestCase;

/**
 * @author Mike Brock
 */
public class ClassTests extends BaseMvelTestCase {
  private final String dir = "src/test/java/" + getClass().getPackage().getName().replaceAll("\\.", "/");

  public void testScript() throws IOException {
    final Object o = MVEL.evalFile(new File(dir + "/demo.mvel"), new HashMap<String, Object>());
  }

}
