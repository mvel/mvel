package org.mvel.optimizers;

import org.mvel.AbstractParser;

/**
 * @author Christopher Brock
 */
public class AbstractOptimizer extends AbstractParser {
    protected static final int BEAN = 0;
    protected static final int METH = 1;
    protected static final int COL = 2;

    protected int start = 0;
  
    protected Object tryStaticAccess() {    
        try {
            /**
             * Try to resolve this *smartly* as a static class reference.
             *
             * This starts at the end of the token and starts to step backwards to figure out whether
             * or not this may be a static class reference.  We search for method calls simply by
             * inspecting for ()'s.  The first union area we come to where no brackets are present is our
             * test-point for a class reference.  If we find a class, we pass the reference to the
             * property accessor along  with trailing methods (if any).
             *
             */
            boolean meth = false;
            int depth = 0;
            int last = length;
            for (int i = length - 1; i > 0; i--) {
                switch (expr[i]) {
                    case'.':
                        if (!meth) {
                            try {
                                cursor = last;
                                return Class.forName(new String(expr, 0, last));
                            }
                            catch (ClassNotFoundException e) {
                                // return a field instead

                                return Class.forName(new String(expr, 0, i))
                                        .getField(new String(expr, i + 1, expr.length - i - 1));
                            }
                        }

                        meth = false;
                        last = i;
                        break;
                    case')':
                        if (depth++ == 0)
                            meth = true;
                        break;
                    case'(':
                        depth--;
                        break;
                }
            }
        }
        catch (Exception cnfe) {
            // do nothing.
        }

        return null;
    }

    protected int nextSubToken() {
        switch (expr[start = cursor]) {
            case'[':
                return COL;
            case'.':
                cursor = ++start;
        }

        //noinspection StatementWithEmptyBody
        while (++cursor < length && Character.isJavaIdentifierPart(expr[cursor])) ;


        if (cursor < length) {
            switch (expr[cursor]) {
                case'[':
                    return COL;
                case'(':
                    return METH;
                default:
                    return 0;
            }
        }
        return 0;
    }

    protected String capture() {
        return new String(expr, start, cursor - start);
    }

    protected void whiteSpaceSkip() {
        if (cursor < length)
            //noinspection StatementWithEmptyBody
            while (Character.isWhitespace(expr[cursor]) && ++cursor < length) ;
    }

    protected boolean scanTo(char c) {
        for (; cursor < length; cursor++) {
            if (expr[cursor] == c) {
                return true;
            }
        }
        return false;
    }

    protected int containsStringLiteralTermination() {
        int pos = cursor;
        for (pos--; pos > 0; pos--) {
            if (expr[pos] == '\'' || expr[pos] == '"') return pos;
            else if (!Character.isWhitespace(expr[pos])) return pos;
        }
        return -1;
    }
}
