/*
 * Copyright 2025 Firefly Software Solutions Inc.
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

package com.firefly.core.ecm.adapter.local;

import com.firefly.core.ecm.adapter.AdapterFeature;
import com.firefly.core.ecm.adapter.EcmAdapter;
import com.firefly.core.ecm.domain.dto.search.DocumentSearchCriteria;
import com.firefly.core.ecm.domain.model.document.Document;
import com.firefly.core.ecm.port.document.DocumentSearchPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Local in-memory search adapter implementing DocumentSearchPort.
 * Stores indexed documents in-memory and supports basic filters.
 */
@Slf4j
@Component
@EcmAdapter(
        type = "local-search",
        description = "Local in-memory DocumentSearchPort adapter",
        supportedFeatures = { AdapterFeature.SEARCH, AdapterFeature.METADATA_SEARCH }
)
@ConditionalOnProperty(name = "firefly.ecm.search.enabled", havingValue = "true", matchIfMissing = false)
public class LocalDocumentSearchAdapter implements DocumentSearchPort {

    private final Map<UUID, Document> index = new ConcurrentHashMap<>();

    @Override
    public Flux<Document> fullTextSearch(String query, Integer limit) {
        String q = query == null ? "" : query.toLowerCase();
        return Flux.fromStream(index.values().stream()
                .filter(d -> contains(d.getName(), q) || contains(d.getDescription(), q))
                .limit(limitOrUnlimited(limit)));
    }

    @Override
    public Flux<Document> searchByName(String namePattern, Integer limit) {
        Pattern pattern = wildcardToPattern(namePattern);
        return Flux.fromStream(index.values().stream()
                .filter(d -> d.getName() != null && pattern.matcher(d.getName()).find())
                .limit(limitOrUnlimited(limit)));
    }

    @Override
    public Flux<Document> searchByMetadata(Map<String, Object> metadata, Integer limit) {
        if (metadata == null || metadata.isEmpty()) return Flux.empty();
        return Flux.fromStream(index.values().stream()
                .filter(d -> d.getMetadata() != null && metadata.entrySet().stream()
                        .allMatch(e -> Objects.equals(d.getMetadata().get(e.getKey()), e.getValue())))
                .limit(limitOrUnlimited(limit)));
    }

    @Override
    public Flux<Document> searchByTags(Set<String> tags, Boolean matchAll, Integer limit) {
        if (tags == null || tags.isEmpty()) return Flux.empty();
        return Flux.fromStream(index.values().stream()
                .filter(d -> d.getTags() != null && !d.getTags().isEmpty())
                .filter(d -> matchAll != null && matchAll
                        ? d.getTags().containsAll(tags)
                        : d.getTags().stream().anyMatch(tags::contains))
                .limit(limitOrUnlimited(limit)));
    }

    @Override
    public Flux<Document> searchByMimeType(String mimeType, Integer limit) {
        return Flux.fromStream(index.values().stream()
                .filter(d -> Objects.equals(d.getMimeType(), mimeType))
                .limit(limitOrUnlimited(limit)));
    }

    @Override
    public Flux<Document> searchByExtension(String extension, Integer limit) {
        return Flux.fromStream(index.values().stream()
                .filter(d -> Objects.equals(d.getExtension(), extension))
                .limit(limitOrUnlimited(limit)));
    }

    @Override
    public Flux<Document> searchBySize(Long minSize, Long maxSize, Integer limit) {
        return Flux.fromStream(index.values().stream()
                .filter(d -> between(d.getSize(), minSize, maxSize))
                .limit(limitOrUnlimited(limit)));
    }

    @Override
    public Flux<Document> searchByCreationDate(Instant fromDate, Instant toDate, Integer limit) {
        return Flux.fromStream(index.values().stream()
                .filter(d -> between(d.getCreatedAt(), fromDate, toDate))
                .limit(limitOrUnlimited(limit)));
    }

    @Override
    public Flux<Document> searchByModificationDate(Instant fromDate, Instant toDate, Integer limit) {
        return Flux.fromStream(index.values().stream()
                .filter(d -> between(d.getModifiedAt(), fromDate, toDate))
                .limit(limitOrUnlimited(limit)));
    }

    @Override
    public Flux<Document> searchByCreator(UUID createdBy, Integer limit) {
        return Flux.fromStream(index.values().stream()
                .filter(d -> Objects.equals(d.getCreatedBy(), createdBy))
                .limit(limitOrUnlimited(limit)));
    }

    @Override
    public Flux<Document> advancedSearch(DocumentSearchCriteria c) {
        if (c == null) return Flux.empty();
        Stream<Document> stream = index.values().stream();
        if (c.getQuery() != null) {
            String q = c.getQuery().toLowerCase();
            stream = stream.filter(d -> contains(d.getName(), q) || contains(d.getDescription(), q));
        }
        if (c.getNamePattern() != null) {
            Pattern p = wildcardToPattern(c.getNamePattern());
            stream = stream.filter(d -> d.getName() != null && p.matcher(d.getName()).find());
        }
        if (c.getFolderId() != null) stream = stream.filter(d -> Objects.equals(d.getFolderId(), c.getFolderId()));
        if (c.getOwnerId() != null) stream = stream.filter(d -> Objects.equals(d.getOwnerId(), c.getOwnerId()));
        if (c.getCreatedBy() != null) stream = stream.filter(d -> Objects.equals(d.getCreatedBy(), c.getCreatedBy()));
        if (c.getModifiedBy() != null) stream = stream.filter(d -> Objects.equals(d.getModifiedBy(), c.getModifiedBy()));
        if (c.getMimeType() != null) stream = stream.filter(d -> Objects.equals(d.getMimeType(), c.getMimeType()));
        if (c.getExtension() != null) stream = stream.filter(d -> Objects.equals(d.getExtension(), c.getExtension()));
        if (c.getMinSize() != null || c.getMaxSize() != null) stream = stream.filter(d -> between(d.getSize(), c.getMinSize(), c.getMaxSize()));
        if (c.getCreatedAfter() != null || c.getCreatedBefore() != null) stream = stream.filter(d -> between(d.getCreatedAt(), c.getCreatedAfter(), c.getCreatedBefore()));
        if (c.getModifiedAfter() != null || c.getModifiedBefore() != null) stream = stream.filter(d -> between(d.getModifiedAt(), c.getModifiedAfter(), c.getModifiedBefore()));
        if (c.getTags() != null && !c.getTags().isEmpty()) {
            boolean matchAll = Boolean.TRUE.equals(c.getMatchAllTags());
            stream = stream.filter(d -> d.getTags() != null && !d.getTags().isEmpty())
                    .filter(d -> matchAll ? d.getTags().containsAll(c.getTags()) : d.getTags().stream().anyMatch(c.getTags()::contains));
        }
        if (c.getMetadata() != null && !c.getMetadata().isEmpty()) {
            Map<String, Object> md = c.getMetadata();
            stream = stream.filter(d -> d.getMetadata() != null && md.entrySet().stream().allMatch(e -> Objects.equals(d.getMetadata().get(e.getKey()), e.getValue())));
        }
        if (c.getLimit() != null) stream = stream.limit(c.getLimit());
        return Flux.fromStream(stream);
    }

    @Override
    public Flux<String> getSearchSuggestions(String partialQuery, Integer limit) {
        String q = partialQuery == null ? "" : partialQuery.toLowerCase();
        Set<String> suggestions = new LinkedHashSet<>();
        index.values().forEach(d -> {
            if (d.getName() != null && d.getName().toLowerCase().contains(q)) suggestions.add(d.getName());
            if (d.getDescription() != null && d.getDescription().toLowerCase().contains(q)) suggestions.add(d.getDescription());
        });
        return Flux.fromStream(suggestions.stream().limit(limitOrUnlimited(limit)));
    }

    @Override
    public Mono<Void> indexDocument(Document document) {
        if (document == null || document.getId() == null) return Mono.empty();
        index.put(document.getId(), document);
        return Mono.empty();
    }

    @Override
    public Mono<Void> removeFromIndex(UUID documentId) {
        if (documentId != null) index.remove(documentId);
        return Mono.empty();
    }

    @Override
    public Mono<Void> rebuildIndex() {
        // No-op for in-memory index
        return Mono.empty();
    }

    private boolean contains(String text, String q) {
        return text != null && q != null && text.toLowerCase().contains(q);
    }

    private boolean between(Long value, Long min, Long max) {
        if (value == null) return false;
        if (min != null && value < min) return false;
        if (max != null && value > max) return false;
        return true;
    }

    private boolean between(Instant value, Instant from, Instant to) {
        if (value == null) return false;
        if (from != null && value.isBefore(from)) return false;
        if (to != null && value.isAfter(to)) return false;
        return true;
    }

    private long limitOrUnlimited(Integer limit) {
        return limit == null || limit <= 0 ? Long.MAX_VALUE : limit.longValue();
    }

    private Pattern wildcardToPattern(String wildcard) {
        if (wildcard == null || wildcard.isEmpty()) return Pattern.compile(".*");
        String regex = wildcard.replace("*", ".*").replace("?", ".");
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }
}