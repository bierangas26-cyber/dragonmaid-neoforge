package com.example.dragonmaid.compat.jade;

import com.example.dragonmaid.egg.FireEggHatching;
import com.iafenvoy.iceandfire.entity.DragonEggEntity;
import com.iafenvoy.iceandfire.registry.IafDragonTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/** 1.2: Jade 提示 fire dragon egg 的孵化性别（灵魂火 + 维度）。Jade 是可选依赖（libs/ 里仅 compileOnly）。 */
@WailaPlugin
public final class FireEggJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(FireEggJadePlugin.Provider.INSTANCE, DragonEggEntity.class);
    }

    public static enum Provider implements IEntityComponentProvider {
        INSTANCE;

        private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("dragonmaid", "fire_egg_gender");

        @Override
        public ResourceLocation getUid() {
            return ID;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (accessor.getEntity() instanceof DragonEggEntity egg && egg.getEggType().getType() == IafDragonTypes.FIRE) {
                String key = switch (FireEggHatching.expected(egg)) {
                    case MALE -> "jade.dragonmaid.fire_egg.male";
                    case FEMALE -> "jade.dragonmaid.fire_egg.female";
                    case RANDOM -> "jade.dragonmaid.fire_egg.random";
                };
                tooltip.add(Component.translatable("jade.dragonmaid.fire_egg.expected", new Object[]{Component.translatable(key)}));
                return;
            }
        }
    }
}
