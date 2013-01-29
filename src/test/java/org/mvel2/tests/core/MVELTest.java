package org.mvel2.tests.core;

import junit.framework.TestCase;
import org.mvel2.MVEL;
import org.mvel2.util.StringAppender;

import java.io.File;
import java.io.IOException;

/**
 * @author yone098
 */
public class MVELTest extends TestCase {

  private File file;

  public void setUp() {
    file = new File("samples/scripts/multibyte.mvel");
  }

  /**
   * evalFile with encoding(workspace encoding utf-8)
   *
   * @throws IOException
   */
  public void testEvalFile1() throws IOException {
    Object obj = MVEL.evalFile(file, "UTF-8");
    assertEquals("?????", obj);

    // use default encoding
    obj = MVEL.evalFile(file);
    assertEquals("?????", obj);
  }

}
