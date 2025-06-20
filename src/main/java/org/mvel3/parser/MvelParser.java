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
 */

package org.mvel3.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Path;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleDirective;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import org.mvel3.parser.antlr4.Antlr4MvelParser;

/**
 * Interface for parsing MVEL expressions and Java source code.
 * 
 * @author MVEL Team
 */
public interface MvelParser {


    /**
     * @return The configuration for this parser.
     */
    ParserConfiguration getParserConfiguration();

    /**
     * Parses the Java code contained in the {@link InputStream} and returns a
     * {@link CompilationUnit} that represents it.
     *
     * @param in {@link InputStream} containing Java source code. It will be closed after parsing.
     * @param encoding encoding of the source code
     * @return CompilationUnit representing the Java source code
     * @throws ParseProblemException if the source code has parser errors
     */
    ParseResult<CompilationUnit> parse(final InputStream in, Charset encoding);

    /**
     * Parses the Java code contained in the {@link InputStream} and returns a
     * {@link CompilationUnit} that represents it.<br>
     *
     * @param in {@link InputStream} containing Java source code. It will be closed after parsing.
     * @return CompilationUnit representing the Java source code
     * @throws ParseProblemException if the source code has parser errors
     */
    ParseResult<CompilationUnit> parse(final InputStream in);

    /**
     * Parses the Java code contained in a {@link File} and returns a
     * {@link CompilationUnit} that represents it.
     *
     * @param file {@link File} containing Java source code. It will be closed after parsing.
     * @param encoding encoding of the source code
     * @return CompilationUnit representing the Java source code
     * @throws ParseProblemException if the source code has parser errors
     * @throws FileNotFoundException the file was not found
     * @deprecated set the encoding in the {@link ParserConfiguration}
     */
    @Deprecated
    ParseResult<CompilationUnit> parse(final File file, final Charset encoding) throws FileNotFoundException;

    /**
     * Parses the Java code contained in a {@link File} and returns a
     * {@link CompilationUnit} that represents it.<br>
     *
     * @param file {@link File} containing Java source code. It will be closed after parsing.
     * @return CompilationUnit representing the Java source code
     * @throws ParseProblemException if the source code has parser errors
     * @throws FileNotFoundException the file was not found
     */
    ParseResult<CompilationUnit> parse(final File file) throws FileNotFoundException;

    /**
     * Parses the Java code contained in a file and returns a
     * {@link CompilationUnit} that represents it.
     *
     * @param path path to a file containing Java source code
     * @param encoding encoding of the source code
     * @return CompilationUnit representing the Java source code
     * @throws IOException the path could not be accessed
     * @throws ParseProblemException if the source code has parser errors
     * @deprecated set the encoding in the {@link ParserConfiguration}
     */
    @Deprecated
    ParseResult<CompilationUnit> parse(final Path path, final Charset encoding) throws IOException;

    /**
     * Parses the Java code contained in a file and returns a
     * {@link CompilationUnit} that represents it.<br>
     *
     * @param path path to a file containing Java source code
     * @return CompilationUnit representing the Java source code
     * @throws ParseProblemException if the source code has parser errors
     * @throws IOException the path could not be accessed
     */
    ParseResult<CompilationUnit> parse(final Path path) throws IOException;

    /**
     * Parses the Java code contained in a resource and returns a
     * {@link CompilationUnit} that represents it.<br>
     *
     * @param path path to a resource containing Java source code. As resource is accessed through a class loader, a
     * leading "/" is not allowed in pathToResource
     * @return CompilationUnit representing the Java source code
     * @throws ParseProblemException if the source code has parser errors
     * @throws IOException the path could not be accessed
     */
    ParseResult<CompilationUnit> parseResource(final String path) throws IOException;

    /**
     * Parses the Java code contained in a resource and returns a
     * {@link CompilationUnit} that represents it.<br>
     *
     * @param path path to a resource containing Java source code. As resource is accessed through a class loader, a
     * leading "/" is not allowed in pathToResource
     * @param encoding encoding of the source code
     * @return CompilationUnit representing the Java source code
     * @throws ParseProblemException if the source code has parser errors
     * @throws IOException the path could not be accessed
     * @deprecated set the encoding in the {@link ParserConfiguration}
     */
    @Deprecated
    ParseResult<CompilationUnit> parseResource(final String path, Charset encoding) throws IOException;

    /**
     * Parses the Java code contained in a resource and returns a
     * {@link CompilationUnit} that represents it.<br>
     *
     * @param classLoader the classLoader that is asked to load the resource
     * @param path path to a resource containing Java source code. As resource is accessed through a class loader, a
     * leading "/" is not allowed in pathToResource
     * @return CompilationUnit representing the Java source code
     * @throws ParseProblemException if the source code has parser errors
     * @throws IOException the path could not be accessed
     * @deprecated set the encoding in the {@link ParserConfiguration}
     */
    @Deprecated
    ParseResult<CompilationUnit> parseResource(final ClassLoader classLoader, final String path, Charset encoding) throws IOException;

    /**
     * Parses Java code from a Reader and returns a
     * {@link CompilationUnit} that represents it.<br>
     *
     * @param reader the reader containing Java source code. It will be closed after parsing.
     * @return CompilationUnit representing the Java source code
     * @throws ParseProblemException if the source code has parser errors
     */
    ParseResult<CompilationUnit> parse(final Reader reader);

    /**
     * Parses the Java code contained in code and returns a
     * {@link CompilationUnit} that represents it.
     *
     * @param code Java source code
     * @return CompilationUnit representing the Java source code
     * @throws ParseProblemException if the source code has parser errors
     */
    ParseResult<CompilationUnit> parse(String code);

    /**
     * Parses the Java block contained in a {@link String} and returns a
     * {@link BlockStmt} that represents it.
     *
     * @param blockStatement {@link String} containing Java block code
     * @return BlockStmt representing the Java block
     * @throws ParseProblemException if the source code has parser errors
     */
    ParseResult<BlockStmt> parseBlock(final String blockStatement);

    /**
     * Parses the Java statement contained in a {@link String} and returns a
     * {@link Statement} that represents it.
     *
     * @param statement {@link String} containing Java statement code
     * @return Statement representing the Java statement
     * @throws ParseProblemException if the source code has parser errors
     */
    ParseResult<Statement> parseStatement(final String statement);

    /**
     * Parses the Java import contained in a {@link String} and returns a
     * {@link ImportDeclaration} that represents it.
     *
     * @param importDeclaration {@link String} containing Java import code
     * @return ImportDeclaration representing the Java import declaration
     * @throws ParseProblemException if the source code has parser errors
     */
    ParseResult<ImportDeclaration> parseImport(final String importDeclaration);

    /**
     * Parses the Java expression contained in a {@link String} and returns a
     * {@link Expression} that represents it.
     *
     * @param expression {@link String} containing Java expression
     * @return Expression representing the Java expression
     * @throws ParseProblemException if the source code has parser errors
     */
    @SuppressWarnings("unchecked")
    <T extends Expression> ParseResult<T> parseExpression(final String expression);

    /**
     * Parses the Java annotation contained in a {@link String} and returns a
     * {@link AnnotationExpr} that represents it.
     *
     * @param annotation {@link String} containing Java annotation
     * @return AnnotationExpr representing the Java annotation
     * @throws ParseProblemException if the source code has parser errors
     */
    ParseResult<AnnotationExpr> parseAnnotation(final String annotation);

    /**
     * Parses the Java annotation body declaration(e.g fields or methods) contained in a
     * {@link String} and returns a {@link BodyDeclaration} that represents it.
     *
     * @param body {@link String} containing Java body declaration
     * @return BodyDeclaration representing the Java annotation
     * @throws ParseProblemException if the source code has parser errors
     */
    ParseResult<BodyDeclaration<?>> parseAnnotationBodyDeclaration(final String body);

    /**
     * Parses a Java class or interface body declaration(e.g fields or methods) and returns a
     * {@link BodyDeclaration} that represents it.
     *
     * @param body the body of a class or interface
     * @return BodyDeclaration representing the Java interface body
     * @throws ParseProblemException if the source code has parser errors
     */
    @SuppressWarnings("unchecked")
    <T extends BodyDeclaration<?>> ParseResult<T> parseBodyDeclaration(String body);

    /**
     * Parses a Java class or interface type name and returns a {@link ClassOrInterfaceType} that represents it.
     *
     * @param type the type name like a.b.c.X or Y
     * @return ClassOrInterfaceType representing the type
     * @throws ParseProblemException if the source code has parser errors
     */
    ParseResult<ClassOrInterfaceType> parseClassOrInterfaceType(String type);

    /**
     * Parses a Java type name and returns a {@link Type} that represents it.
     *
     * @param type the type name like a.b.c.X, Y, or int
     * @return ClassOrInterfaceType representing the type
     * @throws ParseProblemException if the source code has parser errors
     */
    ParseResult<Type> parseType(String type);

    /**
     * Parses a variable declaration expression and returns a {@link VariableDeclarationExpr}
     * that represents it.
     *
     * @param declaration a variable declaration like {@code int x=2;}
     * @return VariableDeclarationExpr representing the type
     * @throws ParseProblemException if the source code has parser errors
     */
    ParseResult<VariableDeclarationExpr> parseVariableDeclarationExpr(String declaration);

    /**
     * Parses the this(...) and super(...) statements that may occur at the start of a constructor.
     *
     * @param statement a statement like super("hello");
     * @return the AST for the statement.
     * @throws ParseProblemException if the source code has parser errors
     */
    ParseResult<ExplicitConstructorInvocationStmt> parseExplicitConstructorInvocationStmt(String statement);

    /**
     * Parses a qualified name (one that can have "."s in it) and returns it as a Name.
     *
     * @param qualifiedName a name like "com.laamella.parameter_source"
     * @return the AST for the name
     * @throws ParseProblemException if the source code has parser errors
     */
    ParseResult<Name> parseName(String qualifiedName);

    /**
     * Parses a simple name (one that can NOT have "."s in it) and returns it as a SimpleName.
     *
     * @param name a name like "parameter_source"
     * @return the AST for the name
     * @throws ParseProblemException if the source code has parser errors
     */
    ParseResult<SimpleName> parseSimpleName(String name);

    /**
     * Parses a single parameter (a type and a name) and returns it as a Parameter.
     *
     * @param parameter a parameter like "int[] x"
     * @return the AST for the parameter
     * @throws ParseProblemException if the source code has parser errors
     */
    ParseResult<Parameter> parseParameter(String parameter);

    /**
     * Parses a package declaration and returns it as a PackageDeclaration.
     *
     * @param packageDeclaration a declaration like "package com.microsoft.java;"
     * @return the AST for the parameter
     * @throws ParseProblemException if the source code has parser errors
     */
    ParseResult<PackageDeclaration> parsePackageDeclaration(String packageDeclaration);

    /**
     * Parses a type declaration and returns it as a TypeDeclaration.
     *
     * @param typeDeclaration a declaration like "class X {}"
     * @return the AST for the type declaration
     * @throws ParseProblemException if the source code has parser errors
     */
    ParseResult<TypeDeclaration<?>> parseTypeDeclaration(String typeDeclaration);

    /**
     * Parses a module declaration and returns it as a ModuleDeclaration.
     *
     * @param moduleDeclaration a declaration like "module X {}"
     * @return the AST for the module declaration
     * @throws ParseProblemException if the source code has parser errors
     * @see ModuleDeclaration
     */
    ParseResult<ModuleDeclaration> parseModuleDeclaration(String moduleDeclaration);

    /**
     * Parses a module directive and returns it as a ModuleDirective.
     *
     * @param moduleDirective a directive like "opens C;"
     * @return the AST for the module directive
     * @throws ParseProblemException if the source code has parser errors
     * @see ModuleDirective
     */
    ParseResult<ModuleDirective> parseModuleDirective(String moduleDirective);

    /**
     * Parses a type parameter and returns it as a TypeParameter
     *
     * @param typeParameter a parameter like "T extends Serializable"
     * @return the AST for the type parameter
     * @throws ParseProblemException if the source code has parser errors
     */
    ParseResult<TypeParameter> parseTypeParameter(String typeParameter);

    /**
     * Parses a method declaration and returns it as a MethodDeclaration.
     *
     * @param methodDeclaration a method declaration like "void foo() {}"
     * @return the AST for the method declaration
     * @throws ParseProblemException if the source code has parser errors
     * @see MethodDeclaration
     */
    ParseResult<MethodDeclaration> parseMethodDeclaration(String methodDeclaration);

    /**
     * Factory class to provide MvelParser implementations.
     */
    class Factory {

        // for test convenience. we will eventually remove JavaParserMvelParser and use Antlr4MvelParser only.
        public static boolean USE_ANTLR = true;

        /**
         * Get the default MvelParser implementation.
         * 
         * @return the default parser implementation
         */
        public static MvelParser get() {
            if (USE_ANTLR) {
                return new Antlr4MvelParser();
            } else {
                return new JavaParserMvelParser();
            }
        }

        /**
         * Get MvelParser implementation with custom configuration.
         * 
         * @param configuration the parser configuration
         * @return the parser implementation with given configuration
         */
        public static MvelParser get(ParserConfiguration configuration) {
            if (USE_ANTLR) {
                return new Antlr4MvelParser(configuration);
            } else {
                return new JavaParserMvelParser(configuration);
            }
        }
    }
}