package com.example.dragonmaid.menu;

import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.HumanoidLife;
import com.example.dragonmaid.data.InjuryRecovery;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.item.DragonHornItem;
import com.iafenvoy.iceandfire.item.SummoningCrystalItem;
import com.iafenvoy.iceandfire.registry.IafItems;
import java.lang.reflect.Method;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.util.ObfuscationReflectionHelper;

/**
 * 1.2.7: decides whether a right-click on a humanoid dragon should be handed to the vanilla item
 * (native IAF interactions, food, tools) or consumed by the Dragon Maid panel / command cycling.
 *
 * <p>Port note: {@code TagKey} must be built through {@code Registries.ITEM} in 1.21.1, and the IAF
 * item classes were renamed (ItemDragonHorn -> DragonHornItem, ItemSummoningCrystal ->
 * SummoningCrystalItem, IafItemRegistry -> IafItems).
 */
public final class DragonPanelInteraction {
    public static final TagKey<Item> PRIORITY_ITEMS =
        TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("dragonmaid", "dragon_interaction_priority"));

    private static final Method USE = ObfuscationReflectionHelper.findMethod(
        Item.class, "use", Level.class, Player.class, InteractionHand.class
    );
    private static final Method INTERACT = ObfuscationReflectionHelper.findMethod(
        Item.class, "interactLivingEntity", ItemStack.class, Player.class, LivingEntity.class, InteractionHand.class
    );
    private static final ClassValue<Boolean> CUSTOM_ACTION = new ClassValue<Boolean>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod(USE.getName(), USE.getParameterTypes()).getDeclaringClass() != Item.class
                    || type.getMethod(INTERACT.getName(), INTERACT.getParameterTypes()).getDeclaringClass() != Item.class;
            } catch (SecurityException | ReflectiveOperationException exception) {
                return true;
            }
        }
    };

    private DragonPanelInteraction() {
    }

    private static boolean canCycleCommand(DragonBaseEntity dragon) {
        return DragonForms.isHumanoid(dragon) && dragon.isAlive() && !dragon.isModelDead() && !HumanoidLife.isInjured(dragon);
    }

    private static boolean nativeDragonItem(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof DragonHornItem
            || item instanceof SummoningCrystalItem
            || item == IafItems.DRAGON_STAFF.get()
            || item == IafItems.DRAGON_DEBUG_STICK.get()
            || item == IafItems.DRAGON_MEAL.get()
            || item == IafItems.CREATIVE_DRAGON_MEAL.get()
            || item == IafItems.SICKLY_DRAGON_MEAL.get()
            || stack.is(HumanoidLife.EXECUTION_TOOLS);
    }

    public static boolean hasPriorityAction(ItemStack stack) {
        return !stack.isEmpty()
            && (stack.is(PRIORITY_ITEMS) || nativeDragonItem(stack) || stack.has(DataComponents.FOOD)
                || CUSTOM_ACTION.get(stack.getItem().getClass()));
    }

    public static boolean deferToPlayer(DragonBaseEntity dragon, Player player, InteractionHand hand) {
        if (!DragonAccessories.canManage(dragon, player)) {
            return false;
        }

        if (!player.isShiftKeyDown()) {
            return canCycleCommand(dragon) && player.getMainHandItem().isEmpty() && player.getItemInHand(hand).isEmpty();
        }

        if (hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty() && !player.getOffhandItem().isEmpty()) {
            return true;
        }

        ItemStack held = player.getItemInHand(hand);
        return HumanoidLife.isInjured(dragon) && !held.isEmpty() && !nativeDragonItem(held)
            && !InjuryRecovery.nativeInteraction(dragon, player, hand);
    }

    public static InteractionResult afterPass(DragonBaseEntity dragon, Player player, InteractionResult result) {
        if (result != InteractionResult.PASS
            || !DragonAccessories.canManage(dragon, player)
            || hasPriorityAction(player.getMainHandItem())
            || hasPriorityAction(player.getOffhandItem())) {
            return result;
        }

        if (player.isShiftKeyDown()) {
            // IAF-CE: DragonBaseEntity is the MenuProvider; openInventory(Player) is gone.
            player.openMenu(dragon);
        } else {
            if (!canCycleCommand(dragon)) {
                return result;
            }

            if (!player.level().isClientSide) {
                cycleCommand(dragon, player);
            }
        }

        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    private static void cycleCommand(DragonBaseEntity dragon, Player player) {
        int previous = DragonForms.state(dragon).dragonmaid$isCombatInterruptedRest() ? 1 : dragon.getCommand();
        int command = Math.floorMod(previous + 1, 3);
        dragon.setCommand(command);

        String mode = switch (command) {
            case 1 -> "rest";
            case 2 -> "escort";
            default -> "wander";
        };
        player.displayClientMessage(Component.translatable("message.dragonmaid.command." + mode), true);
    }
}
