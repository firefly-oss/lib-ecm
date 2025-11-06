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
package com.firefly.core.ecm.domain.enums.audit;

/**
 * Audit event type enumeration for tracking ECM operations.
 */
public enum AuditEventType {
    
    // Document Operations
    DOCUMENT_CREATED,
    DOCUMENT_UPDATED,
    DOCUMENT_DELETED,
    DOCUMENT_VIEWED,
    DOCUMENT_DOWNLOADED,
    DOCUMENT_UPLOADED,
    DOCUMENT_MOVED,
    DOCUMENT_COPIED,
    DOCUMENT_CHECKED_OUT,
    DOCUMENT_CHECKED_IN,
    DOCUMENT_VERSION_CREATED,
    DOCUMENT_VERSION_DELETED,
    DOCUMENT_METADATA_UPDATED,
    DOCUMENT_STATUS_CHANGED,
    
    // Folder Operations
    FOLDER_CREATED,
    FOLDER_UPDATED,
    FOLDER_DELETED,
    FOLDER_MOVED,
    FOLDER_COPIED,
    FOLDER_PERMISSIONS_CHANGED,
    
    // Permission Operations
    PERMISSION_GRANTED,
    PERMISSION_REVOKED,
    PERMISSION_MODIFIED,
    PERMISSION_INHERITED,
    
    // Security Operations
    ACCESS_GRANTED,
    ACCESS_DENIED,
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    LOGOUT,
    SESSION_EXPIRED,
    
    // eSignature Operations
    ENVELOPE_CREATED,
    ENVELOPE_SENT,
    ENVELOPE_SIGNED,
    ENVELOPE_COMPLETED,
    ENVELOPE_DECLINED,
    ENVELOPE_VOIDED,
    ENVELOPE_EXPIRED,
    SIGNATURE_REQUESTED,
    SIGNATURE_COMPLETED,
    SIGNATURE_DECLINED,
    
    // System Operations
    SYSTEM_STARTUP,
    SYSTEM_SHUTDOWN,
    CONFIGURATION_CHANGED,
    ADAPTER_SELECTED,
    ADAPTER_FAILED,
    
    // Compliance Operations
    RETENTION_POLICY_APPLIED,
    LEGAL_HOLD_APPLIED,
    LEGAL_HOLD_REMOVED,
    DOCUMENT_ARCHIVED,
    DOCUMENT_PURGED,
    
    // Error Events
    OPERATION_FAILED,
    VALIDATION_FAILED,
    SECURITY_VIOLATION,
    SYSTEM_ERROR
}
