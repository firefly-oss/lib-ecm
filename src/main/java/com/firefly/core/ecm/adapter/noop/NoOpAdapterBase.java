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

/**
 * Base class for no-op adapter implementations that provides common logging functionality.
 *
 * <p>This class serves as the foundation for all no-op adapters in the ECM system.
 * No-op adapters are used as fallbacks when no real adapter implementations are
 * available, allowing the application to start successfully while providing clear
 * feedback about missing functionality.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Consistent warning logging when methods are invoked</li>
 *   <li>Standardized error messages for missing functionality</li>
 *   <li>Support for graceful degradation patterns</li>
 * </ul>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 */
@Slf4j
public abstract class NoOpAdapterBase {

    /** The name of the adapter type for logging purposes. */
    private final String adapterType;

    /**
     * Constructs a new no-op adapter base with the specified adapter type.
     *
     * @param adapterType the type name of the adapter (e.g., "DocumentPort", "ContentPort")
     */
    protected NoOpAdapterBase(String adapterType) {
        this.adapterType = adapterType;
    }

    /**
     * Logs a warning message indicating that a method was called on a no-op adapter.
     *
     * <p>This method should be called at the beginning of each no-op method implementation
     * to provide clear feedback that the functionality is not available.</p>
     *
     * @param methodName the name of the method that was called
     */
    protected void logNoOpWarning(String methodName) {
        log.warn("Method '{}' called on no-op {} adapter. " +
                "This functionality is not available because no suitable adapter implementation was found. " +
                "To enable this feature, configure an appropriate adapter in your application properties.",
                methodName, adapterType);
    }

    /**
     * Logs a warning message with additional context about the missing functionality.
     *
     * @param methodName the name of the method that was called
     * @param context additional context about what functionality is missing
     */
    protected void logNoOpWarning(String methodName, String context) {
        log.warn("Method '{}' called on no-op {} adapter. {}. " +
                "To enable this feature, configure an appropriate adapter in your application properties.",
                methodName, adapterType, context);
    }

    /**
     * Returns the adapter type name for identification purposes.
     *
     * @return the adapter type name
     */
    public String getAdapterType() {
        return adapterType;
    }
}
