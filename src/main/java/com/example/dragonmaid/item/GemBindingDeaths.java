package com.example.dragonmaid.item;

import com.example.dragonmaid.data.HumanoidLife;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * 1.2: 记录已死亡（真正死亡，不是受伤状态）的龙 UUID，用于清除三龙宝石上的失效绑定。
 * 1.21.1: SavedData#save 带 HolderLookup.Provider 参数，Factory 用 (ctor, deserializer) 重载。
 */
@EventBusSubscriber(modid = "dragonmaid")
public final class GemBindingDeaths extends SavedData {
    private static final String NAME = "dragonmaid_gem_deaths";
    private final Set<UUID> dead = new HashSet<>();

    private GemBindingDeaths() {
    }

    public static GemBindingDeaths get(ServerLevel level) {
        return level.getServer()
            .overworld()
            .getDataStorage()
            .computeIfAbsent(new SavedData.Factory<>(GemBindingDeaths::new, GemBindingDeaths::load), NAME);
    }

    private static GemBindingDeaths load(CompoundTag tag, HolderLookup.Provider registries) {
        GemBindingDeaths data = new GemBindingDeaths();

        for (Tag entry : tag.getList("Dead", 10)) {
            CompoundTag value = (CompoundTag)entry;
            if (value.hasUUID("Id")) {
                data.dead.add(value.getUUID("Id"));
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();

        for (UUID id : this.dead) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", id);
            list.add(entry);
        }

        tag.put("Dead", list);
        return tag;
    }

    public boolean contains(UUID id) {
        return this.dead.contains(id);
    }

    public static void markDead(DragonBaseEntity dragon) {
        if (dragon.level() instanceof ServerLevel level && dragon.isTame()) {
            GemBindingDeaths data = get(level);
            if (!data.dead.add(dragon.getUUID())) {
                return;
            }

            data.setDirty();

            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (stack.getItem() instanceof TrinityDragonGemItem) {
                        TrinityDragonGemItem.clearDeadBinding(stack, level);
                    }
                }

                player.containerMenu.broadcastChanges();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent event) {
        if (event.getEntity() instanceof DragonBaseEntity dragon && !event.isCanceled() && !HumanoidLife.isInjured(dragon)) {
            markDead(dragon);
        }
    }
}
