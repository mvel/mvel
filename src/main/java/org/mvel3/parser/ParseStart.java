/*
 * Copyright (C) 2007-2010 Júlio Vilmar Gesser.
 * Copyright (C) 2011, 2013-2023 The JavaParser Team.
 *
 * This file is part of JavaParser.
 *
 * JavaParser can be used either under the terms of
 * a) the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * b) the terms of the Apache License
 *
 * You should have received a copy of both licenses in LICENCE.LGPL and
 * LICENCE.APACHE. Please refer to those files for details.
 *
 * JavaParser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 */
package org.mvel3.parser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleDirective;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;

/**
 * The start production for JavaParser.
 * Tells JavaParser what piece of Java code it can expect.
 * For example,
 * COMPILATION_UNIT indicates a complete Java file,
 * and CLASS_BODY would indicate the part of a class that is within { and }.
 *
 * @see MvelParser#parse(ParseStart, Provider)
 */
@FunctionalInterface
public interface ParseStart<R> {

    ParseStart<CompilationUnit> COMPILATION_UNIT = GeneratedMvelParser::CompilationUnit;

    ParseStart<CompilationUnit> DRLX_COMPILATION_UNIT = GeneratedMvelParser::DrlxCompilationUnit;

    ParseStart<BlockStmt> BLOCK = GeneratedMvelParser::BlockParseStart;

    ParseStart<Statement> STATEMENT = GeneratedMvelParser::BlockStatementParseStart;

    ParseStart<ImportDeclaration> IMPORT_DECLARATION = GeneratedMvelParser::ImportDeclarationParseStart;

    ParseStart<Expression> EXPRESSION = GeneratedMvelParser::ExpressionParseStart;

    ParseStart<AnnotationExpr> ANNOTATION = GeneratedMvelParser::AnnotationParseStart;

    ParseStart<BodyDeclaration<?>> ANNOTATION_BODY = GeneratedMvelParser::AnnotationBodyDeclarationParseStart;

    ParseStart<BodyDeclaration<?>> CLASS_BODY = GeneratedMvelParser::ClassOrInterfaceBodyDeclarationParseStart;

    ParseStart<ClassOrInterfaceType> CLASS_OR_INTERFACE_TYPE = GeneratedMvelParser::ClassOrInterfaceTypeParseStart;

    ParseStart<Type> TYPE = GeneratedMvelParser::ResultTypeParseStart;

    ParseStart<TypeParameter> TYPE_PARAMETER = GeneratedMvelParser::TypeParameterParseStart;

    ParseStart<VariableDeclarationExpr> VARIABLE_DECLARATION_EXPR = GeneratedMvelParser::VariableDeclarationExpressionParseStart;

    ParseStart<ExplicitConstructorInvocationStmt> EXPLICIT_CONSTRUCTOR_INVOCATION_STMT = GeneratedMvelParser::ExplicitConstructorInvocationParseStart;

    ParseStart<Name> NAME = GeneratedMvelParser::NameParseStart;

    ParseStart<SimpleName> SIMPLE_NAME = GeneratedMvelParser::SimpleNameParseStart;

    ParseStart<Parameter> PARAMETER = GeneratedMvelParser::ParameterParseStart;

    ParseStart<PackageDeclaration> PACKAGE_DECLARATION = GeneratedMvelParser::PackageDeclarationParseStart;

    ParseStart<TypeDeclaration<?>> TYPE_DECLARATION = GeneratedMvelParser::TypeDeclarationParseStart;

    ParseStart<ModuleDeclaration> MODULE_DECLARATION = GeneratedMvelParser::ModuleDeclarationParseStart;

    ParseStart<ModuleDirective> MODULE_DIRECTIVE = GeneratedMvelParser::ModuleDirectiveParseStart;

    ParseStart<MethodDeclaration> METHOD_DECLARATION = GeneratedMvelParser::MethodDeclarationParseStart;

    R parse(GeneratedMvelParser parser) throws ParseException;
}