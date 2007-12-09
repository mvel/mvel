package org.mvel.debug;

import org.mvel.Operator;
import static org.mvel.Operator.ADD;
import static org.mvel.Operator.SUB;
import org.mvel.ast.ASTNode;
import org.mvel.ast.BinaryOperation;
import org.mvel.ast.NestedStatement;
import org.mvel.ast.Substatement;
import org.mvel.compiler.CompiledExpression;
import org.mvel.compiler.ExecutableAccessor;
import org.mvel.compiler.ExecutableLiteral;
import org.mvel.integration.VariableResolver;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.util.ASTIterator;
import static org.mvel.util.ParseTools.getSimpleClassName;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Christopher Brock
 */
public class DebugTools {

    public static String decompile(Serializable expr) {
        if (expr instanceof CompiledExpression) return decompile((CompiledExpression) expr);
        else if (expr instanceof ExecutableAccessor)
            return "CANNOT DECOMPILE OPTIMIZED STATEMENT (Run with -Dmvel.optimizer=false)";
        else if (expr instanceof ExecutableLiteral) {
            return "LITERAL: " + ((ExecutableLiteral) expr).getValue(null, null);
        }
        else return "NOT A KNOWN PAYLOAD: " + expr.getClass().getName();
    }

    public static String decompile(CompiledExpression cExp) {
        return decompile(cExp, false, new DecompileContext());
    }

    private static final class DecompileContext {
        public int node = 0;
    }

    private static String decompile(CompiledExpression cExp, boolean nest, DecompileContext context) {
        ASTIterator iter = cExp.getTokens();
        ASTNode tk;

        //   int node = 0;

        StringBuffer sbuf = new StringBuffer();

        if (!nest) {
            sbuf.append("Expression Decompile\n-------------\n");
        }

        while (iter.hasMoreNodes()) {
            tk = iter.nextNode();

            sbuf.append("(").append(context.node++).append(") ");

            if (tk instanceof NestedStatement
                    && ((NestedStatement) tk).getNestedStatement() instanceof CompiledExpression) {
                //noinspection StringConcatenationInsideStringBufferAppend
                sbuf.append("NEST [" + getSimpleClassName(tk.getClass()) + "]: { " + tk.getName() + " }\n");
                sbuf.append(decompile((CompiledExpression) ((NestedStatement) tk).getNestedStatement(), true, context));
            }
            if (tk instanceof Substatement
                    && ((Substatement) tk).getStatement() instanceof CompiledExpression) {
                //noinspection StringConcatenationInsideStringBufferAppend
                sbuf.append("NEST [" + getSimpleClassName(tk.getClass()) + "]: { " + tk.getName() + " }\n");
                sbuf.append(decompile((CompiledExpression) ((Substatement) tk).getStatement(), true, context));
            }
            else if (tk.isDebuggingSymbol()) {
                //noinspection StringConcatenationInsideStringBufferAppend
                sbuf.append("DEBUG_SYMBOL :: " + tk.toString());
            }
            else if (tk.isLiteral()) {
                sbuf.append("LITERAL :: ").append(tk.getLiteralValue()).append("'");
            }
            else if (tk.isOperator()) {
                sbuf.append("OPERATOR [").append(getOperatorName(tk.getOperator())).append("]: ")
                        .append(tk.getName());

                if (tk.isOperator(Operator.END_OF_STMT)) sbuf.append("\n");
            }
            else if (tk.isIdentifier()) {
                sbuf.append("REFERENCE :: ").append(getSimpleClassName(tk.getClass())).append(":").append(tk.getName());
            }
            else if (tk instanceof BinaryOperation) {
                BinaryOperation bo = (BinaryOperation) tk;
                sbuf.append("OPERATION [" + getOperatorName(bo.getOperation()) + "] {").append(bo.getLeft().getName())
                        .append("} {").append(bo.getRight().getName()).append("}");
            }
            else {
                //noinspection StringConcatenationInsideStringBufferAppend
                sbuf.append("NODE [" + getSimpleClassName(tk.getClass()) + "] :: " + tk.getName());
            }

            sbuf.append("\n");

        }

        sbuf.append("==END==");

        return sbuf.toString();
    }


    public static String getOperatorName(int operator) {
        switch (operator) {
            case ADD:
                return "ADD";
            case SUB:
                return "SUBTRACT";
            case Operator.ASSIGN:
                return "ASSIGN";
            case Operator.ASSIGN_ADD:
                return "ASSIGN_ADD";
            case Operator.ASSIGN_STR_APPEND:
                return "ASSIGN_STR_APPEND";
            case Operator.ASSIGN_SUB:
                return "ASSIGN_SUB";
            case Operator.BW_AND:
                return "BIT_AND";
            case Operator.BW_OR:
                return "BIT_OR";
            case Operator.BW_SHIFT_LEFT:
                return "BIT_SHIFT_LEFT";
            case Operator.BW_SHIFT_RIGHT:
                return "BIT_SHIFT_RIGHT";
            case Operator.BW_USHIFT_LEFT:
                return "BIT_UNSIGNED_SHIFT_LEFT";
            case Operator.BW_USHIFT_RIGHT:
                return "BIT_UNSIGNED_SHIFT_RIGHT";
            case Operator.BW_XOR:
                return "BIT_XOR";
            case Operator.CONTAINS:
                return "CONTAINS";
            case Operator.CONVERTABLE_TO:
                return "CONVERTABLE_TO";
            case Operator.DEC:
                return "DECREMENT";
            case Operator.DEC_ASSIGN:
                return "DECREMENT_ASSIGN";
            case Operator.DIV:
                return "DIVIDE";
            case Operator.DO:
                return "DO";
            case Operator.ELSE:
                return "ELSE";
            case Operator.END_OF_STMT:
                return "END_OF_STATEMENT";
            case Operator.EQUAL:
                return "EQUAL";
            case Operator.FOR:
                return "FOR";
            case Operator.FOREACH:
                return "FOREACH";
            case Operator.FUNCTION:
                return "FUNCTION";
            case Operator.GETHAN:
                return "GREATER_THAN_OR_EQUAL";
            case Operator.GTHAN:
                return "GREATHER_THAN";
            case Operator.IF:
                return "IF";
            case Operator.INC:
                return "INCREMENT";
            case Operator.INC_ASSIGN:
                return "INCREMENT_ASSIGN";
            case Operator.INSTANCEOF:
                return "INSTANCEOF";
            case Operator.LETHAN:
                return "LESS_THAN_OR_EQUAL";
            case Operator.LTHAN:
                return "LESS_THAN";
            case Operator.MOD:
                return "MODULUS";
            case Operator.MULT:
                return "MULTIPLY";
            case Operator.NEQUAL:
                return "NOT_EQUAL";
            case Operator.NEW:
                return "NEW_OBJECT";
            case Operator.OR:
                return "OR";
            case Operator.POWER:
                return "POWER_OF";
            case Operator.PROJECTION:
                return "PROJECT";
            case Operator.REGEX:
                return "REGEX";
            case Operator.RETURN:
                return "RETURN";
            case Operator.SIMILARITY:
                return "SIMILARITY";
            case Operator.SOUNDEX:
                return "SOUNDEX";
            case Operator.STR_APPEND:
                return "STR_APPEND";
            case Operator.SWITCH:
                return "SWITCH";
            case Operator.TERNARY:
                return "TERNARY_IF";
            case Operator.TERNARY_ELSE:
                return "TERNARY_ELSE";
            case Operator.WHILE:
                return "WHILE";
            case Operator.CHOR:
                return "CHAINED_OR";
        }


        return "UNKNOWN_OPERATOR";
    }

    public static Class determineType(String name, CompiledExpression compiledExpression) {
        ASTIterator iter = compiledExpression.getTokenIterator();
        ASTNode node;
        while (iter.hasMoreNodes()) {
            if (name.equals((node = iter.nextNode()).getName()) && node.isAssignment()) {
                return node.getEgressType();
            }
        }

        return null;
    }

    public static Map<String, VariableResolver> getAllVariableResolvers(VariableResolverFactory rootFactory) {

        Map<String, VariableResolver> allVariableResolvers = new HashMap<String, VariableResolver>();

        VariableResolverFactory vrf = rootFactory;
        do {
            for (String var : vrf.getKnownVariables()) {
                allVariableResolvers.put(var, vrf.getVariableResolver(var));

            }
        }
        while ((vrf = vrf.getNextFactory()) != null);

        return allVariableResolvers;
    }

}
