package org.mvel2.tests.templates;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;

public class TemplateNestedIncludeCurrentDirBugTest {

    @Test
    public void testBug(){
        
    //    InputStream input = TemplateCurrentDirBugTest.class.getResourceAsStream("/templates/templateA.tpl");
       File input = new File("src/test/resources/templates/templateA.tpl");
        CompiledTemplate compiled = TemplateCompiler.compileTemplate(input);
        
        Map vars = new HashMap();
        Object output = TemplateRuntime.execute(compiled.getRoot(), compiled.getTemplate(), new StringBuilder(), null, new MapVariableResolverFactory(vars), null,input.getParent());
        System.out.println(output);
    }
}
