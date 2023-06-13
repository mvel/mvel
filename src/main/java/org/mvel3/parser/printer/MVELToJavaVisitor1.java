/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package org.mvel3.parser.printer;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.Node.TreeTraversal;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import org.mvel3.parser.ast.visitor.DrlVoidVisitor;
import org.mvel3.parser.ast.visitor.DrlVoidVisitorAdapter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MVELToJavaVisitor1 extends DrlVoidVisitorAdapter<Void> implements DrlVoidVisitor<Void> {

    private TypeSolver typeSolver;

    private JavaParserFacade facade;

    private ResolvedType bigDecimalType;
    private ResolvedType bigIntegerType;

    private ResolvedType egressType;

    private ResolvedType widestEgressType;

    public MVELToJavaVisitor1(TypeSolver typeSolver) {
        this.typeSolver = typeSolver;

        facade = JavaParserFacade.get(typeSolver);
    }

    @Override
    public void visit(FieldAccessExpr n, Void arg) {
        if ( n.getParentNode().get() instanceof  AssignExpr) {
            // skip setters

        } else {
            MethodUsage getter = getMethod("get", n, 0);

            if (getter != null) {
                MethodCallExpr methodCallExpr = new MethodCallExpr(getter.getName());
                methodCallExpr.setScope(n.getScope());
                n.replace(methodCallExpr);
            }
        }
    }

    private static MethodUsage getMethod(String name, FieldAccessExpr n, int x) {
        MethodUsage method = null;
        ResolvedType type = n.getScope().calculateResolvedType();
        ReflectionClassDeclaration d = (ReflectionClassDeclaration) type.asReferenceType().getTypeDeclaration().get();
        String target = name + n.getNameAsString().toLowerCase();
        for (MethodUsage candidate : d.getAllMethods()) {
            if (!candidate.getDeclaration().isStatic() &&
                candidate.getName().toLowerCase().equals(target) && candidate.getNoParams() == x) {
                method = candidate;
            }
        }
        return method;
    }

}
