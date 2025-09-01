/*
 * Copyright 2024 Firefly Software Solutions Inc.
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
package com.firefly.core.ecm.adapter;

import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark ECM adapter implementations.
 * Adapters are automatically registered and can be selected via configuration.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface EcmAdapter {
    
    /**
     * The adapter type identifier (e.g., "s3", "azure-blob", "minio", "alfresco").
     * This value is used in configuration to select the adapter.
     *
     * @return the adapter type
     */
    String type();
    
    /**
     * The adapter priority when multiple adapters of the same type are available.
     * Higher values have higher priority.
     *
     * @return the adapter priority (default: 0)
     */
    int priority() default 0;
    
    /**
     * Whether this adapter is enabled by default.
     *
     * @return true if enabled by default, false otherwise (default: true)
     */
    boolean enabled() default true;
    
    /**
     * The adapter description for documentation purposes.
     *
     * @return the adapter description (default: empty string)
     */
    String description() default "";
    
    /**
     * The adapter version.
     *
     * @return the adapter version (default: "1.0.0")
     */
    String version() default "1.0.0";
    
    /**
     * The vendor/provider of the adapter.
     *
     * @return the adapter vendor (default: "Firefly Software Solutions Inc.")
     */
    String vendor() default "Firefly Software Solutions Inc.";
    
    /**
     * Required configuration properties for this adapter.
     * Used for validation and documentation.
     *
     * @return array of required property names (default: empty array)
     */
    String[] requiredProperties() default {};
    
    /**
     * Optional configuration properties for this adapter.
     * Used for documentation purposes.
     *
     * @return array of optional property names (default: empty array)
     */
    String[] optionalProperties() default {};
    
    /**
     * Supported features by this adapter.
     * Used to determine adapter capabilities.
     *
     * @return array of supported features (default: empty array)
     */
    AdapterFeature[] supportedFeatures() default {};
    
    /**
     * Minimum required configuration profile for this adapter.
     *
     * @return the minimum profile (default: BASIC)
     */
    AdapterProfile minimumProfile() default AdapterProfile.BASIC;
}
