/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates.
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

package org.mvel3.parser.antlr4;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import com.github.javaparser.ast.comments.CommentsCollection;
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
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.mvel3.parser.MvelParser;
import org.mvel3.parser.Provider;
import org.mvel3.util.ProviderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mvel3.parser.Providers.provider;

public class Antlr4MvelParser implements MvelParser {
    private static final Logger logger = LoggerFactory.getLogger(Antlr4MvelParser.class);

    private final ParserConfiguration configuration;

    public Antlr4MvelParser() {
        this(new ParserConfiguration());
    }

    public Antlr4MvelParser(ParserConfiguration configuration) {
        this.configuration = configuration;
    }

    // Simple parse to return Antlr AST. For testing purposes only.
    public static ParseTree parseExpressionAsAntlrAST(final String expression) {
        try {
            // Create ANTLR4 lexer and parser
            CharStream charStream = CharStreams.fromString(expression);
            Mvel3Lexer lexer = new Mvel3Lexer(charStream);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            Mvel3Parser parser = new Mvel3Parser(tokens);
            
            // Add error handling
            List<String> errors = new ArrayList<>();
            parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                      int line, int charPositionInLine, String msg, RecognitionException e) {
                    errors.add(String.format("line %d:%d %s", line, charPositionInLine, msg));
                }
            });
            
            // Parse the expression
            ParseTree tree = parser.mvelStart();
            
            if (!errors.isEmpty()) {
                throw new RuntimeException("Parse errors: " + String.join(", ", errors));
            }
            
            return tree;
            
        } catch (Exception e) {
            throw new RuntimeException("Parse error: " + e.getMessage(), e);
        }
    }

    /**
     * @return The configuration for this parser.
     */
    @Override
    public ParserConfiguration getParserConfiguration() {
        return this.configuration;
    }


    // The main parse method
    public ParseResult parse(Antlr4ParseStart start, final Provider provider) {
        try {
            String input = ProviderUtils.readAll(provider);
            logger.debug("Parsing with input: {}", input);
            
            // Create ANTLR4 lexer and parser
            CharStream charStream = CharStreams.fromString(input);
            Mvel3Lexer lexer = new Mvel3Lexer(charStream);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            Mvel3Parser parser = new Mvel3Parser(tokens);

            // Add error handling
            List<String> errors = new ArrayList<>();
            parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                        int line, int charPositionInLine, String msg, RecognitionException e) {
                    errors.add(String.format("line %d:%d %s", line, charPositionInLine, msg));
                }
            });

            ParseTree tree = start.parse(parser);
            logger.debug("Parse tree: {}", tree.toStringTree(parser));

            Mvel3ToJavaParserVisitor visitor = new Mvel3ToJavaParserVisitor();
            Node result = visitor.visit(tree);
            logger.debug("Visitor result type: {}, value: {}", result != null ? result.getClass().getSimpleName() : "null", result);

            if (!errors.isEmpty()) {
                throw new RuntimeException("Parse errors: " + String.join(", ", errors));
            }

            return new ParseResult<>(result, Collections.emptyList(), new CommentsCollection());

        } catch (Exception e) {
            throw new RuntimeException("Parse error: " + e.getMessage(), e);
        }
    }

    /**
     * Parses the Java code contained in the {@link InputStream} and returns a
     * {@link CompilationUnit} that represents it.
     *
     * @param in       {@link InputStream} containing Java source code. It will be closed after parsing.
     * @param encoding encoding of the source code
     * @return CompilationUnit representing the Java source code
     * @throws ParseProblemException if the source code has parser errors
     */
    @Override
    public ParseResult<CompilationUnit> parse(InputStream in, Charset encoding) {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses the Java code contained in the {@link InputStream} and returns a
     * {@link CompilationUnit} that represents it.<br>
     *
     * @param in {@link InputStream} containing Java source code. It will be closed after parsing.
     * @return CompilationUnit representing the Java source code
     * @throws ParseProblemException if the source code has parser errors
     */
    @Override
    public ParseResult<CompilationUnit> parse(InputStream in) {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses the Java code contained in a {@link File} and returns a
     * {@link CompilationUnit} that represents it.
     *
     * @param file     {@link File} containing Java source code. It will be closed after parsing.
     * @param encoding encoding of the source code
     * @return CompilationUnit representing the Java source code
     * @throws ParseProblemException if the source code has parser errors
     * @throws FileNotFoundException the file was not found
     * @deprecated set the encoding in the {@link ParserConfiguration}
     */
    @Override
    public ParseResult<CompilationUnit> parse(File file, Charset encoding) throws FileNotFoundException {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses the Java code contained in a {@link File} and returns a
     * {@link CompilationUnit} that represents it.<br>
     *
     * @param file {@link File} containing Java source code. It will be closed after parsing.
     * @return CompilationUnit representing the Java source code
     * @throws ParseProblemException if the source code has parser errors
     * @throws FileNotFoundException the file was not found
     */
    @Override
    public ParseResult<CompilationUnit> parse(File file) throws FileNotFoundException {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses the Java code contained in a file and returns a
     * {@link CompilationUnit} that represents it.
     *
     * @param path     path to a file containing Java source code
     * @param encoding encoding of the source code
     * @return CompilationUnit representing the Java source code
     * @throws IOException           the path could not be accessed
     * @throws ParseProblemException if the source code has parser errors
     * @deprecated set the encoding in the {@link ParserConfiguration}
     */
    @Override
    public ParseResult<CompilationUnit> parse(Path path, Charset encoding) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses the Java code contained in a file and returns a
     * {@link CompilationUnit} that represents it.<br>
     *
     * @param path path to a file containing Java source code
     * @return CompilationUnit representing the Java source code
     * @throws ParseProblemException if the source code has parser errors
     * @throws IOException           the path could not be accessed
     */
    @Override
    public ParseResult<CompilationUnit> parse(Path path) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses the Java code contained in a resource and returns a
     * {@link CompilationUnit} that represents it.<br>
     *
     * @param path path to a resource containing Java source code. As resource is accessed through a class loader, a
     *             leading "/" is not allowed in pathToResource
     * @return CompilationUnit representing the Java source code
     * @throws ParseProblemException if the source code has parser errors
     * @throws IOException           the path could not be accessed
     */
    @Override
    public ParseResult<CompilationUnit> parseResource(String path) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses the Java code contained in a resource and returns a
     * {@link CompilationUnit} that represents it.<br>
     *
     * @param path     path to a resource containing Java source code. As resource is accessed through a class loader, a
     *                 leading "/" is not allowed in pathToResource
     * @param encoding encoding of the source code
     * @return CompilationUnit representing the Java source code
     * @throws ParseProblemException if the source code has parser errors
     * @throws IOException           the path could not be accessed
     * @deprecated set the encoding in the {@link ParserConfiguration}
     */
    @Override
    public ParseResult<CompilationUnit> parseResource(String path, Charset encoding) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses the Java code contained in a resource and returns a
     * {@link CompilationUnit} that represents it.<br>
     *
     * @param classLoader the classLoader that is asked to load the resource
     * @param path        path to a resource containing Java source code. As resource is accessed through a class loader, a
     *                    leading "/" is not allowed in pathToResource
     * @param encoding
     * @return CompilationUnit representing the Java source code
     * @throws ParseProblemException if the source code has parser errors
     * @throws IOException           the path could not be accessed
     * @deprecated set the encoding in the {@link ParserConfiguration}
     */
    @Override
    public ParseResult<CompilationUnit> parseResource(ClassLoader classLoader, String path, Charset encoding) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses Java code from a Reader and returns a
     * {@link CompilationUnit} that represents it.<br>
     *
     * @param reader the reader containing Java source code. It will be closed after parsing.
     * @return CompilationUnit representing the Java source code
     * @throws ParseProblemException if the source code has parser errors
     */
    @Override
    public ParseResult<CompilationUnit> parse(Reader reader) {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses the Java code contained in code and returns a
     * {@link CompilationUnit} that represents it.
     *
     * @param code Java source code
     * @return CompilationUnit representing the Java source code
     * @throws ParseProblemException if the source code has parser errors
     */
    @Override
    public ParseResult<CompilationUnit> parse(String code) {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses the Java block contained in a {@link String} and returns a
     * {@link BlockStmt} that represents it.
     *
     * @param blockStatement {@link String} containing Java block code
     * @return BlockStmt representing the Java block
     * @throws ParseProblemException if the source code has parser errors
     */
    @Override
    public ParseResult<BlockStmt> parseBlock(String blockStatement) {
        return parse(Antlr4ParseStart.BLOCK, provider(blockStatement));
    }

    /**
     * Parses the Java statement contained in a {@link String} and returns a
     * {@link Statement} that represents it.
     *
     * @param statement {@link String} containing Java statement code
     * @return Statement representing the Java statement
     * @throws ParseProblemException if the source code has parser errors
     */
    @Override
    public ParseResult<Statement> parseStatement(String statement) {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses the Java import contained in a {@link String} and returns a
     * {@link ImportDeclaration} that represents it.
     *
     * @param importDeclaration {@link String} containing Java import code
     * @return ImportDeclaration representing the Java import declaration
     * @throws ParseProblemException if the source code has parser errors
     */
    @Override
    public ParseResult<ImportDeclaration> parseImport(String importDeclaration) {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses the Java expression contained in a {@link String} and returns a
     * {@link Expression} that represents it.
     *
     * @param expression {@link String} containing Java expression
     * @return Expression representing the Java expression
     * @throws ParseProblemException if the source code has parser errors
     */
    @Override
    public <T extends Expression> ParseResult<T> parseExpression(String expression) {
        return parse(Antlr4ParseStart.EXPRESSION, provider(expression));
    }

    /**
     * Parses the Java annotation contained in a {@link String} and returns a
     * {@link AnnotationExpr} that represents it.
     *
     * @param annotation {@link String} containing Java annotation
     * @return AnnotationExpr representing the Java annotation
     * @throws ParseProblemException if the source code has parser errors
     */
    @Override
    public ParseResult<AnnotationExpr> parseAnnotation(String annotation) {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses the Java annotation body declaration(e.g fields or methods) contained in a
     * {@link String} and returns a {@link BodyDeclaration} that represents it.
     *
     * @param body {@link String} containing Java body declaration
     * @return BodyDeclaration representing the Java annotation
     * @throws ParseProblemException if the source code has parser errors
     */
    @Override
    public ParseResult<BodyDeclaration<?>> parseAnnotationBodyDeclaration(String body) {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses a Java class or interface body declaration(e.g fields or methods) and returns a
     * {@link BodyDeclaration} that represents it.
     *
     * @param body the body of a class or interface
     * @return BodyDeclaration representing the Java interface body
     * @throws ParseProblemException if the source code has parser errors
     */
    @Override
    public <T extends BodyDeclaration<?>> ParseResult<T> parseBodyDeclaration(String body) {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses a Java class or interface type name and returns a {@link ClassOrInterfaceType} that represents it.
     *
     * @param type the type name like a.b.c.X or Y
     * @return ClassOrInterfaceType representing the type
     * @throws ParseProblemException if the source code has parser errors
     */
    @Override
    public ParseResult<ClassOrInterfaceType> parseClassOrInterfaceType(String type) {
        return parse(Antlr4ParseStart.CLASS_OR_INTERFACE_TYPE, provider(type));
    }

    /**
     * Parses a Java type name and returns a {@link Type} that represents it.
     *
     * @param type the type name like a.b.c.X, Y, or int
     * @return ClassOrInterfaceType representing the type
     * @throws ParseProblemException if the source code has parser errors
     */
    @Override
    public ParseResult<Type> parseType(String type) {
        return parse(Antlr4ParseStart.TYPE_TYPE, provider(type));
    }

    /**
     * Parses a variable declaration expression and returns a {@link VariableDeclarationExpr}
     * that represents it.
     *
     * @param declaration a variable declaration like {@code int x=2;}
     * @return VariableDeclarationExpr representing the type
     * @throws ParseProblemException if the source code has parser errors
     */
    @Override
    public ParseResult<VariableDeclarationExpr> parseVariableDeclarationExpr(String declaration) {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses the this(...) and super(...) statements that may occur at the start of a constructor.
     *
     * @param statement a statement like super("hello");
     * @return the AST for the statement.
     * @throws ParseProblemException if the source code has parser errors
     */
    @Override
    public ParseResult<ExplicitConstructorInvocationStmt> parseExplicitConstructorInvocationStmt(String statement) {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses a qualified name (one that can have "."s in it) and returns it as a Name.
     *
     * @param qualifiedName a name like "com.laamella.parameter_source"
     * @return the AST for the name
     * @throws ParseProblemException if the source code has parser errors
     */
    @Override
    public ParseResult<Name> parseName(String qualifiedName) {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses a simple name (one that can NOT have "."s in it) and returns it as a SimpleName.
     *
     * @param name a name like "parameter_source"
     * @return the AST for the name
     * @throws ParseProblemException if the source code has parser errors
     */
    @Override
    public ParseResult<SimpleName> parseSimpleName(String name) {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses a single parameter (a type and a name) and returns it as a Parameter.
     *
     * @param parameter a parameter like "int[] x"
     * @return the AST for the parameter
     * @throws ParseProblemException if the source code has parser errors
     */
    @Override
    public ParseResult<Parameter> parseParameter(String parameter) {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses a package declaration and returns it as a PackageDeclaration.
     *
     * @param packageDeclaration a declaration like "package com.microsoft.java;"
     * @return the AST for the parameter
     * @throws ParseProblemException if the source code has parser errors
     */
    @Override
    public ParseResult<PackageDeclaration> parsePackageDeclaration(String packageDeclaration) {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses a type declaration and returns it as a TypeDeclaration.
     *
     * @param typeDeclaration a declaration like "class X {}"
     * @return the AST for the type declaration
     * @throws ParseProblemException if the source code has parser errors
     */
    @Override
    public ParseResult<TypeDeclaration<?>> parseTypeDeclaration(String typeDeclaration) {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses a module declaration and returns it as a ModuleDeclaration.
     *
     * @param moduleDeclaration a declaration like "module X {}"
     * @return the AST for the module declaration
     * @throws ParseProblemException if the source code has parser errors
     * @see ModuleDeclaration
     */
    @Override
    public ParseResult<ModuleDeclaration> parseModuleDeclaration(String moduleDeclaration) {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses a module directive and returns it as a ModuleDirective.
     *
     * @param moduleDirective a directive like "opens C;"
     * @return the AST for the module directive
     * @throws ParseProblemException if the source code has parser errors
     * @see ModuleDirective
     */
    @Override
    public ParseResult<ModuleDirective> parseModuleDirective(String moduleDirective) {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses a type parameter and returns it as a TypeParameter
     *
     * @param typeParameter a parameter like "T extends Serializable"
     * @return the AST for the type parameter
     * @throws ParseProblemException if the source code has parser errors
     */
    @Override
    public ParseResult<TypeParameter> parseTypeParameter(String typeParameter) {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses a method declaration and returns it as a MethodDeclaration.
     *
     * @param methodDeclaration a method declaration like "void foo() {}"
     * @return the AST for the method declaration
     * @throws ParseProblemException if the source code has parser errors
     * @see MethodDeclaration
     */
    @Override
    public ParseResult<MethodDeclaration> parseMethodDeclaration(String methodDeclaration) {
        throw new UnsupportedOperationException();
    }
}