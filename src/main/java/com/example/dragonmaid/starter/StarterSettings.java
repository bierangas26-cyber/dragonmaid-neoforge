package com.example.dragonmaid.starter;

import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.accessory.DragonAccessoryInventory;
import com.example.dragonmaid.compat.StackCodec;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.DragonState;
import com.example.dragonmaid.item.DragonAccessoryItem;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import com.iafenvoy.iceandfire.registry.IafEntities;
import com.iafenvoy.iceandfire.registry.IafItems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1.2.7 starting-companion model (species / colour / gender / age / knowledge / form / accessories).
 *
 * <p>IAF-CE port notes: {@code IafItemRegistry} -> {@code IafItems}, {@code IafEntityRegistry} ->
 * {@code IafEntities}, item NBT goes through {@code compat/StackCodec}, and
 * {@code DragonBaseEntity#setVariant} now takes the colour <em>name</em> instead of an index, so the
 * 12 upstream egg variants are mapped back onto IAF colour names.
 */
public final class StarterSettings {
    public static final int RANDOM = -1;
    public static final int SPECIES_COUNT = 3;
    public static final int COLOR_COUNT = 4;

    /** IAF-CE colour names, indexed species * 4 + colour (same order as the upstream egg table). */
    private static final String[][] VARIANTS = new String[][]{
        {"red", "green", "bronze", "gray"},
        {"blue", "white", "sapphire", "silver"},
        {"electric", "amethyst", "copper", "black"}
    };

    public boolean enabled;
    public boolean horn;
    public boolean male;
    public Form form = Form.HUMANOID;
    public int days = 75;
    public int knowledge = 1;
    public int species = 1;
    public int color;
    public String name = "";
    public final List<ItemStack> accessories = new ArrayList<>(Collections.nCopies(5, ItemStack.EMPTY));

    public static int stage(int days) {
        return days >= 100 ? 5 : (days >= 75 ? 4 : (days >= 50 ? 3 : (days >= 25 ? 2 : 1)));
    }

    public boolean canBeHuman() {
        return stage(this.days) >= 3 && this.knowledge >= 1;
    }

    public boolean isEgg() {
        return this.form == Form.EGG;
    }

    /** Accessory slots for a stage/knowledge combination (1.2.7 rule: growth 1-2 + knowledge 0-3, capped at 5). */
    public static int slotsFor(int stage, int knowledge) {
        return DragonAccessories.unlockedSlots(stage, knowledge);
    }

    public int slots() {
        return this.isEgg() ? 0 : slotsFor(stage(this.days), this.knowledge);
    }

    public void normalize(StarterConfig.Limits limits) {
        this.days = limits.days(this.days);
        this.knowledge = limits.knowledge(this.knowledge);
        if (this.species != RANDOM) {
            this.species = Math.floorMod(this.species, SPECIES_COUNT);
        }

        if (this.color != RANDOM) {
            this.color = Math.floorMod(this.color, COLOR_COUNT);
        }

        if (this.form == Form.HUMANOID && !this.canBeHuman()) {
            this.form = Form.DRAGON;
        }

        this.name = this.name == null ? "" : this.name.strip();
        if (this.name.length() > 48) {
            this.name = this.name.substring(0, 48);
        }

        int unlocked = slotsFor(stage(this.days), this.knowledge);
        for (int i = unlocked; i < this.accessories.size(); i++) {
            this.accessories.set(i, ItemStack.EMPTY);
        }
    }

    void resolveRandomAppearance(UUID companionId) {
        if (this.species != RANDOM && this.color != RANDOM) {
            return;
        }

        RandomSource random = RandomSource.create(companionId.getMostSignificantBits() ^ companionId.getLeastSignificantBits());
        if (this.species == RANDOM) {
            this.species = random.nextInt(SPECIES_COUNT);
        }

        if (this.color == RANDOM) {
            this.color = random.nextInt(COLOR_COUNT);
        }
    }

    public StarterSettings copy() {
        return load(this.save());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Enabled", this.enabled);
        tag.putBoolean("Horn", this.horn);
        tag.putBoolean("Male", this.male);
        tag.putString("Form", this.form.key());
        tag.putInt("Days", this.days);
        tag.putInt("Knowledge", this.knowledge);
        tag.putInt("Species", this.species);
        tag.putInt("Color", this.color);
        tag.putString("Name", this.name);
        ListTag list = new ListTag();
        for (int i = 0; i < this.accessories.size(); i++) {
            if (!this.accessories.get(i).isEmpty()) {
                CompoundTag item = StackCodec.save(this.accessories.get(i), new CompoundTag());
                item.putInt("Slot", i);
                list.add(item);
            }
        }

        tag.put("Accessories", list);
        return tag;
    }

    public static StarterSettings load(CompoundTag tag) {
        StarterSettings settings = new StarterSettings();
        settings.enabled = tag.getBoolean("Enabled");
        settings.horn = tag.getBoolean("Horn");
        settings.male = tag.getBoolean("Male");
        settings.form = switch (tag.getString("Form")) {
            case "human" -> Form.HUMANOID;
            case "egg" -> Form.EGG;
            case "dragon" -> Form.DRAGON;
            default -> tag.getBoolean("Humanoid") ? Form.HUMANOID : Form.DRAGON;
        };
        settings.days = tag.getInt("Days");
        settings.knowledge = tag.getInt("Knowledge");
        settings.species = tag.getInt("Species");
        settings.color = tag.getInt("Color");
        settings.name = tag.getString("Name");
        for (Tag element : tag.getList("Accessories", Tag.TAG_COMPOUND)) {
            CompoundTag item = (CompoundTag) element;
            int slot = item.getInt("Slot");
            if (slot >= 0 && slot < settings.accessories.size()) {
                settings.accessories.set(slot, StackCodec.load(item));
            }
        }

        return settings;
    }

    public ItemStack createEgg() {
        return new ItemStack(switch (this.species * 4 + this.color) {
            case 0 -> IafItems.DRAGONEGG_RED.get();
            case 1 -> IafItems.DRAGONEGG_GREEN.get();
            case 2 -> IafItems.DRAGONEGG_BRONZE.get();
            case 3 -> IafItems.DRAGONEGG_GRAY.get();
            case 4 -> IafItems.DRAGONEGG_BLUE.get();
            case 5 -> IafItems.DRAGONEGG_WHITE.get();
            case 6 -> IafItems.DRAGONEGG_SAPPHIRE.get();
            case 7 -> IafItems.DRAGONEGG_SILVER.get();
            case 8 -> IafItems.DRAGONEGG_ELECTRIC.get();
            case 9 -> IafItems.DRAGONEGG_AMETHYST.get();
            case 10 -> IafItems.DRAGONEGG_COPPER.get();
            case 11 -> IafItems.DRAGONEGG_BLACK.get();
            default -> throw new IllegalStateException("Invalid starting dragon egg variant");
        });
    }

    public DragonBaseEntity create(Level level, UUID owner) {
        if (this.isEgg()) {
            throw new IllegalStateException("A starting dragon egg must be delivered as an item");
        }

        DragonBaseEntity dragon = switch (this.species) {
            case 0 -> IafEntities.FIRE_DRAGON.get().create(level);
            case 1 -> IafEntities.ICE_DRAGON.get().create(level);
            default -> IafEntities.LIGHTNING_DRAGON.get().create(level);
        };
        if (dragon == null) {
            throw new IllegalStateException("Starting dragon type unavailable");
        }

        dragon.setTame(true, false);
        dragon.setOwnerUUID(owner);
        dragon.setCustomName(
            this.name.isBlank() ? Component.translatable("starter.dragonmaid.default_name") : Component.literal(this.name)
        );
        dragon.setGender(this.male);
        dragon.setVariant(VARIANTS[Math.floorMod(this.species, SPECIES_COUNT)][Math.floorMod(this.color, COLOR_COUNT)]);
        dragon.setAgeInDays(this.days);
        dragon.growDragon(0);
        dragon.setHunger(100);
        DragonState state = DragonForms.state(dragon);
        state.dragonmaid$setKnowledge(switch (this.knowledge) {
            case 1 -> 300;
            case 2 -> 1000;
            case 3 -> 2500;
            default -> 0;
        });
        state.dragonmaid$setKnowledgeLevel(this.knowledge);
        state.dragonmaid$setHumanoid(this.form == Form.HUMANOID && this.canBeHuman());
        dragon.setCommand(2);
        dragon.setOrderedToSit(false);
        dragon.setInSittingPose(false);
        DragonAccessoryInventory inventory = state.dragonmaid$getAccessories();
        for (int i = 0; i < this.accessories.size(); i++) {
            ItemStack item = this.accessories.get(i);
            boolean valid = i < this.slots() && item.getItem() instanceof DragonAccessoryItem && DragonAccessories.accepts(item, dragon, i);
            if (valid) {
                ItemStack one = item.copy();
                one.setCount(1);
                inventory.setItem(i, one);
            } else {
                this.accessories.set(i, ItemStack.EMPTY);
            }
        }

        DragonAccessories.refresh(dragon);
        dragon.setHealth(dragon.getMaxHealth());
        dragon.refreshDimensions();
        return dragon;
    }

    public enum Form {
        HUMANOID("human"),
        DRAGON("dragon"),
        EGG("egg");

        private final String key;

        Form(String key) {
            this.key = key;
        }

        public String key() {
            return this.key;
        }
    }
}
