package com.example.dragonmaid.client;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

final class HumanoidBannerLayer extends RenderLayer<DragonBaseEntity, HumanoidModel<DragonBaseEntity>> {
    private final ItemInHandRenderer items;

    HumanoidBannerLayer(HumanoidDragonRenderer parent, Context context) {
        super(parent);
        this.items = context.getItemInHandRenderer();
    }

    public void render(
        PoseStack pose,
        MultiBufferSource buffer,
        int light,
        DragonBaseEntity dragon,
        float limb,
        float amount,
        float partial,
        float age,
        float yaw,
        float pitch
    ) {
        ItemStack itemstack = dragon.getOffhandItem();
        if (!dragon.isInvisible() && itemstack.getItem() instanceof BannerItem) {
            pose.pushPose();
            ((HumanoidModel)this.getParentModel()).body.translateAndRotate(pose);
            CustomHeadLayer.translateToHead(pose, false);
            this.items.renderItem(dragon, itemstack, ItemDisplayContext.HEAD, false, pose, buffer, light);
            pose.popPose();
        }
    }
}
