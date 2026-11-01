package com.example.dragonmaid.menu;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.world.Container;

public interface DragonDetailsMenu {
    int ACCESSORY_START = 41;
    int ACCESSORY_END = 44;

    DragonBaseEntity dragonmaid$viewDragon();

    Container dragonmaid$accessories();

    int dragonmaid$knowledgeLevel();

    int dragonmaid$unlockedSlots();
}
