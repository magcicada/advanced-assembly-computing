package com.syaru.advancedassemblycomputing.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import org.junit.jupiter.api.Test;

class AACThreadSidecarMigrationTest {
    @Test
    void migratesAac101SidecarWithoutState() throws Exception {
        CompoundTag sidecar = fixture("legacy-aac-1.0.1.snbt");

        AACThreadSidecarMigration.StateResolution result =
                AACThreadSidecarMigration.resolve(
                        sidecar.getInt("schema"),
                        sidecar.contains("state"),
                        sidecar.getString("state"),
                        sidecar.contains("payloadDigest"),
                        false);

        assertEquals(AacThreadState.RUNNING, result.state());
        assertTrue(result.migrated());
    }

    @Test
    void keepsExplicitNoneAsNoneWhenThereIsNoPayload() throws Exception {
        CompoundTag sidecar = fixture("explicit-none.snbt");

        AACThreadSidecarMigration.StateResolution result =
                AACThreadSidecarMigration.resolve(
                        sidecar.getInt("schema"),
                        sidecar.contains("state", Tag.TAG_STRING),
                        sidecar.getString("state"),
                        sidecar.contains("payloadDigest"),
                        false);

        assertEquals(AacThreadState.NONE, result.state());
        assertTrue(!result.migrated());
    }

    @Test
    void rejectsPayloadInExplicitNoneState() throws Exception {
        CompoundTag sidecar = fixture("explicit-none-with-payload.snbt");

        InvalidSidecarException failure =
                assertThrows(
                        InvalidSidecarException.class,
                        () -> AACThreadSidecarMigration.resolve(
                                sidecar.getInt("schema"),
                                true,
                                sidecar.getString("state"),
                                sidecar.contains("payloadDigest"),
                                false));

        assertEquals(
                AACThreadSidecarFailure.INVALID_STATE,
                failure.category());
    }

    @Test
    void rejectsMissingStateInCurrentSchema() throws Exception {
        CompoundTag sidecar = fixture("malformed-missing-state.snbt");

        InvalidSidecarException failure =
                assertThrows(
                        InvalidSidecarException.class,
                        () -> AACThreadSidecarMigration.resolve(
                                sidecar.getInt("schema"),
                                false,
                                "",
                                true,
                                false));

        assertEquals(
                AACThreadSidecarFailure.INVALID_STATE,
                failure.category());
    }

    @Test
    void rejectsUnknownSchemaFromFixture() throws Exception {
        CompoundTag sidecar = fixture("malformed-unknown-schema.snbt");

        InvalidSidecarException failure =
                assertThrows(
                        InvalidSidecarException.class,
                        () -> AACThreadSidecarMigration.resolve(
                                sidecar.getInt("schema"),
                                true,
                                sidecar.getString("state"),
                                true,
                                false));

        assertEquals(
                AACThreadSidecarFailure.UNKNOWN_SCHEMA,
               failure.category());
    }

    @Test
    void loadsRequiredMalformedSidecarFixtures() throws Exception {
        CompoundTag missingUuid = fixture("malformed-missing-uuid.snbt");
        CompoundTag duplicateKey = fixture("malformed-duplicate-key.snbt");
        CompoundTag oversizedCount = fixture("malformed-oversized-count.snbt");

        assertTrue(!missingUuid.hasUUID("transactionId"));
        assertEquals(
                2,
                duplicateKey.getList("exactOutputs", Tag.TAG_COMPOUND).size());
        assertEquals(
                8193,
                oversizedCount
                        .getList("exactOutputs", Tag.TAG_COMPOUND)
                        .getCompound(0)
                        .getByteArray("amount")
                        .length);
    }

    private static CompoundTag fixture(String name) throws Exception {
        try (InputStream input =
                AACThreadSidecarMigrationTest.class
                        .getResourceAsStream("/sidecars/" + name)) {
            if (input == null) {
                throw new IOException("missing sidecar fixture: " + name);
            }
            String snbt =
                    new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return TagParser.parseTag(snbt);
        }
    }
}
