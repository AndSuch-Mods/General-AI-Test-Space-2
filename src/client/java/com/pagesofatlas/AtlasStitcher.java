package com.pagesofatlas;

import com.pagesofatlas.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
import java.util.concurrent.*;

public final class AtlasStitcher {
    private record Entry(SpriteContents contents) implements PagesOfAtlasPager.Entry {
        public int width() { return contents.width(); }
        public int height() { return contents.height(); }
        public String name() { return contents.name().toString(); }
    }
    private static final class Sprite extends TextureAtlasSprite {
        Sprite(ResourceLocation atlas, SpriteContents contents, int w, int h, int x, int y) { super(atlas, contents, w, h, x, y); }
    }
    public static SpriteLoader.Preparations stitch(ResourceLocation location, int maxSupportedTextureSize, List<SpriteContents> sprites, int requestedMip, Executor executor) {
        int limit = PagesOfAtlasClient.pageLimit(maxSupportedTextureSize);
        int mip = requestedMip;
        for (SpriteContents sprite : sprites) {
            mip = Math.min(mip, Integer.numberOfTrailingZeros(sprite.width() | sprite.height()));
        }
        // Match 1.21.1's requested alignment even when a tiny sprite reduces generated mip levels.
        var packed = PagesOfAtlasPager.pack(sprites.stream().map(Entry::new).toList(), limit, limit, requestedMip, 0);
        if (packed.pages().size() <= 1) return null;
        if (packed.pages().size() > 4) throw new IllegalStateException("Pages of Atlas supports at most 4 physical pages; " + location + " needs " + packed.pages().size());
        int w = packed.pages().stream().mapToInt(PagesOfAtlasPager.Page::width).max().orElseThrow();
        int h = packed.pages().stream().mapToInt(PagesOfAtlasPager.Page::height).max().orElseThrow();
        PageLayout layout = new PageLayout(w, h, packed.pages().size());
        Map<ResourceLocation, TextureAtlasSprite> regions = new HashMap<>();
        for (var page : packed.pages()) for (var placement : page.placements()) {
            SpriteContents contents = placement.entry().contents();
            regions.put(contents.name(), new Sprite(location, contents, layout.logicalWidth(), layout.logicalHeight(),
                    layout.x(page.number(), placement.x()), layout.y(page.number(), placement.y())));
        }
        final int actualMip = mip;
        var ready = CompletableFuture.runAsync(() -> sprites.forEach(s -> s.increaseMipLevel(actualMip)), executor);
        var result = new SpriteLoader.Preparations(layout.logicalWidth(), layout.logicalHeight(), mip,
                Objects.requireNonNull(regions.get(MissingTextureAtlasSprite.getLocation())), Map.copyOf(regions), ready);
        PagedTextures.stage(result, layout);
        PagesOfAtlasClient.LOGGER.info("Planned {}: {} sprites across {} pages", location, sprites.size(), layout.pages());
        return result;
    }
}
