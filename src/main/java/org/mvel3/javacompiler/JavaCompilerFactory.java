/*
 * Copyright (c) 2020. Red Hat, Inc. and/or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel3.javacompiler;

import java.util.Optional;

/**
 * Creates JavaCompilers
 */
public class JavaCompilerFactory {

    public static JavaCompiler loadCompiler( JavaConfiguration configuration) {
        return loadCompiler( configuration.getJavaLanguageLevel() );
    }

    public static JavaCompiler loadCompiler(String lngLevel ) {
        return loadCompiler( lngLevel, "" );
    }

    public static JavaCompiler loadCompiler(String lngLevel, String sourceFolder ) {
        JavaCompiler compiler = new JavaCompiler();
        compiler.setJavaCompilerSettings( createSettings( compiler, lngLevel ) );
        return compiler;
    }

    private static JavaCompilerSettings createSettings( JavaCompiler compiler, String lngLevel ) {
        JavaCompilerSettings settings = compiler.createDefaultSettings();
        settings.setTargetVersion( lngLevel );
        settings.setSourceVersion( lngLevel );
        return settings;
    }
}
