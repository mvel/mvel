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

package org.mvel3.util;

import org.mvel3.parser.Provider;

import java.io.IOException;

/**
 * Utility methods for working with Provider instances.
 */
public class ProviderUtils {
    
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    
    /**
     * Reads all content from a Provider and returns it as a String.
     * The Provider will be closed after reading.
     * 
     * @param provider the Provider to read from
     * @return the complete content as a String
     * @throws IOException if an I/O error occurs during reading
     */
    public static String readAll(Provider provider) throws IOException {
        return readAll(provider, DEFAULT_BUFFER_SIZE);
    }
    
    /**
     * Reads all content from a Provider and returns it as a String.
     * The Provider will be closed after reading.
     * 
     * @param provider the Provider to read from
     * @param bufferSize the size of the buffer to use for reading
     * @return the complete content as a String
     * @throws IOException if an I/O error occurs during reading
     */
    public static String readAll(Provider provider, int bufferSize) throws IOException {
        if (provider == null) {
            throw new IllegalArgumentException("Provider cannot be null");
        }
        
        StringBuilder content = new StringBuilder();
        char[] buffer = new char[bufferSize];
        
        try {
            int charsRead;
            while ((charsRead = provider.read(buffer, 0, buffer.length)) != -1) {
                content.append(buffer, 0, charsRead);
            }
        } finally {
            provider.close();
        }
        
        return content.toString();
    }
    
    /**
     * Reads all content from a Provider without closing it.
     * Useful when you need to read the content but want to manage the Provider lifecycle separately.
     * 
     * @param provider the Provider to read from
     * @return the complete content as a String
     * @throws IOException if an I/O error occurs during reading
     */
    public static String readAllWithoutClosing(Provider provider) throws IOException {
        return readAllWithoutClosing(provider, DEFAULT_BUFFER_SIZE);
    }
    
    /**
     * Reads all content from a Provider without closing it.
     * Useful when you need to read the content but want to manage the Provider lifecycle separately.
     * 
     * @param provider the Provider to read from
     * @param bufferSize the size of the buffer to use for reading
     * @return the complete content as a String
     * @throws IOException if an I/O error occurs during reading
     */
    public static String readAllWithoutClosing(Provider provider, int bufferSize) throws IOException {
        if (provider == null) {
            throw new IllegalArgumentException("Provider cannot be null");
        }
        
        StringBuilder content = new StringBuilder();
        char[] buffer = new char[bufferSize];
        
        int charsRead;
        while ((charsRead = provider.read(buffer, 0, buffer.length)) != -1) {
            content.append(buffer, 0, charsRead);
        }
        
        return content.toString();
    }
}