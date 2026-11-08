package com.example.dragonmaid.compat.jade;

import com.example.dragonmaid.egg.IceEggHatching;
import com.iafenvoy.iceandfire.item.block.EggInIceBlock;
import com.iafenvoy.iceandfire.item.block.entity.EggInIceBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/** 1.2: Jade 提示 ice dragon egg 的孵化性别（龙冰 / 普通冰）。 */
@WailaPlugin
public final class IceEggJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(IceEggJadePlugin.Provider.INSTANCE, EggInIceBlock.class);
    }

    public static enum Provider implements IBlockComponentProvider {
        INSTANCE;

        private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("dragonmaid", "ice_egg_gender");

        @Override
        public ResourceLocation getUid() {
            return ID;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlockEntity() instanceof EggInIceBlockEntity) {
                String key = switch (IceEggHatching.expected(accessor.getLevel(), accessor.getPosition())) {
                    case MALE -> "jade.dragonmaid.ice_egg.male";
                    case FEMALE -> "jade.dragonmaid.ice_egg.female";
                    case RANDOM -> "jade.dragonmaid.ice_egg.random";
                };
                tooltip.add(Component.translatable("jade.dragonmaid.ice_egg.expected", new Object[]{Component.translatable(key)}));
            }
        }
    }
}
