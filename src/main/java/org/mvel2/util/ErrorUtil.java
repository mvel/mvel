package org.mvel2.util;

import org.mvel2.CompileException;
import org.mvel2.ErrorDetail;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mike Brock .
 */
public class ErrorUtil {
  private static final Logger LOG = Logger.getLogger(ErrorUtil.class.getName());

  public static CompileException rewriteIfNeeded(CompileException caught, char[] outer, int outerCursor) {
    if (caught.getExpr() == null) {
        return caught;
    }
    if (outer != caught.getExpr()) {
      if (caught.getExpr().length <= caught.getCursor()) {
        caught.setCursor(caught.getExpr().length - 1);
      }

      try {
      String innerExpr = new String(caught.getExpr()).substring(caught.getCursor());
      caught.setExpr(outer);

      String outerStr = new String(outer);

      int newCursor = outerStr.substring(outerStr.indexOf(new String(caught.getExpr())))
          .indexOf(innerExpr);

      caught.setCursor(newCursor);
      }
      catch (Throwable t) {
        LOG.log(Level.WARNING, "", t);
      }
    }
    return caught;
  }

  public static ErrorDetail rewriteIfNeeded(ErrorDetail detail, char[] outer, int outerCursor) {
    if (outer != detail.getExpr()) {
      String innerExpr = new String(detail.getExpr()).substring(detail.getCursor());
      detail.setExpr(outer);

      int newCursor = outerCursor;
      newCursor += new String(outer).substring(outerCursor).indexOf(innerExpr);

      detail.setCursor(newCursor);
    }
    return detail;
  }
}
