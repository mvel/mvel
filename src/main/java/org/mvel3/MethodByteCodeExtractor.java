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

package org.mvel3;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * The purpose of this utility it to check if 2 method implementations are equivalent, by comparing the bytecode.
 * This essentual for node sharing where java semantics are involved.
 */
public class MethodByteCodeExtractor {


    /**
     * This will return a series of bytecode instructions which can be used to compare one method with another.
     * debug info like local var declarations and line numbers are ignored, so the focus is on the content.
     */
    public static String extract(final String methodName,
                                 final byte[] bytes) {
        final Tracer visit = new Tracer( methodName );
        final ClassReader classReader = new ClassReader(bytes );
        classReader.accept( visit,
                            ClassReader.SKIP_DEBUG );
        return visit.getText();
    }

    public static class Tracer
        extends
        ClassVisitor {

        private String             methodName;
        private String             text;

        public Tracer(final String methodName) {
            super(Opcodes.ASM7);
            this.methodName = methodName;
        }

        public void visit(final int version,
                          final int access,
                          final String name,
                          final String signature,
                          final String superName,
                          final String[] interfaces) {
        }

        public AnnotationVisitor visitAnnotation(final String desc,
                                                 final boolean visible) {
            return new DummyAnnotationVisitor();
        }

        public void visitAttribute(final Attribute attr) {
        }

        public void visitEnd() {
        }

        public FieldVisitor visitField(final int access,
                                       final String name,
                                       final String desc,
                                       final String signature,
                                       final Object value) {
            return null;
        }

        public void visitInnerClass(final String name,
                                    final String outerName,
                                    final String innerName,
                                    final int access) {
        }

        public MethodVisitor visitMethod(final int access,
                                         final String name,
                                         final String desc,
                                         final String signature,
                                         final String[] exceptions) {

            return this.methodName.equals( name ) ? new DumpMethodVisitor(this::setText) : null;
        }

        public void visitOuterClass(final String owner,
                                    final String name,
                                    final String desc) {
        }

        public void visitSource(final String source,
                                final String debug) {
        }

        public String getText() {
            return text;
        }

        public void setText( String text ) {
            this.text = text;
        }
    }

    static class DummyAnnotationVisitor
        extends
        AnnotationVisitor {

        public DummyAnnotationVisitor() {
            super(Opcodes.ASM7);
        }

        public void visit(final String name,
                          final Object value) {
        }

        public AnnotationVisitor visitAnnotation(final String name,
                                                 final String desc) {
            return new DummyAnnotationVisitor();
        }

        public AnnotationVisitor visitArray(final String name) {
            return new DummyAnnotationVisitor();
        }

        public void visitEnd() {
        }

        public void visitEnum(final String name,
                              final String desc,
                              final String value) {
        }

    }

}
