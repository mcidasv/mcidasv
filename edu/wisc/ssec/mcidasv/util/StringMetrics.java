package edu.wisc.ssec.mcidasv.util;


import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;

public class StringMetrics {
    private final Graphics2D graphics;
    private final FontRenderContext context;
    public StringMetrics(Graphics2D graphics) {
        this.graphics = graphics;
        this.context = graphics.getFontRenderContext();
    }
    
    public Rectangle2D getBounds(Font font, String text) {
        GlyphVector gv = font.createGlyphVector(context, text);
        return gv.getLogicalBounds();
    }
}