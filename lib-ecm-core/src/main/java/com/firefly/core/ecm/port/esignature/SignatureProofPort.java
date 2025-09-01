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
package com.firefly.core.ecm.port.esignature;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * Port interface for signature proof and evidence management operations.
 * Handles generation, storage, and retrieval of signature proof documents and audit trails.
 */
public interface SignatureProofPort {
    
    /**
     * Generate signature proof document for an envelope.
     *
     * @param envelopeId the envelope ID
     * @param proofFormat the proof format (PDF, XML, JSON)
     * @return Mono containing the proof document as byte array
     */
    Mono<byte[]> generateSignatureProof(UUID envelopeId, String proofFormat);
    
    /**
     * Generate detailed audit trail for an envelope.
     *
     * @param envelopeId the envelope ID
     * @return Mono containing the audit trail document
     */
    Mono<byte[]> generateAuditTrail(UUID envelopeId);
    
    /**
     * Generate certificate of completion for an envelope.
     *
     * @param envelopeId the envelope ID
     * @return Mono containing the certificate of completion
     */
    Mono<byte[]> generateCompletionCertificate(UUID envelopeId);
    
    /**
     * Generate signature summary report.
     *
     * @param envelopeId the envelope ID
     * @return Mono containing the signature summary report
     */
    Mono<byte[]> generateSignatureSummary(UUID envelopeId);
    
    /**
     * Store signature proof evidence.
     *
     * @param envelopeId the envelope ID
     * @param evidenceType the type of evidence
     * @param evidenceData the evidence data
     * @param metadata additional metadata
     * @return Mono containing the evidence storage ID
     */
    Mono<UUID> storeSignatureEvidence(UUID envelopeId, String evidenceType, byte[] evidenceData, Map<String, Object> metadata);
    
    /**
     * Retrieve signature evidence by ID.
     *
     * @param evidenceId the evidence ID
     * @return Mono containing the evidence data
     */
    Mono<byte[]> getSignatureEvidence(UUID evidenceId);
    
    /**
     * Get all evidence for an envelope.
     *
     * @param envelopeId the envelope ID
     * @return Flux of evidence metadata
     */
    Flux<Map<String, Object>> getEnvelopeEvidence(UUID envelopeId);
    
    /**
     * Generate legal proof package for court proceedings.
     *
     * @param envelopeId the envelope ID
     * @param jurisdiction the legal jurisdiction
     * @return Mono containing the legal proof package
     */
    Mono<byte[]> generateLegalProofPackage(UUID envelopeId, String jurisdiction);
    
    /**
     * Generate compliance report for regulatory requirements.
     *
     * @param envelopeId the envelope ID
     * @param regulationStandard the regulation standard
     * @return Mono containing the compliance report
     */
    Mono<byte[]> generateComplianceReport(UUID envelopeId, String regulationStandard);
    
    /**
     * Create tamper-evident seal for signature proof.
     *
     * @param proofData the proof data to seal
     * @return Mono containing the sealed proof data
     */
    Mono<byte[]> createTamperEvidentSeal(byte[] proofData);
    
    /**
     * Verify tamper-evident seal integrity.
     *
     * @param sealedData the sealed data to verify
     * @return Mono containing true if seal is intact, false otherwise
     */
    Mono<Boolean> verifyTamperEvidentSeal(byte[] sealedData);
    
    /**
     * Generate blockchain proof of existence.
     *
     * @param envelopeId the envelope ID
     * @return Mono containing blockchain proof data
     */
    Mono<String> generateBlockchainProof(UUID envelopeId);
    
    /**
     * Verify blockchain proof of existence.
     *
     * @param blockchainProof the blockchain proof to verify
     * @return Mono containing verification result
     */
    Mono<Boolean> verifyBlockchainProof(String blockchainProof);
    
    /**
     * Generate qualified electronic signature proof (eIDAS compliant).
     *
     * @param envelopeId the envelope ID
     * @return Mono containing qualified signature proof
     */
    Mono<byte[]> generateQualifiedSignatureProof(UUID envelopeId);
    
    /**
     * Archive signature proof for long-term preservation.
     *
     * @param envelopeId the envelope ID
     * @param archiveFormat the archive format
     * @return Mono containing archive reference ID
     */
    Mono<String> archiveSignatureProof(UUID envelopeId, String archiveFormat);
    
    /**
     * Retrieve archived signature proof.
     *
     * @param archiveReferenceId the archive reference ID
     * @return Mono containing archived proof data
     */
    Mono<byte[]> retrieveArchivedProof(String archiveReferenceId);
    
    /**
     * Generate signature proof with custom template.
     *
     * @param envelopeId the envelope ID
     * @param templateId the custom template ID
     * @param parameters template parameters
     * @return Mono containing the custom proof document
     */
    Mono<byte[]> generateCustomProof(UUID envelopeId, String templateId, Map<String, Object> parameters);
    
    /**
     * Batch generate signature proofs for multiple envelopes.
     *
     * @param envelopeIds the envelope IDs
     * @param proofFormat the proof format
     * @return Mono containing batch proof generation results
     */
    Mono<Map<UUID, byte[]>> batchGenerateProofs(java.util.Set<UUID> envelopeIds, String proofFormat);
    
    /**
     * Delete signature proof and evidence.
     *
     * @param envelopeId the envelope ID
     * @return Mono indicating completion
     */
    Mono<Void> deleteSignatureProof(UUID envelopeId);
}
