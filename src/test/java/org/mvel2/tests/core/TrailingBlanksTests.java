package org.mvel2.tests.core;

import org.mvel2.MVEL;

import java.io.Serializable;

public class TrailingBlanksTests extends AbstractTest{


    public void testWithTrailingNewLine() {

        String expression = "if (true) {'ok';} else if (false) {'ko';} else {'ko';}\n";
        Serializable serializable = MVEL.compileExpression(expression);
        Object result = MVEL.executeExpression(serializable);

        assertEquals("ok", result);

    }


    public void testWithTrailingBlank() {

        String expression = "if (true) {'ok';} else if (false) {'ko';} else {'ko';} ";
        Serializable serializable = MVEL.compileExpression(expression);
        Object result = MVEL.executeExpression(serializable);

        assertEquals("ok", result);

    }

    public void testWithoutTrailingBlank() {

        String expression = "if (true) {'ok';} else if (false) {'ko';} else {'ko';}";
        Serializable serializable = MVEL.compileExpression(expression);
        Object result = MVEL.executeExpression(serializable);

        assertEquals("ok", result);

    }
}
