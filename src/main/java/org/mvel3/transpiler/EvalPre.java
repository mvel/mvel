package org.mvel3.transpiler;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.stmt.Statement;
import org.mvel3.EvaluatorBuilder.EvaluatorInfo;
import org.mvel3.transpiler.context.TranspilerContext;

public interface EvalPre {
    NodeList<Statement> evalPre(EvaluatorInfo<?, ?, ?> evalInfo, TranspilerContext<?, ?, ?> context, NodeList<Statement> statements);
}
