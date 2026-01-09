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
package org.mvel3.parser.antlr4;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.antlr.v4.runtime.tree.ParseTree;
import org.mvel3.parser.ParseException;

/**
 * The start production for Antlr.
 * Tells Antlr what piece of Java code it can expect.
 */
@FunctionalInterface
public interface Antlr4ParseStart {

    ParseTree parse(Mvel3Parser parser) throws ParseException;

    Antlr4ParseStart COMPILATION_UNIT = Mvel3Parser::compilationUnit;
    Antlr4ParseStart CLASS_OR_INTERFACE_TYPE = Mvel3Parser::classOrInterfaceType;
    Antlr4ParseStart TYPE_TYPE = Mvel3Parser::typeType;
    Antlr4ParseStart EXPRESSION = Mvel3Parser::expression;
    Antlr4ParseStart BLOCK = Mvel3Parser::block;
}