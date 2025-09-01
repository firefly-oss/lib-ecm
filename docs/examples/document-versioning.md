# Document Versioning Examples

This document provides practical examples for managing document versions using the Firefly ECM Library's versioning capabilities.

## Table of Contents

1. [Basic Version Operations](#1-basic-version-operations)
2. [Version History Management](#2-version-history-management)
3. [Version Comparison](#3-version-comparison)
4. [Version Restoration](#4-version-restoration)
5. [Advanced Versioning Scenarios](#5-advanced-versioning-scenarios)
6. [Version Policies](#6-version-policies)

## 1. Basic Version Operations

### Creating Document Versions

```java
@Service
public class DocumentVersionService {
    
    @Autowired
    private DocumentVersionPort documentVersionPort;
    
    @Autowired
    private DocumentPort documentPort;
    
    @Autowired
    private DocumentContentPort contentPort;
    
    /**
     * Creates a new version of an existing document.
     */
    public Mono<DocumentVersion> createNewVersion(UUID documentId, byte[] newContent, 
                                                 String versionComment, Long createdBy) {
        return documentPort.getDocument(documentId)
            .flatMap(document -> {
                // Calculate new version number
                return getNextVersionNumber(documentId)
                    .flatMap(versionNumber -> {
                        DocumentVersion version = DocumentVersion.builder()
                            .documentId(documentId)
                            .versionNumber(versionNumber)
                            .versionComment(versionComment)
                            .size((long) newContent.length)
                            .checksum(calculateChecksum(newContent))
                            .checksumAlgorithm("SHA-256")
                            .createdBy(createdBy)
                            .createdAt(Instant.now())
                            .build();
                        
                        return documentVersionPort.createVersion(version, newContent);
                    });
            })
            .doOnSuccess(version -> 
                log.info("New version created: {} v{} ({})", 
                        documentId, version.getVersionNumber(), version.getId()))
            .doOnError(error -> 
                log.error("Failed to create version for document: {}", documentId, error));
    }
    
    /**
     * Creates a minor version (increments patch number).
     */
    public Mono<DocumentVersion> createMinorVersion(UUID documentId, byte[] content, 
                                                   String comment, Long createdBy) {
        return createVersionWithType(documentId, content, comment, createdBy, VersionType.MINOR);
    }
    
    /**
     * Creates a major version (increments major number).
     */
    public Mono<DocumentVersion> createMajorVersion(UUID documentId, byte[] content, 
                                                   String comment, Long createdBy) {
        return createVersionWithType(documentId, content, comment, createdBy, VersionType.MAJOR);
    }
    
    private Mono<DocumentVersion> createVersionWithType(UUID documentId, byte[] content, 
                                                       String comment, Long createdBy, VersionType type) {
        return getLatestVersion(documentId)
            .map(latestVersion -> calculateNextVersion(latestVersion.getVersionNumber(), type))
            .switchIfEmpty(Mono.just("1.0"))
            .flatMap(versionNumber -> {
                DocumentVersion version = DocumentVersion.builder()
                    .documentId(documentId)
                    .versionNumber(versionNumber)
                    .versionComment(comment)
                    .size((long) content.length)
                    .checksum(calculateChecksum(content))
                    .checksumAlgorithm("SHA-256")
                    .createdBy(createdBy)
                    .createdAt(Instant.now())
                    .build();
                
                return documentVersionPort.createVersion(version, content);
            });
    }
}
```

### Retrieving Document Versions

```java
/**
 * Gets the latest version of a document.
 */
public Mono<DocumentVersion> getLatestVersion(UUID documentId) {
    return documentVersionPort.getLatestVersion(documentId)
        .doOnNext(version -> 
            log.debug("Latest version for document {}: v{}", documentId, version.getVersionNumber()))
        .switchIfEmpty(Mono.error(new VersionNotFoundException("No versions found for document: " + documentId)));
}

/**
 * Gets a specific version by version number.
 */
public Mono<DocumentVersion> getVersion(UUID documentId, String versionNumber) {
    return documentVersionPort.getVersion(documentId, versionNumber)
        .doOnNext(version -> 
            log.debug("Retrieved version {} for document {}", versionNumber, documentId))
        .switchIfEmpty(Mono.error(new VersionNotFoundException(
            String.format("Version %s not found for document %s", versionNumber, documentId))));
}

/**
 * Gets all versions of a document.
 */
public Flux<DocumentVersion> getAllVersions(UUID documentId) {
    return documentVersionPort.getVersionHistory(documentId)
        .doOnNext(version -> 
            log.debug("Version in history: {} v{} ({})", 
                     documentId, version.getVersionNumber(), version.getCreatedAt()))
        .doOnComplete(() -> 
            log.debug("Retrieved complete version history for document: {}", documentId));
}
```

## 2. Version History Management

### Version History Display

```java
/**
 * Gets formatted version history for display.
 */
public Mono<List<VersionHistoryEntry>> getVersionHistoryForDisplay(UUID documentId) {
    return documentVersionPort.getVersionHistory(documentId)
        .map(this::createHistoryEntry)
        .collectList()
        .map(entries -> {
            // Sort by version number (latest first)
            entries.sort((a, b) -> compareVersions(b.getVersionNumber(), a.getVersionNumber()));
            return entries;
        })
        .doOnNext(history -> 
            log.debug("Version history for document {}: {} versions", documentId, history.size()));
}

private VersionHistoryEntry createHistoryEntry(DocumentVersion version) {
    return VersionHistoryEntry.builder()
        .versionId(version.getId())
        .versionNumber(version.getVersionNumber())
        .comment(version.getVersionComment())
        .size(version.getSize())
        .createdBy(version.getCreatedBy())
        .createdAt(version.getCreatedAt())
        .isCurrent(version.isCurrent())
        .build();
}

/**
 * Gets version statistics.
 */
public Mono<VersionStatistics> getVersionStatistics(UUID documentId) {
    return documentVersionPort.getVersionHistory(documentId)
        .collectList()
        .map(versions -> {
            long totalSize = versions.stream().mapToLong(DocumentVersion::getSize).sum();
            Optional<DocumentVersion> latest = versions.stream()
                .filter(DocumentVersion::isCurrent)
                .findFirst();
            
            return VersionStatistics.builder()
                .documentId(documentId)
                .totalVersions(versions.size())
                .totalSize(totalSize)
                .latestVersion(latest.map(DocumentVersion::getVersionNumber).orElse("N/A"))
                .oldestVersion(versions.stream()
                    .min((a, b) -> compareVersions(a.getVersionNumber(), b.getVersionNumber()))
                    .map(DocumentVersion::getVersionNumber)
                    .orElse("N/A"))
                .build();
        });
}
```

### Version Cleanup

```java
/**
 * Cleans up old versions based on retention policy.
 */
public Mono<Integer> cleanupOldVersions(UUID documentId, VersionRetentionPolicy policy) {
    return documentVersionPort.getVersionHistory(documentId)
        .collectList()
        .flatMap(versions -> {
            List<DocumentVersion> versionsToDelete = selectVersionsForDeletion(versions, policy);
            
            return Flux.fromIterable(versionsToDelete)
                .flatMap(version -> documentVersionPort.deleteVersion(version.getId()))
                .count()
                .map(Math::toIntExact);
        })
        .doOnSuccess(deletedCount -> 
            log.info("Cleaned up {} old versions for document {}", deletedCount, documentId));
}

private List<DocumentVersion> selectVersionsForDeletion(List<DocumentVersion> versions, 
                                                       VersionRetentionPolicy policy) {
    // Sort versions by creation date (oldest first)
    versions.sort(Comparator.comparing(DocumentVersion::getCreatedAt));
    
    List<DocumentVersion> toDelete = new ArrayList<>();
    
    switch (policy.getType()) {
        case KEEP_LAST_N:
            if (versions.size() > policy.getKeepCount()) {
                toDelete = versions.subList(0, versions.size() - policy.getKeepCount());
            }
            break;
            
        case KEEP_BY_AGE:
            Instant cutoffDate = Instant.now().minus(policy.getRetentionDays(), ChronoUnit.DAYS);
            toDelete = versions.stream()
                .filter(v -> v.getCreatedAt().isBefore(cutoffDate))
                .filter(v -> !v.isCurrent()) // Never delete current version
                .collect(Collectors.toList());
            break;
            
        case KEEP_MAJOR_VERSIONS:
            toDelete = versions.stream()
                .filter(v -> !isMajorVersion(v.getVersionNumber()))
                .filter(v -> !v.isCurrent())
                .collect(Collectors.toList());
            break;
    }
    
    return toDelete;
}
```

## 3. Version Comparison

### Content Comparison

```java
/**
 * Compares content between two versions.
 */
public Mono<VersionComparison> compareVersions(UUID documentId, String version1, String version2) {
    Mono<DocumentVersion> v1 = getVersion(documentId, version1);
    Mono<DocumentVersion> v2 = getVersion(documentId, version2);
    
    return Mono.zip(v1, v2)
        .flatMap(tuple -> {
            DocumentVersion ver1 = tuple.getT1();
            DocumentVersion ver2 = tuple.getT2();
            
            // Get content for both versions
            Mono<byte[]> content1 = contentPort.getVersionContent(ver1.getId());
            Mono<byte[]> content2 = contentPort.getVersionContent(ver2.getId());
            
            return Mono.zip(content1, content2)
                .map(contentTuple -> createComparison(ver1, ver2, contentTuple.getT1(), contentTuple.getT2()));
        })
        .doOnNext(comparison -> 
            log.debug("Compared versions {} and {} for document {}: {} differences", 
                     version1, version2, documentId, comparison.getDifferences().size()));
}

private VersionComparison createComparison(DocumentVersion v1, DocumentVersion v2, 
                                         byte[] content1, byte[] content2) {
    List<ContentDifference> differences = new ArrayList<>();
    
    // Size comparison
    if (!Objects.equals(v1.getSize(), v2.getSize())) {
        differences.add(ContentDifference.builder()
            .type(DifferenceType.SIZE_CHANGE)
            .description(String.format("Size changed from %d to %d bytes", v1.getSize(), v2.getSize()))
            .build());
    }
    
    // Checksum comparison
    if (!Objects.equals(v1.getChecksum(), v2.getChecksum())) {
        differences.add(ContentDifference.builder()
            .type(DifferenceType.CONTENT_CHANGE)
            .description("Content has been modified")
            .build());
    }
    
    // For text files, perform detailed comparison
    if (isTextFile(v1) && isTextFile(v2)) {
        differences.addAll(compareTextContent(content1, content2));
    }
    
    return VersionComparison.builder()
        .version1(v1)
        .version2(v2)
        .differences(differences)
        .identical(differences.isEmpty())
        .comparedAt(Instant.now())
        .build();
}
```

### Metadata Comparison

```java
/**
 * Compares metadata between versions.
 */
public Mono<MetadataComparison> compareVersionMetadata(UUID documentId, String version1, String version2) {
    return Mono.zip(getVersion(documentId, version1), getVersion(documentId, version2))
        .map(tuple -> {
            DocumentVersion v1 = tuple.getT1();
            DocumentVersion v2 = tuple.getT2();
            
            List<MetadataChange> changes = new ArrayList<>();
            
            // Compare version comments
            if (!Objects.equals(v1.getVersionComment(), v2.getVersionComment())) {
                changes.add(MetadataChange.builder()
                    .field("versionComment")
                    .oldValue(v1.getVersionComment())
                    .newValue(v2.getVersionComment())
                    .build());
            }
            
            // Compare creation times
            changes.add(MetadataChange.builder()
                .field("createdAt")
                .oldValue(v1.getCreatedAt().toString())
                .newValue(v2.getCreatedAt().toString())
                .build());
            
            // Compare creators
            if (!Objects.equals(v1.getCreatedBy(), v2.getCreatedBy())) {
                changes.add(MetadataChange.builder()
                    .field("createdBy")
                    .oldValue(v1.getCreatedBy().toString())
                    .newValue(v2.getCreatedBy().toString())
                    .build());
            }
            
            return MetadataComparison.builder()
                .version1(version1)
                .version2(version2)
                .changes(changes)
                .build();
        });
}
```

## 4. Version Restoration

### Restoring Previous Versions

```java
/**
 * Restores a previous version as the current version.
 */
public Mono<DocumentVersion> restoreVersion(UUID documentId, String versionToRestore, 
                                          String restoreComment, Long restoredBy) {
    return getVersion(documentId, versionToRestore)
        .flatMap(versionToRestore -> {
            // Get the content of the version to restore
            return contentPort.getVersionContent(versionToRestore.getId())
                .flatMap(content -> {
                    // Create a new version with the restored content
                    String comment = String.format("Restored from version %s: %s", 
                                                  versionToRestore.getVersionNumber(), 
                                                  restoreComment != null ? restoreComment : "Version restored");
                    
                    return createNewVersion(documentId, content, comment, restoredBy);
                });
        })
        .doOnSuccess(restoredVersion -> 
            log.info("Version {} restored for document {} as new version {}", 
                    versionToRestore, documentId, restoredVersion.getVersionNumber()));
}

/**
 * Creates a branch from a specific version.
 */
public Mono<DocumentVersion> createBranchFromVersion(UUID documentId, String sourceVersion, 
                                                   String branchName, Long createdBy) {
    return getVersion(documentId, sourceVersion)
        .flatMap(sourceVer -> {
            return contentPort.getVersionContent(sourceVer.getId())
                .flatMap(content -> {
                    String branchVersionNumber = sourceVersion + "-" + branchName;
                    String comment = String.format("Branch '%s' created from version %s", 
                                                  branchName, sourceVersion);
                    
                    DocumentVersion branchVersion = DocumentVersion.builder()
                        .documentId(documentId)
                        .versionNumber(branchVersionNumber)
                        .versionComment(comment)
                        .size((long) content.length)
                        .checksum(calculateChecksum(content))
                        .checksumAlgorithm("SHA-256")
                        .createdBy(createdBy)
                        .createdAt(Instant.now())
                        .isBranch(true)
                        .branchName(branchName)
                        .parentVersionId(sourceVer.getId())
                        .build();
                    
                    return documentVersionPort.createVersion(branchVersion, content);
                });
        })
        .doOnSuccess(branch -> 
            log.info("Branch '{}' created from version {} for document {}", 
                    branchName, sourceVersion, documentId));
}
```

## 5. Advanced Versioning Scenarios

### Automatic Versioning

```java
/**
 * Automatically creates versions based on content changes.
 */
public Mono<DocumentVersion> updateDocumentWithAutoVersioning(UUID documentId, byte[] newContent, 
                                                            Long modifiedBy, VersioningPolicy policy) {
    return documentPort.getDocument(documentId)
        .flatMap(document -> {
            // Get current content
            return contentPort.getContent(documentId)
                .flatMap(currentContent -> {
                    // Check if content has changed
                    if (Arrays.equals(currentContent, newContent)) {
                        log.debug("No content changes detected for document {}", documentId);
                        return getLatestVersion(documentId);
                    }
                    
                    // Determine version type based on policy
                    return determineVersionType(currentContent, newContent, policy)
                        .flatMap(versionType -> {
                            String comment = generateAutoVersionComment(currentContent, newContent, versionType);
                            return createVersionWithType(documentId, newContent, comment, modifiedBy, versionType);
                        });
                });
        });
}

private Mono<VersionType> determineVersionType(byte[] oldContent, byte[] newContent, VersioningPolicy policy) {
    return Mono.fromCallable(() -> {
        double changePercentage = calculateChangePercentage(oldContent, newContent);
        
        if (changePercentage > policy.getMajorChangeThreshold()) {
            return VersionType.MAJOR;
        } else if (changePercentage > policy.getMinorChangeThreshold()) {
            return VersionType.MINOR;
        } else {
            return VersionType.PATCH;
        }
    });
}
```

### Version Merging

```java
/**
 * Merges changes from a branch back to the main version line.
 */
public Mono<DocumentVersion> mergeBranch(UUID documentId, String branchName, 
                                       String mergeComment, Long mergedBy) {
    return documentVersionPort.getBranchVersions(documentId, branchName)
        .collectList()
        .flatMap(branchVersions -> {
            if (branchVersions.isEmpty()) {
                return Mono.error(new VersionNotFoundException("Branch not found: " + branchName));
            }
            
            // Get the latest version in the branch
            DocumentVersion latestBranchVersion = branchVersions.stream()
                .max(Comparator.comparing(DocumentVersion::getCreatedAt))
                .orElseThrow();
            
            // Get the content from the latest branch version
            return contentPort.getVersionContent(latestBranchVersion.getId())
                .flatMap(branchContent -> {
                    String comment = String.format("Merged branch '%s': %s", 
                                                  branchName, 
                                                  mergeComment != null ? mergeComment : "Branch merged");
                    
                    return createMajorVersion(documentId, branchContent, comment, mergedBy);
                });
        })
        .doOnSuccess(mergedVersion -> 
            log.info("Branch '{}' merged for document {} as version {}", 
                    branchName, documentId, mergedVersion.getVersionNumber()));
}
```

## 6. Version Policies

### Version Retention Policies

```java
/**
 * Applies version retention policies to a document.
 */
public Mono<Void> applyRetentionPolicy(UUID documentId, VersionRetentionPolicy policy) {
    return cleanupOldVersions(documentId, policy)
        .flatMap(deletedCount -> {
            // Log retention policy application
            return documentVersionPort.logRetentionPolicyApplication(
                documentId, policy, deletedCount, Instant.now());
        })
        .then()
        .doOnSuccess(unused -> 
            log.info("Retention policy applied to document {}: {}", documentId, policy));
}

/**
 * Sets up automatic version cleanup.
 */
@Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
public void performScheduledVersionCleanup() {
    log.info("Starting scheduled version cleanup");
    
    // Get all documents with version retention policies
    documentPort.findAllDocuments()
        .filter(document -> document.getVersionRetentionPolicy() != null)
        .flatMap(document -> 
            applyRetentionPolicy(document.getId(), document.getVersionRetentionPolicy())
                .onErrorContinue((error, doc) -> 
                    log.error("Failed to apply retention policy for document {}", 
                             ((Document) doc).getId(), error)))
        .doOnComplete(() -> log.info("Scheduled version cleanup completed"))
        .subscribe();
}
```

These examples demonstrate comprehensive document versioning capabilities using the Firefly ECM Library's versioning ports and domain models. All examples are based on the actual port interfaces and follow reactive programming best practices.
