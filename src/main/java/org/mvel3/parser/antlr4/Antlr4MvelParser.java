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

package org.mvel3.parser.antlr4;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class Antlr4MvelParser {

    private Antlr4MvelParser() {
        // Creating instances of util classes is forbidden.
    }

    public static ParseTree parseExpression(final String expression) {
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
}