package com.casxduois.client.modules.render;

import com.casxduois.client.config.BooleanSetting;
import com.casxduois.client.config.NumberSetting;
import com.casxduois.client.event.EventBus;
import com.casxduois.client.event.events.EventRender3D;
import com.casxduois.client.modules.Category;
import com.casxduois.client.modules.Module;
import com.casxduois.client.util.RenderUtils3D;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

/**
 * ESPModule: Định vị người chơi và rương kho đồ qua tường theo phong cách Cyberpunk Cyan
 */
public class ESPModule extends Module {

    public static final Color COLOR_PLAYER = new Color(0, 242, 254);        // Neon Cyan #00F2FE
    public static final Color COLOR_CHEST = new Color(255, 159, 67);        // Neon Orange #FF9F43
    public static final Color COLOR_ENDER_CHEST = new Color(168, 85, 247);  // Neon Purple #A855F7

    public final BooleanSetting players = new BooleanSetting("Players", "ESP Người Chơi", "Khung hộp 3D Cyan bao quanh người chơi", true);
    public final BooleanSetting chests = new BooleanSetting("Chests", "ESP Rương Thường", "Viền hộp Cam cho rương gỗ", true);
    public final BooleanSetting enderChests = new BooleanSetting("Ender Chests", "ESP Rương Ender", "Viền hộp Tím cho rương Ender", true);
    public final NumberSetting maxDistance = new NumberSetting("Max Distance", "Khoảng Cách Quét", "Khoảng cách tối đa để hiện ESP (blocks)", 128.0, 16.0, 256.0, 8.0);
    public final BooleanSetting boxFill = new BooleanSetting("Box Fill", "Tô Kính Mờ", "Đổ màu bán trong suốt cho các mặt hộp", true);
    public final BooleanSetting drawNametags = new BooleanSetting("Nametags", "Tên & Khoảng Cách", "Hiển thị tên người chơi và số mét", true);

    public ESPModule() {
        super("ESP", "ESP", "ESP Xuyên Tường", null, Keyboard.KEY_NONE);
        addSetting(players);
        addSetting(chests);
        addSetting(enderChests);
        addSetting(maxDistance);
        addSetting(boxFill);
        addSetting(drawNametags);
        setToggled(true);
    }

    @EventBus.Subscribe
    public void onRender3D(EventRender3D event) {
        if (mc.world == null || mc.player == null) {
            return;
        }

        float partialTicks = event.getPartialTicks();
        double maxDistSq = maxDistance.getValue() * maxDistance.getValue();

        // 1. ESP Người chơi
        if (players.isEnabled()) {
            for (EntityPlayer player : mc.world.playerEntities) {
                if (player == mc.player || player.isDead) continue;

                double distSq = mc.player.getDistanceSq(player);
                if (distSq > maxDistSq) continue;

                double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
                double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
                double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

                double w = player.width / 2.0;
                double h = player.height;
                AxisAlignedBB bb = new AxisAlignedBB(x - w, y, z - w, x + w, y + h, z + w);

                RenderUtils3D.drawBox3D(bb, COLOR_PLAYER, 1.8F, boxFill.isEnabled(), 35);

                if (drawNametags.isEnabled()) {
                    double dist = Math.sqrt(distSq);
                    String tag = String.format("%s §b[%.1fm]", player.getName(), dist);
                    RenderUtils3D.drawNametag(tag, x, y + h + 0.35, z, COLOR_PLAYER);
                }
            }
        }

        // 2. ESP Rương đồ & Rương Ender
        if (chests.isEnabled() || enderChests.isEnabled()) {
            Set<BlockPos> processedChests = new HashSet<>();

            for (TileEntity tile : mc.world.loadedTileEntityList) {
                if (tile == null) continue;

                BlockPos pos = tile.getPos();
                double distSq = mc.player.getDistanceSq(pos);
                if (distSq > maxDistSq) continue;

                // Rương thường
                if (chests.isEnabled() && tile instanceof TileEntityChest) {
                    TileEntityChest chest = (TileEntityChest) tile;

                    // Tránh vẽ trùng rương đôi: nếu là nửa phụ (âm), bỏ qua
                    if (chest.adjacentChestXNeg != null || chest.adjacentChestZNeg != null) {
                        continue;
                    }

                    if (!processedChests.add(pos)) {
                        continue;
                    }

                    AxisAlignedBB bb;
                    if (chest.adjacentChestXPos != null) {
                        // Rương đôi trục X
                        bb = new AxisAlignedBB(pos.getX() + 0.0625, pos.getY(), pos.getZ() + 0.0625,
                                pos.getX() + 1.9375, pos.getY() + 0.875, pos.getZ() + 0.9375);
                    } else if (chest.adjacentChestZPos != null) {
                        // Rương đôi trục Z
                        bb = new AxisAlignedBB(pos.getX() + 0.0625, pos.getY(), pos.getZ() + 0.0625,
                                pos.getX() + 0.9375, pos.getY() + 0.875, pos.getZ() + 1.9375);
                    } else {
                        // Rương đơn
                        bb = new AxisAlignedBB(pos.getX() + 0.0625, pos.getY(), pos.getZ() + 0.0625,
                                pos.getX() + 0.9375, pos.getY() + 0.875, pos.getZ() + 0.9375);
                    }

                    RenderUtils3D.drawBox3D(bb, COLOR_CHEST, 1.5F, boxFill.isEnabled(), 30);
                }

                // Rương Ender
                if (enderChests.isEnabled() && tile instanceof TileEntityEnderChest) {
                    AxisAlignedBB bb = new AxisAlignedBB(pos.getX() + 0.0625, pos.getY(), pos.getZ() + 0.0625,
                            pos.getX() + 0.9375, pos.getY() + 0.875, pos.getZ() + 0.9375);

                    RenderUtils3D.drawBox3D(bb, COLOR_ENDER_CHEST, 1.5F, boxFill.isEnabled(), 30);
                }
            }
        }
    }
}
