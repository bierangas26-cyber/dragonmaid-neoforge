package com.example.dragonmaid.compat.jade;

import com.example.dragonmaid.egg.LightningEggHatching;
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

/** 1.2: Jade 提示 lightning dragon egg 的红石信号档位（>=8 female / 1..7 male / 0 random）。 */
@WailaPlugin
public final class LightningEggJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(LightningEggJadePlugin.Provider.INSTANCE, DragonEggEntity.class);
    }

    public static enum Provider implements IEntityComponentProvider {
        INSTANCE;

        private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("dragonmaid", "lightning_egg_gender");

        @Override
        public ResourceLocation getUid() {
            return ID;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (accessor.getEntity() instanceof DragonEggEntity egg && egg.getEggType().getType() == IafDragonTypes.LIGHTNING) {
                int signal = LightningEggHatching.signal(egg.level(), egg.blockPosition());
                String key = signal == 0
                    ? "jade.dragonmaid.lightning_egg.random"
                    : (signal < 8 ? "jade.dragonmaid.lightning_egg.male" : "jade.dragonmaid.lightning_egg.female");
                tooltip.add(Component.translatable("jade.dragonmaid.lightning_egg.expected", new Object[]{Component.translatable(key)}));
                return;
            }
        }
    }
}
