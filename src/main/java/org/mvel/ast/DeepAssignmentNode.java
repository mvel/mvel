package org.mvel.ast;

import org.mvel.*;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.util.ParseTools;
import static org.mvel.util.ArrayTools.findFirst;
import static org.mvel.util.ParseTools.subset;
import static org.mvel.util.PropertyTools.find;

/**
 * @author Christopher Brock
 */
public class DeepAssignmentNode extends ASTNode implements Assignment {
     private String property;

  //  private Accessor baseAccessor;

    private CompiledSetExpression set;
    private Accessor statement;

    public DeepAssignmentNode(char[] expr, int fields) {
        super(expr, fields);

       String name;

        int mark;
        if ((mark = find(expr, '=')) != -1) {
            name = new String(expr, 0, mark).trim();
            statement = (ExecutableStatement) ParseTools.subCompileExpression(subset(expr, mark + 1));
                       
        }
        else {
            name = new String(expr);
        }

//        baseAccessor = (Accessor) ParseTools.subCompileExpression(name.substring(0, mark = name.indexOf('.')));
        property = name;
        
        set = (CompiledSetExpression) MVEL.compileSetExpression(property.toCharArray());




    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        Object val;
//        MVEL.setProperty(baseAccessor.getValue(ctx, thisValue, factory), property,
//                val = statement.getValue(ctx, thisValue, factory));

        set.setValue(ctx, factory, val = statement.getValue(ctx, thisValue, factory));

        return val;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getReducedValueAccelerated(ctx, thisValue, factory);
    }


    public String getAssignmentVar() {
        return property;
    }
}
