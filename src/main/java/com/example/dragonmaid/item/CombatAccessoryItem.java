package com.example.dragonmaid.item;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** 1.2: 战斗饰品（9 种 Kind），效果通过 DragonAccessoryBehavior 的 effect 集合表达。 */
public final class CombatAccessoryItem extends DragonAccessoryItem {
    private final CombatAccessoryItem.Kind kind;

    public CombatAccessoryItem(CombatAccessoryItem.Kind kind) {
        this.kind = kind;
    }

    @Override
    public boolean canEquip(ItemStack stack, DragonBaseEntity dragon) {
        return switch (this.kind) {
            case VENOMFANG -> dragon instanceof FireDragonEntity;
            case FROSTY_TAIL -> dragon instanceof IceDragonEntity;
            default -> true;
        };
    }

    @Override
    public double armorBonus(ItemStack stack, DragonBaseEntity dragon) {
        return this.kind == CombatAccessoryItem.Kind.GUARDIAN_THORNFEATHER ? SustainingAccessoryItem.wardingArmorBonus(dragon) : 0.0;
    }

    @Override
    public Set<ResourceLocation> activeEffects(ItemStack stack, DragonBaseEntity dragon) {
        if (!this.canEquip(stack, dragon)) {
            return Set.of();
        } else {
            return switch (this.kind) {
                case VENOMFANG -> Set.of(FIRE_FANG);
                case FROSTY_TAIL -> Set.of(ICE_TAIL);
                case MAJESTY -> Set.of(ROAR_MAJESTY);
                case THORNFEATHER -> Set.of(THORNS);
                case MINI -> Set.of(MINIATURE);
                case WILD -> Set.of(MELEE_DAMAGE, WILD_RHYTHM);
                case RENDING -> Set.of(MELEE_DAMAGE, CLAW_SWEEP);
                case GUARDIAN_THORNFEATHER -> Set.of(ARMOR, THORNS);
                case HUNTER_KING -> Set.of(MELEE_DAMAGE, WILD_RHYTHM, CLAW_SWEEP);
            };
        }
    }

    @Override
    public Set<ResourceLocation> exclusiveEffects(ItemStack stack, DragonBaseEntity dragon) {
        Set<ResourceLocation> keys = new HashSet<>(this.activeEffects(stack, dragon));
        if (this.kind == CombatAccessoryItem.Kind.VENOMFANG) {
            keys.add(REGENERATION);
        }

        return keys;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String key = "tooltip.dragonmaid." + this.kind.id;
        tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(key + ".2").withStyle(ChatFormatting.GRAY));
        if (this.kind == CombatAccessoryItem.Kind.GUARDIAN_THORNFEATHER
            || this.kind == CombatAccessoryItem.Kind.WILD
            || this.kind == CombatAccessoryItem.Kind.HUNTER_KING) {
            tooltip.add(Component.translatable(key + ".3").withStyle(ChatFormatting.GRAY));
        }
    }

    public static enum Kind {
        MAJESTY("majesty_eye_pendant"),
        THORNFEATHER("thornfeather_charm"),
        VENOMFANG("venomfang_pendant"),
        FROSTY_TAIL("frosty_tail_fin"),
        WILD("wild_pendant"),
        RENDING("rending_claw"),
        MINI("miniaturization_charm"),
        GUARDIAN_THORNFEATHER("guardian_thornfeather_emblem"),
        HUNTER_KING("hunter_king_pendant");

        private final String id;

        private Kind(String id) {
            this.id = id;
        }
    }
}
