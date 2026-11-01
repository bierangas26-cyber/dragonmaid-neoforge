# IAF 2.1.13 → IAF CE 2.0 映射与 DragonMixin 核对（task-3 P2）

> 生成方式：对 _port_src/mapped（原 212 个反编译文件机械改写版）重新扫描 com.github.alexthe666.* import；对 libs/iceandfire-2.0.jar、uranus/jupiter 与 _port_src/decompiled_iaf（590 个 CE 2.0 源文件）逐项用 javap/反编译源码核对。

## 0. 总览

| 项 | 数值 |
|---|---|
| 旧 IAF import 种类 | 50 |
| 旧 IAF 引用总次数 | 249 |
| 可机械映射（同名/改名） | 30 |
| 已不存在/需重写/不支持 | 20 |
| 被 exclude 源码文件 | 155 |
| CE 2.0 反编译源文件 | 590 |

## 1. 50 个 IAF import → CE 2.0 映射表

| # | 旧类（com.github.alexthe666.…） | 次数 | CE 2.0 目标 | 状态 | 说明 |
|---|---|---|---|---|---|
| 1 | com.github.alexthe666.iceandfire.entity.EntityDragonBase | 98 | com.iafenvoy.iceandfire.entity.DragonBaseEntity | DONE | 机械改名 |
| 2 | com.github.alexthe666.iceandfire.entity.EntityIceDragon | 17 | com.iafenvoy.iceandfire.entity.IceDragonEntity | DONE | 机械改名 |
| 3 | com.github.alexthe666.iceandfire.entity.EntityLightningDragon | 16 | com.iafenvoy.iceandfire.entity.LightningDragonEntity | DONE | 机械改名 |
| 4 | com.github.alexthe666.iceandfire.entity.EntityStoneStatue | 11 | com.iafenvoy.iceandfire.entity.StoneStatueEntity | DONE | 机械改名 |
| 5 | com.github.alexthe666.iceandfire.entity.EntityFireDragon | 9 | com.iafenvoy.iceandfire.entity.FireDragonEntity | DONE | 机械改名 |
| 6 | com.github.alexthe666.iceandfire.entity.IafEntityRegistry | 8 | com.iafenvoy.iceandfire.registry.IafEntities | DONE | 机械改名 |
| 7 | com.github.alexthe666.iceandfire.entity.EntityGorgon | 7 | com.iafenvoy.iceandfire.entity.GorgonEntity | DONE | 机械改名 |
| 8 | com.github.alexthe666.iceandfire.inventory.ContainerDragon | 6 | com.iafenvoy.iceandfire.screen.menu.DragonMenu | DONE | 机械改名 |
| 9 | com.github.alexthe666.iceandfire.item.IafItemRegistry | 6 | com.iafenvoy.iceandfire.registry.IafItems | DONE | 机械改名 |
| 10 | com.github.alexthe666.iceandfire.entity.tile.TileEntityDragonforgeInput | 5 | com.iafenvoy.iceandfire.item.block.entity.DragonForgeInputBlockEntity | DONE | 机械改名 |
| 11 | com.github.alexthe666.iceandfire.item.ItemDragonHorn | 5 | com.iafenvoy.iceandfire.item.DragonHornItem | DONE | 机械改名 |
| 12 | com.github.alexthe666.iceandfire.item.ItemDragonArmor | 4 | com.iafenvoy.iceandfire.item.DragonArmorItem | DONE | 机械改名 |
| 13 | com.github.alexthe666.iceandfire.IceAndFire | 3 | com.iafenvoy.iceandfire.IceAndFire | DONE | 机械改名 |
| 14 | com.github.alexthe666.iceandfire.block.IafBlockRegistry | 3 | com.iafenvoy.iceandfire.registry.IafBlocks | DONE | 机械改名 |
| 15 | com.github.alexthe666.iceandfire.client.gui.GuiDragon | 3 | com.iafenvoy.iceandfire.screen.gui.DragonScreen | DECOMPILED_OK | 客户端；类名/包名变，getMenu() 仍可用 |
| 16 | com.github.alexthe666.iceandfire.entity.tile.TileEntityDragonforge | 3 | com.iafenvoy.iceandfire.item.block.entity.DragonForgeBlockEntity | DONE | 机械改名 |
| 17 | com.github.alexthe666.iceandfire.world.DragonPosWorldData | 3 | com.iafenvoy.iceandfire.world.DragonPosWorldData | DONE | 机械改名 |
| 18 | com.github.alexthe666.iceandfire.IafConfig | 2 | com.iafenvoy.iceandfire.config.IafCommonConfig | DECOMPILED_OK | INSTANCE.dragon.<field>.getValue()；工程已有 compat.IafCompatConfig 垫片承接 |
| 19 | com.github.alexthe666.iceandfire.entity.EntityDragonCharge | 2 | com.iafenvoy.iceandfire.entity.DragonChargeEntity | DONE | 机械改名 |
| 20 | com.github.alexthe666.iceandfire.entity.props.EntityData | 2 | 不支持（等价物：uranus EntityPropertyDelegate / SynchedEntityData） | REWRITE_API | IAF 2.x 的 citadel 数据 capability 已被删除；本 mod 仅 2 处使用，改为自建 iafcompat.EntityDataCompat |
| 21 | com.github.alexthe666.iceandfire.entity.props.EntityDataProvider | 2 | 不支持（同上） | REWRITE_API | getCapability(e) → iafcompat.EntityDataCompat.of(e)（persistent data / SynchedEntityData） |
| 22 | com.github.alexthe666.iceandfire.entity.util.DragonUtils | 2 | com.iafenvoy.iceandfire.entity.util.dragon.DragonUtils | DONE | 机械改名 |
| 23 | com.github.alexthe666.iceandfire.enums.EnumDragonTextures.Armor | 2 | com.iafenvoy.iceandfire.data.DragonArmorMaterial | REWRITE_API | 旧 Armor 枚举只用于客户端纹理；CE 纹理由 DragonTextureProvider/DragonArmorMaterial 提供 |
| 24 | com.github.alexthe666.iceandfire.item.ItemDragonArmor.DragonArmorType | 2 | com.iafenvoy.iceandfire.data.DragonArmorMaterial + DragonArmorPart | REWRITE_API | 构造从 (DragonArmorType, slot) 改为 (DragonArmorPart, DragonArmorMaterial, ...)；枚举拆成两个 |
| 25 | com.github.alexthe666.iceandfire.item.ItemSummoningCrystal | 2 | com.iafenvoy.iceandfire.item.SummoningCrystalItem | DONE | 机械改名 |
| 26 | com.github.alexthe666.iceandfire.misc.IafDamageRegistry | 2 | com.iafenvoy.iceandfire.registry.IafDamageTypes | DONE | 机械改名 |
| 27 | com.github.alexthe666.citadel.client.model.AdvancedEntityModel | 1 | com.iafenvoy.uranus.client.model.AdvancedEntityModel | DONE | citadel→uranus；工程已改 import，但 mixin target 仍用旧类 |
| 28 | com.github.alexthe666.iceandfire.CommonProxy | 1 | 不支持（CE 无 proxy 类） | UNSUPPORTED | 唯一使用点在客户端 DragonDetailsScreenMixin；改为从 Screen 的 menu 直接取 dragon |
| 29 | com.github.alexthe666.iceandfire.api.FoodUtils | 1 | com.iafenvoy.uranus.object.item.FoodUtils | DONE | 工程已改（InjuryRecovery） |
| 30 | com.github.alexthe666.iceandfire.api.event.GenericGriefEvent | 1 | com.iafenvoy.iceandfire.event.GriefBreakBlockEvent | REWRITE_API | 语义从"可取消 API 事件"变为 CE 内部事件类；需改写事件订阅方式 |
| 31 | com.github.alexthe666.iceandfire.block.BlockIceSpikes | 1 | com.iafenvoy.iceandfire.item.block.IceSpikesBlock | DONE | 机械改名 |
| 32 | com.github.alexthe666.iceandfire.client.model.armor.ModelCopperArmor | 1 | com.iafenvoy.iceandfire.render.model.armor.CopperArmorModel | DECOMPILED_OK | 客户端；createMesh 签名需核对 |
| 33 | com.github.alexthe666.iceandfire.client.model.armor.ModelSilverArmor | 1 | com.iafenvoy.iceandfire.render.model.armor.SilverArmorModel | DECOMPILED_OK | 客户端；createMesh 签名需核对 |
| 34 | com.github.alexthe666.iceandfire.client.render.IafRenderType | 1 | com.iafenvoy.iceandfire.registry.IafRenderTypes | DECOMPILED_OK | 客户端；getStoneCrackRenderType 需查 IafRenderTypes 对应方法 |
| 35 | com.github.alexthe666.iceandfire.client.render.entity.RenderLightningDragon | 1 | com.iafenvoy.iceandfire.render.entity.LightningDragonEntityRenderer | DECOMPILED_OK | 客户端 mixin target 改名 |
| 36 | com.github.alexthe666.iceandfire.client.render.entity.RenderStoneStatue | 1 | com.iafenvoy.iceandfire.render.entity.StoneStatueEntityRenderer | DECOMPILED_OK | 客户端 mixin target 改名 |
| 37 | com.github.alexthe666.iceandfire.client.render.entity.layer.LayerDragonArmor | 1 | com.iafenvoy.iceandfire.render.entity.feature.DragonArmorFeatureRenderer | DECOMPILED_OK | 客户端 mixin target 改名 |
| 38 | com.github.alexthe666.iceandfire.entity.EntityDragonFireCharge | 1 | com.iafenvoy.iceandfire.entity.FireDragonChargeEntity | DONE | 机械改名 |
| 39 | com.github.alexthe666.iceandfire.entity.EntityDragonIceCharge | 1 | com.iafenvoy.iceandfire.entity.IceDragonChargeEntity | DONE | 机械改名 |
| 40 | com.github.alexthe666.iceandfire.entity.EntityDragonLightningCharge | 1 | com.iafenvoy.iceandfire.entity.LightningDragonChargeEntity | DONE | 机械改名 |
| 41 | com.github.alexthe666.iceandfire.entity.EntityMutlipartPart | 1 | com.iafenvoy.iceandfire.entity.MultipartPartEntity | DONE | 机械改名 |
| 42 | com.github.alexthe666.iceandfire.entity.IafDragonDestructionManager | 1 | com.iafenvoy.iceandfire.entity.util.dragon.IafDragonDestructionManager | DONE | 机械改名 |
| 43 | com.github.alexthe666.iceandfire.entity.IafDragonLogic | 1 | com.iafenvoy.iceandfire.entity.util.dragon.IafDragonLogic | DONE | 机械改名 |
| 44 | com.github.alexthe666.iceandfire.item.ItemGorgonHead | 1 | com.iafenvoy.iceandfire.item.GorgonHeadItem | DONE | 机械改名 |
| 45 | com.github.alexthe666.iceandfire.item.ItemStoneStatue | 1 | com.iafenvoy.iceandfire.item.StoneStatueItem | DONE | 机械改名 |
| 46 | com.github.alexthe666.iceandfire.message.MessageUpdateDragonforge | 1 | 不支持（CE 无对应 payload） | UNSUPPORTED | CE 网络只有 DragonControl/Multipart/... 等包，没有 dragonforge 更新包；ForgeCooperation 的旧消息发送需删除，龙炉同步由 CE 自身处理 |
| 47 | com.github.alexthe666.iceandfire.misc.IafDamageRegistry.CustomEntityDamageSource | 1 | com.iafenvoy.iceandfire.registry.IafDamageTypes.CustomEntityDamageSource | DONE | 工程已按此改写 |
| 48 | com.github.alexthe666.iceandfire.misc.IafSoundRegistry | 1 | com.iafenvoy.iceandfire.registry.IafSounds | DONE | 机械改名 |
| 49 | com.github.alexthe666.iceandfire.pathfinding.raycoms.AdvancedPathNavigate.MovementType | 1 | com.iafenvoy.uranus.object.entity.pathfinding.raycoms.AdvancedPathNavigate.MovementType | DONE | citadel→uranus 包迁移 |
| 50 | com.github.alexthe666.iceandfire.pathfinding.raycoms.PathingStuckHandler | 1 | com.iafenvoy.uranus.object.entity.pathfinding.raycoms.PathingStuckHandler | DONE | citadel→uranus 包迁移 |

## 2. 不存在的 19 个类：替代方案 / 不支持

| # | 旧类 | 次数 | 方案 | 状态 |
|---|---|---|---|---|
| 1 | com.github.alexthe666.iceandfire.client.gui.GuiDragon | 3 | com.iafenvoy.iceandfire.screen.gui.DragonScreen（客户端；类名/包名变，getMenu() 仍可用） | DECOMPILED_OK |
| 2 | com.github.alexthe666.iceandfire.IafConfig | 2 | com.iafenvoy.iceandfire.config.IafCommonConfig（INSTANCE.dragon.<field>.getValue()；工程已有 compat.IafCompatConfig 垫片承接） | DECOMPILED_OK |
| 3 | com.github.alexthe666.iceandfire.entity.props.EntityData | 2 | 不支持（等价物：uranus EntityPropertyDelegate / SynchedEntityData）（IAF 2.x 的 citadel 数据 capability 已被删除；本 mod 仅 2 处使用，改为自建 iafcompat.EntityDataCompat） | REWRITE_API |
| 4 | com.github.alexthe666.iceandfire.entity.props.EntityDataProvider | 2 | 不支持（同上）（getCapability(e) → iafcompat.EntityDataCompat.of(e)（persistent data / SynchedEntityData）） | REWRITE_API |
| 5 | com.github.alexthe666.iceandfire.enums.EnumDragonTextures.Armor | 2 | com.iafenvoy.iceandfire.data.DragonArmorMaterial（旧 Armor 枚举只用于客户端纹理；CE 纹理由 DragonTextureProvider/DragonArmorMaterial 提供） | REWRITE_API |
| 6 | com.github.alexthe666.iceandfire.item.ItemDragonArmor.DragonArmorType | 2 | com.iafenvoy.iceandfire.data.DragonArmorMaterial + DragonArmorPart（构造从 (DragonArmorType, slot) 改为 (DragonArmorPart, DragonArmorMaterial, ...)；枚举拆成两个） | REWRITE_API |
| 7 | com.github.alexthe666.citadel.client.model.AdvancedEntityModel | 1 | com.iafenvoy.uranus.client.model.AdvancedEntityModel（citadel→uranus；工程已改 import，但 mixin target 仍用旧类） | DONE |
| 8 | com.github.alexthe666.iceandfire.CommonProxy | 1 | 不支持（CE 无 proxy 类）（唯一使用点在客户端 DragonDetailsScreenMixin；改为从 Screen 的 menu 直接取 dragon） | UNSUPPORTED |
| 9 | com.github.alexthe666.iceandfire.api.FoodUtils | 1 | com.iafenvoy.uranus.object.item.FoodUtils（工程已改（InjuryRecovery）） | DONE |
| 10 | com.github.alexthe666.iceandfire.api.event.GenericGriefEvent | 1 | com.iafenvoy.iceandfire.event.GriefBreakBlockEvent（语义从"可取消 API 事件"变为 CE 内部事件类；需改写事件订阅方式） | REWRITE_API |
| 11 | com.github.alexthe666.iceandfire.client.model.armor.ModelCopperArmor | 1 | com.iafenvoy.iceandfire.render.model.armor.CopperArmorModel（客户端；createMesh 签名需核对） | DECOMPILED_OK |
| 12 | com.github.alexthe666.iceandfire.client.model.armor.ModelSilverArmor | 1 | com.iafenvoy.iceandfire.render.model.armor.SilverArmorModel（客户端；createMesh 签名需核对） | DECOMPILED_OK |
| 13 | com.github.alexthe666.iceandfire.client.render.IafRenderType | 1 | com.iafenvoy.iceandfire.registry.IafRenderTypes（客户端；getStoneCrackRenderType 需查 IafRenderTypes 对应方法） | DECOMPILED_OK |
| 14 | com.github.alexthe666.iceandfire.client.render.entity.RenderLightningDragon | 1 | com.iafenvoy.iceandfire.render.entity.LightningDragonEntityRenderer（客户端 mixin target 改名） | DECOMPILED_OK |
| 15 | com.github.alexthe666.iceandfire.client.render.entity.RenderStoneStatue | 1 | com.iafenvoy.iceandfire.render.entity.StoneStatueEntityRenderer（客户端 mixin target 改名） | DECOMPILED_OK |
| 16 | com.github.alexthe666.iceandfire.client.render.entity.layer.LayerDragonArmor | 1 | com.iafenvoy.iceandfire.render.entity.feature.DragonArmorFeatureRenderer（客户端 mixin target 改名） | DECOMPILED_OK |
| 17 | com.github.alexthe666.iceandfire.message.MessageUpdateDragonforge | 1 | 不支持（CE 无对应 payload）（CE 网络只有 DragonControl/Multipart/... 等包，没有 dragonforge 更新包；ForgeCooperation 的旧消息发送需删除，龙炉同步由 CE 自身处理） | UNSUPPORTED |
| 18 | com.github.alexthe666.iceandfire.misc.IafDamageRegistry.CustomEntityDamageSource | 1 | com.iafenvoy.iceandfire.registry.IafDamageTypes.CustomEntityDamageSource（工程已按此改写） | DONE |
| 19 | com.github.alexthe666.iceandfire.pathfinding.raycoms.AdvancedPathNavigate.MovementType | 1 | com.iafenvoy.uranus.object.entity.pathfinding.raycoms.AdvancedPathNavigate.MovementType（citadel→uranus 包迁移） | DONE |
| 20 | com.github.alexthe666.iceandfire.pathfinding.raycoms.PathingStuckHandler | 1 | com.iafenvoy.uranus.object.entity.pathfinding.raycoms.PathingStuckHandler（citadel→uranus 包迁移） | DONE |

## 3. DragonMixin 注入点 / Shadow 成员核对（重点）

目标类：旧 EntityDragonBase → CE com.iafenvoy.iceandfire.entity.DragonBaseEntity（3024 行反编译源码逐项核对）。

| # | 类型 | 旧目标 | CE 2.0 是否仍存在 | 证据 | 处理 |
|---|---|---|---|---|---|
| 1 | @Shadow | protected int fireTicks | NO | MC 1.21.1 Entity 已无 fireTicks（private remainingFireTicks + get/setRemainingFireTicks） | 改为 @Shadow 方法 getRemainingFireTicks()/setRemainingFireTicks(int) |
| 2 | @Shadow | switchNavigator(int) | YES | DragonBaseEntity.switchNavigator(int) 仍在 | 保留 |
| 3 | @Shadow | isBreakable(BlockPos,BlockState,float,DragonBaseEntity) | YES | CE 同名同签名（protected） | 保留 |
| 4 | @Inject HEAD | spawnItemCrackParticles | YES | CE: spawnItemCrackParticles(Item) | 保留 |
| 5 | @Inject HEAD | hurt | YES | CE: hurt(DamageSource,float) | 保留 |
| 6 | @Inject HEAD | remove | YES | CE: remove(RemovalReason) | 保留 |
| 7 | @Inject RETURN | hurt | YES | 同上 | 保留 |
| 8 | @Inject HEAD | setInSittingPose | YES | CE: setInSittingPose(boolean) 同时承担 sleep 语义 | 保留 |
| 9 | @Inject HEAD | setBreathingFire | YES | CE: setBreathingFire(boolean) | 保留 |
| 10 | @Inject HEAD | roar | YES | CE: roar() | 保留 |
| 11 | @Inject TAIL | defineSynchedData | YES(签名变) | CE: defineSynchedData(SynchedEntityData.Builder)（1.21.1 新签名） | 回调参数类型改为 Builder |
| 12 | @ModifyVariable HEAD | setTarget | YES | CE: setTarget(LivingEntity) | 保留 |
| 13 | @Inject TAIL | addAdditionalSaveData / readAdditionalSaveData | YES | CE 同名(CompoundTag) | 保留 |
| 14 | @Inject HEAD | setModelDead | YES | CE: setModelDead(boolean) | 保留 |
| 15 | @Inject HEAD/TAIL | setCommand | YES | CE: setCommand(int) | 保留 |
| 16 | @Inject HEAD/RETURN | setAgeInTicks / setAgeInDays | YES | CE 同名 | 保留 |
| 17 | @Inject TAIL | updateAttributes | NO | CE 改名为 refreshDirtyAttributes() / setConfigurableAttributes() | target 改 refreshDirtyAttributes |
| 18 | @Inject TAIL | registerGoals | YES | CE 同名(protected) | 保留 |
| 19 | @Inject HEAD/TAIL | tick | YES | CE 同名 | 保留 |
| 20 | @Inject HEAD | updateBurnTarget | YES | CE 同名 | 保留 |
| 21 | @Inject HEAD | getDimensions | YES | CE: getDimensions(Pose) | 保留 |
| 22 | @Inject HEAD | getScale | NO | CE DragonBaseEntity 不再 override getScale（1.21.1 Entity 也没有可注入实现） | 删除该注入（或改为 updateScale/getRenderSize） |
| 23 | @Inject HEAD | getStepHeight | NO | 1.21.1 用 maxUpStep()；CE DragonBaseEntity 有 maxUpStep() | target 改 maxUpStep |
| 24 | @Inject HEAD | resetParts | NO | CE 改名 removeParts() | target 改 removeParts |
| 25 | @Inject HEAD | updateParts / breakBlocks | YES | CE: updateParts(), breakBlocks(boolean) | 保留 |
| 26 | @Inject HEAD | setFlying / setHovering | YES | CE 同名 | 保留 |
| 27 | @Inject HEAD | isAllowedToTriggerFlight | YES | CE 同名 | 保留 |
| 28 | @Inject HEAD | canAddPassenger | YES | CE 同名(Entity) | 保留 |
| 29 | @Inject HEAD | mobInteract / interactAt | YES | CE 同名(Player, ...) | 保留 |
| 30 | @Inject createNavigator | createNavigator(Level, MovementType, PathingStuckHandler, float, float) | NO(签名变) | CE 只有 (Level, MovementType) / (Level, MovementType, PathingStuckHandler) / (…, float)；包名 uranus | target 描述符改为 CE 4 参版本，回调参数 4 个 |
| 31 | @Inject TAIL | switchNavigator | YES | CE 同名 | 保留 |
| 32 | @Inject HEAD | travel / isInWater / getHeadPosition | YES | CE 同名 | 保留 |
| 33 | @Redirect | DragonBaseEntity.getCommand()I | YES | CE 同名方法 | 保留 |
| 34 | @Redirect | DragonBaseEntity.getSummoningCrystal()Item | NO | CE 无 getSummoningCrystal；物品为 IafItems.SUMMONING_CRYSTAL_FIRE/ICE/LIGHTNING，type.getCrystalItem() | 去重定向或改为读 IafItems/dragonType |
| 35 | @Redirect | DragonBaseEntity.isModelDead()Z | YES | CE 同名 | 保留 |
| 36 | @Redirect | DragonBaseEntity.isFood(ItemStack)Z | NO | CE DragonBaseEntity 无 isFood（食物判断在 uranus/item 逻辑） | 改为 isFoodItem 兼容层（iafcompat.DragonApiCompat.isFood） |
| 37 | @Redirect | DragonBaseEntity.heal(F)V | YES | Entity.heal(float) 仍在 | 保留 |

**结论**：38 个注入/Shadow 目标中 31 个原样可用；7 处必须改：fireTicks（1.21.1 移除）、updateAttributes→refreshDirtyAttributes、getScale（删除）、getStepHeight→maxUpStep、resetParts→removeParts、createNavigator 签名/包名、getSummoningCrystal/isFood 两个 Redirect 目标消失。

## 4. 155 个 exclude 文件分级计划

| 分组 | 文件数 |
|---|---|
| D P1 注册/网络（构建负责人 task-2 主责，我配合 IAF 桥接） | 23 |
| A 核心链（化形/形态切换/护卫/龙息/数据）——优先 | 65 |
| B 其他服务端（item/block/entity/brewing/compat/jei 等） | 24 |
| C 客户端（task-4 负责，我不动） | 43 |

### D P1 注册/网络（构建负责人 task-2 主责，我配合 IAF 桥接）（23）

| 文件 | 当前 javac 错误数 |
|---|---|
| com/example/dragonmaid/Config.java | 11 |
| com/example/dragonmaid/DragonMaid.java | 26 |
| com/example/dragonmaid/block/DragonRecallAltarBlock.java | 1 |
| com/example/dragonmaid/entity/ModEntities.java | 2 |
| com/example/dragonmaid/item/ModItems.java | 2 |
| com/example/dragonmaid/menu/AccessoryMenu.java | 2 |
| com/example/dragonmaid/menu/DragonEquipmentSlots.java | 1 |
| com/example/dragonmaid/menu/KnowledgeMenu.java | 1 |
| com/example/dragonmaid/menu/ModMenus.java | 0 |
| com/example/dragonmaid/menu/RecallMenu.java | 0 |
| com/example/dragonmaid/network/AccessoryMenuAction.java | 0 |
| com/example/dragonmaid/network/BreathAnimationMessage.java | 2 |
| com/example/dragonmaid/network/BreathForecastMessage.java | 2 |
| com/example/dragonmaid/network/BreathImpactMessage.java | 2 |
| com/example/dragonmaid/network/BreathWaveMessage.java | 2 |
| com/example/dragonmaid/network/DragonNetwork.java | 0 |
| com/example/dragonmaid/network/ExecutionConfirmation.java | 0 |
| com/example/dragonmaid/network/ExecutionPromptMessage.java | 4 |
| com/example/dragonmaid/network/ExecutionResponseMessage.java | 0 |
| com/example/dragonmaid/network/FeedingEffectMessage.java | 2 |
| com/example/dragonmaid/network/RecallRequestMessage.java | 0 |
| com/example/dragonmaid/network/RecallRosterMessage.java | 4 |
| com/example/dragonmaid/network/TailSweepMessage.java | 2 |

### A 核心链（化形/形态切换/护卫/龙息/数据）——优先（65）

| 文件 | 当前 javac 错误数 |
|---|---|
| com/example/dragonmaid/accessory/DragonAccessories.java | 4 |
| com/example/dragonmaid/accessory/DragonAccessoryInventory.java | 2 |
| com/example/dragonmaid/accessory/DragonStorage.java | 0 |
| com/example/dragonmaid/accessory/DragonStorageInventory.java | 2 |
| com/example/dragonmaid/advancement/DragonAdvancements.java | 16 |
| com/example/dragonmaid/advancement/HumanoidCompanions.java | 12 |
| com/example/dragonmaid/combat/BreathAnimationSync.java | 5 |
| com/example/dragonmaid/combat/BreathProfile.java | 0 |
| com/example/dragonmaid/combat/CompanionDefense.java | 8 |
| com/example/dragonmaid/combat/DragonArmor.java | 6 |
| com/example/dragonmaid/combat/GuardEscort.java | 1 |
| com/example/dragonmaid/combat/HumanoidCombat.java | 3 |
| com/example/dragonmaid/combat/HumanoidGoal.java | 0 |
| com/example/dragonmaid/combat/HumanoidIdleLook.java | 0 |
| com/example/dragonmaid/combat/HumanoidRoar.java | 0 |
| com/example/dragonmaid/combat/HumanoidTailSweep.java | 0 |
| com/example/dragonmaid/combat/HumanoidTargetGoal.java | 0 |
| com/example/dragonmaid/combat/MovingBreaths.java | 9 |
| com/example/dragonmaid/combat/NativeCompanionTargetGoal.java | 0 |
| com/example/dragonmaid/combat/SafeDragonCharges.java | 3 |
| com/example/dragonmaid/combat/TailSweepGeometry.java | 0 |
| com/example/dragonmaid/combat/TailSweepSync.java | 6 |
| com/example/dragonmaid/combat/TamedDragonResistance.java | 11 |
| com/example/dragonmaid/combat/WildDragonCombat.java | 0 |
| com/example/dragonmaid/combat/WildDragonTargetGoal.java | 0 |
| com/example/dragonmaid/data/DragonEvents.java | 16 |
| com/example/dragonmaid/data/DragonForms.java | 5 |
| com/example/dragonmaid/data/DragonState.java | 0 |
| com/example/dragonmaid/data/HumanoidLife.java | 0 |
| com/example/dragonmaid/data/HumanoidWater.java | 0 |
| com/example/dragonmaid/data/InjuryRecovery.java | 2 |
| com/example/dragonmaid/learning/KnowledgeBalanceMigration.java | 18 |
| com/example/dragonmaid/learning/KnowledgeMaterials.java | 0 |
| com/example/dragonmaid/learning/KnowledgeUpgrades.java | 6 |
| com/example/dragonmaid/mixin/DragonChargeMixin.java | 18 |
| com/example/dragonmaid/mixin/DragonDetailsMenuMixin.java | 32 |
| com/example/dragonmaid/mixin/DragonForgeBreathMixin.java | 8 |
| com/example/dragonmaid/mixin/DragonForgeInputMixin.java | 10 |
| com/example/dragonmaid/mixin/DragonHornMixin.java | 15 |
| com/example/dragonmaid/mixin/DragonInjuryMixin.java | 31 |
| com/example/dragonmaid/mixin/DragonLogicMixin.java | 12 |
| com/example/dragonmaid/mixin/DragonMixin.java | 206 |
| com/example/dragonmaid/mixin/DragonPartInjuryMixin.java | 5 |
| com/example/dragonmaid/mixin/DragonRecallMixin.java | 17 |
| com/example/dragonmaid/mixin/DragonVariantMixin.java | 20 |
| com/example/dragonmaid/mixin/GorgonHeadPetrificationMixin.java | 10 |
| com/example/dragonmaid/mixin/GorgonPetrificationMixin.java | 10 |
| com/example/dragonmaid/mixin/HumanoidIdleLookBodyMixin.java | 9 |
| com/example/dragonmaid/mixin/IceDragonWaterMixin.java | 14 |
| com/example/dragonmaid/mixin/LivingDragonArmorMixin.java | 9 |
| com/example/dragonmaid/mixin/RecallEntityMixin.java | 17 |
| com/example/dragonmaid/mixin/RecallTameMixin.java | 9 |
| com/example/dragonmaid/mixin/StoneStatueItemMixin.java | 8 |
| com/example/dragonmaid/movement/HumanoidAmphibiousNavigation.java | 3 |
| com/example/dragonmaid/movement/HumanoidDoors.java | 6 |
| com/example/dragonmaid/movement/HumanoidGroundNavigation.java | 3 |
| com/example/dragonmaid/movement/HumanoidMoveControl.java | 1 |
| com/example/dragonmaid/recall/RecallClientContent.java | 1 |
| com/example/dragonmaid/recall/RecallContent.java | 4 |
| com/example/dragonmaid/recall/RecallDragonPetrification.java | 9 |
| com/example/dragonmaid/recall/RecallHorn.java | 8 |
| com/example/dragonmaid/recall/RecallLedger.java | 5 |
| com/example/dragonmaid/recall/RecallPersistence.java | 1 |
| com/example/dragonmaid/recall/RecallService.java | 15 |
| com/example/dragonmaid/recall/RecallTracking.java | 16 |

### B 其他服务端（item/block/entity/brewing/compat/jei 等）（24）

| 文件 | 当前 javac 错误数 |
|---|---|
| com/example/dragonmaid/brewing/RemedyBrewing.java | 13 |
| com/example/dragonmaid/compat/ForgeCooperation.java | 20 |
| com/example/dragonmaid/compat/HumanoidForge.java | 2 |
| com/example/dragonmaid/compat/StackNbt.java | 0 |
| com/example/dragonmaid/compat/WriteBackTag.java | 5 |
| com/example/dragonmaid/compat/jei/DragonMaidJeiPlugin.java | 0 |
| com/example/dragonmaid/compat/net/NetworkHooks.java | 1 |
| com/example/dragonmaid/compat/net/NetworkRegistry.java | 0 |
| com/example/dragonmaid/compat/net/SimpleChannel.java | 4 |
| com/example/dragonmaid/entity/ReinforcedHornItemEntity.java | 1 |
| com/example/dragonmaid/item/PetrificationRemedyItem.java | 0 |
| com/example/dragonmaid/item/ReinforcedDragonHornItem.java | 4 |
| com/example/dragonmaid/item/ServiceDragonArmorItem.java | 2 |
| com/example/dragonmaid/item/TrinityDragonGemItem.java | 0 |
| com/example/dragonmaid/particle/ModParticles.java | 0 |
| com/example/dragonmaid/petrification/BasicDragonPetrification.java | 5 |
| com/example/dragonmaid/petrification/DragonPetrification.java | 9 |
| com/example/dragonmaid/petrification/PetrificationLedger.java | 3 |
| com/example/dragonmaid/recipe/DragonBreathEmblemRecipe.java | 8 |
| com/example/dragonmaid/recipe/KaishakuBladeRecipe.java | 6 |
| com/example/dragonmaid/recipe/ModRecipes.java | 2 |
| com/example/dragonmaid/recipe/ReinforcedDragonHornRecipe.java | 8 |
| com/example/dragonmaid/recipe/UltimateDragonBreathRecipe.java | 8 |
| com/example/dragonmaid/recipe/VerticalAccessoryRecipe.java | 8 |

### C 客户端（task-4 负责，我不动）（43）

| 文件 | 当前 javac 错误数 |
|---|---|
| com/example/dragonmaid/client/AccessoryScreen.java | 2 |
| com/example/dragonmaid/client/ArticulatedArmorMesh.java | 6 |
| com/example/dragonmaid/client/BreathPoseLayer.java | 0 |
| com/example/dragonmaid/client/BreathVisuals.java | 0 |
| com/example/dragonmaid/client/ClientBreathAnimations.java | 0 |
| com/example/dragonmaid/client/ClientBreathWaves.java | 8 |
| com/example/dragonmaid/client/ClientEvents.java | 19 |
| com/example/dragonmaid/client/ClientMouthEffects.java | 11 |
| com/example/dragonmaid/client/ClientRegistration.java | 19 |
| com/example/dragonmaid/client/ClientTailSweepAnimations.java | 0 |
| com/example/dragonmaid/client/DragonScreenTabs.java | 0 |
| com/example/dragonmaid/client/ExecutionPrompt.java | 1 |
| com/example/dragonmaid/client/FadingBreathParticle.java | 0 |
| com/example/dragonmaid/client/FemaleChestplateMaterial.java | 2 |
| com/example/dragonmaid/client/FemaleChestplateMesh.java | 35 |
| com/example/dragonmaid/client/FemaleChestplateResources.java | 8 |
| com/example/dragonmaid/client/HumanoidBannerLayer.java | 0 |
| com/example/dragonmaid/client/HumanoidDragonRenderer.java | 5 |
| com/example/dragonmaid/client/HumanoidMotion.java | 0 |
| com/example/dragonmaid/client/HumanoidStatueMaterials.java | 4 |
| com/example/dragonmaid/client/HumanoidStatueRenderer.java | 3 |
| com/example/dragonmaid/client/IceFemaleAnimatable.java | 7 |
| com/example/dragonmaid/client/IceFemaleEquipmentLayer.java | 4 |
| com/example/dragonmaid/client/IceFemaleModel.java | 0 |
| com/example/dragonmaid/client/IceFemaleRenderer.java | 18 |
| com/example/dragonmaid/client/KnowledgeScreen.java | 1 |
| com/example/dragonmaid/client/LegArmorMesh.java | 0 |
| com/example/dragonmaid/client/ManagedLayeredTexture.java | 1 |
| com/example/dragonmaid/client/NativeMetalArmor.java | 6 |
| com/example/dragonmaid/client/NativeMetalEquipment.java | 0 |
| com/example/dragonmaid/client/RecallScreen.java | 1 |
| com/example/dragonmaid/client/ReinforcedHornRendering.java | 8 |
| com/example/dragonmaid/client/ServiceArmorResources.java | 41 |
| com/example/dragonmaid/client/ServiceDragonArmorTextures.java | 7 |
| com/example/dragonmaid/client/SharedAnimationPose.java | 15 |
| com/example/dragonmaid/client/SwimPose.java | 0 |
| com/example/dragonmaid/client/TailArmorLayer.java | 5 |
| com/example/dragonmaid/mixin/client/DragonDetailsScreenMixin.java | 13 |
| com/example/dragonmaid/mixin/client/HumanoidStatueMixin.java | 10 |
| com/example/dragonmaid/mixin/client/JadeDragonArmorMixin.java | 8 |
| com/example/dragonmaid/mixin/client/LightningMouthMixin.java | 8 |
| com/example/dragonmaid/mixin/client/PatchouliBookTitleMixin.java | 19 |
| com/example/dragonmaid/mixin/client/ServiceDragonArmorMixin.java | 11 |

## 5. 迁移陷阱清单（已实测）

1. DragonBaseEntity.defineSynchedData 在 1.21.1 变成 defineSynchedData(SynchedEntityData.Builder)，mixin 回调参数必须同步。
2. Entity.fireTicks 在 1.21.1 被移除（private remainingFireTicks + getRemainingFireTicks/setRemainingFireTicks），@Shadow 字段要改方法。
3. getStepHeight() 在 1.21.1 改为 maxUpStep()（属性 Attributes.STEP_HEIGHT）。
4. CE 的 DragonBaseEntity 不再 override getScale()；对它的 @Inject 必须删。
5. CE 龙甲模型是 DragonArmorPart(HEAD/NECK/BODY/TAIL) + DragonArmorMaterial(IRON/COPPER/SILVER/GOLD/DIAMOND/NETHERITE/DRAGON_STEEL_FIRE/ICE/LIGHTNING) 两个枚举。
6. CE 召唤水晶是 3 个物品（SUMMONING_CRYSTAL_FIRE/ICE/LIGHTNING）+ DragonType.getCrystalItem()，没有 getSummoningCrystal()。
7. CE 没有 IafConfig：配置在 IafCommonConfig.INSTANCE.dragon.*（jupiter，getValue()）；工程里 compat.IafCompatConfig 已是垫片。
8. CE 没有 citadel 的 EntityData/EntityDataProvider；只有 uranus EntityPropertyDelegate（ContainerData 整数槽），本 mod 数据建议走 Entity.getPersistentData() + 现有 16 个网络消息同步。
9. CommonProxy 不存在；MessageUpdateDragonforge 无对应 payload（CE 龙炉同步自带宽）。
10. 客户端类全部改名（Renderer→*EntityRenderer、LayerDragonArmor→DragonArmorFeatureRenderer、GuiDragon→DragonScreen、Model*Armor→*ArmorModel），归 task-4。

## 7. P2 执行进度（2026-11-01）

### 已完成（用 javac 直连 build/moddev/artifacts/neoforge-21.1.264-merged.jar + libs/*.jar 做本地类型检查，不跑 gradle、不写 build/）

| 项 | 结果 |
|---|---|
| `mixin/DragonMixin.java` 改写 | **javac 本文件错误 51 → 0** |
| `iafcompat/IafCompat.java`（新增） | 0 错误（CE 2.0 桥：召唤水晶/食物/NoAI/菜单/火焰 tick/喂食） |
| `iafcompat/EntityDataCompat.java`（新增） | 0 错误（替代 EntityData/EntityDataProvider：PersistentData + 1.21.1 原生冻结 tick） |

DragonMixin 具体改动（对应 §3 表的"处理"列）：
1. `@Shadow protected int fireTicks` → `getRemainingFireTicks()/setRemainingFireTicks(int)`（1.21.1 移除字段；3 处读写同步改）。
2. `defineSynchedData` 回调改 `(SynchedEntityData.Builder builder, CallbackInfo ci)`，`this.entityData.define(...)` → `builder.define(...)`。
3. `updateAttributes` → `refreshDirtyAttributes`（@Inject target）。
4. `getScale` @Inject 删除（CE 不再 override）；`getStepHeight` → `maxUpStep`；`resetParts` target → `removeParts`，调用 `resetParts(float)` → `updateParts()`。
5. `createNavigator` 描述符改 CE/uranus 4 参版本，回调参数去掉 height。
6. `getSummoningCrystal()` Redirect → `DragonType.getCrystalItem()`（handler 参数改 DragonType）。
7. `openInventory(Player)` → `player.openMenu(dragon)`（MenuProvider）。
8. `isAiDisabled()` → `isNoAi()`。
9. 43 处 `(DragonBaseEntity)this` → `(DragonBaseEntity)(Object)this`（反编译丢掉了 Object 中转，javac 才能过；mixin 运行时等价）。
10. 旧 Redirect 描述符 `Lcom/github/alexthe666/iceandfire/entity/DragonBaseEntity` 全部改 `Lcom/iafenvoy/...`。

### 下一步（文件→计划）
- A 组核心链（非 client，我负责）：`combat/GuardEscort`、`combat/HumanoidCombat`（EntityData→iafcompat.EntityDataCompat）、`combat/HumanoidHome`（IafConfig→compat.IafCompatConfig）、`item/ServiceDragonArmorItem`（DragonArmorType→DragonArmorPart+DragonArmorMaterial）、`data/DragonState|DragonForms|DragonEvents|HumanoidLife|HumanoidWater`、`movement/*`、`network/Breath*`。
- B 组其他服务端 24 个（item/block/entity/brewing/compat/jei）。
- C 组客户端 43 个归 task-4；D 组注册/网络 23 个归 build owner（我配合 IAF 桥接）。
- 交付节奏：每批改完把文件清单发给 build owner 统一编译，跨包错误回给我修。

