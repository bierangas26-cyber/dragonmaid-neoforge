package com.example.dragonmaid.menu;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.world.Container;

public interface DragonDetailsMenu {
    int ACCESSORY_START = 41;
    /** 1.2.7: five accessory slots (was three) — growth stage grants 1-2 and knowledge the rest. */
    int ACCESSORY_END = 46;

    /** Screen position of accessory slot (first three in a column, the last two underneath). */
    static int accessoryX(int slot) {
        return slot < 3 ? 153 : 8;
    }

    static int accessoryY(int slot) {
        return slot < 3 ? 54 + slot * 18 : 72 + (slot - 3) * 18;
    }

    DragonBaseEntity dragonmaid$viewDragon();

    Container dragonmaid$accessories();

    int dragonmaid$knowledgeLevel();

    int dragonmaid$unlockedSlots();
}
