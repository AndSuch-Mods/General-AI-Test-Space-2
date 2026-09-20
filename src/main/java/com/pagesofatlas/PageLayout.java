package com.pagesofatlas;

/** Shared by diffuse and material uploads; logical UVs remain in [0,1]. */
public record PageLayout(int width, int height, int pages) {
    public PageLayout {
        if (pages < 2 || pages > 4 || width < 1 || height < 1
                || Integer.bitCount(width) != 1 || Integer.bitCount(height) != 1) {
            throw new IllegalArgumentException("Expected 2..4 power-of-two physical pages");
        }
    }
    public int columns() { return 2; }
    public int rows() { return (pages + 1) / 2; }
    public int logicalWidth() { return width * columns(); }
    public int logicalHeight() { return height * rows(); }
    public int x(int page, int localX) { return (page % columns()) * width + localX; }
    public int y(int page, int localY) { return (page / columns()) * height + localY; }
    public Upload upload(int level, int x, int y, int w, int h) {
        int pw = width >> level, ph = height >> level;
        if (pw < 1 || ph < 1 || x < 0 || y < 0 || w < 1 || h < 1) {
            throw new IllegalArgumentException("Invalid atlas upload");
        }
        int column = x / pw, row = y / ph, page = row * columns() + column;
        if (column >= columns() || row >= rows() || page >= pages || x % pw + w > pw || y % ph + h > ph) {
            throw new IllegalArgumentException("Sprite upload crosses a physical atlas page");
        }
        return new Upload(page, x % pw, y % ph);
    }
    public record Upload(int page, int x, int y) {}
}
