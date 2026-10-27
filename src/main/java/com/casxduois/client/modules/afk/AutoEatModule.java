package com.casxduois.client.modules.afk;

import com.casxduois.client.config.NumberSetting;
import com.casxduois.client.event.EventBus;
import com.casxduois.client.event.events.EventTick;
import com.casxduois.client.modules.Category;
import com.casxduois.client.modules.Module;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;

public class AutoEatModule extends Module {

    public final NumberSetting minHunger = new NumberSetting("Min Hunger", "Mức Đói Tối Thiểu", "Tự ăn khi thanh đói giảm xuống dưới mức này", 16.0, 6.0, 19.0, 1.0);

    private int oldSlot = -1;
    private boolean isEating = false;

    public AutoEatModule() {
        super("AutoEat", "Tự Động Ăn", "Tự động chọn thức ăn ngon nhất và ăn khi đói", Category.AUTOMATION, Keyboard.KEY_NONE);
        addSetting(minHunger);
    }

    @EventBus.Subscribe
    public void onTick(EventTick event) {
        if (event.getPhase() != EventTick.Phase.START || mc.player == null) return;

        int foodLevel = mc.player.getFoodStats().getFoodLevel();

        if (foodLevel <= minHunger.getValue() && !isEating) {
            int foodSlot = findBestFoodSlot();
            if (foodSlot != -1) {
                oldSlot = mc.player.inventory.currentItem;
                mc.player.inventory.currentItem = foodSlot;
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
                isEating = true;
            }
        } else if (isEating) {
            if (foodLevel >= 20 || !mc.player.getFoodStats().needFood()) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                if (oldSlot != -1) {
                    mc.player.inventory.currentItem = oldSlot;
                    oldSlot = -1;
                }
                isEating = false;
            }
        }
    }

    private int findBestFoodSlot() {
        int bestSlot = -1;
        int maxHeal = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemFood) {
                ItemFood food = (ItemFood) stack.getItem();
                int heal = food.getHealAmount(stack);
                if (heal > maxHeal) {
                    maxHeal = heal;
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    @Override
    public void onDisable() {
        if (isEating) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            if (oldSlot != -1 && mc.player != null) {
                mc.player.inventory.currentItem = oldSlot;
            }
            isEating = false;
        }
    }
}
