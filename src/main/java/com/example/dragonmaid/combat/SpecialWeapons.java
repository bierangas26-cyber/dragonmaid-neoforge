package com.example.dragonmaid.combat;

import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.accessory.DragonAccessoryBehavior;
import com.example.dragonmaid.accessory.DragonHands;
import com.example.dragonmaid.data.DragonForms;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;

@EventBusSubscriber(
   modid = "dragonmaid"
)
public final class SpecialWeapons {
   public static final float FIRE_AXE_ARMOR_IGNORED = 0.4F;
   private static final double LONGSWORD_REACH = 1.0;
   private static final ResourceLocation LONGSWORD_REACH_ID = ResourceLocation.fromNamespaceAndPath("dragonmaid", "longsword_reach");
   public static final TagKey<Item> TRIDENTS = tag("tridents");
   public static final TagKey<Item> LONGSWORDS = tag("longswords");
   public static final TagKey<Item> KATANAS = tag("katanas");

   private SpecialWeapons() {
   }

   public static boolean trained(DragonBaseEntity dragon) {
      return dragon.isTame() && DragonForms.isHumanoid(dragon) && BladeCompat.unlocked(DragonForms.state(dragon).dragonmaid$getKnowledgeLevel());
   }

   public static boolean isAxe(ItemStack stack) {
      return stack.getItem() instanceof AxeItem || stack.is(ItemTags.AXES);
   }

   public static boolean isTrident(ItemStack stack) {
      return stack.getItem() instanceof TridentItem || stack.is(TRIDENTS);
   }

   static boolean isIceTrident(DragonBaseEntity dragon, ItemStack stack) {
      return dragon instanceof IceDragonEntity && isTrident(stack);
   }

   public static boolean isLongsword(ItemStack stack) {
      return ownWeapon(stack, LONGSWORDS);
   }

   public static boolean isKatana(ItemStack stack) {
      return ownWeapon(stack, KATANAS);
   }

   public static boolean eligibleForFirstThrust(ItemStack stack) {
      return DragonHands.accepts(0, stack) && !isAxe(stack) && !isTrident(stack) && !isLongsword(stack) && !isKatana(stack);
   }

   static boolean wingGuard(DragonBaseEntity dragon) {
      return dragon instanceof LightningDragonEntity && dragon.isTame() && DragonAccessories.hasEffect(dragon, DragonAccessoryBehavior.WING_GUARD);
   }

   public static boolean fireAxe(DragonBaseEntity dragon, ItemStack stack) {
      return dragon instanceof FireDragonEntity && trained(dragon) && isAxe(stack);
   }

   static double fireAxeDamageMultiplier(float maxHealth) {
      if (maxHealth < 40.0F) {
         return 1.0;
      } else {
         double progress = ((double)Math.min(maxHealth, 400.0F) - 40.0) / 360.0;
         return 1.1 + 0.3 * progress;
      }
   }

   public static boolean throwableIceTrident(DragonBaseEntity dragon, ItemStack stack) {
      return isIceTrident(dragon, stack) && trained(dragon) && BladeCompat.riptide(stack) == 0;
   }

   public static boolean riptideIceTrident(DragonBaseEntity dragon, ItemStack stack) {
      return isIceTrident(dragon, stack) && trained(dragon) && BladeCompat.riptide(stack) > 0;
   }

   public static double longswordExtraReach(DragonBaseEntity dragon) {
      return dragon instanceof LightningDragonEntity && trained(dragon) && isLongsword(BladeCompat.availableMainHand(dragon)) ? 1.0 : 0.0;
   }

   public static boolean canFallbackToLongsword(DragonBaseEntity dragon, boolean shieldAvailable) {
      return !shieldAvailable && longswordExtraReach(dragon) > 0.0;
   }

   @SubscribeEvent
   public static void playerReach(ItemAttributeModifierEvent event) {
      // 1.21.1：ForgeMod.ENTITY_REACH 已并入原版 Attributes.ENTITY_INTERACTION_RANGE；
      // ItemAttributeModifierEvent 不再有 getSlotType()，改由 addModifier 传入 EquipmentSlotGroup。
      if (isLongsword(event.getItemStack())) {
         event.addModifier(
            Attributes.ENTITY_INTERACTION_RANGE,
            new AttributeModifier(LONGSWORD_REACH_ID, 1.0, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND
         );
      }
   }

   private static boolean ownWeapon(ItemStack stack, TagKey<Item> kind) {
      if (!stack.is(kind)) {
         return false;
      } else {
         ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
         return id != null && "dragonmaid".equals(id.getNamespace());
      }
   }

   private static TagKey<Item> tag(String name) {
      return ItemTags.create(ResourceLocation.fromNamespaceAndPath("dragonmaid", name));
   }
}