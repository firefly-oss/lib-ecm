/*
 * Copyright 2025 Firefly Software Solutions Inc
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


package com.firefly.core.ecm.adapter.noop;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Generic no-op adapter implementation that uses dynamic proxies to handle any port interface.
 *
 * <p>This class provides a flexible way to create no-op implementations for any ECM port interface
 * without having to manually implement each interface. It uses Java's dynamic proxy mechanism
 * to intercept method calls and provide appropriate no-op responses.</p>
 *
 * <p>Response patterns:</p>
 * <ul>
 *   <li>Methods returning {@link Mono}: Return empty Mono or error for modifications</li>
 *   <li>Methods returning {@link Flux}: Return empty Flux</li>
 *   <li>Methods returning {@link Boolean}: Return false for existence checks, true for permissions</li>
 *   <li>Methods returning {@link String}: Return adapter name for getAdapterName(), empty for others</li>
 *   <li>Other return types: Return null or appropriate defaults</li>
 * </ul>
 *
 * @param <T> the port interface type
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 * @see NoOpAdapterBase
 */
@Slf4j
public class NoOpGenericAdapter<T> extends NoOpAdapterBase implements InvocationHandler {

    private final Class<T> interfaceClass;
    private final T proxyInstance;

    /**
     * Creates a new generic no-op adapter for the specified interface.
     *
     * @param adapterType the adapter type name for logging
     * @param interfaceClass the port interface class
     */
    @SuppressWarnings("unchecked")
    public NoOpGenericAdapter(String adapterType, Class<T> interfaceClass) {
        super(adapterType);
        this.interfaceClass = interfaceClass;
        this.proxyInstance = (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                this
        );
    }

    /**
     * Returns the proxy instance that implements the port interface.
     *
     * @return the proxy instance
     */
    public T getProxy() {
        return proxyInstance;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        Class<?> returnType = method.getReturnType();

        // Handle getAdapterName method specifically
        if ("getAdapterName".equals(methodName)) {
            return "NoOp" + interfaceClass.getSimpleName() + "Adapter";
        }

        // Log the no-op warning
        logNoOpWarning(methodName);

        // Determine appropriate response based on return type and method name
        if (returnType == Mono.class) {
            return handleMonoReturn(methodName);
        } else if (returnType == Flux.class) {
            return Flux.empty();
        } else if (returnType == Boolean.class || returnType == boolean.class) {
            return handleBooleanReturn(methodName);
        } else if (returnType == String.class) {
            return null; // Return null for string methods (except getAdapterName)
        } else if (returnType == Long.class || returnType == long.class) {
            return 0L;
        } else if (returnType == Integer.class || returnType == int.class) {
            return 0;
        } else if (returnType == void.class || returnType == Void.class) {
            return null;
        } else {
            return null; // Default for other types
        }
    }

    /**
     * Handles return values for methods that return Mono.
     *
     * @param methodName the name of the method being called
     * @return appropriate Mono response
     */
    private Mono<?> handleMonoReturn(String methodName) {
        String lowerMethodName = methodName.toLowerCase();

        // For boolean permission/access methods, return true (permissive default) - CHECK FIRST
        if (lowerMethodName.startsWith("can") ||
            lowerMethodName.contains("access") ||
            lowerMethodName.contains("permission") ||
            lowerMethodName.contains("allow")) {
            return Mono.just(true);
        }

        // For boolean existence/status checks, return false
        if (lowerMethodName.startsWith("exists") ||
            lowerMethodName.startsWith("has") ||
            lowerMethodName.startsWith("is") ||
            lowerMethodName.startsWith("contains")) {
            return Mono.just(false);
        }

        // For modification operations, return error
        if (lowerMethodName.startsWith("create") ||
            lowerMethodName.startsWith("update") ||
            lowerMethodName.startsWith("delete") ||
            lowerMethodName.startsWith("save") ||
            lowerMethodName.startsWith("store") ||
            lowerMethodName.startsWith("apply") ||
            lowerMethodName.startsWith("remove") ||
            lowerMethodName.startsWith("set") ||
            lowerMethodName.startsWith("move") ||
            lowerMethodName.startsWith("copy") ||
            lowerMethodName.contains("delete") ||
            lowerMethodName.contains("store") ||
            lowerMethodName.contains("encrypt")) {

            return Mono.error(new UnsupportedOperationException(
                    String.format("%s operation is not available. No %s adapter is configured.",
                            methodName, getAdapterType())));
        }

        // For query operations, return empty
        return Mono.empty();
    }

    /**
     * Handles return values for methods that return boolean.
     *
     * @param methodName the name of the method being called
     * @return appropriate boolean response
     */
    private boolean handleBooleanReturn(String methodName) {
        String lowerMethodName = methodName.toLowerCase();

        // For permission/access methods, return true (permissive default)
        if (lowerMethodName.startsWith("can") ||
            lowerMethodName.contains("access") ||
            lowerMethodName.contains("permission") ||
            lowerMethodName.contains("allow")) {
            return true;
        }

        // For existence/status checks, return false
        if (lowerMethodName.startsWith("exists") ||
            lowerMethodName.startsWith("has") ||
            lowerMethodName.startsWith("is") ||
            lowerMethodName.startsWith("contains")) {
            return false;
        }

        // Default to false for other boolean methods
        return false;
    }
}
