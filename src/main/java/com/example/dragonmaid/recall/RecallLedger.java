package com.example.dragonmaid.recall;

import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.DragonState;
import com.example.dragonmaid.data.HumanoidLife;
import com.example.dragonmaid.data.LifeStateMigration;
import com.example.dragonmaid.learning.Knowledge;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.RegistryAccess;

public final class RecallLedger extends SavedData {
    public static final String ID = "dragonmaid_recall";
    private final Map<UUID, RecallLedger.Entry> entries = new HashMap<>();
    private final Map<UUID, Set<UUID>> owners = new HashMap<>();
    private final Map<UUID, RecallLedger.Transfer> pending = new HashMap<>();
    private final Set<UUID> reserved = new HashSet<>();
    private ServerLevel storageLevel;
    private long revision;

    public RecallLedger() {
    }

    public static RecallLedger get(ServerLevel level) {
        ServerLevel serverlevel = level.getServer().overworld();
        RecallLedger recallledger = serverlevel.getDataStorage().computeIfAbsent(new SavedData.Factory<>(RecallLedger::new, RecallLedger::load), "dragonmaid_recall");
        recallledger.storageLevel = serverlevel;
        return recallledger;
    }

    public RecallLedger.Entry entry(UUID dragon) {
        return this.entries.get(dragon);
    }

    public long revision() {
        return this.revision;
    }

    public List<RecallLedger.Entry> owned(UUID owner) {
        return this.owners
            .getOrDefault(owner, Set.of())
            .stream()
            .map(this.entries::get)
            .filter(e -> e != null && e.state() != RecallLedger.State.DEAD)
            .sorted(Comparator.comparing(RecallLedger.Entry::name).thenComparing(e -> e.dragon().toString()))
            .toList();
    }

    public boolean reserve(UUID dragon) {
        return !this.pending.containsKey(dragon) && this.reserved.add(dragon);
    }

    public boolean isReserved(UUID dragon) {
        return this.reserved.contains(dragon);
    }

    public boolean reserveRecovery(UUID dragon) {
        return this.reserved.add(dragon);
    }

    public void release(UUID dragon) {
        this.reserved.remove(dragon);
    }

    public RecallLedger.Transfer pending(UUID dragon) {
        return this.pending.get(dragon);
    }

    public Collection<RecallLedger.Transfer> pendingTransfers() {
        return List.copyOf(this.pending.values());
    }

    private void put(RecallLedger.Entry entry) {
        RecallLedger.Entry recallledger$entry = this.entries.put(entry.dragon(), entry);
        if (recallledger$entry != null && !recallledger$entry.owner().equals(entry.owner())) {
            Set<UUID> set = this.owners.get(recallledger$entry.owner());
            if (set != null) {
                set.remove(entry.dragon());
            }
        }

        this.owners.computeIfAbsent(entry.owner(), ignored -> new HashSet<>()).add(entry.dragon());
        this.revision++;
        this.setDirty();
    }

    public RecallLedger.Entry observe(DragonBaseEntity dragon) {
        if (RecallTracking.supported(dragon) && dragon.getOwnerUUID() != null) {
            RecallLedger.Entry recallledger$entry = this.entry(dragon.getUUID());
            if (recallledger$entry == null
                || recallledger$entry.state() == RecallLedger.State.WORLD
                    && !this.pending.containsKey(recallledger$entry.dragon())
                    && RecallTracking.matches(dragon, recallledger$entry)) {
                if (DragonForms.state(dragon).dragonmaid$isExecuted()) {
                    if (recallledger$entry != null) {
                        this.markDead(dragon.getUUID());
                    }

                    return this.entry(dragon.getUUID());
                } else if (!dragon.isAlive() && !HumanoidLife.isInjured(dragon)) {
                    return recallledger$entry;
                } else {
                    DragonState dragonstate = DragonForms.state(dragon);
                    RecallLedger.Entry recallledger$entry1 = new RecallLedger.Entry(
                        dragon.getUUID(),
                        dragon.getOwnerUUID(),
                        BuiltInRegistries.ENTITY_TYPE.getKey(dragon.getType()).toString(),
                        dragon.getName().getString(),
                        dragon.isMale(),
                        dragon.getAgeInTicks(),
                        dragonstate.dragonmaid$getKnowledge(),
                        dragonstate.dragonmaid$getKnowledgeLevel(),
                        DragonForms.isHumanoid(dragon),
                        HumanoidLife.isInjured(dragon),
                        RecallLedger.State.WORLD,
                        dragon.level().dimension().location().toString(),
                        dragon.blockPosition(),
                        dragon.getUUID(),
                        recallledger$entry == null ? 0L : recallledger$entry.generation(),
                        recallledger$entry == null ? UUID.randomUUID() : recallledger$entry.token(),
                        new CompoundTag()
                    );
                    if (!recallledger$entry1.equals(recallledger$entry)) {
                        this.put(recallledger$entry1);
                    }

                    RecallTracking.stamp(dragon, recallledger$entry1);
                    return recallledger$entry1;
                }
            } else {
                if (recallledger$entry != null
                    && recallledger$entry.state() == RecallLedger.State.WORLD
                    && !this.pending.containsKey(recallledger$entry.dragon())
                    && !RecallTracking.hasToken(dragon)) {
                    // Entry exists but the entity never got stamped (old save / imported entry):
                    // heal it once so recall verification can succeed again.
                    RecallTracking.stampIfMissing(dragon, recallledger$entry);
                }

                return recallledger$entry;
            }
        } else {
            return null;
        }
    }

    public RecallLedger.Entry importHorn(String type, CompoundTag snapshot, String dimension, BlockPos position) {
        if (snapshot.hasUUID("UUID") && snapshot.hasUUID("Owner")) {
            UUID uuid = snapshot.getUUID("UUID");
            if (this.entries.containsKey(uuid)) {
                return null;
            } else {
                CompoundTag compoundtag = snapshot.getCompound("DragonMaid");
                boolean flag = LifeStateMigration.trackedOnLoad(compoundtag, snapshot.getInt("DeathStage")) && snapshot.getBoolean("ModelDead");
                if (!compoundtag.getBoolean("Executed") && (flag || !snapshot.getBoolean("ModelDead") && !(snapshot.getFloat("Health") <= 0.0F))) {
                    String s = "";

                    try {
                        Component component = Component.Serializer.fromJson(snapshot.getString("CustomName"), RegistryAccess.EMPTY);
                        if (component != null) {
                            s = component.getString();
                        }
                    } catch (RuntimeException runtimeexception1) {
                    }

                    RecallLedger.Entry recallledger$entry = new RecallLedger.Entry(
                        uuid,
                        snapshot.getUUID("Owner"),
                        type,
                        s,
                        snapshot.getBoolean("Gender"),
                        snapshot.getInt("AgeTicks"),
                        compoundtag.getInt("Knowledge"),
                        knowledgeLevel(compoundtag),
                        compoundtag.getBoolean("Humanoid"),
                        flag,
                        RecallLedger.State.HORN,
                        dimension,
                        position,
                        uuid,
                        1L,
                        UUID.randomUUID(),
                        snapshot.copy()
                    );
                    RecallTracking.stamp(recallledger$entry.snapshot(), recallledger$entry);
                    this.put(recallledger$entry);

                    try {
                        this.flush();
                        return recallledger$entry;
                    } catch (RuntimeException runtimeexception) {
                        this.entries.remove(uuid);
                        this.owners.getOrDefault(recallledger$entry.owner(), new HashSet<>()).remove(uuid);
                        this.revision++;
                        this.setDirty();
                        throw runtimeexception;
                    }
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    public RecallLedger.Transfer prepareTransfer(
        UUID dragon, RecallLedger.State target, String dimension, BlockPos pos, UUID holder, CompoundTag authoritativeSnapshot
    ) {
        RecallLedger.Entry recallledger$entry = this.entry(dragon);
        if (recallledger$entry == null || recallledger$entry.state() == RecallLedger.State.DEAD || this.pending.containsKey(dragon)) {
            throw new IllegalStateException("Dragon has no available recall identity: " + dragon);
        } else if (authoritativeSnapshot != null && !authoritativeSnapshot.isEmpty()) {
            RecallLedger.Entry recallledger$entry1 = new RecallLedger.Entry(
                recallledger$entry.dragon(),
                recallledger$entry.owner(),
                recallledger$entry.type(),
                recallledger$entry.name(),
                recallledger$entry.male(),
                recallledger$entry.ageTicks(),
                recallledger$entry.knowledge(),
                recallledger$entry.knowledgeLevel(),
                recallledger$entry.humanoid(),
                recallledger$entry.injured(),
                target,
                dimension,
                pos.immutable(),
                holder,
                recallledger$entry.generation() + 1L,
                UUID.randomUUID(),
                authoritativeSnapshot.copy()
            );
            RecallTracking.stamp(recallledger$entry1.snapshot(), recallledger$entry1);
            RecallLedger.Transfer recallledger$transfer = new RecallLedger.Transfer(UUID.randomUUID(), recallledger$entry, recallledger$entry1);
            this.pending.put(dragon, recallledger$transfer);
            this.put(recallledger$entry1);

            try {
                this.flush();
                return recallledger$transfer;
            } catch (RuntimeException runtimeexception) {
                this.pending.remove(dragon);
                this.put(recallledger$entry);
                throw runtimeexception;
            }
        } else {
            throw new IllegalArgumentException("A transfer requires an authoritative snapshot");
        }
    }

    public void commitTransfer(RecallLedger.Transfer transfer) {
        this.requireCurrent(transfer);
        RecallLedger.Entry recallledger$entry = transfer.after();
        if (this.storageLevel != null && (recallledger$entry.state() == RecallLedger.State.WORLD || recallledger$entry.state() == RecallLedger.State.STONE)) {
            RecallPersistence.checkpoint(this.storageLevel, recallledger$entry);
        }

        if (recallledger$entry.state() == RecallLedger.State.WORLD) {
            this.put(withSnapshot(recallledger$entry, new CompoundTag()));
        }

        this.pending.remove(recallledger$entry.dragon());
        this.setDirty();

        try {
            this.flush();
        } catch (RuntimeException runtimeexception) {
            this.put(recallledger$entry);
            this.pending.put(recallledger$entry.dragon(), transfer);
            throw runtimeexception;
        }
    }

    public void rollbackTransfer(RecallLedger.Transfer transfer) {
        this.requireCurrent(transfer);
        this.put(transfer.before());
        this.pending.remove(transfer.before().dragon());

        try {
            this.flush();
        } catch (RuntimeException runtimeexception) {
            this.put(transfer.after());
            this.pending.put(transfer.after().dragon(), transfer);
            throw runtimeexception;
        }
    }

    public RecallLedger.Transfer retargetRecovery(RecallLedger.Transfer current, String dimension, BlockPos pos, UUID holder) {
        return this.retargetRecovery(current, dimension, pos, holder, current.after().snapshot());
    }

    public RecallLedger.Transfer retargetRecovery(RecallLedger.Transfer current, String dimension, BlockPos pos, UUID holder, CompoundTag authoritativeSnapshot) {
        this.requireCurrent(current);
        if (authoritativeSnapshot != null && !authoritativeSnapshot.isEmpty()) {
            RecallLedger.Entry recallledger$entry = current.after();
            RecallLedger.Entry recallledger$entry1 = new RecallLedger.Entry(
                recallledger$entry.dragon(),
                recallledger$entry.owner(),
                recallledger$entry.type(),
                recallledger$entry.name(),
                recallledger$entry.male(),
                recallledger$entry.ageTicks(),
                recallledger$entry.knowledge(),
                recallledger$entry.knowledgeLevel(),
                recallledger$entry.humanoid(),
                recallledger$entry.injured(),
                recallledger$entry.state(),
                dimension,
                pos.immutable(),
                holder,
                recallledger$entry.generation() + 1L,
                UUID.randomUUID(),
                authoritativeSnapshot.copy()
            );
            RecallTracking.stamp(recallledger$entry1.snapshot(), recallledger$entry1);
            RecallLedger.Transfer recallledger$transfer = new RecallLedger.Transfer(current.id(), current.before(), recallledger$entry1);
            this.pending.put(recallledger$entry1.dragon(), recallledger$transfer);
            this.put(recallledger$entry1);

            try {
                this.flush();
                return recallledger$transfer;
            } catch (RuntimeException runtimeexception) {
                this.pending.put(recallledger$entry.dragon(), current);
                this.put(recallledger$entry);
                throw runtimeexception;
            }
        } else {
            throw new IllegalArgumentException("A transfer requires an authoritative snapshot");
        }
    }

    private void requireCurrent(RecallLedger.Transfer transfer) {
        RecallLedger.Transfer recallledger$transfer = this.pending.get(transfer.after().dragon());
        if (recallledger$transfer == null
            || !recallledger$transfer.id().equals(transfer.id())
            || recallledger$transfer.after().generation() != transfer.after().generation()
            || !recallledger$transfer.after().token().equals(transfer.after().token())) {
            throw new IllegalStateException("Transfer is no longer current");
        }
    }

    public void markDead(UUID dragon) {
        RecallLedger.Entry recallledger$entry = this.entries.get(dragon);
        if (recallledger$entry != null && recallledger$entry.state() != RecallLedger.State.DEAD) {
            RecallLedger.Transfer recallledger$transfer = this.pending.remove(dragon);
            this.put(
                new RecallLedger.Entry(
                    recallledger$entry.dragon(),
                    recallledger$entry.owner(),
                    recallledger$entry.type(),
                    recallledger$entry.name(),
                    recallledger$entry.male(),
                    recallledger$entry.ageTicks(),
                    recallledger$entry.knowledge(),
                    recallledger$entry.knowledgeLevel(),
                    recallledger$entry.humanoid(),
                    false,
                    RecallLedger.State.DEAD,
                    recallledger$entry.dimension(),
                    recallledger$entry.pos(),
                    recallledger$entry.holder(),
                    recallledger$entry.generation() + 1L,
                    UUID.randomUUID(),
                    new CompoundTag()
                )
            );

            try {
                this.flush();
            } catch (RuntimeException runtimeexception) {
                this.put(recallledger$entry);
                if (recallledger$transfer != null) {
                    this.pending.put(dragon, recallledger$transfer);
                }

                throw runtimeexception;
            }
        }
    }

    public RecallLedger.Entry observeStone(UUID dragon, UUID owner, String type, CompoundTag dragonSnapshot, Entity statue) {
        RecallLedger.Entry recallledger$entry = this.entry(dragon);
        if (recallledger$entry == null
            || recallledger$entry.state() == RecallLedger.State.STONE
                && !this.pending.containsKey(dragon)
                && RecallTracking.matches(statue, recallledger$entry)) {
            CompoundTag compoundtag = new CompoundTag();
            if (!statue.save(compoundtag)) {
                return recallledger$entry;
            } else {
                CompoundTag compoundtag1 = dragonSnapshot.getCompound("DragonMaid");
                String s = recallledger$entry == null ? "" : recallledger$entry.name();
                if (recallledger$entry == null) {
                    try {
                        Component component = Component.Serializer.fromJson(dragonSnapshot.getString("CustomName"), RegistryAccess.EMPTY);
                        if (component != null) {
                            s = component.getString();
                        }
                    } catch (RuntimeException runtimeexception) {
                    }
                }

                RecallLedger.Entry recallledger$entry1 = new RecallLedger.Entry(
                    dragon,
                    owner,
                    type,
                    s,
                    dragonSnapshot.getBoolean("Gender"),
                    dragonSnapshot.getInt("AgeTicks"),
                    compoundtag1.getInt("Knowledge"),
                    knowledgeLevel(compoundtag1),
                    compoundtag1.getBoolean("Humanoid"),
                    LifeStateMigration.trackedOnLoad(compoundtag1, dragonSnapshot.getInt("DeathStage")) && dragonSnapshot.getBoolean("ModelDead"),
                    RecallLedger.State.STONE,
                    statue.level().dimension().location().toString(),
                    statue.blockPosition(),
                    statue.getUUID(),
                    recallledger$entry == null ? 0L : recallledger$entry.generation(),
                    recallledger$entry == null ? UUID.randomUUID() : recallledger$entry.token(),
                    compoundtag
                );
                RecallTracking.stamp(statue, recallledger$entry1);
                RecallTracking.stamp(compoundtag, recallledger$entry1);
                if (!recallledger$entry1.equals(recallledger$entry)) {
                    this.put(recallledger$entry1);
                }

                return recallledger$entry1;
            }
        } else {
            return recallledger$entry;
        }
    }

    private static RecallLedger.Entry withSnapshot(RecallLedger.Entry e, CompoundTag snapshot) {
        return new RecallLedger.Entry(
            e.dragon(),
            e.owner(),
            e.type(),
            e.name(),
            e.male(),
            e.ageTicks(),
            e.knowledge(),
            e.knowledgeLevel(),
            e.humanoid(),
            e.injured(),
            e.state(),
            e.dimension(),
            e.pos(),
            e.holder(),
            e.generation(),
            e.token(),
            snapshot
        );
    }

    private static int knowledgeLevel(CompoundTag maid) {
        int i = maid.getInt("Knowledge");
        int j = maid.contains("KnowledgeLevel", 99) ? maid.getInt("KnowledgeLevel") : Knowledge.level(i);
        return Knowledge.normalizeLevel(i, j);
    }

    public void flush() {
        if (this.storageLevel != null) {
            Path path = this.storageLevel.getServer().getWorldPath(LevelResource.ROOT).resolve("data");
            Path path1 = null;

            try {
                Files.createDirectories(path);
                path1 = Files.createTempFile(path, "dragonmaid_recall-", ".tmp");
                CompoundTag compoundtag = new CompoundTag();
                net.minecraft.core.HolderLookup.Provider provider = this.storageLevel.registryAccess();
                compoundtag.put("data", this.save(new CompoundTag(), provider));
                compoundtag.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
                NbtIo.writeCompressed(compoundtag, path1);

                try {
                    Files.move(path1, path.resolve("dragonmaid_recall.dat"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException atomicmovenotsupportedexception) {
                    Files.move(path1, path.resolve("dragonmaid_recall.dat"), StandardCopyOption.REPLACE_EXISTING);
                }

                this.setDirty(false);
            } catch (IOException ioexception1) {
                throw new IllegalStateException("Cannot durably save the dragon recall ledger", ioexception1);
            } finally {
                if (path1 != null) {
                    try {
                        Files.deleteIfExists(path1);
                    } catch (IOException ioexception) {
                    }
                }
            }
        }
    }

    private static CompoundTag encode(RecallLedger.Entry e) {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putUUID("Dragon", e.dragon());
        compoundtag.putUUID("Owner", e.owner());
        compoundtag.putString("Type", e.type());
        compoundtag.putString("Name", e.name());
        compoundtag.putBoolean("Male", e.male());
        compoundtag.putInt("Age", e.ageTicks());
        compoundtag.putInt("Knowledge", e.knowledge());
        compoundtag.putInt("KnowledgeLevel", e.knowledgeLevel());
        compoundtag.putBoolean("Humanoid", e.humanoid());
        compoundtag.putBoolean("Injured", e.injured());
        compoundtag.putString("State", e.state().name());
        compoundtag.putString("Dimension", e.dimension());
        compoundtag.putLong("Pos", e.pos().asLong());
        if (e.holder() != null) {
            compoundtag.putUUID("Holder", e.holder());
        }

        compoundtag.putLong("Generation", e.generation());
        compoundtag.putUUID("Token", e.token());
        if (!e.snapshot().isEmpty()) {
            compoundtag.put("Snapshot", e.snapshot().copy());
        }

        return compoundtag;
    }

    private static RecallLedger.Entry decode(CompoundTag tag) {
        return new RecallLedger.Entry(
            tag.getUUID("Dragon"),
            tag.getUUID("Owner"),
            tag.getString("Type"),
            tag.getString("Name"),
            tag.getBoolean("Male"),
            tag.getInt("Age"),
            tag.getInt("Knowledge"),
            tag.getInt("KnowledgeLevel"),
            tag.getBoolean("Humanoid"),
            tag.getBoolean("Injured"),
            RecallLedger.State.valueOf(tag.getString("State")),
            tag.getString("Dimension"),
            BlockPos.of(tag.getLong("Pos")),
            tag.hasUUID("Holder") ? tag.getUUID("Holder") : tag.getUUID("Dragon"),
            tag.getLong("Generation"),
            tag.getUUID("Token"),
            tag.getCompound("Snapshot").copy()
        );
    }

    public static RecallLedger load(CompoundTag root, net.minecraft.core.HolderLookup.Provider provider) {
        RecallLedger recallledger = new RecallLedger();

        for (Tag tag : root.getList("Dragons", 10)) {
            try {
                recallledger.put(decode((CompoundTag)tag));
            } catch (IllegalArgumentException illegalargumentexception1) {
            }
        }

        for (Tag tag1 : root.getList("Pending", 10)) {
            CompoundTag compoundtag = (CompoundTag)tag1;

            try {
                RecallLedger.Transfer recallledger$transfer = new RecallLedger.Transfer(
                    compoundtag.getUUID("ID"), decode(compoundtag.getCompound("Before")), decode(compoundtag.getCompound("After"))
                );
                recallledger.pending.put(recallledger$transfer.after().dragon(), recallledger$transfer);
            } catch (IllegalArgumentException illegalargumentexception) {
            }
        }

        return recallledger;
    }

    public CompoundTag save(CompoundTag root, net.minecraft.core.HolderLookup.Provider provider) {
        ListTag listtag = new ListTag();

        for (RecallLedger.Entry recallledger$entry : this.entries.values()) {
            listtag.add(encode(recallledger$entry));
        }

        root.put("Dragons", listtag);
        ListTag listtag1 = new ListTag();

        for (RecallLedger.Transfer recallledger$transfer : this.pending.values()) {
            CompoundTag compoundtag = new CompoundTag();
            compoundtag.putUUID("ID", recallledger$transfer.id());
            compoundtag.put("Before", encode(recallledger$transfer.before()));
            compoundtag.put("After", encode(recallledger$transfer.after()));
            listtag1.add(compoundtag);
        }

        root.put("Pending", listtag1);
        return root;
    }

    public static record Entry(
        UUID dragon,
        UUID owner,
        String type,
        String name,
        boolean male,
        int ageTicks,
        int knowledge,
        int knowledgeLevel,
        boolean humanoid,
        boolean injured,
        RecallLedger.State state,
        String dimension,
        BlockPos pos,
        UUID holder,
        long generation,
        UUID token,
        CompoundTag snapshot
    ) {
    }

    public static enum State {
        WORLD,
        HORN,
        VOID,
        STONE,
        DEAD;

        private State() {
        }
    }

    public static record Transfer(UUID id, RecallLedger.Entry before, RecallLedger.Entry after) {
    }
}
