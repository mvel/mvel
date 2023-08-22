package org.mvel2.tests.core;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.mvel2.integration.impl.ImmutableDefaultFactory;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;
import org.mvel2.tests.BaseMvelTestCase;
import org.mvel2.util.StringAppender;

/**
 * @author PÉRIÉ Fabien
 */
public class MVELIncludeTest extends BaseMvelTestCase {

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

  /**
   * evalFile with sub template in utf-8 encoding
   *
   * @throws IOException
   */
  public void testIncludeFileWithNewline() throws IOException {
	    final CompiledTemplate template = TemplateCompiler.compileTemplate(new File("samples/scripts/lineTopLevel.mvel"));
	    
	    String tr = (String) new TemplateRuntime(template.getTemplate(), null, template.getRoot(), "./samples/scripts/")
	    	.execute(new StringAppender(), new HashMap<String, String>(), new ImmutableDefaultFactory());
	    tr = tr.replaceAll("\\r", ""); // Make it consistent across OS's
	    assertEquals("The question is 21 + 21\nThe answer is 42", tr);
  }
}
