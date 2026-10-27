package com.casxduois.client.modules.performance;

import com.casxduois.client.config.BooleanSetting;
import com.casxduois.client.config.NumberSetting;
import com.casxduois.client.modules.Category;
import com.casxduois.client.modules.Module;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Keyboard;

/**
 * EntityCullingModule: Tối ưu số lượng thực thể render theo khoảng cách và Frustum
 */
public class EntityCullingModule extends Module {

    public final NumberSetting maxDistance = new NumberSetting("Max Distance", "Khoảng cách hiển thị", "Khoảng cách xa nhất để vẽ quái và động vật", 64.0, 16.0, 128.0, 4.0);
    public final BooleanSetting cullMonsters = new BooleanSetting("Cull Monsters", "Ẩn quái vật ở xa", "Tạm ẩn quái vật khi đứng ngoài tầm nhìn", true);
    public final BooleanSetting cullPassives = new BooleanSetting("Cull Passives", "Ẩn động vật ở xa", "Tạm ẩn gia súc và thú nuôi khi ở xa", true);
    public final BooleanSetting cullPlayers = new BooleanSetting("Cull Players", "Ẩn người chơi ở xa", "Tạm ẩn người chơi khác khi ở ngoài tầm mắt", false);

    private ICamera camera;

    public EntityCullingModule() {
        super("EntityCulling", "Ẩn Sinh Vật Ở Xa", "Tạm ẩn quái và động vật ở xa giúp game mượt hơn", Category.RENDER_LIMIT, Keyboard.KEY_NONE);
        addSetting(maxDistance);
        addSetting(cullMonsters);
        addSetting(cullPassives);
        addSetting(cullPlayers);
        setToggled(true);
    }

    public void setCamera(ICamera camera) {
        this.camera = camera;
    }

    public boolean shouldRender(Entity entity) {
        if (!isToggled() || entity == null || mc.player == null) {
            return true;
        }

        // Không culling chính bản thân người chơi
        if (entity == mc.player) {
            return true;
        }

        // Bỏ qua render các Entity vô hình hoàn toàn (như Armor Stand marker ở sảnh server)
        if (entity.isInvisible() && !entity.isGlowing()) {
            return false;
        }

        double maxDistSq = maxDistance.getValue() * maxDistance.getValue();
        // Tính bình phương khoảng cách trực tiếp qua tọa độ x,y,z để tối ưu tốc độ và an toàn SRG 100%
        double dx = mc.player.posX - entity.posX;
        double dy = mc.player.posY - entity.posY;
        double dz = mc.player.posZ - entity.posZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        if (entity instanceof EntityMob && cullMonsters.isEnabled() && distSq > maxDistSq) {
            return false;
        }

        if (entity instanceof EntityAnimal && cullPassives.isEnabled() && distSq > maxDistSq) {
            return false;
        }

        if (entity instanceof EntityPlayer && cullPlayers.isEnabled() && distSq > maxDistSq) {
            return false;
        }

        // Kiểm tra Frustum camera nếu có
        if (camera != null && !camera.isBoundingBoxInFrustum(entity.getEntityBoundingBox())) {
            return false;
        }

        return true;
    }
}
