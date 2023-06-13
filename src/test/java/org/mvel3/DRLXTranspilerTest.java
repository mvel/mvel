package org.mvel3;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.Test;
import org.mvel2.tests.core.AbstractTest;
import org.mvel3.parser.MvelParser;
import org.mvel3.transpiler.DRLXTranspiler;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_15;

public class DRLXTranspilerTest {

    @Test
    public void test1() {
        TypeSolver typeSolver = new ReflectionTypeSolver(false);
        JavaSymbolSolver   solver     = new JavaSymbolSolver(typeSolver);

        ParserConfiguration conf = new ParserConfiguration();
        conf.setLanguageLevel(JAVA_15);
        conf.setSymbolResolver(solver);

        MvelParser parser = new MvelParser(conf);

        String str = "public class X {\n" +
                     "    rule fooBar {\n" +
//                     "    or (\n" +
//                     "       Blah blah = /a/b/c,\n" +
//                     "       Flah flah = /a/b/c,\n" +
//                     "       and (\n" +
//                     "          Blah blah = /a/b/c,\n" +
//                     "          Flah flah = /a/b/c,\n" +
//                     "       ),\n" +
//                     "    )," +
//                     "    do {\n" +
//                     "        System.out.println(\"more hello\");\n" +
//                     "    }" +
                     "    }\n" +
                     "}";

        new DRLXTranspiler().transpileClass(str, parser);
        //eval(str);
    }
}
