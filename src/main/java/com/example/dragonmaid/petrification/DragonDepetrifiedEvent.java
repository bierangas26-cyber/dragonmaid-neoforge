package com.example.dragonmaid.petrification;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

public final class DragonDepetrifiedEvent extends Event {
    private final Player player;
    private final DragonBaseEntity dragon;

    public DragonDepetrifiedEvent(Player player, DragonBaseEntity dragon) {
        this.player = player;
        this.dragon = dragon;
    }

    public Player getPlayer() {
        return this.player;
    }

    public DragonBaseEntity getDragon() {
        return this.dragon;
    }
}
