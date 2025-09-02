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
package com.firefly.core.ecm.domain.model.document;

import com.firefly.core.ecm.domain.enums.document.ContentType;
import com.firefly.core.ecm.domain.enums.document.DocumentStatus;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Core document entity representing a document in the Firefly ECM system.
 *
 * <p>This immutable domain entity encapsulates all essential information about a document,
 * including its metadata, content properties, security attributes, and lifecycle information.
 * The class follows Firefly's architectural standards by using UUIDs for entity identifiers
 * and Long values for user/owner references.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Immutable design using Lombok's {@code @Data} and {@code @Builder}</li>
 *   <li>JSON serialization support via {@code @Jacksonized}</li>
 *   <li>Comprehensive metadata and audit trail support</li>
 *   <li>Security and compliance features (encryption, legal hold, retention)</li>
 *   <li>Flexible tagging and categorization system</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * Document document = Document.builder()
 *     .id(UUID.randomUUID())
 *     .name("contract.pdf")
 *     .mimeType("application/pdf")
 *     .size(1024L)
 *     .status(DocumentStatus.ACTIVE)
 *     .ownerId(UUID.randomUUID())
 *     .createdAt(Instant.now())
 *     .build();
 * </pre>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 * @see DocumentStatus
 * @see ContentType
 * @see DocumentVersion
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class Document {

    /**
     * Unique document identifier using Long format.
     *
     * <p>This serves as the primary key for the document and is used for all
     * document references throughout the ECM system. The Long ensures global
     * uniqueness and prevents ID collisions across distributed systems.</p>
     */
    private final UUID id;

    /**
     * Human-readable document name or title.
     *
     * <p>This is typically the filename without path information, but can be
     * customized by users. It's used for display purposes and search operations.</p>
     */
    private final String name;

    /**
     * Optional detailed description of the document.
     *
     * <p>Provides additional context about the document's purpose, content,
     * or significance. This field is searchable and helps with document discovery.</p>
     */
    private final String description;

    /**
     * MIME type indicating the document's content format.
     *
     * <p>Standard MIME type (e.g., "application/pdf", "text/plain", "image/jpeg")
     * that describes the document's format. Used for content type validation,
     * rendering decisions, and security policies.</p>
     */
    private final String mimeType;

    /**
     * File extension extracted from the original filename.
     *
     * <p>The file extension (without the dot) that helps identify the document
     * type and is used for validation against allowed/blocked extension lists.</p>
     */
    private final String extension;

    /**
     * Document size in bytes.
     *
     * <p>The exact size of the document content in bytes. Used for storage
     * calculations, transfer optimization, and size limit enforcement.</p>
     */
    private final Long size;

    /**
     * Internal storage path in the underlying storage system.
     *
     * <p>The actual path or key where the document content is stored in the
     * backend storage system (e.g., S3 key, file system path). This is
     * adapter-specific and should not be exposed to end users.</p>
     */
    private final String storagePath;

    /**
     * Cryptographic checksum for content integrity verification.
     *
     * <p>Hash value computed from the document content using the algorithm
     * specified in {@link #checksumAlgorithm}. Used to detect corruption
     * and verify content integrity during transfers and storage.</p>
     */
    private final String checksum;

    /**
     * Algorithm used to compute the document checksum.
     *
     * <p>The cryptographic hash algorithm (e.g., "SHA-256", "MD5") used to
     * generate the checksum. SHA-256 is recommended for security reasons.</p>
     */
    private final String checksumAlgorithm;

    /**
     * Current version number of the document.
     *
     * <p>Incremental version number starting from 1. Each time the document
     * content is updated, this number is incremented. Used for version
     * control and conflict resolution.</p>
     */
    private final Integer version;

    /**
     * Current lifecycle status of the document.
     *
     * <p>Indicates the document's state in its lifecycle (e.g., DRAFT, ACTIVE,
     * ARCHIVED, DELETED). Controls visibility and available operations.</p>
     *
     * @see DocumentStatus
     */
    private final DocumentStatus status;

    /**
     * Long of the parent folder containing this document.
     *
     * <p>Reference to the folder where this document is located. Can be null
     * for documents in the root folder. Used for hierarchical organization
     * and permission inheritance.</p>
     */
    private final UUID folderId;

    /**
     * User ID of the document owner.
     *
     * <p>Long identifier of the user who owns this document. The owner typically
     * has full control over the document including the ability to delete it
     * and modify permissions.</p>
     */
    private final UUID ownerId;
    
    /**
     * User ID of the person who originally created the document.
     *
     * <p>Long identifier of the user who first uploaded or created this document
     * in the ECM system. This value is immutable and provides audit trail
     * information for compliance and tracking purposes.</p>
     */
    private final UUID createdBy;

    /**
     * User ID of the person who last modified the document.
     *
     * <p>Long identifier of the user who most recently updated the document
     * content or metadata. Updated automatically when document changes are made.</p>
     */
    private final UUID modifiedBy;

    /**
     * Timestamp when the document was originally created.
     *
     * <p>Immutable timestamp marking when the document was first added to the
     * ECM system. Used for audit trails, sorting, and retention policy calculations.</p>
     */
    private final Instant createdAt;

    /**
     * Timestamp when the document was last modified.
     *
     * <p>Updated automatically whenever the document content or metadata changes.
     * Used for change tracking, cache invalidation, and synchronization.</p>
     */
    private final Instant modifiedAt;

    /**
     * Optional expiration timestamp for the document.
     *
     * <p>When set, indicates when the document should be considered expired.
     * Expired documents may be automatically archived or deleted based on
     * system policies. Can be null for documents without expiration.</p>
     */
    private final Instant expiresAt;

    /**
     * Flexible metadata storage as key-value pairs.
     *
     * <p>Allows storage of arbitrary document metadata beyond the standard fields.
     * Values can be of various types (String, Number, Boolean, etc.) and are
     * typically used for custom properties, business-specific data, and
     * integration with external systems.</p>
     */
    private final Map<String, Object> metadata;

    /**
     * Set of tags for document categorization and discovery.
     *
     * <p>Free-form text tags that help categorize and organize documents.
     * Tags are searchable and can be used for filtering, grouping, and
     * automated processing rules.</p>
     */
    private final java.util.Set<String> tags;

    /**
     * Flag indicating whether the document content is encrypted.
     *
     * <p>When true, indicates that the document content has been encrypted
     * either at rest or in transit. This affects how the content is handled
     * during storage, retrieval, and processing operations.</p>
     */
    private final Boolean encrypted;

    /**
     * Classification of the document's content type.
     *
     * <p>High-level categorization of the document content (e.g., TEXT, IMAGE,
     * VIDEO, SPREADSHEET). Used for processing decisions, viewer selection,
     * and feature availability.</p>
     *
     * @see ContentType
     */
    private final ContentType contentType;

    /**
     * Identifier of the retention policy applied to this document.
     *
     * <p>References a retention policy that determines how long the document
     * should be kept and when it can be deleted. Used for compliance with
     * regulatory requirements and organizational policies.</p>
     */
    private final String retentionPolicyId;

    /**
     * Flag indicating whether the document is under legal hold.
     *
     * <p>When true, prevents the document from being deleted or modified
     * regardless of retention policies. Used for litigation support and
     * regulatory compliance. Legal holds override normal retention rules.</p>
     */
    private final Boolean legalHold;
}
