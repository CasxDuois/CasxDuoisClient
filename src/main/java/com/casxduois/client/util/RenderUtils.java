package com.casxduois.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

/**
 * Tiện ích vẽ đồ họa cao cấp: Bo góc (Rounded Rect), Gradient, Đổ bóng mờ cho Dark Theme GUI
 */
public class RenderUtils {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void drawRect(float x, float y, float width, float height, int color) {
        float f3 = (float) (color >> 24 & 255) / 255.0F;
        float f = (float) (color >> 16 & 255) / 255.0F;
        float f1 = (float) (color >> 8 & 255) / 255.0F;
        float f2 = (float) (color & 255) / 255.0F;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(f, f1, f2, f3);

        bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        bufferbuilder.pos(x, y + height, 0.0D).endVertex();
        bufferbuilder.pos(x + width, y + height, 0.0D).endVertex();
        bufferbuilder.pos(x + width, y, 0.0D).endVertex();
        bufferbuilder.pos(x, y, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        float x2 = x + width;
        float y2 = y + height;

        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(r, g, b, a);

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glBegin(GL11.GL_POLYGON);

        // Góc trên bên phải
        for (int i = 0; i <= 90; i += 3) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x2 - radius + Math.sin(rad) * radius, y + radius - Math.cos(rad) * radius);
        }
        // Góc dưới bên phải
        for (int i = 90; i <= 180; i += 3) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x2 - radius + Math.sin(rad) * radius, y2 - radius - Math.cos(rad) * radius);
        }
        // Góc dưới bên trái
        for (int i = 180; i <= 270; i += 3) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x + radius + Math.sin(rad) * radius, y2 - radius - Math.cos(rad) * radius);
        }
        // Góc trên bên trái
        for (int i = 270; i <= 360; i += 3) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x + radius + Math.sin(rad) * radius, y + radius - Math.cos(rad) * radius);
        }

        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public static void drawRoundedOutline(float x, float y, float width, float height, float radius, float lineWidth, int color) {
        float x2 = x + width;
        float y2 = y + height;

        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(r, g, b, a);

        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glBegin(GL11.GL_LINE_LOOP);

        for (int i = 0; i <= 90; i += 3) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x2 - radius + Math.sin(rad) * radius, y + radius - Math.cos(rad) * radius);
        }
        for (int i = 90; i <= 180; i += 3) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x2 - radius + Math.sin(rad) * radius, y2 - radius - Math.cos(rad) * radius);
        }
        for (int i = 180; i <= 270; i += 3) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x + radius + Math.sin(rad) * radius, y2 - radius - Math.cos(rad) * radius);
        }
        for (int i = 270; i <= 360; i += 3) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x + radius + Math.sin(rad) * radius, y + radius - Math.cos(rad) * radius);
        }

        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public static void drawGradientRect(float x, float y, float width, float height, int startColor, int endColor) {
        float f = (float) (startColor >> 24 & 255) / 255.0F;
        float f1 = (float) (startColor >> 16 & 255) / 255.0F;
        float f2 = (float) (startColor >> 8 & 255) / 255.0F;
        float f3 = (float) (startColor & 255) / 255.0F;

        float f4 = (float) (endColor >> 24 & 255) / 255.0F;
        float f5 = (float) (endColor >> 16 & 255) / 255.0F;
        float f6 = (float) (endColor >> 8 & 255) / 255.0F;
        float f7 = (float) (endColor & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(x + width, y, 0.0D).color(f1, f2, f3, f).endVertex();
        bufferbuilder.pos(x, y, 0.0D).color(f1, f2, f3, f).endVertex();
        bufferbuilder.pos(x, y + height, 0.0D).color(f5, f6, f7, f4).endVertex();
        bufferbuilder.pos(x + width, y + height, 0.0D).color(f5, f6, f7, f4).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public static void drawCircle(float cx, float cy, float radius, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(r, g, b, a);

        GL11.glEnable(GL11.GL_POINT_SMOOTH);
        GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 0; i <= 360; i += 12) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(cx + Math.sin(rad) * radius, cy + Math.cos(rad) * radius);
        }
        GL11.glEnd();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public static void drawShadow(float x, float y, float width, float height, float radius, int layers, int baseAlpha) {
        for (int i = layers; i >= 1; i--) {
            float expand = i * 1.8f;
            int alpha = (int) (baseAlpha * (1.0f - (float) (i - 1) / layers));
            int shadowColor = ((alpha & 0xFF) << 24);
            drawRoundedRect(x - expand, y - expand, width + expand * 2f, height + expand * 2f, radius + expand, shadowColor);
        }
    }

    public static void drawHorizontalGradient(float x, float y, float width, float height, int startColor, int endColor) {
        float f = (float) (startColor >> 24 & 255) / 255.0F;
        float f1 = (float) (startColor >> 16 & 255) / 255.0F;
        float f2 = (float) (startColor >> 8 & 255) / 255.0F;
        float f3 = (float) (startColor & 255) / 255.0F;

        float f4 = (float) (endColor >> 24 & 255) / 255.0F;
        float f5 = (float) (endColor >> 16 & 255) / 255.0F;
        float f6 = (float) (endColor >> 8 & 255) / 255.0F;
        float f7 = (float) (endColor & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(x, y, 0.0D).color(f1, f2, f3, f).endVertex();
        bufferbuilder.pos(x, y + height, 0.0D).color(f1, f2, f3, f).endVertex();
        bufferbuilder.pos(x + width, y + height, 0.0D).color(f5, f6, f7, f4).endVertex();
        bufferbuilder.pos(x + width, y, 0.0D).color(f5, f6, f7, f4).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public static void startScissor(float x, float y, float width, float height) {
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = Math.max(1, sr.getScaleFactor());
        int scissorY = (int) (mc.displayHeight - (y + height) * scale);
        int scissorX = (int) (x * scale);
        int scissorWidth = (int) (width * scale);
        int scissorHeight = (int) (height * scale);

        if (scissorX < 0) scissorX = 0;
        if (scissorY < 0) scissorY = 0;
        if (scissorWidth < 0) scissorWidth = 0;
        if (scissorHeight < 0) scissorHeight = 0;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }

    public static void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    /**
     * Vẽ hình chữ nhật bo góc với gradient ngang (Horizontal Gradient Rounded Rect)
     */
    public static void drawRoundedGradientRect(float x, float y, float width, float height, float radius, int startColor, int endColor) {
        float x2 = x + width;
        float y2 = y + height;

        float sA = (float) (startColor >> 24 & 255) / 255.0F;
        float sR = (float) (startColor >> 16 & 255) / 255.0F;
        float sG = (float) (startColor >> 8 & 255) / 255.0F;
        float sB = (float) (startColor & 255) / 255.0F;

        float eA = (float) (endColor >> 24 & 255) / 255.0F;
        float eR = (float) (endColor >> 16 & 255) / 255.0F;
        float eG = (float) (endColor >> 8 & 255) / 255.0F;
        float eB = (float) (endColor & 255) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glBegin(GL11.GL_POLYGON);

        // Góc trên bên phải
        for (int i = 0; i <= 90; i += 3) {
            double rad = Math.toRadians(i);
            double vx = x2 - radius + Math.sin(rad) * radius;
            double vy = y + radius - Math.cos(rad) * radius;
            float t = (float) Math.max(0.0, Math.min(1.0, (vx - x) / width));
            GL11.glColor4f(sR + (eR - sR) * t, sG + (eG - sG) * t, sB + (eB - sB) * t, sA + (eA - sA) * t);
            GL11.glVertex2d(vx, vy);
        }
        // Góc dưới bên phải
        for (int i = 90; i <= 180; i += 3) {
            double rad = Math.toRadians(i);
            double vx = x2 - radius + Math.sin(rad) * radius;
            double vy = y2 - radius - Math.cos(rad) * radius;
            float t = (float) Math.max(0.0, Math.min(1.0, (vx - x) / width));
            GL11.glColor4f(sR + (eR - sR) * t, sG + (eG - sG) * t, sB + (eB - sB) * t, sA + (eA - sA) * t);
            GL11.glVertex2d(vx, vy);
        }
        // Góc dưới bên trái
        for (int i = 180; i <= 270; i += 3) {
            double rad = Math.toRadians(i);
            double vx = x + radius + Math.sin(rad) * radius;
            double vy = y2 - radius - Math.cos(rad) * radius;
            float t = (float) Math.max(0.0, Math.min(1.0, (vx - x) / width));
            GL11.glColor4f(sR + (eR - sR) * t, sG + (eG - sG) * t, sB + (eB - sB) * t, sA + (eA - sA) * t);
            GL11.glVertex2d(vx, vy);
        }
        // Góc trên bên trái
        for (int i = 270; i <= 360; i += 3) {
            double rad = Math.toRadians(i);
            double vx = x + radius + Math.sin(rad) * radius;
            double vy = y + radius - Math.cos(rad) * radius;
            float t = (float) Math.max(0.0, Math.min(1.0, (vx - x) / width));
            GL11.glColor4f(sR + (eR - sR) * t, sG + (eG - sG) * t, sB + (eB - sB) * t, sA + (eA - sA) * t);
            GL11.glVertex2d(vx, vy);
        }

        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.shadeModel(GL11.GL_FLAT);

        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * Vẽ Glassmorphism Panel hoàn chỉnh:
     * 1. Multi-layer shadow đổ bóng mờ
     * 2. Nền kính bán trong mờ ColorUtils.BG_GLASS
     * 3. Viền mảnh kính ColorUtils.BORDER_GLASS
     * 4. Top highlight line gradient accent (Indigo -> Violet) rất mảnh
     */
    public static void drawGlassPanel(float x, float y, float width, float height, float radius) {
        // Đổ bóng sâu
        drawShadow(x, y, width, height, radius, 5, 60);

        // Nền kính mờ Obsidian
        drawRoundedRect(x, y, width, height, radius, ColorUtils.BG_GLASS.getRGB());

        // Viền kính mảnh
        drawRoundedOutline(x, y, width, height, radius, 1.0f, ColorUtils.BORDER_GLASS.getRGB());

        // Vạch ánh kim gradient Indigo -> Violet mảnh ở mép trên
        float lineInset = Math.min(24f, width * 0.10f);
        float lineW = width - (lineInset * 2f);
        if (lineW > 10f) {
            drawHorizontalGradient(x + lineInset, y, lineW, 1.0f,
                    new Color(99, 102, 241, 110).getRGB(),
                    new Color(139, 92, 246, 30).getRGB());
        }
    }

    /**
     * Vạch chỉ báo gradient Accent bo góc (Indicator pill)
     */
    public static void drawAccentIndicator(float x, float y, float width, float height, float radius) {
        drawRoundedGradientRect(x, y, width, height, radius, ColorUtils.ACCENT_START.getRGB(), ColorUtils.ACCENT_END.getRGB());
    }

    /**
     * Chấm tròn Accent indicator phát sáng (Online status / Active dot)
     */
    public static void drawAccentDot(float cx, float cy, float radius, boolean active) {
        if (active) {
            // Glow halo nhẹ
            drawCircle(cx, cy, radius + 2.0f, ColorUtils.ACCENT_GLOW.getRGB());
            // Dot gradient Indigo
            drawCircle(cx, cy, radius, ColorUtils.ACCENT_START.getRGB());
        } else {
            drawCircle(cx, cy, radius, ColorUtils.TEXT_MUTED.getRGB());
        }
    }
}
