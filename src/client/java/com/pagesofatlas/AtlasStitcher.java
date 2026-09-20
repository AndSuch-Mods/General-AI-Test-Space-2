package com.pagesofatlas;

import net.minecraft.client.renderer.texture.*;
import java.util.*;

/** Replaces only the packing engine, preserving SpriteLoader's normal return and other mods' hooks. */
public final class AtlasStitcher extends Stitcher<SpriteContents> {
    private record Entry(SpriteContents contents) implements PagesOfAtlasPager.Entry {
        public int width() { return contents.width(); }
        public int height() { return contents.height(); }
        public String name() { return contents.name().toString(); }
    }
    private final List<Entry> entries = new ArrayList<>();
    private final int limit, mip;
    private PagesOfAtlasPager.Result<Entry> packed;
    private PageLayout layout;
    public AtlasStitcher(int width, int height, int mip) {
        super(width, height, mip);
        this.limit = PagesOfAtlasClient.pageLimit(Math.min(width, height));
        this.mip = mip;
    }
    @Override public void registerSprite(SpriteContents sprite) { entries.add(new Entry(sprite)); }
    @Override public void stitch() {
        try {
            packed = PagesOfAtlasPager.pack(entries, limit, limit, mip, 0);
            if (packed.pages().isEmpty() || packed.pages().size() > 4) throw new IllegalStateException(
                    "Pages of Atlas supports 1..4 physical pages; this atlas needs " + packed.pages().size());
            layout = new PageLayout(packed.pages().stream().mapToInt(PagesOfAtlasPager.Page::width).max().orElseThrow(),
                    packed.pages().stream().mapToInt(PagesOfAtlasPager.Page::height).max().orElseThrow(), packed.pages().size());
        } catch (RuntimeException failure) {
            PagesOfAtlasClient.LOGGER.error("Atlas packing failed: {} sprites, page cap {}, requested mip {}", entries.size(), limit, mip, failure);
            throw failure;
        }
    }
    public PageLayout layout() { return layout; }
    @Override public int getWidth() { return layout.logicalWidth(); }
    @Override public int getHeight() { return layout.logicalHeight(); }
    @Override public void gatherSprites(Stitcher.SpriteLoader<SpriteContents> loader) {
        for (var page : packed.pages()) for (var p : page.placements()) {
            loader.load(p.entry().contents(), layout.x(page.number(), p.x()), layout.y(page.number(), p.y()));
        }
    }
}
