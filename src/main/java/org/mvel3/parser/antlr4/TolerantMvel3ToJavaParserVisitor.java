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

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Node;

/**
 * Tolerant visitor which continues parsing even if there is an error so that code completion and analysis can be performed.
 */
public class TolerantMvel3ToJavaParserVisitor extends Mvel3ToJavaParserVisitor {

    // Associate antlr tokenId with a JavaParser node for identifier, so it can be used for code completion.
    private final Map<Integer, Node> tokenIdJPNodeMap = new HashMap<>();

    public Map<Integer, Node> getTokenIdJPNodeMap() {
        return tokenIdJPNodeMap;
    }
}
