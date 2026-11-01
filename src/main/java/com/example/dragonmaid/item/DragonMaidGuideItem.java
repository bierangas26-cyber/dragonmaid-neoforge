package com.example.dragonmaid.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import vazkii.patchouli.api.PatchouliAPI;

public final class DragonMaidGuideItem extends Item {
    public static final ResourceLocation BOOK_ID = ResourceLocation.fromNamespaceAndPath("dragonmaid", "dragon_maid_guide");

    public DragonMaidGuideItem(Properties properties) {
        super(properties);
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverplayer) {
            PatchouliAPI.get().openBookGUI(serverplayer, BOOK_ID);
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
