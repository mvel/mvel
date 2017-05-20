package org.mvel2.tests.core;

import junit.framework.TestCase;
import org.mvel2.integration.impl.ImmutableDefaultFactory;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;
import org.mvel2.util.StringAppender;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * @author PÉRIÉ Fabien
 */
public class MVELIncludeTest extends TestCase {

  private File file;

  public void setUp() {
    file = new File("samples/scripts/accentTopLevel.mvel");
  }

  /**
   * evalFile with sub template in utf-8 encoding
   *
   * @throws IOException
   */
  public void testEvalFile1() throws IOException {
    final CompiledTemplate template = TemplateCompiler.compileTemplate(file);
    
    final String tr = (String) new TemplateRuntime(template.getTemplate(), null, template.getRoot(), "./samples/scripts/")
    	.execute(new StringAppender(), new HashMap<String, String>(), new ImmutableDefaultFactory());
    assertEquals("Hello mister Gaël Périé", tr);
  }

}
