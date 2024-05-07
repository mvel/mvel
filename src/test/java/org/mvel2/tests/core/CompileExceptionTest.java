package org.mvel2.tests.core;

import org.mvel2.CompileException;

public class CompileExceptionTest extends AbstractTest{
    public void testNoExceptionThrownWhenCreatingCompileExceptionWithCursorLessThanZero() {
        CompileException ex = new CompileException("Dummy message!", "Dummy expression!".toCharArray(), -1);
        try {
            ex.getMessage();
            ex.toString();
        } catch(StringIndexOutOfBoundsException oob) {
            fail("Should not throw exception even if cursor is less than 0!");
        }
    }
}
