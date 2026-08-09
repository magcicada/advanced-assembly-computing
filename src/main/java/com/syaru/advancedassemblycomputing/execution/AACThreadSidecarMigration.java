package com.syaru.advancedassemblycomputing.execution;

/**
 * Versioned state rules for the AAC crafting-thread sidecar.
 *
 * <p>Schema 1 is the AAC 1.0.1 format. It did not persist a state field, so
 * only a complete legacy payload may be inferred as active work. Schema 2
 * requires an explicit state and never treats {@code NONE} as active work.</p>
 */
public final class AACThreadSidecarMigration {
    public static final int LEGACY_SCHEMA = 1;
    public static final int CURRENT_SCHEMA = 2;

    private AACThreadSidecarMigration() {}

    public static StateResolution resolve(
            int schema,
            boolean hasState,
            String storedState,
            boolean hasActivePayload,
            boolean outputReady) {
        if (schema != LEGACY_SCHEMA && schema != CURRENT_SCHEMA) {
            throw new InvalidSidecarException(
                    AACThreadSidecarFailure.UNKNOWN_SCHEMA,
                    "unknown AAC crafting-table batch sidecar schema");
        }

        String state = storedState == null ? "" : storedState;
        if (!hasState || state.isEmpty()) {
            if (schema != LEGACY_SCHEMA) {
                throw new InvalidSidecarException(
                        AACThreadSidecarFailure.INVALID_STATE,
                        "AAC crafting-table batch sidecar is missing its state");
            }
            return new StateResolution(
                    hasActivePayload
                            ? outputReady
                                    ? AacThreadState.OUTPUT_READY
                                    : AacThreadState.RUNNING
                            : AacThreadState.NONE,
                    true);
        }

        AacThreadState parsedState;
        try {
            parsedState = AacThreadState.valueOf(state);
        } catch (IllegalArgumentException invalidState) {
            throw new InvalidSidecarException(
                    AACThreadSidecarFailure.INVALID_STATE,
                    "invalid AAC crafting-table batch state",
                    invalidState);
        }

        if (parsedState == AacThreadState.QUARANTINED) {
            throw new InvalidSidecarException(
                    AACThreadSidecarFailure.INVALID_STATE,
                    "quarantined sidecars must use the quarantine loader");
        }
        if (parsedState == AacThreadState.NONE && hasActivePayload) {
            throw new InvalidSidecarException(
                    AACThreadSidecarFailure.INVALID_STATE,
                    "AAC crafting-table batch sidecar has payload in NONE state");
        }
        return new StateResolution(
                parsedState,
                schema != CURRENT_SCHEMA);
    }

    public record StateResolution(
            AacThreadState state,
            boolean migrated) {}
}
