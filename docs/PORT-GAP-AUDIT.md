# Dragon Maid 1.21.1 NeoForge 移植 · 接线缺口审计（DM-PORT-GAP-AUDIT）

- 审计时间：2026-11-01 17:1x ~ 17:3x（Asia/Shanghai）
- 对象：D:\BaiduNetdiskDownload\projects\medieval-world\dragonmaid-port
- 依据 jar：`libs/iceandfire-2.0.jar`（SHA256 43503392…990B40，与 `_probe_dm_client/mods/iceandfire-2.0.jar` 完全一致）
- 对照：`compiledWithNeoForge_*.jar`（1.21.1 + NeoForge 21.1.x，mojmap 名）、`libs/Patchouli-1.21.1-93-NEOFORGE.jar`
- 禁区（本轮未改）：`accessory/DragonAccessories.java`、`item/ReinforcedDragonHornItem.java`

---

## 0. 一句话结论

- 37 个 mixin 的**目标方法全部存在**；13 处依赖具体 INVOKE 目标的注入点也在目标字节码里逐一确认。**没有发现「目标不存在」的 mixin**。
- 找到 **18 条 gap**（其中 2 条只是脆弱写法/死代码）：**3 类共 5 个文件已修**，4 次编译全部 0 error；其余 9 条需要人决策或属低优先。
- 最要命的两条（都已修）：
  1. `StackNbt.getTag()` 在 1.21.1 返回**副本**（1.20.1 是活引用），`RecallHorn` 往号角里写的「收据」全部写丢 → 被号角收走的龙**再也放不出来**。
  2. 附魔书在 1.21 用 `STORED_ENCHANTMENTS` 组件，代码还在读 1.20.1 的 NBT 列表 → 学习系统对**所有附魔书**判定非法、给 0 经验。

---

## 1. 真实执行过什么 / 没做什么

**执行过（可复现）**

1. `javap -p -s` 导出 IAF CE / MC / Patchouli 的 35 个目标类成员表，逐条核对 mixin 的 `method=` 名与描述符；
2. `javap -p -c` 对 13 处调用点注入（@Redirect / @ModifyArg / @Inject(at=INVOKE)）在**目标方法字节码内**确认被调方法/字段存在；
3. 核对 `dragonmaid.mixins.json` 的 37 条与源码 37 个 .java 一一对应（无「写了类但没登记」）；
4. 读当前活动客户端 `D:\mc-model\work\_probe_dm_data\logs\debug.log`（17:13 写入）与 `_probe_dm_client\logs\latest.log`，grep `Mixin apply` / `InvalidInjection` / `failed to apply` → **0 命中**；grep dragonmaid 的 ERROR/WARN → 只有 1 条 jarjar metadata 的 TRACE 噪音；
5. 检查 `_probe_dm_client\mods`（确认**没有 Jade**）；比对部署 jar 与 build/libs jar 的 mixin 条目（两者都含全部 37 个）；
6. 全仓库 grep `EntityTag` / `getTag(` / `getOrCreateTag` / `CompoundTag` / `hasUUID` / `StackNbt`、grep `TODO/FIXME/return null;/return false;//`、grep 常见 client/common 事件钩子；
7. 4 次 `gradle compileJava --no-daemon -x createMinecraftArtifacts`（baseline 1 次 + 每轮修复后 3 次），**全部 BUILD SUCCESSFUL / 0 error**。

**没做**

- 没改禁区两个文件；
- 没 build jar、没起/停任何服务端或客户端、没碰 _probe_dm / _probe_dm_client / _probe_dm_data 的任何文件；
- **没有做运行期验证**：本次结论 = 静态（javap/字节码）+ 编译验证。修复是否在游戏内生效，需要在下一轮实机确认（见 §7）。

---

## 2. 问题表（全量 18 条）

| # | 问题 | 文件:行 | 类型 | 严重度 | 建议修法 / 现状 |
|---|------|---------|------|--------|-----------------|
| 1 | 号角「收据」写不回物品：`StackNbt.getTag()` 返回 CUSTOM_DATA 副本，`observe()` 里 put 的 EntityTag / DragonMaidRecall 全部丢失 → `release()` 永远认为收据不匹配 → 收进号角的龙放不出来 | recall/RecallHorn.java:331 | 残留（1.20.1→1.21 语义变化） | **P0** | **已修**：改用 `StackNbt.getOrCreateTag()`（WriteBackTag 会回写） |
| 2 | 同源：`clear()` 的 remove 也写在副本上 → 失效号角永远清不干净（每 tick 反复失败） | recall/RecallHorn.java:432 | 残留 | P1 | **已修**：同上 |
| 3 | 号角里已用 CE 组件（DRAGON_HORN）装了龙时仍允许再抓 → 覆盖/丢龙 | recall/RecallHorn.java:57 | 残留 | P1 | **已修**：加 `itemstack.has(IafDataComponents.DRAGON_HORN.get())` |
| 4 | 石化雕像物品数据仍读 `IAFStoneStatueNBT` 自定义 NBT；CE 2.0 改成 `IafDataComponents.STONE_STATUS`（`StoneStatusComponent.nbt()`）→ 自定义放置路径整段**永远不触发** | mixin/StoneStatueItemMixin.java:26 | 残留 | P1 | **已修**：读 STONE_STATUS |
| 5 | 同上：雕像放置主体（账本校验/防重复/回执回写）读写旧 key | recall/RecallDragonPetrification.java:549,613 | 残留 | P1 | **已修**：读组件 + 回写组件 |
| 6 | 同上（旧副本分支） | petrification/BasicDragonPetrification.java:327 | 残留 | P2（该方法本身无人调用，见 #12） | **已修**：保持一致 |
| 7 | 附魔书学习：1.21 附魔书用 `DataComponents.STORED_ENCHANTMENTS`，代码从 custom_data 找 `StoredEnchantments` → `bookKind()` 恒 INVALID、`bookLearningExperience()` 恒 0 | learning/KnowledgeUpgrades.java:114,199 | 残留 | **P0**（学习系统对书全废） | **已修**：新增 `storedEnchantments(stack)` 从组件重建旧列表，两处调用点已换（:145 / :230） |
| 8 | 绑定龙的三龙宝石 tooltip 读 custom_data 的 `Dragon.CustomName`；CE 存在 `CRYSTAL_DRAGON_DATA` 组件里 | item/TrinityDragonGemItem.java:19-20 | 残留 | P3（纯外观） | **未修**：CE 的 key 约定未确认（`hasDragon()` 只判「任一 key 含 Dragon」），猜错无收益 |
| 9 | `DragonHornMixin` 里整套 1.20.1 抓龙实现（写 EntityTag）是 `@Unique` 死方法，从未被调用 | mixin/DragonHornMixin.java:86-153 | 空实现/死代码 | P3 | **未修**：删除或改写为 DRAGON_HORN 组件（与 #10 一起做） |
| 10 | `RecallHorn` 仍用自建 legacy 格式（EntityTag / DragonHornEntityID / DragonMaidRecall）而非 `DRAGON_HORN` 组件；自洽但与 IAF 原版 UI/model/原生释放不一致 | recall/RecallHorn.java 多处 | 残留 | P2 | **未修（需决策）**：迁组件要同时迁 RecallLedger 的 HORN 状态，属中等改造 |
| 11 | **两个 @Mod 入口同 modid 同时生效**：PortBootstrap（只打日志）+ DragonMaid（真注册） | PortBootstrap.java:10-22 | 残留 | P3 | **未修**：日志证据两者构造器都跑了 |
| 12 | `BasicDragonPetrification.placeStatue()` 整段无人调用（真路径走 RecallDragonPetrification） | petrification/BasicDragonPetrification.java:318 | 死代码 | P3 | **未修**：删或合并 |
| 13 | `FMLContext.get()` 恒 null 的占位类 | compat/FMLContext.java:8 | 空实现 | P4 | **未修**：全仓库无调用者，可删 |
| 14 | `StackNbt.getTagElement()` / `isSameItemSameTags()` 无调用者 | compat/StackNbt.java:23,27 | 死代码 | P4 | **未修** |
| 15 | 网络层 7 条消息全走 `ClientBridge` 反射，`catch (Throwable) {}` 静默吞异常 | compat/ClientBridge.java:7-24 | 脆弱接线 | P3 | **未修**：目标方法已逐个确认存在（§4），建议改静态调用 |
| 16 | `JadeDragonArmorMixin` 目标类 `snownee.jade…EntityArmorProvider` **不存在**（运行实例没装 Jade） | mixin/client/JadeDragonArmorMixin.java:12 | 目标不存在（但安全） | P4 | **不修**：@Pseudo 自动跳过；装 Jade 后才有意义 |
| 17 | `DragonForgeBreathMixin` 两个 @Redirect 绑在编译器生成名 `lambda$destroyAreaBreath$0/$1` 上；且 handler 形参表比「receiver + 被调实参 + 捕获参数」的约定少一个 `LivingEntity` | mixin/DragonForgeBreathMixin.java:18-36 | 脆弱 | P2 | **未修（存疑）**：lambda 名确实存在、内部确有 `onHitWithFlame(LivingEntity)` 调用；但 IAF 一重编就会改名。建议改稳定注入点，并实机走到龙焰锻造看日志 |
| 18 | 已知 bug #4：护甲加成不生效（回血生效、护甲不生效） | accessory/DragonAccessories.java:196-210 | 待定（禁区） | **P0（用户可见）** | **未改**（combatkeys 占用）；分析见 §5 |

---

## 3. mixin 审计明细（37 个，全部核对过）

**方法**：`javap -p -s` 核对方法名/描述符；含 @At(INVOKE)/@Redirect/@ModifyArg 的，再用 `javap -p -c` 在**目标方法字节码**里确认被调成员存在。

| mixin | 目标 | 结论 |
|---|---|---|
| DragonChargeMixin | DragonChargeEntity.onHit / canHitMob / tick | 全部存在 |
| DragonDetailsMenuMixin | DragonMenu 4 参构造 / stillValid / quickMoveStack / removed；@Shadow dragonInventory、dragon | 全部存在 |
| DragonForgeAccessor / DragonForgeInputAccessor | DragonForgeBlockEntity.cookTime / DragonForgeInputBlockEntity.core | 字段存在 |
| DragonForgeBreathMixin | IafDragonDestructionManager.lambda$destroyAreaBreath$0/$1 | 存在，lambda 内确有 onHitWithFlame:(LivingEntity)V（脆弱性见 #17） |
| DragonForgeInputMixin | DragonForgeInputBlockEntity.lureDragons | 存在 |
| DragonHornMixin | DragonHornItem.useOn / interactLivingEntity | 存在 |
| DragonInjuryMixin | DragonBaseEntity.die / tickDeath / hurt / isAlliedTo / growDragon / tick；@Redirect 目标 isModelDead | 全部存在；hurt 与 isAlliedTo 字节码里都真有 isModelDead() 调用 |
| DragonLogicMixin | IafDragonLogic.updateDragonAttack + @Shadow dragon | 存在 |
| DragonMenuAccessor | DragonMenu.dragon | 存在 |
| DragonMixin | 35+ 目标：spawnItemCrackParticles / hurt / remove / setInSittingPose / setBreathingFire / roar / defineSynchedData / setTarget / add+readAdditionalSaveData / setModelDead / setCommand / setAgeInTicks / setAgeInDays / refreshDirtyAttributes / registerGoals / tick / updateBurnTarget / getDimensions / maxUpStep / removeParts / updateParts / breakBlocks / setFlying / setHovering / isAllowedToTriggerFlight / canAddPassenger / mobInteract / interactAt / createNavigator(4 参) / switchNavigator / travel / isInWater / getHeadPosition / isBreakable | 全部命中 |
| DragonPartInjuryMixin | MultipartPartEntity.getParent / getParentId | 存在 |
| DragonRecallMixin | growDragon / setModelDead / addAdditionalSaveData | 存在 |
| DragonVariantMixin | Fire/Ice/Lightning.aiStep / doHurtTarget / breathFireAtPos | 存在；三个 aiStep 都真有 DragonBaseEntity.aiStep() 调用（shift=AFTER 成立） |
| GorgonHeadPetrificationMixin | GorgonHeadItem.releaseUsing + Entity.remove / buildStatueEntity / addFreshEntity | 三个 INVOKE 目标都在 releaseUsing 字节码里 |
| GorgonPetrificationMixin | GorgonEntity.aiStep + buildStatueEntity / addFreshEntity / LivingEntity.remove | 三个 INVOKE 目标都在 aiStep 字节码里 |
| HumanoidIdleLookBodyMixin | Mob.tickHeadTurn | 存在 |
| IceDragonWaterMixin | IceDragonEntity.travel / useFlyingPathFinder | 存在 |
| LivingDragonArmorMixin | LivingEntity.getDamageAfterArmorAbsorb + CombatRules.getDamageAfterAbsorb | 方法与被调点都存在 |
| Recall*Accessor | storeChunkSections / permanentStorage / ChunkStorage.worker / ServerLevel.entityManager | 全部存在 |
| RecallEntityMixin | Entity.setCustomName / checkBelowWorld | 存在 |
| RecallTameMixin | TamableAnimal.setOwnerUUID | 存在 |
| StoneStatueItemMixin | StoneStatueItem.useOn | 存在 |
| client.DragonDetailsScreenMixin | DragonScreen.renderBg | 存在 |
| client.DragonPreviewScaleMixin | renderBg 内 InventoryScreen.renderEntityInInventory(GuiGraphics,FFF,Vector3f,Quaternionf,Quaternionf,LivingEntity)V | 字节码里确有该调用，index=3 是 scale |
| client.HumanoidStatueMixin | StoneStatueEntityRenderer.render(StoneStatueEntity,…) | 存在（还有 Entity 重载，描述符已写全） |
| client.LightningMouthMixin | LightningDragonEntityRenderer.render 内 LightningDragonEntity.getHeadPosition() | 存在且被调 |
| client.PatchouliBookTitleMixin | GuiBook.drawCenteredStringNoShadow ×2 | 两个重载都存在；require=0 |
| client.PatchouliEntryTitleMixin | GuiButtonEntry.renderWidget 内 GuiGraphics.drawString(Font,Component,IIIZ)I | 存在且被调 |
| client.RendererAccessor | EntityRenderer.shadowRadius | 存在 |
| client.ServiceDragonArmorMixin | DragonArmorFeatureRenderer.render(PoseStack,MultiBufferSource,I,DragonBaseEntity,FFFFFF) | 存在 |
| client.StatueCompositeRenderTypeAccessor | RenderType$CompositeRenderType.state | 存在 |
| client.StatueCompositeStateAccessor | RenderType$CompositeState.textureState | 存在（public final EmptyTextureStateShard） |
| client.StatueTextureStateInvoker | EmptyTextureStateShard.cutoutTexture | 存在 |
| client.JadeDragonArmorMixin | snownee.jade…EntityArmorProvider | **类不存在**（Jade 未装）→ @Pseudo 跳过 |

> 结论：**没有「目标不存在」或「注入点不存在」的 mixin**。唯一「不会被调用」的是 Jade 那条（按设计跳过）。

---

## 4. 事件注册 / 接线审计

| 需要的注册 | 现状 | 证据 |
|---|---|---|
| 粒子 provider | 12 个 particle 全注册 | client/ClientRegistration.particles() |
| 菜单屏 | Knowledge / Accessories（ClientRegistration）+ Recall（RecallClientContent 自注册） | 是 |
| Entity renderer | 只有自建 ReinforcedHornItemEntity，已注册 ItemEntityRenderer | client/ReinforcedHornRendering |
| RenderLayer / 女性龙渲染器 | EntityRenderersEvent.AddLayers 里建 27 个 IceFemaleRenderer | client/ClientRegistration.renderers() |
| 客户端资源重载监听 | 2 个 | RegisterClientReloadListenersEvent |
| Item properties | iceorfire / has_dragon | FMLClientSetupEvent |
| clientTick | ClientTickEvent.Post | client/ClientEvents.motion() |
| 帧前后/登出/卸载 | RenderFrameEvent.Pre/Post、LoggingOut、LevelEvent.Unload | 是 |
| 网络 payload | RegisterPayloadHandlersEvent | network/DragonNetwork |
| **自定义键位** | **本仓库 0 处**（KeyMapping / GLFW / InputEvent / isKeyDown 全 0 命中） | —— |

> 「三处客户端接线（键位 / clientTick / 粒子 provider）」在本仓库：clientTick 与粒子 provider 都在线；**键位根本不存在** —— 如果预期有技能键，那是「功能没实现」，不是「漏注册」。
> 另注：**没有** EntityAttributeCreationEvent / RegisterSpawnPlacementsEvent 是正常的（本 mod 只注册了一个 item 实体，没有生物）。

---

## 5. 已知 bug #4（护甲不生效）的静态分析（未改，供 combatkeys 参考）

1. `item/DragonAccessoryItem` 只实现接口、**不覆写 armorBonus**（默认 0）；真正给护甲的是 `item/SustainingAccessoryItem`：armor ? clamp(stage,1,5)+1 : 0。
2. 对应物品：WARDING_EMBLEM=(true,false)、GUARDIAN_PENDANT=(true,false,true)、DRAGON_HEART_EMBLEM_NECKLACE=(true,true) —— **armorBonus 都是 2~6，不是 0**。
3. 唯一写入点：DragonAccessories.refresh() → `addTransientModifier(new AttributeModifier(ARMOR_ID, effects.armor, ADD_VALUE))`（DragonAccessories.java:204）。
4. 调用链：DragonMixin.dragonmaid$clearStoppedBreath（tick TAIL）→ DragonAccessories.serverTick → 条件满足时 refresh()；另有 refreshDirtyAttributes TAIL 与 setAgeInDays RETURN 两个补丁点（DragonMixin.java:616-630）。
5. 回血生效说明 serverTick/refresh 确实在跑（回血读的是同一个 effects.regenerates）。
6. 伤害结算不走原版护甲属性：LivingDragonArmorMixin 在 getDamageAfterArmorAbsorb 里直接 setReturnValue(DragonArmor.afterArmor(damage, rawArmor(dragon), toughness))；rawArmor 又把 Attributes.ARMOR 的 modifiers 自己加回去。

因此「属性里看不到 dragonmaid 的 armor modifier」最可能的两种解释：

- **(a) transient 修改器被 IAF 的 refreshDirtyAttributes() 清掉 / 未同步** —— 建议改成 `addPermanentModifier`（注意：同 id 已存在时直接 add 会抛 "Modifier is already applied"，必须先 removeModifier(ARMOR_ID)）；
- **(b) 观察侧看不到** —— 1.21.1 里 transient modifier 的同步/持久化语义与 1.20.1 不同，客户端 attributes 里可能确实没有，但服务端有。**先确认你查的是服务端 /attribute 还是客户端**。

> 该文件被 combatkeys 占用，本轮**一个字符都没改**。

---

## 6. 本轮已修（5 个文件 / 4 次编译全 0 error）

| 文件 | 位置 | 改动 |
|---|---|---|
| recall/RecallHorn.java | :331 observe()、:432 clear() | `StackNbt.getTag()` → `getOrCreateTag()`（写回 CUSTOM_DATA） |
| recall/RecallHorn.java | :57 | 抓龙前先看 `stack.has(IafDataComponents.DRAGON_HORN.get())` |
| mixin/StoneStatueItemMixin.java | :26-27 | 改读 `IafDataComponents.STONE_STATUS` → `StoneStatusComponent.nbt()` |
| recall/RecallDragonPetrification.java | :549-550、:613-616 | 同上读取；回执写回改为 `itemstack.set(STONE_STATUS, new StoneStatusComponent(isPlayer, entityType, nbt))` |
| petrification/BasicDragonPetrification.java | :327-329 | 同上读取（该方法当前无人调用，保持一致） |
| learning/KnowledgeUpgrades.java | :112-133（新增 helper）、:145、:230 | 从 `DataComponents.STORED_ENCHANTMENTS` 重建 1.20.1 形状的 ListTag，原解析逻辑不变 |

编译记录（全部 `--no-daemon -x createMinecraftArtifacts`）：baseline UP-TO-DATE/SUCCESSFUL → RecallHorn 后 13s SUCCESSFUL → 雕像组件后 10s SUCCESSFUL → KnowledgeUpgrades 后 15s SUCCESSFUL → 最后一次（含 Basic 的 .copy()）11s SUCCESSFUL。

---

## 7. 建议的实机验证（下一轮）

1. **号角**：抓一只已驯服龙 → 看 NBT 里出现 `dragonmaid:DragonMaidRecall`（不再是空）→ 右键地面放出来。修复前会提示 busy/horn_expired 并失败。
2. **附魔书学习**：把附魔书放进知识界面 → 应显示可学习并给经验；修复前恒 INVALID。
3. **雕像**：把被石化的龙雕像挖下来再放下 → 应走自定义路径（账本防重复）；看日志有没有 `Could not place managed dragon statue`。
4. **龙焰锻造**：让龙对人造龙炉喷一次 → 留意日志有没有 InvalidInjectionException（对应 #17）。
5. **护甲**：装备 WARDING_EMBLEM 后，用**服务端** `/attribute @e[type=iceandfire:fire_dragon,limit=1] minecraft:armor get` 看基础值/修改器（见 §5 的 (a)/(b) 判断）。

---

## 8. 我没做但建议下一轮做的

- 把 ClientBridge 的 7 处反射改成静态调用（目标方法都在，纯清理）；
- 把 RecallHorn 整体迁到 DRAGON_HORN 组件（要一起迁 RecallLedger 的 HORN 快照，属功能改造，建议单独排期）；
- 删 PortBootstrap（或把 DragonMaid 的角色说清楚），避免两个 @Mod 同 modid；
- TrinityDragonGem 的绑定名 tooltip：需要先反编译 SummoningCrystalItem 的 key 约定再改。
