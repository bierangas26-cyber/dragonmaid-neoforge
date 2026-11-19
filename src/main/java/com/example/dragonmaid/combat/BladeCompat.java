package com.example.dragonmaid.combat;

import com.example.dragonmaid.accessory.DragonHands;
import com.example.dragonmaid.entity.DragonTridentFlight;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.lang.reflect.Method;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

/**
 * 1.2.7 战斗系统的跨包桥接层。
 *
 * <p>1.2.7 的战斗代码引用了若干「别的包在本移植仓尚未落地」的符号（accessory/DragonHands 的
 * availableMainHand/unlocked、movement/HumanoidFlight、config/DragonMaidConfig 的长剑段、
 * network 的三个新包、ModParticles.BLADE_SWEEP、ModEntities.DRAGON_TRIDENT、附魔 Holder 化）。
 * 为保证战斗包单独可编译，这里集中做一层**降级桥接**：能本地等价实现的直接实现，能按注册名
 * 解析的按注册名解析，需要他人包的用反射「有则生效、无则静默跳过」。</p>
 *
 * <p>等对应包落地后，可逐项把调用点换回直连（本类保留方法签名即可）。</p>
 */
public final class BladeCompat {
    private static final ResourceLocation BLADE_SWEEP_ID = ResourceLocation.fromNamespaceAndPath("dragonmaid", "blade_sweep");
    private static final ResourceLocation TRIDENT_FLIGHT_ID =
            ResourceLocation.fromNamespaceAndPath("dragonmaid", "dragon_trident_flight");

    private BladeCompat() {
    }

    // ------------------------------------------------------------------ 手部/知识等级

    /** accessory/DragonHands#availableMainHand（1.2.7：三叉戟飞出时主手视为空，由 accessory 包实现）。 */
    public static ItemStack availableMainHand(DragonBaseEntity dragon) {
        return DragonHands.availableMainHand(dragon);
    }

    public static boolean unlocked(int knowledgeLevel) {
        return DragonHands.unlocked(knowledgeLevel);
    }

    // ------------------------------------------------------------------ 飞行桥接（movement 包未落地）

    public static boolean flightAirborne(DragonBaseEntity dragon) {
        return com.example.dragonmaid.movement.HumanoidFlight.airborne(dragon);
    }

    public static boolean flightValidRider(DragonBaseEntity dragon, Entity rider) {
        return rider instanceof net.minecraft.world.entity.player.Player player
                && com.example.dragonmaid.movement.HumanoidFlight.validRider(dragon, player);
    }

    public static boolean flightEmptyHands(LivingEntity rider) {
        return rider instanceof net.minecraft.world.entity.player.Player player
                && com.example.dragonmaid.movement.HumanoidFlight.emptyHands(player);
    }

    public static boolean flightWantsGuard(DragonBaseEntity dragon) {
        return com.example.dragonmaid.movement.HumanoidFlight.of(dragon).wantsGuard();
    }

    // ------------------------------------------------------------------ config（长剑段未落地）

    private static int ticks(double seconds) {
        return (int) Math.round(Math.max(0.0, seconds) * 20.0);
    }

    public static int guardDelayTicks() {
        return ticks(com.example.dragonmaid.DragonMaidConfig.LONGSWORD_GUARD_DELAY.get());
    }

    public static int parryWindowTicks() {
        return ticks(com.example.dragonmaid.DragonMaidConfig.LONGSWORD_PARRY_WINDOW.get());
    }

    public static int parryCooldownTicks() {
        return ticks(com.example.dragonmaid.DragonMaidConfig.LONGSWORD_PARRY_COOLDOWN.get());
    }

    public static boolean secondChanceTriggersCooldown() {
        return com.example.dragonmaid.DragonMaidConfig.LONGSWORD_SECOND_CHANCE_COOLDOWN.get();
    }

    // ------------------------------------------------------------------ 附魔（1.21.1 Holder 化）

    public static int enchantLevel(ItemStack stack, ResourceKey<Enchantment> key) {
        int best = 0;
        for (var entry : stack.getEnchantments().entrySet()) {
            if (entry.getKey().is(key)) {
                best = Math.max(best, entry.getIntValue());
            }
        }
        return best;
    }

    public static int riptide(ItemStack stack) {
        return enchantLevel(stack, Enchantments.RIPTIDE);
    }

    public static int loyalty(ItemStack stack) {
        return enchantLevel(stack, Enchantments.LOYALTY);
    }

    public static int sweepingEdge(ItemStack stack) {
        return enchantLevel(stack, Enchantments.SWEEPING_EDGE);
    }

    /**
     * EnchantmentHelper#getDamageBonus(ItemStack, MobType) 的 1.21.1 等价实现：
     * MobType 已删除，改按 EntityTypeTags 判定，数值与上游一致（Smite/Bane 各 2.5/级）。
     */
    public static float damageBonus(ItemStack stack, LivingEntity target) {
        if (target == null) {
            return 0.0F;
        }
        EntityType<?> type = target.getType();
        float bonus = 0.0F;
        if (type.is(EntityTypeTags.UNDEAD)) {
            bonus += 2.5F * (float) enchantLevel(stack, Enchantments.SMITE);
        }
        if (type.is(EntityTypeTags.ARTHROPOD) || type.is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) {
            bonus += 2.5F * (float) enchantLevel(stack, Enchantments.BANE_OF_ARTHROPODS);
        }
        return bonus;
    }

    /** EnchantmentHelper.doPostHurtEffects/doPostDamageEffects 的 1.21.1 等价实现。 */
    public static void postAttackEffects(LivingEntity attacker, LivingEntity victim) {
        if (attacker != null && victim != null && victim.level() instanceof ServerLevel level) {
            DamageSource source = attacker.damageSources().mobAttack(attacker);
            EnchantmentHelper.doPostAttackEffects(level, victim, source);
        }
    }

    // ------------------------------------------------------------------ 三叉戟飞行追踪（accessory/DragonHands 的 flight 字段未落地）

    public static boolean tridentAway(DragonBaseEntity dragon) {
        return DragonHands.inventory(dragon).tridentAway();
    }

    public static boolean trackFlight(DragonBaseEntity dragon, DragonTridentFlight flight) {
        return DragonHands.inventory(dragon).trackFlight(flight);
    }

    public static boolean ownsFlight(DragonBaseEntity dragon, DragonTridentFlight flight) {
        return DragonHands.inventory(dragon).ownsFlight(flight);
    }

    public static void finishFlight(DragonBaseEntity dragon, DragonTridentFlight flight) {
        DragonHands.inventory(dragon).finishFlight(flight);
    }

    // ------------------------------------------------------------------ 注册项桥接

    public static SimpleParticleType bladeSweepParticle() {
        return com.example.dragonmaid.particle.ModParticles.BLADE_SWEEP.get();
    }

    /** ModEntities.DRAGON_TRIDENT 未落地前按注册名解析（core 注册后自动生效）。 */
    @SuppressWarnings("unchecked")
    public static EntityType<DragonTridentFlight> tridentFlightType() {
        return (EntityType<DragonTridentFlight>) (EntityType<?>) BuiltInRegistries.ENTITY_TYPE.get(TRIDENT_FLIGHT_ID);
    }

    // ------------------------------------------------------------------ 网络包桥接（network 包未落地）

    public static void sendRiposte(ServerPlayer player, Item item, int useSequence, int recoveryTicks) {
        if (player == null || item == null) {
            return;
        }
        com.example.dragonmaid.network.PlayerRiposteMessage.send(player, item, useSequence, recoveryTicks);
    }

    public static void sendKatanaDraw(ServerPlayer viewer, ServerPlayer wielder) {
        if (viewer == null || wielder == null) {
            return;
        }
        com.example.dragonmaid.network.PlayerKatanaDrawMessage.send(viewer, wielder);
    }

    /** 第一人称拔刀动画（client/PlayerBladeAnimation 未落地时的反射桥）。 */
    public static void clientReleaseBlade(net.minecraft.world.entity.player.Player player, ItemStack stack, int ticks) {
        try {
            Class<?> type = Class.forName("com.example.dragonmaid.client.PlayerBladeAnimation");
            Method target = type.getMethod("release", net.minecraft.world.entity.player.Player.class, ItemStack.class, int.class);
            target.invoke(null, player, stack, ticks);
        } catch (Throwable ignored) {
            // 客户端第一人称动画尚未移植：服务端判定不受影响。
        }
    }

    // ------------------------------------------------------------------ data/DragonState 1.2.7 新方法桥接

    /**
     * 1.2.7 的 {@code DragonState#dragonmaid$setBladeMotion(action, started)}。
     * data 包尚未补该接口方法时，退化为 setAction（拔刀动作时间轴会丢起始帧，不影响命中判定）。
     */
    public static void setBladeMotion(com.example.dragonmaid.data.DragonState state, int action, int started) {
        try {
            Method target = com.example.dragonmaid.data.DragonState.class
                    .getMethod("dragonmaid$setBladeMotion", int.class, int.class);
            target.invoke(state, action, started);
        } catch (Throwable ignored) {
            state.dragonmaid$setAction(action);
        }
    }

    /** 1.2.7 的 {@code DragonState#dragonmaid$isReturningToRest()}；未落地时视为 false。 */
    public static boolean isReturningToRest(com.example.dragonmaid.data.DragonState state) {
        try {
            Method target = com.example.dragonmaid.data.DragonState.class.getMethod("dragonmaid$isReturningToRest");
            Object result = target.invoke(state);
            return result instanceof Boolean flag && flag;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** 供渲染/粒子代码取当前游戏时间，避免各调用点各写一份。 */
    public static long gameTime(Level level) {
        return level == null ? 0L : level.getGameTime();
    }
}