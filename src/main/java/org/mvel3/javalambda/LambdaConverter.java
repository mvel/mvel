package org.mvel3.javalambda;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.List;

public final class LambdaConverter {

    private LambdaConverter() {}

    public static MethodDeclaration toMethodDeclaration(LambdaExpr lambdaExpr, String methodName) {
        MethodDeclaration md = new MethodDeclaration();
        md.setModifiers(Modifier.Keyword.PUBLIC);
        md.setName(methodName);
        md.setType(new VoidType());

        NodeList<Parameter> params = new NodeList<>();
        List<Parameter> lambdaParams = lambdaExpr.getParameters();
        for (Parameter original : lambdaParams) {
            Type type = resolveParamType(original);
            params.add(new Parameter(type, original.getNameAsString()));
        }
        md.setParameters(params);

        BlockStmt body;
        if (lambdaExpr.getBody().isBlockStmt()) {
            body = lambdaExpr.getBody().asBlockStmt().clone();
        } else {
            body = new BlockStmt();
            body.addStatement(new ReturnStmt(lambdaExpr.getExpressionBody().orElseThrow().clone()));
        }
        md.setBody(body);

        return md;
    }

    private static Type resolveParamType(Parameter param) {
        try {
            ResolvedType resolved = param.getType().resolve();
            String fqcn = resolved.describe();
            return new ClassOrInterfaceType(null, fqcn);
        } catch (Exception e) {
            return new ClassOrInterfaceType(null, "java.lang.Object");
        }
    }
}
