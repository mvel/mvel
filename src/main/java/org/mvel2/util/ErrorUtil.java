package org.mvel2.util;

import org.mvel2.CompileException;

/**
 * @author Mike Brock .
 */
public class ErrorUtil {
    public static CompileException rewriteIfNeeded(CompileException caught, char[] outer, int outerCursor) {
        if (outer != caught.getExpr()) {
            String innerExpr = new String(caught.getExpr()).substring(caught.getCursor());
            caught.setExpr(outer);

            int newCursor = outerCursor;
            newCursor += new String(outer).substring(outerCursor).indexOf(innerExpr);

            caught.setCursor(newCursor);
        }
        return caught;
    }
}
