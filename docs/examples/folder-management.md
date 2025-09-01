# Folder Management Examples

This document provides practical examples for managing folders and hierarchical structures using the Firefly ECM Library.

## Table of Contents

1. [Basic Folder Operations](#1-basic-folder-operations)
2. [Hierarchical Folder Structure](#2-hierarchical-folder-structure)
3. [Folder Permissions](#3-folder-permissions)
4. [Folder Navigation](#4-folder-navigation)
5. [Bulk Operations](#5-bulk-operations)
6. [Advanced Scenarios](#6-advanced-scenarios)

## 1. Basic Folder Operations

### Creating Folders

```java
@Service
public class FolderService {
    
    @Autowired
    private FolderPort folderPort;
    
    /**
     * Creates a new folder.
     */
    public Mono<Folder> createFolder(String name, String description, UUID parentId) {
        Folder folder = Folder.builder()
            .name(name)
            .description(description)
            .parentId(parentId)
            .createdAt(Instant.now())
            .build();
        
        return folderPort.createFolder(folder)
            .doOnSuccess(created -> 
                log.info("Folder created: {} (ID: {})", created.getName(), created.getId()))
            .doOnError(error -> 
                log.error("Failed to create folder: {}", name, error));
    }
    
    /**
     * Creates a folder at a specific path.
     */
    public Mono<Folder> createFolderAtPath(String path) {
        return folderPort.createFolderPath(path)
            .doOnSuccess(folder -> 
                log.info("Folder path created: {} (ID: {})", path, folder.getId()));
    }
}
```

### Retrieving Folders

```java
/**
 * Gets folder by ID.
 */
public Mono<Folder> getFolder(UUID folderId) {
    return folderPort.getFolder(folderId)
        .doOnNext(folder -> 
            log.debug("Retrieved folder: {} ({})", folder.getName(), folder.getPath()))
        .switchIfEmpty(Mono.error(new FolderNotFoundException("Folder not found: " + folderId)));
}

/**
 * Gets folder by path.
 */
public Mono<Folder> getFolderByPath(String path) {
    return folderPort.getFolderByPath(path)
        .doOnNext(folder -> 
            log.debug("Found folder at path: {} (ID: {})", path, folder.getId()))
        .switchIfEmpty(Mono.error(new FolderNotFoundException("Folder not found at path: " + path)));
}
```

### Updating Folders

```java
/**
 * Updates folder metadata.
 */
public Mono<Folder> updateFolder(UUID folderId, String newName, String newDescription) {
    return folderPort.getFolder(folderId)
        .flatMap(folder -> {
            Folder updatedFolder = folder.toBuilder()
                .name(newName != null ? newName : folder.getName())
                .description(newDescription != null ? newDescription : folder.getDescription())
                .modifiedAt(Instant.now())
                .build();
            
            return folderPort.updateFolder(updatedFolder);
        })
        .doOnSuccess(updated -> 
            log.info("Folder updated: {} (ID: {})", updated.getName(), updated.getId()));
}
```

### Deleting Folders

```java
/**
 * Deletes a folder (must be empty).
 */
public Mono<Void> deleteFolder(UUID folderId) {
    return folderPort.deleteFolder(folderId)
        .doOnSuccess(unused -> 
            log.info("Folder deleted: {}", folderId))
        .doOnError(error -> 
            log.error("Failed to delete folder: {}", folderId, error));
}

/**
 * Deletes a folder and all its contents recursively.
 */
public Mono<Void> deleteFolderRecursively(UUID folderId) {
    return folderPort.deleteFolderRecursively(folderId)
        .doOnSuccess(unused -> 
            log.info("Folder and contents deleted recursively: {}", folderId))
        .doOnError(error -> 
            log.error("Failed to delete folder recursively: {}", folderId, error));
}
```

## 2. Hierarchical Folder Structure

### Creating Folder Hierarchies

```java
/**
 * Creates a complete folder hierarchy.
 */
public Mono<Folder> createProjectStructure(String projectName) {
    // Create root project folder
    return createFolder(projectName, "Project root folder", null)
        .flatMap(projectFolder -> {
            // Create subfolders
            List<Mono<Folder>> subfolderCreations = Arrays.asList(
                createFolder("Documents", "Project documents", projectFolder.getId()),
                createFolder("Images", "Project images", projectFolder.getId()),
                createFolder("Archives", "Archived files", projectFolder.getId())
            );
            
            return Flux.merge(subfolderCreations)
                .then(Mono.just(projectFolder));
        })
        .doOnSuccess(project -> 
            log.info("Project structure created: {}", projectName));
}

/**
 * Creates nested folder structure using paths.
 */
public Mono<Folder> createNestedStructure() {
    List<String> paths = Arrays.asList(
        "/Company/HR/Policies",
        "/Company/HR/Employee Records",
        "/Company/Finance/Budgets",
        "/Company/Finance/Reports",
        "/Company/Legal/Contracts",
        "/Company/Legal/Compliance"
    );
    
    return Flux.fromIterable(paths)
        .flatMap(this::createFolderAtPath)
        .then(getFolderByPath("/Company"))
        .doOnSuccess(company -> 
            log.info("Company structure created with {} departments", paths.size()));
}
```

### Navigating Folder Hierarchies

```java
/**
 * Gets the complete folder hierarchy starting from a root folder.
 */
public Mono<FolderTree> getFolderHierarchy(UUID rootFolderId) {
    return folderPort.getFolder(rootFolderId)
        .flatMap(rootFolder -> {
            return buildFolderTree(rootFolder);
        });
}

private Mono<FolderTree> buildFolderTree(Folder folder) {
    return folderPort.getSubfolders(folder.getId())
        .flatMap(this::buildFolderTree)
        .collectList()
        .map(children -> FolderTree.builder()
            .folder(folder)
            .children(children)
            .build());
}

/**
 * Gets folder breadcrumb path.
 */
public Mono<List<Folder>> getFolderBreadcrumb(UUID folderId) {
    return folderPort.getFolder(folderId)
        .expand(folder -> {
            if (folder.getParentId() != null) {
                return folderPort.getFolder(folder.getParentId());
            } else {
                return Mono.empty();
            }
        })
        .collectList()
        .map(folders -> {
            Collections.reverse(folders); // Root first
            return folders;
        });
}
```

## 3. Folder Permissions

### Setting Folder Permissions

```java
@Autowired
private FolderPermissionPort folderPermissionPort;

/**
 * Sets permissions for a folder.
 */
public Mono<FolderPermission> setFolderPermissions(UUID folderId, Long userId, 
                                                  Set<PermissionType> permissions) {
    FolderPermission folderPermission = FolderPermission.builder()
        .folderId(folderId)
        .userId(userId)
        .permissions(permissions)
        .grantedAt(Instant.now())
        .build();
    
    return folderPermissionPort.grantPermission(folderPermission)
        .doOnSuccess(granted -> 
            log.info("Permissions granted for folder {} to user {}: {}", 
                    folderId, userId, permissions));
}

/**
 * Inherits permissions from parent folder.
 */
public Mono<Void> inheritParentPermissions(UUID folderId) {
    return folderPort.getFolder(folderId)
        .filter(folder -> folder.getParentId() != null)
        .flatMap(folder -> folderPermissionPort.inheritPermissions(folderId, folder.getParentId()))
        .doOnSuccess(unused -> 
            log.info("Permissions inherited for folder: {}", folderId));
}
```

### Checking Folder Access

```java
/**
 * Checks if user has access to folder.
 */
public Mono<Boolean> hasAccess(UUID folderId, Long userId, PermissionType permission) {
    return folderPermissionPort.hasPermission(folderId, userId, permission)
        .doOnNext(hasAccess -> 
            log.debug("Access check for folder {} user {} permission {}: {}", 
                     folderId, userId, permission, hasAccess));
}

/**
 * Gets effective permissions for a user on a folder.
 */
public Mono<Set<PermissionType>> getEffectivePermissions(UUID folderId, Long userId) {
    return folderPermissionPort.getEffectivePermissions(folderId, userId)
        .doOnNext(permissions -> 
            log.debug("Effective permissions for folder {} user {}: {}", 
                     folderId, userId, permissions));
}
```

## 4. Folder Navigation

### Listing Folder Contents

```java
/**
 * Gets all contents of a folder (documents and subfolders).
 */
public Mono<FolderContents> getFolderContents(UUID folderId) {
    Mono<List<Document>> documents = documentPort.findDocumentsByFolder(folderId)
        .collectList();
    
    Mono<List<Folder>> subfolders = folderPort.getSubfolders(folderId)
        .collectList();
    
    return Mono.zip(documents, subfolders)
        .map(tuple -> FolderContents.builder()
            .documents(tuple.getT1())
            .subfolders(tuple.getT2())
            .build())
        .doOnNext(contents -> 
            log.debug("Folder {} contains {} documents and {} subfolders", 
                     folderId, contents.getDocuments().size(), contents.getSubfolders().size()));
}

/**
 * Gets paginated folder contents.
 */
public Mono<PagedFolderContents> getFolderContentsPaged(UUID folderId, int page, int size) {
    return folderPort.getFolderContentsPaged(folderId, page, size)
        .doOnNext(contents -> 
            log.debug("Page {} of folder {} contents: {} items", 
                     page, folderId, contents.getContent().size()));
}
```

### Searching Within Folders

```java
/**
 * Searches for documents within a folder and its subfolders.
 */
public Flux<Document> searchInFolder(UUID folderId, String query, boolean includeSubfolders) {
    if (includeSubfolders) {
        return folderPort.getFolder(folderId)
            .flatMapMany(folder -> documentPort.searchDocuments(
                DocumentSearchCriteria.builder()
                    .query(query)
                    .folderPath(folder.getPath())
                    .includeSubfolders(true)
                    .build()))
            .doOnNext(document -> 
                log.debug("Found document in folder search: {} ({})", 
                         document.getName(), document.getId()));
    } else {
        return documentPort.findDocumentsByFolder(folderId)
            .filter(document -> document.getName().toLowerCase().contains(query.toLowerCase()))
            .doOnNext(document -> 
                log.debug("Found document in folder: {} ({})", 
                         document.getName(), document.getId()));
    }
}
```

## 5. Bulk Operations

### Moving Multiple Items

```java
/**
 * Moves multiple documents to a target folder.
 */
public Mono<Void> moveDocumentsToFolder(List<UUID> documentIds, UUID targetFolderId) {
    return Flux.fromIterable(documentIds)
        .flatMap(documentId -> 
            documentPort.getDocument(documentId)
                .flatMap(document -> {
                    Document movedDocument = document.toBuilder()
                        .folderId(targetFolderId)
                        .modifiedAt(Instant.now())
                        .build();
                    return documentPort.updateDocument(movedDocument);
                }))
        .then()
        .doOnSuccess(unused -> 
            log.info("Moved {} documents to folder {}", documentIds.size(), targetFolderId));
}

/**
 * Copies folder structure to another location.
 */
public Mono<Folder> copyFolderStructure(UUID sourceFolderId, UUID targetParentId) {
    return folderPort.getFolder(sourceFolderId)
        .flatMap(sourceFolder -> {
            // Create copy of the folder
            Folder copiedFolder = sourceFolder.toBuilder()
                .id(null) // New ID will be generated
                .parentId(targetParentId)
                .name(sourceFolder.getName() + " (Copy)")
                .createdAt(Instant.now())
                .build();
            
            return folderPort.createFolder(copiedFolder)
                .flatMap(newFolder -> {
                    // Copy subfolders recursively
                    return folderPort.getSubfolders(sourceFolderId)
                        .flatMap(subfolder -> copyFolderStructure(subfolder.getId(), newFolder.getId()))
                        .then(Mono.just(newFolder));
                });
        })
        .doOnSuccess(copied -> 
            log.info("Folder structure copied: {} -> {}", sourceFolderId, copied.getId()));
}
```

## 6. Advanced Scenarios

### Folder Templates

```java
/**
 * Creates a folder from a predefined template.
 */
public Mono<Folder> createFromTemplate(String templateName, String folderName, UUID parentId) {
    return getTemplate(templateName)
        .flatMap(template -> createFolderFromTemplate(template, folderName, parentId))
        .doOnSuccess(folder -> 
            log.info("Folder created from template '{}': {} ({})", 
                    templateName, folderName, folder.getId()));
}

private Mono<FolderTemplate> getTemplate(String templateName) {
    // Load template from configuration or database
    Map<String, FolderTemplate> templates = Map.of(
        "project", FolderTemplate.builder()
            .name("Project Template")
            .structure(Arrays.asList(
                "Documents/Requirements",
                "Documents/Design",
                "Documents/Testing",
                "Resources/Images",
                "Resources/Assets",
                "Archive"
            ))
            .build()
    );
    
    return Mono.justOrEmpty(templates.get(templateName));
}

private Mono<Folder> createFolderFromTemplate(FolderTemplate template, String folderName, UUID parentId) {
    return createFolder(folderName, "Created from template: " + template.getName(), parentId)
        .flatMap(rootFolder -> {
            return Flux.fromIterable(template.getStructure())
                .flatMap(path -> createFolderAtPath(rootFolder.getPath() + "/" + path))
                .then(Mono.just(rootFolder));
        });
}
```

### Folder Statistics

```java
/**
 * Gets comprehensive folder statistics.
 */
public Mono<FolderStatistics> getFolderStatistics(UUID folderId) {
    Mono<Long> documentCount = documentPort.findDocumentsByFolder(folderId).count();
    Mono<Long> subfolderCount = folderPort.getSubfolders(folderId).count();
    Mono<Long> totalSize = documentPort.findDocumentsByFolder(folderId)
        .map(Document::getSize)
        .reduce(0L, Long::sum);
    
    return Mono.zip(documentCount, subfolderCount, totalSize)
        .map(tuple -> FolderStatistics.builder()
            .folderId(folderId)
            .documentCount(tuple.getT1())
            .subfolderCount(tuple.getT2())
            .totalSize(tuple.getT3())
            .calculatedAt(Instant.now())
            .build())
        .doOnNext(stats -> 
            log.debug("Folder {} statistics: {} docs, {} folders, {} bytes", 
                     folderId, stats.getDocumentCount(), stats.getSubfolderCount(), stats.getTotalSize()));
}
```

### Folder Cleanup

```java
/**
 * Cleans up empty folders in a hierarchy.
 */
public Mono<Integer> cleanupEmptyFolders(UUID rootFolderId) {
    return folderPort.getSubfolders(rootFolderId)
        .flatMap(this::cleanupEmptyFolders) // Recursive cleanup
        .reduce(0, Integer::sum)
        .flatMap(cleanedSubfolders -> {
            // Check if this folder is now empty
            return getFolderContents(rootFolderId)
                .flatMap(contents -> {
                    if (contents.getDocuments().isEmpty() && contents.getSubfolders().isEmpty()) {
                        return folderPort.deleteFolder(rootFolderId)
                            .then(Mono.just(cleanedSubfolders + 1));
                    } else {
                        return Mono.just(cleanedSubfolders);
                    }
                });
        })
        .doOnSuccess(count -> 
            log.info("Cleaned up {} empty folders", count));
}
```

These examples demonstrate comprehensive folder management capabilities using the Firefly ECM Library's folder ports and domain models. All examples are based on the actual port interfaces and follow reactive programming best practices.
