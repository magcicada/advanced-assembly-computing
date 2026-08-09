package com.syaru.advancedassemblycomputing.integration;

import com.syaru.ae2craftingoptimizer.api.batch.v2.BatchTransactionRecord;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

/** Reads ACO's canonical recovery digest without importing ACO implementation types. */
public final class AACBatchTransactionDigest {
    private static final String METHOD_NAME = "payloadDigest";

    private AACBatchTransactionDigest() {
    }

    /**
     * Returns the digest from the public recovery contract when the installed
     * ACO exposes it. An absent or malformed contract is not replaced by a
     * second digest implementation; callers must quarantine that recovery.
     */
    public static Optional<String> fromPublicRecord(
            BatchTransactionRecord record) {
        if (record == null) {
            return Optional.empty();
        }
        return invoke(record);
    }

    static Optional<String> invoke(Object record) {
        if (record == null) {
            return Optional.empty();
        }
        try {
            Method method = record.getClass().getMethod(METHOD_NAME);
            if (method.getReturnType() != String.class) {
                return Optional.empty();
            }
            Object value = method.invoke(record);
            if (!(value instanceof String digest) || digest.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(digest);
        } catch (IllegalAccessException
                | InvocationTargetException
                | NoSuchMethodException
                | SecurityException
                | LinkageError unavailable) {
            return Optional.empty();
        }
    }
}
