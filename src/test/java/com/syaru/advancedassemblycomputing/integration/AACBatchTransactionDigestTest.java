package com.syaru.advancedassemblycomputing.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class AACBatchTransactionDigestTest {
    @Test
    void usesThePublicRecordContractWhenPresent() {
        assertEquals(
                Optional.of("aco-canonical-digest"),
                AACBatchTransactionDigest.invoke(
                        new PublicRecoveryRecord()));
    }

    @Test
    void doesNotRecreateADigestForARecordWithoutTheNewContract() {
        assertTrue(
                AACBatchTransactionDigest.invoke(
                        new LegacyRecoveryRecord())
                        .isEmpty());
    }

    @Test
    void rejectsBlankOrNonStringContractValues() {
        assertTrue(
                AACBatchTransactionDigest.invoke(
                        new BlankRecoveryRecord())
                        .isEmpty());
        assertTrue(
                AACBatchTransactionDigest.invoke(
                        new NonStringRecoveryRecord())
                        .isEmpty());
    }

    @Test
    void treatsContractFailuresAsUnavailable() {
        assertTrue(
                AACBatchTransactionDigest.invoke(
                        new FailingRecoveryRecord())
                        .isEmpty());
    }

    public static final class PublicRecoveryRecord {
        public String payloadDigest() {
            return "aco-canonical-digest";
        }
    }

    public static final class LegacyRecoveryRecord {
        public String receipt() {
            return "legacy-receipt";
        }
    }

    public static final class BlankRecoveryRecord {
        public String payloadDigest() {
            return "  ";
        }
    }

    public static final class NonStringRecoveryRecord {
        public long payloadDigest() {
            return 1L;
        }
    }

    public static final class FailingRecoveryRecord {
        public String payloadDigest() {
            throw new IllegalStateException("digest unavailable");
        }
    }
}
