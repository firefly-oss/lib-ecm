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
package com.firefly.core.ecm.domain.enums.document;

/**
 * Content type classification enumeration.
 */
public enum ContentType {
    
    /**
     * Text documents (DOC, DOCX, TXT, RTF)
     */
    TEXT,
    
    /**
     * Spreadsheet documents (XLS, XLSX, CSV)
     */
    SPREADSHEET,
    
    /**
     * Presentation documents (PPT, PPTX)
     */
    PRESENTATION,
    
    /**
     * PDF documents
     */
    PDF,
    
    /**
     * Image files (JPG, PNG, GIF, BMP)
     */
    IMAGE,
    
    /**
     * Video files (MP4, AVI, MOV)
     */
    VIDEO,
    
    /**
     * Audio files (MP3, WAV, AAC)
     */
    AUDIO,
    
    /**
     * Archive files (ZIP, RAR, TAR)
     */
    ARCHIVE,
    
    /**
     * Email files (EML, MSG)
     */
    EMAIL,
    
    /**
     * Web files (HTML, XML, JSON)
     */
    WEB,
    
    /**
     * Code files (JAVA, JS, CSS)
     */
    CODE,
    
    /**
     * CAD files (DWG, DXF)
     */
    CAD,
    
    /**
     * Other/unknown file types
     */
    OTHER
}
