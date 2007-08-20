package org.mvel.ast;

import org.mvel.*;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.util.ThisLiteral;

/**
 * @author Christopher Brock
 */
public class PropertyASTNode extends ASTNode {
    private ASTNode wrappedNode;

    public PropertyASTNode(char[] expr, int start, int end, int fields) {
        super(expr, start, end, fields);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        try {
            return wrappedNode.getReducedValueAccelerated(ctx, thisValue, factory);
        }
        catch (NullPointerException e) {
            if (wrappedNode == null) {
                return initializePropertyNode(ctx, thisValue, factory);
            }
            else {
                throw e;
            }
        }
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return super.getReducedValue(ctx, thisValue, factory);
    }

    private Object initializePropertyNode(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if ((fields & STR_LITERAL) != 0) {
            wrappedNode = new LiteralNode(new String(name));
            return wrappedNode.getReducedValueAccelerated(ctx, thisValue, factory);
        }
        else if ((fields & LITERAL) != 0) {
            if ((fields & THISREF) != 0) {
                wrappedNode = new ThisValNode(name, fields);
                return wrappedNode.getReducedValueAccelerated(ctx, thisValue, factory);
            }
            else {
                wrappedNode = new LiteralNode(literal);
                return wrappedNode.getReducedValueAccelerated(ctx, thisValue, factory);
            }
        }
        else if ((fields & DEEP_PROPERTY) != 0) {
            /**
             * The token is a DEEP PROPERTY (meaning it contains unions) in which case we need to traverse an object
             * graph.
             */

            String s;
            if (AbstractParser.LITERALS.containsKey(s = getAbsoluteRootElement())) {
                /**
                 * The root of the DEEP PROPERTY is a literal.
                 */
                Object literal = AbstractParser.LITERALS.get(s);
                if (literal == ThisLiteral.class) {
                    wrappedNode = new ThisValDeepPropertyNode(name, fields);
                    return wrappedNode.getReducedValueAccelerated(ctx, thisValue, factory);
                }
                else {
                    wrappedNode = new LiteralDeepPropertyNode(name, fields, literal);
                    return wrappedNode.getReducedValueAccelerated(ctx, thisValue, factory);
                }
            }
            else if (factory != null && factory.isResolveable(s)) {
                /**
                 * The root of the DEEP PROPERTY is a local or global var.
                 */

                wrappedNode = new VariableDeepPropertyNode(name, fields);
                return wrappedNode.getReducedValueAccelerated(ctx, thisValue, factory);
            }
            else {

                if (ctx != null) {
                    try {
                        wrappedNode = new ContextDeepPropertyNode(name, fields);
                        return wrappedNode.getReducedValueAccelerated(ctx, thisValue, factory);

                    }
                    catch (PropertyAccessException e) {
                        /**
                         * No luck. Make a last-ditch effort to resolve this as a static-class reference.
                         */
                    }
                }

                Object sa = tryStaticAccess(ctx, factory);
                if (sa == null) throw new PropertyAccessException("unable to resolve token: " + new String(name));

                wrappedNode = new LiteralNode(sa);
                return wrappedNode.getReducedValueAccelerated(ctx, thisValue, factory);
            }
        }
        else {
            String s;
            if (factory != null && factory.isResolveable(s = getAbsoluteName())) {
                /**
                 * The token is a local or global var.
                 */
                if (s.length() < name.length) {
                    /**
                     * This is probably an indexed property.
                     */

                    wrappedNode = new VariableDeepPropertyNode(name, fields);

                }
                else {
                    wrappedNode = new VarPropertyNode(name, fields, s);
                }

                return wrappedNode.getReducedValueAccelerated(ctx, thisValue, factory);
            }
            else if (ctx != null) {
                /**
                 * Check to see if the var exists in the VROOT.
                 */

                wrappedNode = new ContextDeepPropertyNode(name, fields);

                try {
                    return wrappedNode.getReducedValueAccelerated(ctx, thisValue, factory);
                }
                catch (RuntimeException e) {
                    e.printStackTrace();
                    throw new UnresolveablePropertyException(this);
                }
            }
            else {
                if (isOperator()) {
                    throw new CompileException("incomplete statement");
                }


                throw new UnresolveablePropertyException(this);
            }
        }
    }


    public ASTNode getWrappedNode() {
        return wrappedNode;
    }

}
