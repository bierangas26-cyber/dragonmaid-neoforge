package com.example.dragonmaid.accessory;

import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;

public final class DragonStorage {
    public static final int CAPACITY = 27;

    private DragonStorage() {
    }

    public static int unlockedSlots(DragonBaseEntity dragon) {
        if (dragon == null) {
            return 0;
        } else {
            if (!dragon.level().isClientSide) {
                DragonAccessories.refresh(dragon);
            }

            return DragonForms.state(dragon).dragonmaid$getAccessories().effects.storageSlots;
        }
    }
}
