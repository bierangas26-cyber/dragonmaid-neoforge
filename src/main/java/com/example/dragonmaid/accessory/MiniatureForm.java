package com.example.dragonmaid.accessory;

import com.example.dragonmaid.compat.net.PacketDistributor;
import com.example.dragonmaid.config.CompanionBalanceConfig;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.network.DragonNetwork;
import com.example.dragonmaid.network.MiniatureSizeMessage;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.util.dragon.DragonSize;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 1.2: 迷你化（Faerie's Wish 饰品）——龙实际年龄/阶段不变，只是视觉小 + 可被玩家扛在肩上。
 * 状态位（small / miniWorn）由 mixin/DragonMixin 的 MiniatureForm.Holder 保存，本类只做规则与同步。
 *
 * 移植说明（1.21.1 + IAF CE 2.2）：
 * - Forge PacketDistributor 用 port 的 compat.net.PacketDistributor；
 * - Entity#isAddedToWorld() 更名 isAddedToLevel()；
 * - EntityDimensions 的字段是 record 访问器 width()/height()；
 * - 上游 growth_stages[0] 在 CE 2.2 由 DragonSize.getSize(1) 提供（x0/step 语义一致）；
 * - 直接复用 data/DragonForms 1.2 的 humanoidDimensions(dragon,pose,small) 与 containedInLoadedWorld。
 */
@EventBusSubscriber(
    modid = "dragonmaid"
)
public final class MiniatureForm {
    // 1.21.1 的 AttributeModifier 用 ResourceLocation 作 id（1.20.1 是 UUID）
    private static final ResourceLocation MOVEMENT_MODIFIER = ResourceLocation.fromNamespaceAndPath("dragonmaid", "miniature_movement");

    private MiniatureForm() {
    }

    public static boolean hasDragonEffect(DragonBaseEntity dragon) {
        return isSmall(dragon) && ((MiniatureForm.Holder)dragon).dragonmaid$miniWorn() && !DragonForms.isHumanoid(dragon);
    }

    public static double movementLimit(DragonBaseEntity dragon) {
        CompanionBalanceConfig.Values companionbalanceconfig$values = CompanionBalanceConfig.values();
        return (dragon.minimumSpeed + (dragon.maximumSpeed - dragon.minimumSpeed) * (double)companionbalanceconfig$values.movementAge() / 125.0)
            * companionbalanceconfig$values.movementMultiplier();
    }

    public static void refreshMovement(DragonBaseEntity dragon) {
        if (!dragon.level().isClientSide) {
            AttributeInstance attributeinstance = dragon.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attributeinstance != null) {
                double d0 = hasDragonEffect(dragon) ? Math.min(0.0, movementLimit(dragon) - attributeinstance.getBaseValue()) : 0.0;
                AttributeModifier attributemodifier = attributeinstance.getModifier(MOVEMENT_MODIFIER);
                if (attributemodifier == null || attributemodifier.amount() != d0) {
                    attributeinstance.removeModifier(MOVEMENT_MODIFIER);
                    if (d0 != 0.0) {
                        attributeinstance.addTransientModifier(new AttributeModifier(MOVEMENT_MODIFIER, d0, Operation.ADD_VALUE));
                    }
                }
            }
        }
    }

    public static boolean isSmall(DragonBaseEntity dragon) {
        return ((MiniatureForm.Holder)dragon).dragonmaid$small();
    }

    public static boolean isShoulderMini(DragonBaseEntity dragon) {
        return isSmall(dragon)
            && ((MiniatureForm.Holder)dragon).dragonmaid$miniWorn()
            && !DragonForms.isHumanoid(dragon)
            && dragon.isTame()
            && dragon.getOwnerUUID() != null
            && dragon.isAlive()
            && !dragon.isModelDead();
    }

    public static boolean isMatureMini(DragonBaseEntity dragon) {
        return isShoulderMini(dragon) && dragon.getDragonStage() >= 3;
    }

    public static float visualSize(DragonBaseEntity dragon) {
        float f = dragon.getRenderSize();
        int i = CompanionBalanceConfig.values().visualAge();
        if (isSmall(dragon) && dragon.getAgeInDays() > i) {
            DragonSize dragonsize = DragonSize.getSize(1);
            return Math.min(f, dragonsize.x0() + dragonsize.step() / 25.0F * (float)i);
        } else {
            return f;
        }
    }

    public static int humanoidModelStage(DragonBaseEntity dragon) {
        return isSmall(dragon) ? 3 : dragon.getDragonStage();
    }

    public static void equipmentChanged(DragonBaseEntity dragon, boolean effect) {
        MiniatureForm.Holder miniatureform$holder = (MiniatureForm.Holder)dragon;
        if (!dragon.level().isClientSide && !miniatureform$holder.dragonmaid$readingSize()) {
            boolean flag = effect && dragon.isTame() && dragon.getOwnerUUID() != null;
            boolean flag1 = miniatureform$holder.dragonmaid$miniWorn();
            if (flag1 == flag && isSmall(dragon) == flag) {
                refreshMovement(dragon);
            } else {
                apply(dragon, flag);
                if (flag1 && !flag && isSmall(dragon) && dragon.getOwner() instanceof ServerPlayer serverplayer) {
                    serverplayer.displayClientMessage(Component.translatable("message.dragonmaid.miniature_waiting"), true);
                }
            }
        }
    }

    public static void retry(DragonBaseEntity dragon) {
        MiniatureForm.Holder miniatureform$holder = (MiniatureForm.Holder)dragon;
        if (miniatureform$holder.dragonmaid$small() != miniatureform$holder.dragonmaid$miniWorn()
            && (dragon.tickCount + dragon.getId()) % 20 == 0) {
            apply(dragon, miniatureform$holder.dragonmaid$miniWorn());
        }
    }

    private static void apply(DragonBaseEntity dragon, boolean worn) {
        MiniatureForm.Holder miniatureform$holder = (MiniatureForm.Holder)dragon;
        boolean flag = miniatureform$holder.dragonmaid$small();
        boolean flag1 = worn;
        if (worn && !flag && dragon.getPassengers().stream().anyMatch(Player.class::isInstance)) {
            flag1 = false;
        }

        if (!worn && flag && !canGrowHere(dragon)) {
            flag1 = true;
        }

        if (flag != flag1 || miniatureform$holder.dragonmaid$miniWorn() != worn) {
            setState(dragon, flag1, worn);
            if (dragon.isAddedToLevel()) {
                DragonNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> dragon), MiniatureSizeMessage.of(dragon));
            }
        }
    }

    public static void setState(DragonBaseEntity dragon, boolean small, boolean worn) {
        boolean flag = isSmall(dragon) != small;
        ((MiniatureForm.Holder)dragon).dragonmaid$sizeState(small, worn);
        refreshMovement(dragon);
        if (flag) {
            dragon.refreshDimensions();
        }
    }

    private static boolean canGrowHere(DragonBaseEntity dragon) {
        if (!dragon.isAddedToLevel()) {
            return false;
        } else {
            EntityDimensions entitydimensions = DragonForms.isHumanoid(dragon)
                ? DragonForms.humanoidDimensions(dragon, dragon.getPose(), false)
                : dragon.getType().getDimensions().scale(Math.min(dragon.getRenderSize() * 0.35F, 7.0F));
            if (dragon.isPassenger()) {
                if (!(dragon.getVehicle() instanceof Player player)) {
                    return false;
                }

                if (dragon.getDragonStage() > 1) {
                    return landFromShoulder(dragon, player, entitydimensions);
                }
            }

            return entitydimensions.width() <= dragon.getBbWidth() && entitydimensions.height() <= dragon.getBbHeight()
                ? true
                : clear(dragon, entitydimensions.makeBoundingBox(dragon.position()));
        }
    }

    private static boolean landFromShoulder(DragonBaseEntity dragon, Player rider, EntityDimensions full) {
        if (!rider.onGround()) {
            return false;
        } else {
            double d0 = (double)(full.width() + rider.getBbWidth()) * 0.5 + 0.25;

            for (Direction direction : Plane.HORIZONTAL) {
                Vec3 vec3 = rider.position().add((double)direction.getStepX() * d0, 0.0, (double)direction.getStepZ() * d0);
                AABB aabb = full.makeBoundingBox(vec3);
                AABB aabb1 = dragon.getBoundingBox().move(vec3.subtract(dragon.position()));
                if (clear(dragon, aabb) && clear(dragon, aabb1)) {
                    BlockPos blockpos = BlockPos.containing(vec3.x, vec3.y - 0.01, vec3.z);
                    if (dragon.level().getBlockState(blockpos).isFaceSturdy(dragon.level(), blockpos, Direction.UP)) {
                        dragon.stopRiding();
                        if (dragon.isPassenger()) {
                            return false;
                        }

                        dragon.moveTo(vec3.x, vec3.y, vec3.z, dragon.getYRot(), dragon.getXRot());
                        return true;
                    }
                }
            }

            return false;
        }
    }

    private static boolean clear(DragonBaseEntity dragon, AABB box) {
        AABB aabb = box.deflate(1.0E-4);
        return DragonForms.containedInLoadedWorld(dragon, aabb)
            && !dragon.level().getBlockCollisions(dragon, aabb).iterator().hasNext()
            && dragon.level().getEntities(dragon, aabb, other -> !dragon.isPart(other) && dragon.canCollideWith(other)).isEmpty();
    }

    @SubscribeEvent
    public static void track(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer serverplayer
            && event.getTarget() instanceof DragonBaseEntity entitydragonbase
            && (isSmall(entitydragonbase) || ((MiniatureForm.Holder)entitydragonbase).dragonmaid$miniWorn())) {
            DragonNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverplayer), MiniatureSizeMessage.of(entitydragonbase));
        }
    }

    public interface Holder {
        boolean dragonmaid$small();

        boolean dragonmaid$miniWorn();

        boolean dragonmaid$readingSize();

        void dragonmaid$sizeState(boolean small, boolean worn);
    }
}
