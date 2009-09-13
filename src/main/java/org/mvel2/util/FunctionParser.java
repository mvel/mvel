package org.mvel2.util;

import org.mvel2.CompileException;
import org.mvel2.ParserContext;
import org.mvel2.compiler.AbstractParser;
import org.mvel2.ast.EndOfStatement;
import org.mvel2.ast.Function;
import static org.mvel2.util.ParseTools.balancedCaptureWithLineAccounting;
import static org.mvel2.util.ParseTools.createStringTrimmed;

public class FunctionParser {
    private String name;

    private int cursor;
    private int endOffset;
    private char[] expr;
    private ParserContext pCtx;

    private ExecutionStack splitAccumulator;

    public FunctionParser(String functionName, int cursor, int endOffset, char[] expr, ParserContext pCtx, ExecutionStack splitAccumulator) {
        this.name = functionName;
        this.cursor = cursor;
        this.endOffset = endOffset;
        this.expr = expr;
        this.pCtx = pCtx;
        this.splitAccumulator = splitAccumulator;
    }

    public Function parse() {
        int start = cursor;

        int startCond = 0;
        int endCond = 0;

        int blockStart;
        int blockEnd;

        cursor = ParseTools.captureToNextTokenJunction(expr, cursor, pCtx);

        if (expr[cursor = ParseTools.nextNonBlank(expr, cursor)] == '(') {
            /**
             * If we discover an opening bracket after the function name, we check to see
             * if this function accepts parameters.
             */
            endCond = cursor = balancedCaptureWithLineAccounting(expr, startCond = cursor, '(', pCtx);
            startCond++;
            cursor++;

            cursor = ParseTools.skipWhitespace(expr, cursor, pCtx);

            if (cursor >= endOffset) {
                throw new CompileException("incomplete statement", expr, cursor);
            }
            else if (expr[cursor] == '{') {
                blockEnd = cursor = balancedCaptureWithLineAccounting(expr, blockStart = cursor, '{', pCtx);
            }
            else {
                blockStart = cursor - 1;
                cursor = ParseTools.captureToEOS(expr, cursor, pCtx);
                blockEnd = cursor;
            }
        }
        else {
            /**
             * This function has not parameters.
             */
            if (expr[cursor] == '{') {
                /**
                 * This function is bracketed.  We capture the entire range in the brackets.
                 */
                blockEnd = cursor = balancedCaptureWithLineAccounting(expr, blockStart = cursor, '{', pCtx);
            }
            else {
                /**
                 * This is a single statement function declaration.  We only capture the statement.
                 */
                blockStart = cursor - 1;
                cursor = ParseTools.captureToEOS(expr, cursor, pCtx);
                blockEnd = cursor;
            }
        }

        /**
         * Trim any whitespace from the captured block range.
         */
        blockStart = ParseTools.trimRight(expr, start, blockStart + 1);
        blockEnd = ParseTools.trimLeft(expr, start, blockEnd);

        cursor++;

        /**
         * Check if the function is manually terminated.
         */
        if (splitAccumulator != null && ParseTools.isStatementNotManuallyTerminated(expr, cursor)) {
            /**
             * Add an EndOfStatement to the split accumulator in the parser.
             */
            splitAccumulator.add(new EndOfStatement());
        }

        /**
         * Produce the funciton node.
         */
        return new Function(name, ParseTools.subArray(expr, startCond, endCond), ParseTools.subArray(expr, blockStart, blockEnd),
                pCtx == null ? pCtx = AbstractParser.getCurrentThreadParserContext() : pCtx);
    }

    public String getName() {
        return name;
    }

    public int getCursor() {
        return cursor;
    }
}
