package com.example.dragonmaid.compat;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import org.slf4j.Logger;

public final class CompanionRelations {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final UUID NIL_UUID = new UUID(0L, 0L);

    private CompanionRelations() {
    }

    public static Set<UUID> protectedOwners(LivingEntity entity) {
        if (entity instanceof OwnableEntity ownableentity) {
            UUID uuid = ownableentity.getOwnerUUID();
            if (valid(uuid)) {
                return Set.of(uuid);
            }
        }

        if (entity.level() instanceof ServerLevel serverlevel) {
            CompanionRelations.McaApi companionrelations$mcaapi = CompanionRelations.McaHolder.API;
            return companionrelations$mcaapi == null ? Set.of() : companionrelations$mcaapi.protectedOwners(entity, serverlevel);
        } else {
            return Set.of();
        }
    }

    public static boolean isAlly(DragonBaseEntity dragon, LivingEntity candidate) {
        if (candidate != dragon && !dragon.isAlliedTo(candidate)) {
            UUID uuid = dragon.getOwnerUUID();
            if (valid(uuid) && uuid.equals(candidate.getUUID())) {
                return true;
            } else {
                LivingEntity livingentity = dragon.getOwner();
                if (livingentity != null && livingentity.isAlliedTo(candidate)) {
                    return true;
                } else {
                    Set<UUID> set = protectedOwners(candidate);
                    if (valid(uuid) && set.contains(uuid)) {
                        return true;
                    } else {
                        if (livingentity != null) {
                            for (UUID uuid1 : set) {
                                LivingEntity livingentity1 = candidate.level().getPlayerByUUID(uuid1);
                                if (livingentity1 != null && livingentity.isAlliedTo(livingentity1)) {
                                    return true;
                                }
                            }
                        }

                        return false;
                    }
                }
            }
        } else {
            return true;
        }
    }

    private static boolean valid(UUID uuid) {
        return uuid != null && !NIL_UUID.equals(uuid);
    }

    private static final class McaApi {
        private final Class<?> villagerType;
        private final Method getTree;
        private final Method getOrEmpty;
        private final Method isPlayer;
        private final Method father;
        private final Method mother;
        private final Method children;
        private final Method partner;
        private final Method relationshipState;
        private final Method isMarried;
        private boolean reportedFailure;

        private McaApi(Class<?> villagerType, Class<?> treeType, Class<?> nodeType, Class<?> relationshipType) throws NoSuchMethodException {
            this.villagerType = villagerType;
            this.getTree = treeType.getMethod("get", ServerLevel.class);
            this.getOrEmpty = treeType.getMethod("getOrEmpty", UUID.class);
            this.isPlayer = nodeType.getMethod("isPlayer");
            this.father = nodeType.getMethod("father");
            this.mother = nodeType.getMethod("mother");
            this.children = nodeType.getMethod("children");
            this.partner = nodeType.getMethod("partner");
            this.relationshipState = nodeType.getMethod("getRelationshipState");
            this.isMarried = relationshipType.getMethod("isMarried");
        }

        private static CompanionRelations.McaApi find() {
            ClassLoader classloader = CompanionRelations.class.getClassLoader();

            try {
                return new CompanionRelations.McaApi(
                    Class.forName("net.mca.entity.VillagerEntityMCA", false, classloader),
                    Class.forName("net.mca.server.world.data.FamilyTree", false, classloader),
                    Class.forName("net.mca.server.world.data.FamilyTreeNode", false, classloader),
                    Class.forName("net.mca.entity.ai.relationship.RelationshipState", false, classloader)
                );
            } catch (ClassNotFoundException classnotfoundexception) {
                return null;
            } catch (LinkageError | RuntimeException | ReflectiveOperationException reflectiveoperationexception) {
                CompanionRelations.LOGGER.debug("MCA direct-family integration is unavailable for this API", reflectiveoperationexception);
                return null;
            }
        }

        private Set<UUID> protectedOwners(LivingEntity entity, ServerLevel level) {
            if (!this.villagerType.isInstance(entity)) {
                return Set.of();
            } else {
                try {
                    Object object = this.getTree.invoke(null, level);
                    Object object1 = this.node(object, entity.getUUID());
                    if (object1 == null) {
                        return Set.of();
                    } else {
                        Set<UUID> set = new HashSet<>();
                        addUuid(set, this.father.invoke(object1));
                        addUuid(set, this.mother.invoke(object1));
                        Object object2 = this.children.invoke(object1);
                        if (object2 instanceof Iterable) {
                            for (Object object3 : (Iterable)object2) {
                                addUuid(set, object3);
                            }
                        }

                        Object object5 = this.relationshipState.invoke(object1);
                        if (object5 != null && Boolean.TRUE.equals(this.isMarried.invoke(object5))) {
                            addUuid(set, this.partner.invoke(object1));
                        }

                        Set<UUID> set1 = new HashSet<>();

                        for (UUID uuid : set) {
                            Object object4 = this.node(object, uuid);
                            if (object4 != null && Boolean.TRUE.equals(this.isPlayer.invoke(object4))) {
                                set1.add(uuid);
                            }
                        }

                        return Set.copyOf(set1);
                    }
                } catch (LinkageError | RuntimeException | ReflectiveOperationException reflectiveoperationexception) {
                    if (!this.reportedFailure) {
                        this.reportedFailure = true;
                        CompanionRelations.LOGGER.debug("Could not read an MCA direct-family relationship", reflectiveoperationexception);
                    }

                    return Set.of();
                }
            }
        }

        private Object node(Object tree, UUID uuid) throws ReflectiveOperationException {
            return this.getOrEmpty.invoke(tree, uuid) instanceof Optional<?> optional ? optional.orElse(null) : null;
        }

        private static void addUuid(Set<UUID> ids, Object value) {
            if (value instanceof UUID uuid && CompanionRelations.valid(uuid)) {
                ids.add(uuid);
            }
        }
    }

    private static final class McaHolder {
        private static final CompanionRelations.McaApi API = CompanionRelations.McaApi.find();

        private McaHolder() {
        }
    }
}
