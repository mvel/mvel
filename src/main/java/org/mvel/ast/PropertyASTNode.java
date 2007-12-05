package org.mvel.ast;

import org.mvel.ASTNode;

import java.util.ArrayList;

/**
 * @author Christopher Brock
 */
@SuppressWarnings({"CaughtExceptionImmediatelyRethrown"})
public class PropertyASTNode extends ASTNode {
    //  private transient ASTNode wrappedNode;
    // private transient Accessor accessor;
    private ArrayList<String> indexedVariables;

    public PropertyASTNode(char[] expr, int start, int end, int fields) {
        super(expr, start, end, fields);
    }

    public PropertyASTNode(char[] expr, int start, int end, int fields, ArrayList<String> indexedVariables) {
        super(expr, start, end, fields);
        this.indexedVariables = indexedVariables;
    }

//    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
//        try {
//            return wrappedNode.getReducedValueAccelerated(ctx, thisValue, factory);
//        }
//        catch (NullPointerException e) {
//            if (wrappedNode == null) {
//                return initializePropertyNode(ctx, thisValue, factory);
//            }
//            else {
//                throw e;
//            }
//        }
//    }
//
//    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
//        return super.getReducedValue(ctx, thisValue, factory);
//    }

//    private Object initializePropertyNode(Object ctx, Object thisValue, VariableResolverFactory factory) {
//        if ((fields & STR_LITERAL) != 0) {
//            return (wrappedNode = new LiteralNode(new String(name))).getReducedValueAccelerated(ctx, thisValue, factory);
//        }
//        else if ((fields & LITERAL) != 0) {
//            if ((fields & THISREF) != 0) {
//                return (wrappedNode = new ThisValNode(name, fields)).getReducedValueAccelerated(ctx, thisValue, factory);
//            }
//            else {
//                return (wrappedNode = new LiteralNode(literal)).getReducedValueAccelerated(ctx, thisValue, factory);
//            }
//        }
//        else if ((fields & DEEP_PROPERTY) != 0) {
//            /**
//             * The token is a DEEP PROPERTY (meaning it contains unions) in which case we need to traverse an object
//             * graph.
//             */
//
//            String s;
//            if (AbstractParser.LITERALS.containsKey(s = getAbsoluteRootElement())) {
//                /**
//                 * The root of the DEEP PROPERTY is a literal.
//                 */
//                Object literal = AbstractParser.LITERALS.get(s);
//                if (literal == ThisLiteral.class) {
//                    return (wrappedNode = new ThisValDeepPropertyNode(name, fields)).getReducedValueAccelerated(ctx, thisValue, factory);
//                }
//                else {
//                    return (wrappedNode = new LiteralDeepPropertyNode(name, fields, literal)).getReducedValueAccelerated(ctx, thisValue, factory);
//                }
//            }
//            else if (factory != null && factory.isResolveable(s)) {
//                /**
//                 * The root of the DEEP PROPERTY is a local or global var.
//                 */
//
//                return (wrappedNode = new VariableDeepPropertyNode(name, fields)).getReducedValueAccelerated(ctx, thisValue, factory);
//            }
//            else {
//                if (ctx != null) {
//                    try {
//                        return (wrappedNode = new ContextDeepPropertyNode(name, fields)).getReducedValueAccelerated(ctx, thisValue, factory);
//                    }
//                    catch (CompileException e) {
//                        /**
//                         * No luck. Make a last-ditch effort to resolve this as a static-class reference.
//                         */
//                    }
//                }
//
//                Object sa = tryStaticAccess(ctx, factory);
//                if (sa == null) throw new PropertyAccessException("unable to resolve token: " + new String(name));
//
//                return (wrappedNode = new LiteralNode(sa)).getReducedValueAccelerated(ctx, thisValue, factory);
//            }
//        }
//        else {
//            String s;
//            if (factory != null && factory.isResolveable(s = getAbsoluteName())) {
//                /**
//                 * The token is a local or global var.
//                 */
//                if (s.length() < name.length) {
//                    /**
//                     * This is probably an indexed property.
//                     */
//                    return (wrappedNode = new VariableDeepPropertyNode(name, fields))
//                            .getReducedValueAccelerated(ctx, thisValue, factory);
//                }
//                else {
//                    return (wrappedNode = new VarPropertyNode(name, fields, s))
//                            .getReducedValueAccelerated(ctx, thisValue, factory);
//                }
//
//                //     return wrappedNode.getReducedValueAccelerated(ctx, thisValue, factory);
//            }
//            else if (ctx != null) {
//                /**
//                 * Check to see if the var exists in the VROOT.
//                 */
//
//                try {
//                    return (wrappedNode = new ContextDeepPropertyNode(name, fields)).getReducedValueAccelerated(ctx, thisValue, factory);
//                }
//                catch (RuntimeException e) {
//                    e.printStackTrace();
//                    throw new UnresolveablePropertyException(this, e);
//                }
//            }
//            else {
//                if (isOperator()) {
//                    throw new CompileException("incomplete statement");
//                }
//
//                throw new UnresolveablePropertyException(this);
//            }
//        }
//    }
//
//
//    public ASTNode getWrappedNode() {
//        return wrappedNode;
//    }

}
