package org.mvel3;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * This defines the classes. It ensures only one class exists for an equal set of blocks.
 */
public class ClassManager {
    Map<String, Class<?>> classes;

    Map<ClassEntry, ClassEntry> entries;

    public ClassManager() {
        this.classes = new HashMap<>();
        this.entries = new HashMap<>();
    }

    public <T> Class<T> getClass(String name) {
        return (Class<T>) classes.get(name);
    }

    public Map<String, Class<?>> getClasses() {
        return classes;
    }

    public  void define(Map<String, byte[]> byteCode) {
        for (Map.Entry<String, byte[]> entry : byteCode.entrySet()) {
            try {
                ClassEntry newEntry = new ClassEntry(entry.getKey(), entry.getValue());
                ClassEntry existingEntry = entries.get(newEntry);
                if (existingEntry == null) {
                    entries.put(newEntry, newEntry);

                    MethodHandles.Lookup lookup = MethodHandles.lookup();
                    Class<?> c = lookup.defineHiddenClass(entry.getValue(), true).lookupClass();
                    classes.put(entry.getKey(), c);
                }
            } catch (Exception e) {
                throw new RuntimeException("Unbale to instantiate Lamda", e);
            }
        }
    }

    public static class ClassEntry {
        private String name;
        private byte[] bytes;

        private String byteCode;

        private byte[] hash;

        private int hashCode;

        public ClassEntry(String name, byte[] bytes) {
            this.name = name;
            this.bytes = bytes;

            // TODO in the future this should use JavaParser AST, to avoid the compilation steps.
            this.byteCode = MethodByteCodeExtractor.extract("eval", bytes);

            Murmur3F murmur = new Murmur3F();
            murmur.update(byteCode.getBytes());

            // This murmur hash provides better hashcodes and earlier exit of equals testing
            hash = murmur.getValueBytesBigEndian();
            hashCode = Arrays.hashCode(hash);
        }

        public String getName() {
            return name;
        }

        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ClassEntry that = (ClassEntry) o;

            // Murmur hash comparison has high chance of uniqueness and should provide early exit if not equal
            if (!Arrays.equals(hash, that.hash)) {
                return false;
            }
            return byteCode.equals(that.byteCode);
        }

        @Override
        public String toString() {
            return "ClassEntry{" +
                   "name='" + name + '\'' +
                   '}';
        }
    }


}
