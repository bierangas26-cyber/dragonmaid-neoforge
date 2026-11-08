package com.example.dragonmaid.learning;

import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.DragonState;
import com.example.dragonmaid.recall.RecallTracking;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.registries.BuiltInRegistries;
import com.example.dragonmaid.compat.StackNbt;
import com.example.dragonmaid.compat.Enchants;

public final class KnowledgeUpgrades {
    public static final int MAX_LEVEL = 5;
    public static final int CURRENT_MAX_LEVEL = 3;
    private static final Map<Integer, KnowledgeUpgrades.Requirement> REQUIREMENTS = new ConcurrentHashMap<>();
    private static final Map<Item, Function<ItemStack, ListTag>> BOOK_CARRIERS = new ConcurrentHashMap<>();
    private static final KnowledgeUpgrades.Requirement FIRST_LEVEL = new KnowledgeUpgrades.Requirement(300, 0, 0, 0, stack -> false);
    private static final KnowledgeUpgrades.Progress NO_PROGRESS = new KnowledgeUpgrades.Progress(0, 0);

    private KnowledgeUpgrades() {
    }

    public static void registerDefaults() {
        // 1.21.1 port note: NeoForge's DeferredWorkQueue can run the same enqueued task twice in one
        // launch (e.g. when a resource reload is retried after a failure), so registering the default
        // table has to be idempotent. register() keeps its strict "already registered" contract.
        unregister(2);
        unregister(3);
        register(2, new KnowledgeUpgrades.Requirement(1000, 0, 1, 0, KnowledgeUpgrades::isNonCursedBook));
        register(3, new KnowledgeUpgrades.Requirement(2500, 0, 2, 1, KnowledgeUpgrades::isNonCursedBook));
    }

    public static void register(int targetLevel, KnowledgeUpgrades.Requirement requirement) {
        if (targetLevel >= 2 && targetLevel <= 5) {
            if (REQUIREMENTS.putIfAbsent(targetLevel, Objects.requireNonNull(requirement)) != null) {
                throw new IllegalArgumentException("Knowledge advancement already registered: " + targetLevel);
            }
        } else {
            throw new IllegalArgumentException("Invalid knowledge level");
        }
    }

    public static void unregister(int targetLevel) {
        REQUIREMENTS.remove(targetLevel);
    }

    public static KnowledgeUpgrades.Requirement nextRequirement(DragonBaseEntity dragon) {
        if (dragon == null) {
            return null;
        } else {
            int i = DragonForms.state(dragon).dragonmaid$getKnowledgeLevel();
            return i == 0 ? FIRST_LEVEL : REQUIREMENTS.get(i + 1);
        }
    }

    public static boolean available(DragonBaseEntity dragon) {
        return nextRequirement(dragon) != null;
    }

    public static int learningLimit(DragonBaseEntity dragon) {
        if (dragon == null) {
            return 300;
        } else {
            KnowledgeUpgrades.Requirement knowledgeupgrades$requirement = nextRequirement(dragon);
            if (knowledgeupgrades$requirement != null) {
                return Math.max(300, knowledgeupgrades$requirement.minimumKnowledge());
            } else {
                DragonState dragonstate = DragonForms.state(dragon);
                int i = Math.max(300, dragonstate.dragonmaid$getKnowledge());

                for (Entry<Integer, KnowledgeUpgrades.Requirement> entry : REQUIREMENTS.entrySet()) {
                    if (entry.getKey() <= dragonstate.dragonmaid$getKnowledgeLevel()) {
                        i = Math.max(i, entry.getValue().minimumKnowledge());
                    }
                }

                return i;
            }
        }
    }

    public static void registerBookCarrier(Item item, Function<ItemStack, ListTag> reader) {
        Objects.requireNonNull(item);
        Objects.requireNonNull(reader);
        if (item == Items.ENCHANTED_BOOK || BOOK_CARRIERS.putIfAbsent(item, reader) != null) {
            throw new IllegalArgumentException("Book carrier already registered");
        }
    }

    public static void unregisterBookCarrier(Item item) {
        BOOK_CARRIERS.remove(item);
    }

    public static boolean isBookCandidate(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(Items.ENCHANTED_BOOK) || BOOK_CARRIERS.containsKey(stack.getItem()));
    }

    /**
     * 1.21.1 port: enchanted books keep their enchantments in the STORED_ENCHANTMENTS data component.
     * The 1.20.1 code read a "StoredEnchantments" ListTag out of the item tag (now custom_data), which
     * no longer exists, so every enchanted book was rejected / gave 0 learning XP. Rebuild the legacy
     * list shape so the existing parsing below stays untouched.
     */
    private static ListTag storedEnchantments(ItemStack stack) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) {
            return null;
        }

        ListTag listtag = new ListTag();

        for (var entry : enchantments.entrySet()) {
            ResourceLocation resourcelocation = entry.getKey().unwrapKey().map(key -> key.location()).orElse(null);
            if (resourcelocation == null) {
                continue;
            }

            CompoundTag compoundtag = new CompoundTag();
            compoundtag.putString("id", resourcelocation.toString());
            compoundtag.putInt("lvl", entry.getIntValue());
            listtag.add(compoundtag);
        }

        return listtag.isEmpty() ? null : listtag;
    }

    static int bookLearningExperience(ItemStack stack, int knowledgeLevel) {
        if (knowledgeLevel >= 1 && isBookCandidate(stack)) {
            ListTag listtag;
            if (stack.is(Items.ENCHANTED_BOOK)) {
                ListTag listtag1 = storedEnchantments(stack);
                if (listtag1 == null) {
                    return 0;
                }

                listtag = listtag1;
            } else {
                Function<ItemStack, ListTag> function = BOOK_CARRIERS.get(stack.getItem());
                if (function == null) {
                    return 0;
                }

                try {
                    listtag = function.apply(stack.copy());
                } catch (RuntimeException runtimeexception) {
                    return 0;
                }
            }

            if (listtag != null && !listtag.isEmpty() && listtag.getElementType() == 10) {
                long l = 0L;
                HashSet<ResourceLocation> hashset = new HashSet<>();
                int i = 0;

                while (i < listtag.size()) {
                    CompoundTag compoundtag1 = listtag.getCompound(i);
                    if (compoundtag1.contains("id", 8) && compoundtag1.get("lvl") instanceof NumericTag numerictag) {
                        ResourceLocation resourcelocation = ResourceLocation.tryParse(compoundtag1.getString("id"));
                        if (resourcelocation != null && hashset.add(resourcelocation)) {
                            Enchantment enchantment = Enchants.get(resourcelocation);
                            double d0 = numerictag.getAsDouble();
                            if (enchantment != null && Double.isFinite(d0) && !(d0 < 1.0) && d0 == Math.floor(d0)) {
                                if (Enchants.isCurse(enchantment) && knowledgeLevel < 2) {
                                    return 0;
                                }

                                int j = enchantment.getMaxLevel();
                                if (j < 1) {
                                    return 0;
                                }

                                int k;
                                if (Enchants.isCurse(enchantment) || j == 1) {
                                    k = 60;
                                } else if (j == 2) {
                                    k = d0 == 1.0 ? 20 : (int)(25.0 * Math.min(d0, 10.0));
                                } else if (j <= 5) {
                                    k = Math.max(15, (int)Math.round(60.0 * Math.min(d0, 10.0) / (double)j));
                                } else {
                                    k = 50;
                                }

                                l += (long)k;
                                if (l > 10000L) {
                                    return 0;
                                }

                                i++;
                                continue;
                            }

                            return 0;
                        }

                        return 0;
                    }

                    return 0;
                }

                return (int)l;
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    public static KnowledgeUpgrades.BookKind bookKind(ItemStack stack) {
        if (stack.isEmpty()) {
            return KnowledgeUpgrades.BookKind.INVALID;
        } else {
            ListTag listtag;
            if (stack.is(Items.ENCHANTED_BOOK)) {
                ListTag listtag1 = storedEnchantments(stack);
                if (listtag1 == null) {
                    return KnowledgeUpgrades.BookKind.INVALID;
                }

                listtag = listtag1;
            } else {
                Function<ItemStack, ListTag> function = BOOK_CARRIERS.get(stack.getItem());
                if (function == null) {
                    return KnowledgeUpgrades.BookKind.UNSUPPORTED;
                }

                try {
                    listtag = function.apply(stack.copy());
                } catch (RuntimeException runtimeexception) {
                    return KnowledgeUpgrades.BookKind.UNKNOWN;
                }

                if (listtag == null) {
                    return KnowledgeUpgrades.BookKind.UNKNOWN;
                }
            }

            if (listtag.isEmpty()) {
                return KnowledgeUpgrades.BookKind.INVALID;
            } else if (listtag.getElementType() != 10) {
                return KnowledgeUpgrades.BookKind.UNKNOWN;
            } else {
                boolean flag = false;
                boolean flag1 = false;
                int i = 0;

                while (i < listtag.size()) {
                    CompoundTag compoundtag1 = listtag.getCompound(i);
                    if (compoundtag1.contains("id", 8) && compoundtag1.contains("lvl", 99)) {
                        ResourceLocation resourcelocation = ResourceLocation.tryParse(compoundtag1.getString("id"));
                        if (resourcelocation != null && Enchants.contains(resourcelocation)) {
                            Enchantment enchantment = Enchants.get(resourcelocation);
                            if (enchantment == null) {
                                return KnowledgeUpgrades.BookKind.UNKNOWN;
                            }

                            if (compoundtag1.getInt("lvl") > 0) {
                                if (Enchants.isCurse(enchantment)) {
                                    flag1 = true;
                                } else {
                                    flag = true;
                                }
                            }

                            i++;
                            continue;
                        }

                        return KnowledgeUpgrades.BookKind.UNKNOWN;
                    }

                    return KnowledgeUpgrades.BookKind.UNKNOWN;
                }

                return flag1 ? KnowledgeUpgrades.BookKind.CURSE : (flag ? KnowledgeUpgrades.BookKind.REGULAR : KnowledgeUpgrades.BookKind.INVALID);
            }
        }
    }

    public static boolean acceptsBook(ItemStack stack) {
        KnowledgeUpgrades.BookKind knowledgeupgrades$bookkind = bookKind(stack);
        return knowledgeupgrades$bookkind == KnowledgeUpgrades.BookKind.REGULAR || knowledgeupgrades$bookkind == KnowledgeUpgrades.BookKind.CURSE;
    }

    public static boolean isNonCursedBook(ItemStack stack) {
        return bookKind(stack) == KnowledgeUpgrades.BookKind.REGULAR;
    }

    public static boolean isCursedBook(ItemStack stack) {
        return bookKind(stack) == KnowledgeUpgrades.BookKind.CURSE;
    }

    public static KnowledgeUpgrades.Progress progress(DragonBaseEntity dragon) {
        KnowledgeUpgrades.Requirement requirement = nextRequirement(dragon);
        if (dragon != null && requirement != null) {
            DragonState state = DragonForms.state(dragon);
            String key = Integer.toString(state.dragonmaid$getKnowledgeLevel() + 1);
            CompoundTag all = state.dragonmaid$getKnowledgeProgress();
            if (!all.contains(key, 10)) {
                return NO_PROGRESS;
            } else {
                CompoundTag saved = all.getCompound(key);
                return new KnowledgeUpgrades.Progress(
                    Math.max(0, Math.min(requirement.bookCount(), saved.getInt("Regular"))),
                    Math.max(0, Math.min(requirement.cursedBookCount(), saved.getInt("Curse")))
                );
            }
        } else {
            return NO_PROGRESS;
        }
    }

    private static boolean mayAdvance(DragonBaseEntity dragon) {
        return dragon != null
            && !dragon.level().isClientSide
            && dragon.isAlive()
            && !dragon.isModelDead()
            && !dragon.isRemoved()
            && DragonForms.state(dragon).dragonmaid$getKnowledgeLevel() >= 1
            && nextRequirement(dragon) != null;
    }

    /** 1.2: 本次投入能贡献的普通书/诅咒书数量（不修改物品）。 */
    public static KnowledgeUpgrades.Contribution contribution(DragonBaseEntity dragon, ItemStack regularA, ItemStack regularB, ItemStack curse) {
        KnowledgeUpgrades.Requirement requirement = nextRequirement(dragon);
        if (requirement != null && DragonForms.state(dragon).dragonmaid$getKnowledgeLevel() >= 1) {
            KnowledgeUpgrades.Progress paid = progress(dragon);
            int missing = requirement.bookCount() - paid.regular();
            int a = missing > 0 && matchesPrimary(requirement, regularA) ? 1 : 0;
            int b = missing > a && regularB != regularA && matchesPrimary(requirement, regularB) ? 1 : 0;
            int c = requirement.cursedBookCount() <= paid.curse() || !isCursedBook(curse) || a != 0 && curse == regularA || b != 0 && curse == regularB ? 0 : 1;
            return new KnowledgeUpgrades.Contribution(a, b, c);
        } else {
            return new KnowledgeUpgrades.Contribution(0, 0, 0);
        }
    }

    public static boolean canLearnBooks(DragonBaseEntity dragon, ItemStack regularA, ItemStack regularB, ItemStack curse) {
        return mayAdvance(dragon) && !contribution(dragon, regularA, regularB, curse).empty();
    }

    public static boolean canUpgradeAt(DragonBaseEntity dragon, int knowledge, ItemStack regularA, ItemStack regularB, ItemStack curse) {
        if (!mayAdvance(dragon)) {
            return false;
        } else {
            KnowledgeUpgrades.Requirement requirement = nextRequirement(dragon);
            KnowledgeUpgrades.Progress paid = progress(dragon);
            KnowledgeUpgrades.Contribution incoming = contribution(dragon, regularA, regularB, curse);
            return knowledge >= requirement.minimumKnowledge()
                && paid.regular() + incoming.regular() >= requirement.bookCount()
                && paid.curse() + incoming.curse() >= requirement.cursedBookCount();
        }
    }

    public static boolean canUpgrade(DragonBaseEntity dragon, ItemStack regularA, ItemStack regularB, ItemStack curse) {
        return dragon != null && canUpgradeAt(dragon, DragonForms.state(dragon).dragonmaid$getKnowledge(), regularA, regularB, curse);
    }

    private static boolean matchesPrimary(KnowledgeUpgrades.Requirement requirement, ItemStack stack) {
        return acceptsBook(stack) && requirement.bookPredicate().test(stack);
    }

    private static void consumeAndRecord(
        DragonBaseEntity dragon, KnowledgeUpgrades.Contribution incoming, ItemStack regularA, ItemStack regularB, ItemStack curse
    ) {
        KnowledgeUpgrades.Progress paid = progress(dragon);
        regularA.shrink(incoming.regularA());
        regularB.shrink(incoming.regularB());
        curse.shrink(incoming.curse());
        CompoundTag saved = new CompoundTag();
        saved.putInt("Regular", paid.regular() + incoming.regular());
        saved.putInt("Curse", paid.curse() + incoming.curse());
        DragonState state = DragonForms.state(dragon);
        state.dragonmaid$getKnowledgeProgress().put(Integer.toString(state.dragonmaid$getKnowledgeLevel() + 1), saved);
        RecallTracking.changed(dragon);
    }

    /** 1.2: 分次学习：把书交进去记账，攒够后再升级（不一次吃满）。 */
    public static boolean learnBooks(DragonBaseEntity dragon, ItemStack regularA, ItemStack regularB, ItemStack curse) {
        if (!canLearnBooks(dragon, regularA, regularB, curse)) {
            return false;
        } else {
            consumeAndRecord(dragon, contribution(dragon, regularA, regularB, curse), regularA, regularB, curse);
            tryUpgrade(dragon, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
            return true;
        }
    }

    public static boolean tryUpgrade(DragonBaseEntity dragon, ItemStack regularA, ItemStack regularB, ItemStack curse) {
        if (!canUpgrade(dragon, regularA, regularB, curse)) {
            return false;
        } else {
            DragonState state = DragonForms.state(dragon);
            int nextLevel = state.dragonmaid$getKnowledgeLevel() + 1;
            KnowledgeUpgrades.Requirement requirement = REQUIREMENTS.get(nextLevel);
            KnowledgeUpgrades.Contribution incoming = contribution(dragon, regularA, regularB, curse);
            if (!incoming.empty()) {
                consumeAndRecord(dragon, incoming, regularA, regularB, curse);
            }

            if (requirement.knowledgeCost() > 0) {
                state.dragonmaid$setKnowledge(state.dragonmaid$getKnowledge() - requirement.knowledgeCost());
            }

            state.dragonmaid$setKnowledgeLevel(nextLevel);
            state.dragonmaid$getKnowledgeProgress().remove(Integer.toString(nextLevel));
            return true;
        }
    }

    public static boolean tryUpgrade(DragonBaseEntity dragon, ItemStack book) {
        return tryUpgrade(dragon, book, ItemStack.EMPTY, ItemStack.EMPTY);
    }

    public static enum BookKind {
        REGULAR,
        CURSE,
        UNKNOWN,
        UNSUPPORTED,
        INVALID;

        private BookKind() {
        }
    }

    public static record Contribution(int regularA, int regularB, int curse) {
        public int regular() {
            return this.regularA + this.regularB;
        }

        public boolean empty() {
            return this.regular() + this.curse == 0;
        }
    }

    public static record Progress(int regular, int curse) {
    }

    public static record Requirement(int minimumKnowledge, int knowledgeCost, int bookCount, int cursedBookCount, Predicate<ItemStack> bookPredicate) {
        public Requirement(int minimumKnowledge, int knowledgeCost, int bookCount, int cursedBookCount, Predicate<ItemStack> bookPredicate) {
            if (minimumKnowledge >= 0
                && minimumKnowledge <= 10000
                && knowledgeCost >= 0
                && knowledgeCost <= minimumKnowledge
                && bookCount >= 0
                && bookCount <= 2
                && cursedBookCount >= 0
                && cursedBookCount <= 1) {
                Objects.requireNonNull(bookPredicate);
                this.minimumKnowledge = minimumKnowledge;
                this.knowledgeCost = knowledgeCost;
                this.bookCount = bookCount;
                this.cursedBookCount = cursedBookCount;
                this.bookPredicate = bookPredicate;
            } else {
                throw new IllegalArgumentException("Invalid knowledge advancement requirement");
            }
        }

        public Requirement(int minimumKnowledge, int knowledgeCost, int bookCount, Predicate<ItemStack> bookPredicate) {
            this(minimumKnowledge, knowledgeCost, bookCount, 0, bookPredicate);
        }
    }
}
