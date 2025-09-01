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

import com.firefly.core.ecm.domain.dto.validation.SignatureValidationResult;
import com.firefly.core.ecm.domain.dto.validation.CertificateValidationResult;
import com.firefly.core.ecm.domain.dto.validation.TimestampValidationResult;
import com.firefly.core.ecm.domain.dto.validation.IdentityValidationResult;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * Port interface for signature validation operations.
 * Handles verification of digital signatures, certificates, and signature integrity.
 */
public interface SignatureValidationPort {
    
    /**
     * Validate a signature envelope's integrity and authenticity.
     *
     * @param envelopeId the envelope ID to validate
     * @return Mono containing validation result with details
     */
    Mono<SignatureValidationResult> validateEnvelope(UUID envelopeId);
    
    /**
     * Validate a specific signature request within an envelope.
     *
     * @param requestId the signature request ID to validate
     * @return Mono containing validation result with details
     */
    Mono<SignatureValidationResult> validateSignatureRequest(UUID requestId);
    
    /**
     * Validate a document's digital signature.
     *
     * @param documentId the document ID
     * @param signatureData the signature data to validate
     * @return Mono containing validation result
     */
    Mono<SignatureValidationResult> validateDocumentSignature(UUID documentId, byte[] signatureData);
    
    /**
     * Validate a digital certificate used for signing.
     *
     * @param certificateData the certificate data
     * @return Mono containing certificate validation result
     */
    Mono<CertificateValidationResult> validateCertificate(byte[] certificateData);
    
    /**
     * Validate certificate chain for a signature.
     *
     * @param certificateChain the certificate chain data
     * @return Mono containing chain validation result
     */
    Mono<CertificateValidationResult> validateCertificateChain(byte[] certificateChain);
    
    /**
     * Check if a certificate is revoked.
     *
     * @param certificateData the certificate data
     * @return Mono containing true if certificate is revoked, false otherwise
     */
    Mono<Boolean> isCertificateRevoked(byte[] certificateData);
    
    /**
     * Validate timestamp of a signature.
     *
     * @param signatureData the signature data
     * @param timestampData the timestamp data
     * @return Mono containing timestamp validation result
     */
    Mono<TimestampValidationResult> validateTimestamp(byte[] signatureData, byte[] timestampData);
    
    /**
     * Verify signature integrity (document hasn't been tampered with).
     *
     * @param documentId the document ID
     * @param signatureData the signature data
     * @return Mono containing true if integrity is valid, false otherwise
     */
    Mono<Boolean> verifySignatureIntegrity(UUID documentId, byte[] signatureData);
    
    /**
     * Validate signer identity against signature.
     *
     * @param signatureData the signature data
     * @param signerIdentity the claimed signer identity
     * @return Mono containing identity validation result
     */
    Mono<IdentityValidationResult> validateSignerIdentity(byte[] signatureData, String signerIdentity);
    
    /**
     * Get signature validation report for an envelope.
     *
     * @param envelopeId the envelope ID
     * @return Mono containing detailed validation report
     */
    Mono<String> getValidationReport(UUID envelopeId);
    
    /**
     * Validate signature compliance with regulations.
     *
     * @param envelopeId the envelope ID
     * @param regulationStandard the regulation standard (e.g., "eIDAS", "ESIGN", "UETA")
     * @return Mono containing compliance validation result
     */
    Mono<Map<String, Boolean>> validateCompliance(UUID envelopeId, String regulationStandard);
    
    /**
     * Validate long-term signature preservation.
     *
     * @param envelopeId the envelope ID
     * @return Mono containing long-term validation result
     */
    Mono<Boolean> validateLongTermPreservation(UUID envelopeId);
    
    /**
     * Batch validate multiple envelopes.
     *
     * @param envelopeIds the envelope IDs to validate
     * @return Mono containing batch validation results
     */
    Mono<Map<UUID, SignatureValidationResult>> batchValidateEnvelopes(java.util.Set<UUID> envelopeIds);
    
    /**
     * Validate signature against known signature patterns (fraud detection).
     *
     * @param signatureData the signature data
     * @param signerProfile the signer profile for comparison
     * @return Mono containing fraud detection result
     */
    Mono<Boolean> detectSignatureFraud(byte[] signatureData, String signerProfile);
    
    /**
     * Validate biometric signature data.
     *
     * @param biometricData the biometric signature data
     * @param referenceData the reference biometric data
     * @return Mono containing biometric validation result
     */
    Mono<Boolean> validateBiometricSignature(byte[] biometricData, byte[] referenceData);
    
    /**
     * Generate signature validation certificate.
     *
     * @param envelopeId the envelope ID
     * @return Mono containing validation certificate data
     */
    Mono<byte[]> generateValidationCertificate(UUID envelopeId);
    
    /**
     * Archive signature validation data for long-term preservation.
     *
     * @param envelopeId the envelope ID
     * @return Mono indicating completion
     */
    Mono<Void> archiveValidationData(UUID envelopeId);
}
