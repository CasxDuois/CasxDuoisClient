package com.casxduois.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

/**
 * RenderUtils3D: Bộ công cụ dựng hình 3D trong không gian thế giới (ESP, Box 3D, Nametags)
 * Đảm bảo push/pop ma trận và thuộc tính OpenGL an toàn, không làm hỏng render pipeline của game.
 */
public class RenderUtils3D {

    private static final Minecraft mc = Minecraft.getMinecraft();

    /**
     * Chuẩn bị trạng thái OpenGL cho việc render 3D xuyên tường
     */
    public static void setupRender3D() {
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
    }

    /**
     * Khôi phục toàn bộ trạng thái OpenGL sau khi vẽ
     */
    public static void endRender3D() {
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    /**
     * Vẽ Box 3D theo AxisAlignedBB với đường viền sắc nét và mặt kính mờ (Cyberpunk Style)
     */
    public static void drawBox3D(AxisAlignedBB bb, Color color, float lineWidth, boolean fill, int fillAlpha) {
        setupRender3D();

        double renderX = mc.getRenderManager().viewerPosX;
        double renderY = mc.getRenderManager().viewerPosY;
        double renderZ = mc.getRenderManager().viewerPosZ;

        AxisAlignedBB box = new AxisAlignedBB(
                bb.minX - renderX, bb.minY - renderY, bb.minZ - renderZ,
                bb.maxX - renderX, bb.maxY - renderY, bb.maxZ - renderZ
        );

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // 1. Tô màu các mặt trong suốt nếu được bật
        if (fill) {
            GlStateManager.color(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, fillAlpha / 255.0F);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

            // Dưới
            buffer.pos(box.minX, box.minY, box.minZ).endVertex();
            buffer.pos(box.maxX, box.minY, box.minZ).endVertex();
            buffer.pos(box.maxX, box.minY, box.maxZ).endVertex();
            buffer.pos(box.minX, box.minY, box.maxZ).endVertex();

            // Trên
            buffer.pos(box.minX, box.maxY, box.minZ).endVertex();
            buffer.pos(box.minX, box.maxY, box.maxZ).endVertex();
            buffer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
            buffer.pos(box.maxX, box.maxY, box.minZ).endVertex();

            // Bắc (Z min)
            buffer.pos(box.minX, box.minY, box.minZ).endVertex();
            buffer.pos(box.minX, box.maxY, box.minZ).endVertex();
            buffer.pos(box.maxX, box.maxY, box.minZ).endVertex();
            buffer.pos(box.maxX, box.minY, box.minZ).endVertex();

            // Nam (Z max)
            buffer.pos(box.minX, box.minY, box.maxZ).endVertex();
            buffer.pos(box.maxX, box.minY, box.maxZ).endVertex();
            buffer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
            buffer.pos(box.minX, box.maxY, box.maxZ).endVertex();

            // Tây (X min)
            buffer.pos(box.minX, box.minY, box.minZ).endVertex();
            buffer.pos(box.minX, box.minY, box.maxZ).endVertex();
            buffer.pos(box.minX, box.maxY, box.maxZ).endVertex();
            buffer.pos(box.minX, box.maxY, box.minZ).endVertex();

            // Đông (X max)
            buffer.pos(box.maxX, box.minY, box.minZ).endVertex();
            buffer.pos(box.maxX, box.maxY, box.minZ).endVertex();
            buffer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
            buffer.pos(box.maxX, box.minY, box.maxZ).endVertex();

            tessellator.draw();
        }

        // 2. Vẽ 12 đường viền sắc nét (Wireframe Lines)
        GL11.glLineWidth(lineWidth);
        GlStateManager.color(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);

        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);

        // Khung đáy
        buffer.pos(box.minX, box.minY, box.minZ).endVertex();
        buffer.pos(box.maxX, box.minY, box.minZ).endVertex();

        buffer.pos(box.maxX, box.minY, box.minZ).endVertex();
        buffer.pos(box.maxX, box.minY, box.maxZ).endVertex();

        buffer.pos(box.maxX, box.minY, box.maxZ).endVertex();
        buffer.pos(box.minX, box.minY, box.maxZ).endVertex();

        buffer.pos(box.minX, box.minY, box.maxZ).endVertex();
        buffer.pos(box.minX, box.minY, box.minZ).endVertex();

        // Khung đỉnh
        buffer.pos(box.minX, box.maxY, box.minZ).endVertex();
        buffer.pos(box.maxX, box.maxY, box.minZ).endVertex();

        buffer.pos(box.maxX, box.maxY, box.minZ).endVertex();
        buffer.pos(box.maxX, box.maxY, box.maxZ).endVertex();

        buffer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
        buffer.pos(box.minX, box.maxY, box.maxZ).endVertex();

        buffer.pos(box.minX, box.maxY, box.maxZ).endVertex();
        buffer.pos(box.minX, box.maxY, box.minZ).endVertex();

        // 4 cột đứng
        buffer.pos(box.minX, box.minY, box.minZ).endVertex();
        buffer.pos(box.minX, box.maxY, box.minZ).endVertex();

        buffer.pos(box.maxX, box.minY, box.minZ).endVertex();
        buffer.pos(box.maxX, box.maxY, box.minZ).endVertex();

        buffer.pos(box.maxX, box.minY, box.maxZ).endVertex();
        buffer.pos(box.maxX, box.maxY, box.maxZ).endVertex();

        buffer.pos(box.minX, box.minY, box.maxZ).endVertex();
        buffer.pos(box.minX, box.maxY, box.maxZ).endVertex();

        tessellator.draw();

        endRender3D();
    }

    /**
     * Vẽ Nametag 3D trên đầu đối tượng (Tên + Khoảng cách) luôn hướng mặt về camera
     */
    public static void drawNametag(String text, double x, double y, double z, Color color) {
        double renderX = mc.getRenderManager().viewerPosX;
        double renderY = mc.getRenderManager().viewerPosY;
        double renderZ = mc.getRenderManager().viewerPosZ;

        double dX = x - renderX;
        double dY = y - renderY;
        double dZ = z - renderZ;

        double dist = Math.sqrt(dX * dX + dY * dY + dZ * dZ);
        float scale = (float) Math.max(0.015F, 0.0025F * dist);

        FontRenderer font = mc.fontRenderer;
        int width = font.getStringWidth(text) / 2;

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        GlStateManager.translate(dX, dY, dZ);
        GlStateManager.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-scale, -scale, scale);

        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        // Nền đen mờ Cyberpunk
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.disableTexture2D();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(-width - 3, -2, 0.0).color(0.04F, 0.05F, 0.09F, 0.75F).endVertex();
        buffer.pos(-width - 3, 10, 0.0).color(0.04F, 0.05F, 0.09F, 0.75F).endVertex();
        buffer.pos(width + 3, 10, 0.0).color(0.04F, 0.05F, 0.09F, 0.75F).endVertex();
        buffer.pos(width + 3, -2, 0.0).color(0.04F, 0.05F, 0.09F, 0.75F).endVertex();
        tessellator.draw();

        // Viền trên Neon Cyan
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(-width - 3, -2, 0.0).color(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, 0.9F).endVertex();
        buffer.pos(width + 3, -2, 0.0).color(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, 0.9F).endVertex();
        tessellator.draw();

        GlStateManager.enableTexture2D();
        font.drawString(text, -width, 0, color.getRGB());

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }
}
