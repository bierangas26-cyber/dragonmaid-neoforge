package com.example.dragonmaid.data;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class HumanoidLife {
    public static final TagKey<Item> EXECUTION_TOOLS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("dragonmaid", "mercy_killing_tools"));

    private HumanoidLife() {
    }

    public static boolean isInjured(DragonBaseEntity dragon) {
        return DragonForms.state(dragon).dragonmaid$isLifeTracked() && dragon.isModelDead() && !DragonForms.state(dragon).dragonmaid$isExecuted();
    }

    public static HumanoidLife.Phase phase(DragonBaseEntity dragon) {
        if (!dragon.isModelDead()) {
            return HumanoidLife.Phase.ACTIVE;
        } else if (!DragonForms.state(dragon).dragonmaid$isLifeTracked()) {
            return HumanoidLife.Phase.DEAD;
        } else if (DragonForms.state(dragon).dragonmaid$isExecuted()) {
            return HumanoidLife.Phase.DEAD;
        } else {
            return dragon.modelDeadProgress < 20.0F ? HumanoidLife.Phase.INJURY_ENTER : HumanoidLife.Phase.INJURY_BREATH;
        }
    }

    public static enum Phase {
        ACTIVE(null),
        INJURY_ENTER("animation.dragon_female.injury_enter"),
        INJURY_BREATH("animation.dragon_female.injury_breath"),
        DEAD("animation.dragon_female.dead");

        public final String animationId;

        private Phase(String animationId) {
            this.animationId = animationId;
        }
    }
}
