package com.casxduois.client.modules.performance;

import com.casxduois.client.config.BooleanSetting;
import com.casxduois.client.config.NumberSetting;
import com.casxduois.client.modules.Category;
import com.casxduois.client.modules.Module;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import org.lwjgl.input.Keyboard;

/**
 * TileEntityCullingModule: Giới hạn render các TileEntity (rương, lò, hopper, spawner) theo khoảng cách
 * Rất hiệu quả ở các khu căn cứ, kho đồ lớn nhiều rương (giảm giật lag tới 40%)
 */
public class TileEntityCullingModule extends Module {

    public final NumberSetting maxDistance = new NumberSetting("Max Distance", "Khoảng cách hiển thị", "Khoảng cách xa nhất để vẽ rương và các khối chứa đồ", 36.0, 8.0, 96.0, 4.0);
    public final BooleanSetting cullChests = new BooleanSetting("Cull Chests", "Ẩn rương thường ở xa", "Tạm ẩn rương gỗ khi bạn đứng ngoài tầm nhìn", true);
    public final BooleanSetting cullEnderChests = new BooleanSetting("Cull Ender Chests", "Ẩn rương Ender ở xa", "Tạm ẩn rương Ender khi bạn đứng ngoài tầm nhìn", false);
    public final BooleanSetting cullOthers = new BooleanSetting("Cull Others", "Ẩn lò nung và phễu ở xa", "Tạm ẩn lò nung, phễu nhặt đồ và lồng quái khi ở xa", true);

    public TileEntityCullingModule() {
        super("TileEntityCulling", "Ẩn Rương Và Kho Ở Xa", "Giảm giật lag khi đứng gần kho chứa nhiều rương", Category.RENDER_LIMIT, Keyboard.KEY_NONE);
        addSetting(maxDistance);
        addSetting(cullChests);
        addSetting(cullEnderChests);
        addSetting(cullOthers);
        setToggled(true);
    }

    public boolean shouldRender(TileEntity tileEntity) {
        if (!isToggled() || tileEntity == null || mc.player == null) {
            return true;
        }

        double maxDistSq = maxDistance.getValue() * maxDistance.getValue();
        double distSq = tileEntity.getDistanceSq(mc.player.posX, mc.player.posY, mc.player.posZ);

        if (distSq <= maxDistSq) {
            return true;
        }

        if (tileEntity instanceof TileEntityChest && cullChests.isEnabled()) {
            return false;
        }

        if (tileEntity instanceof TileEntityEnderChest && cullEnderChests.isEnabled()) {
            return false;
        }

        if (!(tileEntity instanceof TileEntityChest) && !(tileEntity instanceof TileEntityEnderChest) && cullOthers.isEnabled()) {
            return false;
        }

        return true;
    }
}
