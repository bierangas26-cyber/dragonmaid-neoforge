package com.example.dragonmaid.command;

import com.example.dragonmaid.accessory.DragonHands;
import com.example.dragonmaid.compat.StackCodec;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.DragonState;
import com.example.dragonmaid.data.HumanoidLife;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 1.2: 调试/管理命令 /dragonmaid（help / teleport / age / hunger / get / egg）。
 * 1.21.1: IAF CE 2.2 的 DragonBaseEntity 用 refreshDirtyAttributes() 重算属性（旧版 @Invoker updateAttributes 已不存在）。
 */
@EventBusSubscriber(modid = "dragonmaid")
public final class CompanionCommands {
    private static Function<ServerPlayer, List<CompanionCommands.Archived>> archive = player -> List.of();
    private static Consumer<DragonBaseEntity> changed = dragon -> {
    };
    private static final int LIMIT = 64;
    private static final int MAX_DAYS = 89478;
    private static final List<String> FIELDS = List.of(
        "name", "species", "age", "owner", "hunger", "health", "position", "uuid", "equipment", "inventory", "accessories", "knowledge", "state", "teleport"
    );

    private CompanionCommands() {
    }

    public static void archives(Function<ServerPlayer, List<CompanionCommands.Archived>> provider, Consumer<DragonBaseEntity> update) {
        archive = provider;
        changed = update;
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("dragonmaid")
            .executes(c -> help((CommandSourceStack)c.getSource()));
        root.then(Commands.literal("help").executes(c -> help((CommandSourceStack)c.getSource())));
        LiteralArgumentBuilder<CommandSourceStack> teleport = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("teleport")
            .requires(s -> s.hasPermission(2));

        for (String mode : List.of("on", "off", "status")) {
            teleport.then(
                Commands.literal(mode)
                    .executes(
                        c -> {
                            ServerPlayer player = ((CommandSourceStack)c.getSource()).getPlayerOrException();
                            if (!mode.equals("status")) {
                                CompanionTeleport.set(player, mode.equals("on"));
                            }

                            ((CommandSourceStack)c.getSource())
                                .sendSuccess(() -> Component.translatable("debug.dragonmaid.teleport", new Object[]{CompanionTeleport.enabled(player)}), false);
                            return 1;
                        }
                    )
            );
        }

        root.then(teleport);

        for (String kind : List.of("age", "hunger")) {
            LiteralArgumentBuilder<CommandSourceStack> branch = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal(kind).requires(s -> s.hasPermission(2));

            for (String action : List.of("set", "add", "remove")) {
                RequiredArgumentBuilder<CommandSourceStack, Integer> amount = Commands.argument(
                    "amount", IntegerArgumentType.integer(0, kind.equals("age") ? 89478 : 100)
                );
                amount.then(filters(kind + ":" + action));
                branch.then(Commands.literal(action).then(amount));
            }

            root.then(branch);
        }

        root.then(
            Commands.literal("get")
                .then(
                    ((RequiredArgumentBuilder)Commands.argument("field", StringArgumentType.string())
                            .suggests((c, b) -> SharedSuggestionProvider.suggest(concatFields(), b))
                            .executes(c -> query((CommandSourceStack)c.getSource(), StringArgumentType.getString(c, "field"), "all", "*", "all")))
                        .then(filters("get"))
                )
        );
        root.then(
            ((LiteralArgumentBuilder)Commands.literal("egg").requires(s -> s.hasPermission(2)))
                .then(
                    Commands.literal("remaining")
                        .then(
                            Commands.argument("seconds", IntegerArgumentType.integer(0, 86400))
                                .executes(c -> EggIncubation.setNearest((CommandSourceStack)c.getSource(), IntegerArgumentType.getInteger(c, "seconds")))
                        )
                )
        );
        dispatcher.register(root);
    }

    private static List<String> concatFields() {
        ArrayList<String> list = new ArrayList<>(FIELDS);
        list.add("all");
        return list;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> filters(String operation) {
        return ((RequiredArgumentBuilder)Commands.argument("species", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("all", "fire", "ice", "lightning"), b))
                .then(Commands.argument("name", StringArgumentType.string()).suggests((c, b) -> {
                    ServerPlayer p = ((CommandSourceStack)c.getSource()).getPlayer();
                    if (p == null) {
                        return b.buildFuture();
                    } else {
                        ArrayList<String> names = new ArrayList<>();
                        names.add("*");

                        for (CompanionCommands.View v : views(p)) {
                            names.add(StringArgumentType.escapeIfRequired(v.name()));
                        }

                        return SharedSuggestionProvider.suggest(names, b);
                    }
                }).then(count(operation, false))))
            .then(Commands.literal("*").then(count(operation, true)));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> count(String operation, boolean wildcard) {
        return Commands.argument("count", StringArgumentType.word())
            .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("1", "2", "3", "all"), b))
            .executes(
                c -> {
                    String type = StringArgumentType.getString(c, "species");
                    String name = wildcard ? "*" : StringArgumentType.getString(c, "name");
                    String count = StringArgumentType.getString(c, "count");
                    return operation.equals("get")
                        ? query((CommandSourceStack)c.getSource(), StringArgumentType.getString(c, "field"), type, name, count)
                        : modify((CommandSourceStack)c.getSource(), operation, IntegerArgumentType.getInteger(c, "amount"), type, name, count);
                }
            );
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("debug.dragonmaid.help"), false);
        return 1;
    }

    public static String species(DragonBaseEntity d) {
        return d instanceof FireDragonEntity ? "fire" : (d instanceof IceDragonEntity ? "ice" : (d instanceof LightningDragonEntity ? "lightning" : "other"));
    }

    public static List<CompanionCommands.View> views(ServerPlayer player) {
        LinkedHashMap<UUID, CompanionCommands.View> result = new LinkedHashMap<>();

        for (CompanionCommands.Archived a : archive.apply(player)) {
            result.put(a.uuid(), new CompanionCommands.View(a.uuid(), a.species(), a.name(), a.dimension(), a.position(), a.fields(), null));
        }

        for (ServerLevel level : player.server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof DragonBaseEntity) {
                    DragonBaseEntity d = (DragonBaseEntity)entity;
                    if (player.getUUID().equals(d.getOwnerUUID())
                        && d.isTame()
                        && !DragonForms.state(d).dragonmaid$isExecuted()
                        && (d.isAlive() || HumanoidLife.isInjured(d))) {
                        result.put(
                            d.getUUID(),
                            new CompanionCommands.View(
                                d.getUUID(), species(d), d.getName().getString(), level.dimension().location().toString(), d.position(), Map.of(), d
                            )
                        );
                    }
                }
            }
        }

        return new ArrayList<>(result.values());
    }

    public static List<CompanionCommands.View> select(ServerPlayer player, String species, String name, String count, boolean editing) throws CommandSyntaxException {
        if (!List.of("all", "fire", "ice", "lightning").contains(species)) {
            throw error("selector");
        } else {
            int limit;
            try {
                limit = count.equals("all") ? 64 : Integer.parseInt(count);
                if (limit < 1 || limit > 64) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException var10) {
                throw error("count");
            }

            UUID identity = null;
            if (name.startsWith("uuid:") || name.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")) {
                try {
                    identity = UUID.fromString(name.startsWith("uuid:") ? name.substring(5) : name);
                } catch (IllegalArgumentException var9) {
                    throw error("selector");
                }
            }

            UUID wanted = identity;
            String dimension = player.level().dimension().location().toString();
            return views(player)
                .stream()
                .filter(
                    v -> (!editing || v.live() != null && v.live().level() == player.level())
                        && (species.equals("all") || species.equals(v.species()))
                        && (wanted != null ? wanted.equals(v.uuid()) : name.equals("*") || name.equals(v.name()))
                )
                .sorted(
                    Comparator.<CompanionCommands.View>comparingDouble(
                            v -> v.dimension().equals(dimension) ? v.position().distanceToSqr(player.position()) : Double.POSITIVE_INFINITY
                        )
                        .thenComparing(v -> v.uuid().toString())
                )
                .limit((long)limit)
                .toList();
        }
    }

    public static int modify(CommandSourceStack source, String operation, int amount, String species, String name, String count) throws CommandSyntaxException {
        if (!source.hasPermission(2)) {
            throw error("permission");
        } else {
            ServerPlayer player = source.getPlayerOrException();
            List<CompanionCommands.View> selected = select(player, species, name, count, true);
            if (selected.isEmpty()) {
                throw error("missing");
            } else {
                int done = 0;

                for (CompanionCommands.View v : selected) {
                    DragonBaseEntity d = v.live();
                    String[] parts = operation.split(":");
                    boolean age = parts[0].equals("age");
                    long before = age ? (long)d.getAgeInDays() : (long)d.getHunger();
                    String next = parts[1];

                    long requested = switch (next) {
                        case "set" -> (long)amount;
                        case "add" -> before + (long)amount;
                        case "remove" -> before - (long)amount;
                        default -> throw error("selector");
                    };
                    int nextValue = (int)Math.max(0L, Math.min(age ? 89478L : 100L, requested));
                    if (age) {
                        if (HumanoidLife.isInjured(d) && (long)nextValue > before) {
                            source.sendFailure(Component.translatable("debug.dragonmaid.injured", new Object[]{v.name()}));
                            continue;
                        }

                        int fraction = d.getAgeInTicks() % 24000;
                        d.setAgeInTicks((int)Math.min(2147483647L, (long)nextValue * 24000L + (long)fraction));
                        d.refreshDirtyAttributes();
                        d.refreshDimensions();
                        d.setHealth(Math.min(d.getHealth(), d.getMaxHealth()));
                    } else {
                        d.setHunger(nextValue);
                    }

                    changed.accept(d);
                    done++;
                    source.sendSuccess(
                        () -> copy(Component.literal(v.name() + " [" + v.uuid() + "] " + parts[0] + ": " + before + " → " + nextValue), v.uuid().toString()), false
                    );
                }

                return done;
            }
        }
    }

    public static int query(CommandSourceStack source, String field, String species, String name, String count) throws CommandSyntaxException {
        List<CompanionCommands.View> selected = select(source.getPlayerOrException(), species, name, count, false);
        List<String> fields = field.equals("all") ? FIELDS : List.of(field.split(","));
        if (fields.stream().anyMatch(f -> !FIELDS.contains(f))) {
            throw error("field");
        } else if (selected.isEmpty()) {
            throw error("missing");
        } else {
            for (CompanionCommands.View v : selected) {
                Map<String, String> data = v.live() == null ? v.fields() : liveFields(v.live());
                String command = "/dragonmaid get all all " + v.uuid() + " 1";
                source.sendSuccess(
                    () -> Component.literal(v.name() + " [" + v.uuid() + "]")
                            .withStyle(
                                s -> s.withClickEvent(new ClickEvent(Action.SUGGEST_COMMAND, command))
                                        .withHoverEvent(
                                            new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.translatable("debug.dragonmaid.uuid_hint"))
                                        )
                            ),
                    false
                );

                for (String key : fields) {
                    String value = data.getOrDefault(key, "? (unloaded)");
                    source.sendSuccess(() -> copy(Component.translatable("debug.dragonmaid.field." + key).append(": " + value), value), false);
                }
            }

            return selected.size();
        }
    }

    public static Component copy(Component label, String value) {
        return label.copy()
            .withStyle(
                s -> s.withClickEvent(new ClickEvent(Action.COPY_TO_CLIPBOARD, value))
                        .withHoverEvent(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.translatable("debug.dragonmaid.copy")))
            );
    }

    public static Map<String, String> liveFields(DragonBaseEntity d) {
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        DragonState state = DragonForms.state(d);
        fields.put("name", d.getName().getString());
        fields.put("species", species(d));
        fields.put("age", d.getAgeInDays() + " d (" + d.getAgeInTicks() + " ticks)");
        fields.put("owner", d.getOwnerUUID() + (d.getOwner() != null ? " (" + d.getOwner().getName().getString() + ")" : ""));
        fields.put("hunger", Integer.toString(d.getHunger()));
        fields.put("health", d.getHealth() + " / " + d.getMaxHealth());
        fields.put("position", d.level().dimension().location() + " " + d.getX() + " " + d.getY() + " " + d.getZ());
        fields.put("uuid", d.getUUID().toString());
        fields.put("equipment", contents(d.dragonInventory) + "; hands=" + contents(DragonHands.inventory(d)));
        fields.put("inventory", contents(state.dragonmaid$getStorage()));
        fields.put("accessories", contents(state.dragonmaid$getAccessories()));
        fields.put("knowledge", state.dragonmaid$getKnowledgeLevel() + " (XP " + state.dragonmaid$getKnowledge() + ")");
        fields.put(
            "state",
            (DragonForms.isHumanoid(d) ? "humanoid" : "dragon")
                + ", "
                + (HumanoidLife.isInjured(d) ? "injured" : "healthy")
                + ", command="
                + d.getCommand()
                + ", agingDisabled="
                + d.isAgingDisabled()
        );
        fields.put("teleport", Boolean.toString(CompanionTeleport.allowed(d)));
        return fields;
    }

    private static String contents(Container container) {
        ArrayList<String> result = new ArrayList<>();

        for (int i = 0; i < container.getContainerSize(); i++) {
            if (!container.getItem(i).isEmpty()) {
                result.add(i + "=" + StackCodec.save(container.getItem(i).copy(), new CompoundTag()));
            }
        }

        return container.getContainerSize() + " slots: " + result;
    }

    public static Map<String, String> snapshotFields(CompoundTag snapshot) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        if (snapshot.isEmpty()) {
            return result;
        } else {
            result.put("hunger", Integer.toString(snapshot.getInt("Hunger")));
            result.put("health", Float.toString(snapshot.getFloat("Health")));
            CompoundTag maid = snapshot.getCompound("DragonMaid");
            result.put("inventory", maid.getList("Storage", 10).toString());
            result.put("accessories", maid.getList("Accessories", 10).toString());
            result.put("equipment", snapshot.getList("Items", 10) + "; hands=" + snapshot.getCompound("DragonMaidHands"));
            return result;
        }
    }

    static CommandSyntaxException error(String key) {
        return new SimpleCommandExceptionType(Component.translatable("debug.dragonmaid.error." + key)).create();
    }

    public static record Archived(UUID uuid, String species, String name, String dimension, Vec3 position, Map<String, String> fields) {
    }

    public static record View(UUID uuid, String species, String name, String dimension, Vec3 position, Map<String, String> fields, DragonBaseEntity live) {
    }
}
