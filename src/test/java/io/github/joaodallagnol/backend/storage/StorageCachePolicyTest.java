package io.github.joaodallagnol.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StorageCachePolicyTest {

    @Test
    void publicObjectsAreImmutableAndPrivateObjectsCannotBeStored() {
        assertThat(StorageCachePolicy.forScope(StorageScope.PUBLIC))
                .isEqualTo("public, max-age=31536000, immutable");
        assertThat(StorageCachePolicy.forScope(StorageScope.PRIVATE))
                .isEqualTo("private, no-store");
    }
}
