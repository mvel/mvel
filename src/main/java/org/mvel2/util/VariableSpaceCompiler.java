package org.mvel2.util;

import org.mvel2.MVEL;
import org.mvel2.ParserContext;

import java.util.Set;

/**
 * @author Mike Brock .
 */
public class VariableSpaceCompiler {
    public static VariableSpaceModel compile(String expr, ParserContext pCtx, Object[] vars) {
        String[] varNames = pCtx.getIndexedVarNames();

        ParserContext analysisContext = ParserContext.create();
        analysisContext.setIndexAllocation(true);

        MVEL.analysisCompile(expr, analysisContext);

        Set<String> localNames = analysisContext.getVariables().keySet();

        pCtx.addIndexedLocals(localNames);

        String[] locals = localNames.toArray(new String[localNames.size()]);
        String[] allVars = new String[varNames.length + locals.length];

        System.arraycopy(varNames, 0, allVars, 0, varNames.length);
        System.arraycopy(locals, 0, allVars, varNames.length, locals.length);

        return new VariableSpaceModel(allVars, vars);
    }

}
