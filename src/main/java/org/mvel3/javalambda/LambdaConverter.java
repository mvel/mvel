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
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.ArrayList;
import java.util.List;

public final class LambdaConverter {

    private LambdaConverter() {}

    public static MethodDeclaration toMethodDeclaration(LambdaExpr lambdaExpr, String methodName) {
        MethodDeclaration md = new MethodDeclaration();
        md.setModifiers(Modifier.Keyword.PUBLIC);
        md.setName(methodName);
        md.setType(new VoidType());

        List<String> inferredTypes = inferParameterTypes(lambdaExpr);

        NodeList<Parameter> params = new NodeList<>();
        List<Parameter> lambdaParams = lambdaExpr.getParameters();
        for (int i = 0; i < lambdaParams.size(); i++) {
            Parameter original = lambdaParams.get(i);
            Type type;
            if (original.getType() instanceof UnknownType) {
                String fqcn = i < inferredTypes.size() ? inferredTypes.get(i) : "java.lang.Object";
                type = new ClassOrInterfaceType(null, fqcn);
            } else {
                type = resolveExplicitType(original);
            }
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

    private static List<String> inferParameterTypes(LambdaExpr lambdaExpr) {
        List<String> types = new ArrayList<>();
        try {
            ResolvedType lambdaType = lambdaExpr.calculateResolvedType();
            if (lambdaType.isReferenceType()) {
                var typeParams = lambdaType.asReferenceType().typeParametersValues();
                for (ResolvedType tp : typeParams) {
                    types.add(tp.describe());
                }
            }
        } catch (Exception e) {
            // Type resolution failed — will fall back to Object per-param
        }
        return types;
    }

    private static Type resolveExplicitType(Parameter param) {
        try {
            ResolvedType resolved = param.getType().resolve();
            return new ClassOrInterfaceType(null, resolved.describe());
        } catch (Exception e) {
            return new ClassOrInterfaceType(null, "java.lang.Object");
        }
    }
}
