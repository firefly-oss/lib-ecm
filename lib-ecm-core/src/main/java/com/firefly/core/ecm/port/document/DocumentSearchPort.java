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
package com.firefly.core.ecm.port.document;

import com.firefly.core.ecm.domain.model.document.Document;
import com.firefly.core.ecm.domain.dto.search.DocumentSearchCriteria;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Port interface for document search and query operations.
 * Provides various search capabilities including full-text, metadata, and advanced queries.
 */
public interface DocumentSearchPort {
    
    /**
     * Perform full-text search across document content and metadata.
     *
     * @param query the search query
     * @param limit maximum number of results to return
     * @return Flux of documents matching the search criteria
     */
    Flux<Document> fullTextSearch(String query, Integer limit);
    
    /**
     * Search documents by name pattern.
     *
     * @param namePattern the name pattern (supports wildcards)
     * @param limit maximum number of results to return
     * @return Flux of documents matching the name pattern
     */
    Flux<Document> searchByName(String namePattern, Integer limit);
    
    /**
     * Search documents by metadata criteria.
     *
     * @param metadata the metadata key-value pairs to search for
     * @param limit maximum number of results to return
     * @return Flux of documents matching the metadata criteria
     */
    Flux<Document> searchByMetadata(Map<String, Object> metadata, Integer limit);
    
    /**
     * Search documents by tags.
     *
     * @param tags the tags to search for
     * @param matchAll whether all tags must match (true) or any tag (false)
     * @param limit maximum number of results to return
     * @return Flux of documents matching the tag criteria
     */
    Flux<Document> searchByTags(Set<String> tags, Boolean matchAll, Integer limit);
    
    /**
     * Search documents by MIME type.
     *
     * @param mimeType the MIME type to search for
     * @param limit maximum number of results to return
     * @return Flux of documents with the specified MIME type
     */
    Flux<Document> searchByMimeType(String mimeType, Integer limit);
    
    /**
     * Search documents by file extension.
     *
     * @param extension the file extension to search for
     * @param limit maximum number of results to return
     * @return Flux of documents with the specified extension
     */
    Flux<Document> searchByExtension(String extension, Integer limit);
    
    /**
     * Search documents by size range.
     *
     * @param minSize minimum file size in bytes (inclusive)
     * @param maxSize maximum file size in bytes (inclusive)
     * @param limit maximum number of results to return
     * @return Flux of documents within the size range
     */
    Flux<Document> searchBySize(Long minSize, Long maxSize, Integer limit);
    
    /**
     * Search documents by creation date range.
     *
     * @param fromDate start date (inclusive)
     * @param toDate end date (inclusive)
     * @param limit maximum number of results to return
     * @return Flux of documents created within the date range
     */
    Flux<Document> searchByCreationDate(Instant fromDate, Instant toDate, Integer limit);
    
    /**
     * Search documents by modification date range.
     *
     * @param fromDate start date (inclusive)
     * @param toDate end date (inclusive)
     * @param limit maximum number of results to return
     * @return Flux of documents modified within the date range
     */
    Flux<Document> searchByModificationDate(Instant fromDate, Instant toDate, Integer limit);
    
    /**
     * Search documents by creator.
     *
     * @param createdBy the user ID who created the documents
     * @param limit maximum number of results to return
     * @return Flux of documents created by the specified user
     */
    Flux<Document> searchByCreator(Long createdBy, Integer limit);
    
    /**
     * Advanced search with multiple criteria.
     *
     * @param searchCriteria the search criteria object
     * @return Flux of documents matching the search criteria
     */
    Flux<Document> advancedSearch(DocumentSearchCriteria searchCriteria);
    
    /**
     * Get search suggestions based on partial query.
     *
     * @param partialQuery the partial search query
     * @param limit maximum number of suggestions to return
     * @return Flux of search suggestions
     */
    Flux<String> getSearchSuggestions(String partialQuery, Integer limit);
    
    /**
     * Index a document for search.
     *
     * @param document the document to index
     * @return Mono indicating completion
     */
    Mono<Void> indexDocument(Document document);
    
    /**
     * Remove a document from search index.
     *
     * @param documentId the document ID to remove from index
     * @return Mono indicating completion
     */
    Mono<Void> removeFromIndex(UUID documentId);
    
    /**
     * Rebuild the entire search index.
     *
     * @return Mono indicating completion
     */
    Mono<Void> rebuildIndex();
}
