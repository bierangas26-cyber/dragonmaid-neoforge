package com.example.dragonmaid.learning;

import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.data.DragonState;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.core.registries.BuiltInRegistries;
import com.example.dragonmaid.compat.StackNbt;
import com.example.dragonmaid.compat.Enchants;

public final class KnowledgeUpgrades {
    public static final int MAX_LEVEL = 5;
    public static final int CURRENT_MAX_LEVEL = 3;
    private static final Map<Integer, KnowledgeUpgrades.Requirement> REQUIREMENTS = new ConcurrentHashMap<>();
    private static final Map<Item, Function<ItemStack, ListTag>> BOOK_CARRIERS = new ConcurrentHashMap<>();
    private static final KnowledgeUpgrades.Requirement FIRST_LEVEL = new KnowledgeUpgrades.Requirement(300, 0, 0, 0, stack -> false);

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

    static int bookLearningExperience(ItemStack stack, int knowledgeLevel) {
        if (knowledgeLevel >= 1 && isBookCandidate(stack)) {
            ListTag listtag;
            if (stack.is(Items.ENCHANTED_BOOK)) {
                CompoundTag compoundtag = StackNbt.getTag(stack);
                if (compoundtag == null || !(compoundtag.get("StoredEnchantments") instanceof ListTag listtag1)) {
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
                CompoundTag compoundtag = StackNbt.getTag(stack);
                if (compoundtag == null || !compoundtag.contains("StoredEnchantments")) {
                    return KnowledgeUpgrades.BookKind.INVALID;
                }

                if (!(compoundtag.get("StoredEnchantments") instanceof ListTag listtag1)) {
                    return KnowledgeUpgrades.BookKind.UNKNOWN;
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

    public static boolean canUpgrade(DragonBaseEntity dragon, ItemStack regularA, ItemStack regularB, ItemStack curse) {
        if (dragon != null && !dragon.level().isClientSide && dragon.isAlive() && !dragon.isModelDead() && !dragon.isRemoved()) {
            if (DragonForms.state(dragon).dragonmaid$getKnowledgeLevel() < 1) {
                return false;
            } else {
                KnowledgeUpgrades.Requirement knowledgeupgrades$requirement = nextRequirement(dragon);
                if (knowledgeupgrades$requirement == null
                    || DragonForms.state(dragon).dragonmaid$getKnowledge() < knowledgeupgrades$requirement.minimumKnowledge()) {
                    return false;
                } else if (knowledgeupgrades$requirement.bookCount() > 0 && !matchesPrimary(knowledgeupgrades$requirement, regularA)) {
                    return false;
                } else {
                    return knowledgeupgrades$requirement.bookCount() <= 1 || regularA != regularB && matchesPrimary(knowledgeupgrades$requirement, regularB)
                        ? knowledgeupgrades$requirement.cursedBookCount() <= 0
                            || isCursedBook(curse)
                                && (knowledgeupgrades$requirement.bookCount() <= 0 || curse != regularA)
                                && (knowledgeupgrades$requirement.bookCount() <= 1 || curse != regularB)
                        : false;
                }
            }
        } else {
            return false;
        }
    }

    private static boolean matchesPrimary(KnowledgeUpgrades.Requirement requirement, ItemStack stack) {
        return acceptsBook(stack) && requirement.bookPredicate().test(stack);
    }

    public static boolean tryUpgrade(DragonBaseEntity dragon, ItemStack regularA, ItemStack regularB, ItemStack curse) {
        if (!canUpgrade(dragon, regularA, regularB, curse)) {
            return false;
        } else {
            DragonState dragonstate = DragonForms.state(dragon);
            int i = dragonstate.dragonmaid$getKnowledgeLevel() + 1;
            KnowledgeUpgrades.Requirement knowledgeupgrades$requirement = REQUIREMENTS.get(i);
            if (knowledgeupgrades$requirement.bookCount() > 0) {
                regularA.shrink(1);
            }

            if (knowledgeupgrades$requirement.bookCount() > 1) {
                regularB.shrink(1);
            }

            if (knowledgeupgrades$requirement.cursedBookCount() > 0) {
                curse.shrink(1);
            }

            if (knowledgeupgrades$requirement.knowledgeCost() > 0) {
                dragonstate.dragonmaid$setKnowledge(dragonstate.dragonmaid$getKnowledge() - knowledgeupgrades$requirement.knowledgeCost());
            }

            dragonstate.dragonmaid$setKnowledgeLevel(i);
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
