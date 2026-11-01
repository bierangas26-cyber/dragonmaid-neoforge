package com.example.dragonmaid.petrification;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;

public final class PetrificationLedger extends SavedData {
    public static final String ID = "dragonmaid_petrification";
    private final Map<UUID, PetrificationLedger.Entry> entries = new HashMap<>();
    private final Set<UUID> reserved = new HashSet<>();
    private ServerLevel storageLevel;

    public PetrificationLedger() {
    }

    public static PetrificationLedger get(ServerLevel level) {
        PetrificationLedger petrificationledger = level.getServer().overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(PetrificationLedger::new, PetrificationLedger::load), "dragonmaid_petrification");
        petrificationledger.storageLevel = level.getServer().overworld();
        return petrificationledger;
    }

    public static PetrificationLedger load(CompoundTag root, net.minecraft.core.HolderLookup.Provider provider) {
        PetrificationLedger petrificationledger = new PetrificationLedger();

        for (Tag tag : root.getList("Dragons", 10)) {
            CompoundTag compoundtag = (CompoundTag)tag;
            if (compoundtag.hasUUID("Dragon") && compoundtag.hasUUID("Token")) {
                UUID uuid = compoundtag.getUUID("Dragon");
                petrificationledger.entries
                    .put(
                        uuid,
                        new PetrificationLedger.Entry(
                            uuid,
                            compoundtag.getUUID("Token"),
                            compoundtag.getString("Type"),
                            compoundtag.getCompound("Snapshot").copy(),
                            compoundtag.getBoolean("Active")
                        )
                    );
            }
        }

        return petrificationledger;
    }

    public CompoundTag save(CompoundTag root, net.minecraft.core.HolderLookup.Provider provider) {
        ListTag listtag = new ListTag();

        for (PetrificationLedger.Entry petrificationledger$entry : this.entries.values()) {
            CompoundTag compoundtag = new CompoundTag();
            compoundtag.putUUID("Dragon", petrificationledger$entry.dragon());
            compoundtag.putUUID("Token", petrificationledger$entry.token());
            compoundtag.putString("Type", petrificationledger$entry.type());
            compoundtag.putBoolean("Active", petrificationledger$entry.active());
            if (petrificationledger$entry.active()) {
                compoundtag.put("Snapshot", petrificationledger$entry.snapshot().copy());
            }

            listtag.add(compoundtag);
        }

        root.put("Dragons", listtag);
        return root;
    }

    public PetrificationLedger.Entry entry(UUID dragon) {
        return this.entries.get(dragon);
    }

    public boolean active(UUID dragon) {
        PetrificationLedger.Entry petrificationledger$entry = this.entries.get(dragon);
        return petrificationledger$entry != null && petrificationledger$entry.active();
    }

    public boolean reserve(UUID dragon) {
        return this.reserved.add(dragon);
    }

    public void release(UUID dragon) {
        this.reserved.remove(dragon);
    }

    public void capture(PetrificationLedger.Entry entry) {
        this.entries.put(entry.dragon(), entry);
        this.setDirty();
    }

    public void complete(PetrificationLedger.Entry entry) {
        this.entries.put(entry.dragon(), new PetrificationLedger.Entry(entry.dragon(), entry.token(), entry.type(), new CompoundTag(), false));
        this.setDirty();
    }

    public void rollbackCapture(UUID dragon, PetrificationLedger.Entry previous) {
        if (previous == null) {
            this.entries.remove(dragon);
        } else {
            this.entries.put(dragon, previous);
        }

        this.setDirty();
    }

    public void flush() {
        if (this.storageLevel != null) {
            Path path = this.storageLevel.getServer().getWorldPath(LevelResource.ROOT).resolve("data");
            Path path1 = null;

            try {
                Files.createDirectories(path);
                path1 = Files.createTempFile(path, "dragonmaid_petrification-", ".tmp");
                CompoundTag compoundtag = new CompoundTag();
                compoundtag.put("data", this.save(new CompoundTag(), this.storageLevel.registryAccess()));
                compoundtag.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
                NbtIo.writeCompressed(compoundtag, path1);

                try {
                    Files.move(path1, path.resolve("dragonmaid_petrification.dat"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException atomicmovenotsupportedexception) {
                    Files.move(path1, path.resolve("dragonmaid_petrification.dat"), StandardCopyOption.REPLACE_EXISTING);
                }

                this.setDirty(false);
            } catch (IOException ioexception1) {
                throw new IllegalStateException("Cannot save petrification identity", ioexception1);
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

    public static record Entry(UUID dragon, UUID token, String type, CompoundTag snapshot, boolean active) {
    }
}
