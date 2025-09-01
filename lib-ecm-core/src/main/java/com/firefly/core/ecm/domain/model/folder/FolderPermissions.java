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
package com.firefly.core.ecm.domain.model.folder;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * Folder-level permissions configuration.
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class FolderPermissions {
    
    /**
     * Whether users can create documents in this folder
     */
    private final Boolean allowCreateDocuments;
    
    /**
     * Whether users can create subfolders
     */
    private final Boolean allowCreateFolders;
    
    /**
     * Whether users can delete documents from this folder
     */
    private final Boolean allowDeleteDocuments;
    
    /**
     * Whether users can delete this folder
     */
    private final Boolean allowDeleteFolder;
    
    /**
     * Whether users can modify folder properties
     */
    private final Boolean allowModifyFolder;
    
    /**
     * Whether permissions are inherited from parent folder
     */
    private final Boolean inheritPermissions;
    
    /**
     * Whether this folder is read-only
     */
    private final Boolean readOnly;
    
    /**
     * Whether this folder requires approval for document uploads
     */
    private final Boolean requireApproval;
}
