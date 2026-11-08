package com.example.dragonmaid.recall;

import com.example.dragonmaid.command.CompanionCommands;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import net.minecraft.world.phys.Vec3;

/**
 * 1.2 上游 {@code recall/RecallCommandArchive}：把召回账本（{@link RecallLedger}）里的“已归档”龙
 * 接进 {@code /dragonmaid get} 的 archive provider，使已收进号角 / 掉进虚空 / 石化的龙仍能被查询。
 *
 * <p>移植说明（1.21.1 / NeoForge）：
 * <ul>
 *   <li>{@code player.m_284548_()} → {@link net.minecraft.server.level.ServerPlayer#serverLevel()}
 *       （1.21.1 的 DragonBaseEntity/RecallLedger 均按官方名重写，无 SRG 名字）。</li>
 *   <li>{@code Vec3.m_82512_(BlockPos)} → {@link Vec3#atCenterOf(net.minecraft.core.Vec3i)}。</li>
 *   <li>API 与上游等价：{@link CompanionCommands#archives} / {@link CompanionCommands#snapshotFields} /
 *       {@link CompanionCommands.Archived} / {@link RecallLedger#get} / {@link RecallLedger#owned} /
 *       {@link RecallTracking#changed}。</li>
 * </ul>
 */
final class RecallCommandArchive {
    private RecallCommandArchive() {
    }

    static void setup() {
        CompanionCommands.archives(
            player -> {
                ArrayList<CompanionCommands.Archived> result = new ArrayList<>();

                for (RecallLedger.Entry entry : RecallLedger.get(player.serverLevel()).owned(player.getUUID())) {
                    LinkedHashMap<String, String> fields = new LinkedHashMap<>(CompanionCommands.snapshotFields(entry.snapshot()));
                    String species = species(entry.type());
                    fields.put("name", entry.name());
                    fields.put("species", species);
                    fields.put("uuid", entry.dragon().toString());
                    fields.put("owner", entry.owner().toString());
                    fields.put("age", entry.ageTicks() / 24000 + " d (" + entry.ageTicks() + " ticks)");
                    fields.put(
                        "position",
                        entry.dimension() + " " + entry.pos().getX() + " " + entry.pos().getY() + " " + entry.pos().getZ()
                    );
                    fields.put(
                        "state",
                        entry.state() + ", " + (entry.humanoid() ? "humanoid" : "dragon") + ", " + (entry.injured() ? "injured" : "healthy") + " (archive)"
                    );
                    fields.put("knowledge", entry.knowledgeLevel() + " (XP " + entry.knowledge() + ")");
                    result.add(new CompanionCommands.Archived(entry.dragon(), species, entry.name(), entry.dimension(), Vec3.atCenterOf(entry.pos()), fields));
                }

                return result;
            },
            RecallTracking::changed
        );
    }

    /** 上游按 entity type id 后缀判定龙种，保持同样的映射（fire / ice / lightning）。 */
    private static String species(String type) {
        if (type.endsWith("fire_dragon")) {
            return "fire";
        } else {
            return type.endsWith("ice_dragon") ? "ice" : "lightning";
        }
    }
}
