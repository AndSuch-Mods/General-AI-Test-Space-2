package com.pagesofatlas;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PagesOfAtlasClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("pagesofatlas");
    @Override public void onInitializeClient() {
        LOGGER.info("Unofficial Pages of Atlas 1.21.1 backport; GPU compatibility requires user testing");
        for (String id : new String[]{"minecraft", "fabricloader", "fabric-api", "sodium", "iris", "continuity"}) {
            LOGGER.info("Dependency {}: {}", id, FabricLoader.getInstance().getModContainer(id)
                    .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("absent"));
        }
        for (String id : new String[]{"distanthorizons", "physicsmod", "dynamictrees"}) {
            if (FabricLoader.getInstance().isModLoaded(id)) LOGGER.warn("{} is installed; its custom rendering paths have not been validated with paged atlases", id);
        }
    }
    public static int pageLimit(int hardware) {
        int size = Integer.getInteger("pagesofatlas.devPageSize", 0);
        // Sodium 0.6 has 15 fractional UV bits: the virtual extent must not exceed 32768.
        if (size == 0) return Math.min(16384, Integer.highestOneBit(hardware));
        if (!Boolean.getBoolean("pagesofatlas.development")) {
            throw new IllegalStateException("devPageSize requires -Dpagesofatlas.development=true");
        }
        if (size < 256 || size > Math.min(16384, hardware) || Integer.bitCount(size) != 1) {
            throw new IllegalArgumentException("devPageSize must be a power of two in [256, GL_MAX_TEXTURE_SIZE]");
        }
        LOGGER.warn("Development physical page cap: {} (hardware {})", size, hardware);
        return size;
    }
}
