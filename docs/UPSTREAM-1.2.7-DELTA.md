# 上游增量报告：Dragon Maid Companion 1.2.0-beta.4 → 1.2.7

> 供主代理分派移植用。**只读分析**，未修改移植仓 src。

| 项目 | 值 |
| --- | --- |
| 基线 | dragonmaid-1.2.0-beta.4.jar（7,221,444 B，= 移植仓当前追平版本） |
| 目标 | dragonmaid-1.2.7.jar（10,309,731 B，sha256 1D315508…46D57D80） |
| 基线/目标类文件 | 444 / 585（.class 计数，含内部类与 lambda 类） |
| 反编译产物 | decompiled_120（300 .java）/ decompiled_127（383 .java），Vineflower 1dgs=1 1rsy=1 |
| 机器清单 | D:\mc-model\work\_port_src\analysis_127\ |

## 0. 一句话结论

1.2.7 是一次**大版本式内容更新**，不是补丁：新增 142 个类、修改 142 个类（占总类数 48%）、删除 1 个类；新增 265 个 assets 与 92 个 data 文件、160 个语言 key。核心新增五块内容——**刀/太刀/长剑/三叉戟的完整武器战斗系统（附带 4 个新附魔）**、**人形龙的自主飞行与载人骑乘**、**开局伙伴选择（含 CreateWorldScreen 改造）**、**四套龙铠（+冰霜银）四件套与装饰网格**、**召回台方块实体与号角恢复流程**。

---

## 1. 类级增量

### 1.1 总量

| 指标 | 数量 | 说明 |
| --- | --- | --- |
| 新增类 | **142** | 其中顶层类 84，内部类/record 58 |
| 修改类 | **142** | 同一路径但字节不同（sha256 判定），其中顶层类 103 |
| 删除类 | **1** | com/example/dragonmaid/mixin/client/JadeDragonArmorMixin |
| 未变化类 | 301 | 多为工具、命令、石化、学习、兼容层 |
| 目标类总数 | 585 | 直接读 .class 的绝对数 |

判定方法：对 extracted_120 / extracted_127 下全部 .class 取 sha256，不依赖反编译，因此「修改」是**字节级确定**的集合，不存在漏判。

### 1.2 新增类按 package 分组（142）

| package | 新增数 | 主要内容 |
| --- | --- | --- |
| com/example/dragonmaid/client | **55** | 连续翼膜网格、龙铠装饰、刀鞘层、第一人称刀动画、开局伙伴 UI、召回台渲染、玩家骑乘坐姿 |
| com/example/dragonmaid/combat | **20** | BladeCombat、HumanoidArmedSweep、HumanoidWingThrust、LongswordParry、LongswordProjectiles、KatanaCharge/KatanaSweep、SpecialWeapons、DragonTridentFlight 驱动器、RestReturnGoal、WaterfowlLeap |
| com/example/dragonmaid/mixin/client | **17** | 骑乘相机/骑手模型/渲染剔除/刀模型/CreateWorldScreen/预览世界 |
| com/example/dragonmaid/mixin | **9** | 飞行休息返航、重登载具、长剑横扫/爆炸/使用序列、妖精偷窃、玩家龙面板与飞行 |
| com/example/dragonmaid/item | **7** | CompanionBladeItem、DragonBloodBladeItem、BladeEnchantments、ServiceDragonArmorItem.Style、ModItems.BladePair |
| com/example/dragonmaid/movement | **6** | HumanoidFlight（1093 行）、EscortFlightOrbit |
| com/example/dragonmaid/starter | **5** | StarterConfig / StarterSettings / StarterWorldData |
| com/example/dragonmaid/recall | **5** | RecallHornRecovery（号角事务恢复） |
| com/example/dragonmaid/config | **4** | GlobalBalanceConfig.Scope（全局/存档双层配置）、HumanoidRidingConfig |
| com/example/dragonmaid/data | **3** | HumanoidGroundMount、HumanoidHealth |
| com/example/dragonmaid/network | **3** | HumanoidFlightInputMessage、PlayerKatanaDrawMessage、PlayerRiposteMessage |
| com/example/dragonmaid/entity | **3** | DragonTridentFlight（extends ThrownTrident） |
| com/example/dragonmaid/menu | **2** | DragonPanelInteraction |
| com/example/dragonmaid/recipe | **2** | DragonBloodBladeRecipe |
| com/example/dragonmaid/block | **1** | RecallAltarBlockEntity |

完整清单见 analysis_127/new_by_package.txt 与 classes_new.txt。

### 1.3 删除类（1）

| 类 | 原因 |
| --- | --- |
| mixin/client/JadeDragonArmorMixin | 从 mixins.json 的 client 列表移除。上游不再对 Jade 的龙铠显示做 mixin（推测改为 ServiceDragonArmorMixin + DragonArmorOrnaments 自绘），移植时**不要**继续保留这个 mixin。 |

### 1.4 修改类按功能分组（142）

| 分组 | 新增 | 修改 | 合计 |
| --- | --- | --- | --- |
| A 战斗/武器（刀/太刀/长剑/三叉戟/盾） | 59 | 47 | **106** |
| F 渲染/动画/客户端 | 22 | 30 | **52** |
| B 飞行/载人/骑乘 | 22 | 1 | 23 |
| K 其他（数据/实体/工具/主体） | 3 | 18 | 21 |
| D 龙铠四件套 | 7 | 11 | 18 |
| E 饰品/召回台 | 7 | 11 | 18 |
| C 开局伙伴选择 | 16 | 0 | 16 |
| I 服务端 mixin / IAF 交互 | 3 | 10 | 13 |
| H 配置 | 2 | 5 | 7 |
| G 网络包 | 1 | 4 | 5 |
| J 物品/配方/附魔 | 0 | 5 | 5 |
| **合计** | **142** | **142** | **284** |

注：分组是按类名/路径的启发式归类，边界类（如 client/IceFemaleModel 同时属于渲染与龙铠）归入一个组，逐类明细见 analysis_127/classes_by_group.txt。

---
## 2. 各分组的人类可读改动摘要

### 2.1 A 组：战斗 / 武器系统（最大改动块）

这是 1.2.7 的主体。1.2.0-beta.4 的武器只有「剑/斧/尾扫」三档抽象；1.2.7 拆成了**四类武器 × 三套架势机制**。

**新增武器品类与判定（combat/SpecialWeapons，124 行，全新）**

| 机制 | 说明 |
| --- | --- |
| 三个物品 tag | dragonmaid:tridents / longswords / katanas（data/dragonmaid/tags/items/ 下新增），且要求物品命名空间必须是 dragonmaid（ownWeapon 校验） |
| 长剑额外触及 | 装备长剑时 +1.0 格（AttributeModifier，UUID 16800d2a-…-0e9add020044）；仅雷电龙 + 已训练 + 主手长剑生效 |
| 火斧 | 仅火龙 + 已训练 + 斧；忽略 40% 护甲；伤害随最大生命 40→400 从 x1.1 线性到 x1.4 |
| 冰三叉戟 | 分「可投掷」（无忠诚附魔）与「激流」两条分支，仅冰龙 + 已训练 |
| 翼卫（wing_guard） | 仅雷电龙 + 已驯服 + 佩戴 wing_guard_legend 饰品；可用普通近战武器起手一次翼助推 |

**新增三套架势（combat/BladeMotion，动作编号 7-14）**

| 动作 id | 语义 | 关键常数 |
| --- | --- | --- |
| 7 / 8 / 9 / 10 | 长剑 起手 / 命中 / 反击 / 收势 | LONGSWORD_ENTER_TICKS=5、RIPOSTE_TICKS=11 |
| 11 / 12 / 13 | 太刀 蓄力 / 释放 / 取消 | KATANA_WINDUP=5、KATANA_RELEASE_TICKS=18、KATANA_FINISH=31 |
| 14 | 空中三叉戟投掷（combat/AirTridentThrow） | RELEASE_TICK=13、DURATION=32 |
| 15 | 骑乘直击（HumanoidGoal.MOUNTED_DIRECT_STRIKE_ACTION） | POSE_TICKS=6 |

**太刀蓄力（combat/KatanaCharge，40 行，全新）**：满蓄 60 tick、三阶各 20 tick；伤害 x2.0，且护甲越低加成越高（lightArmorMultiplier = 1 + 0.05 x max(0, 10 - armorPoints)）；蓄力时移速 x0.4；三阶配色 4303615 / 11760383 / 16748595。

**太刀横扫（combat/KatanaSweep，81 行，全新）**：240 度弧形判定（HALF_ARC_COS = cos 120）、半厚 1.0；对 AABB 做「最近点 + 8 角点」采样 + 视线遮挡检查，实现贴合弧形的命中盒。

**长剑格挡/弹反（combat/LongswordParry + LongswordProjectiles + BladeCombat，全新 121 + 380 + 530 行）**

| 能力 | 实现要点 |
| --- | --- |
| 格挡窗口 | 起手延迟 guardDelaySeconds（默认 0）、弹反窗口 parryWindowSeconds（默认 0.75s）、弹反后冷却 parryCooldownSeconds（默认 1.0s），三项**均为新增配置项**并经 BladeEnchantments 加成 |
| 近战弹反 | 命中判定要求攻击者是直接来源、且不属于「不可格挡」伤害类型标签（DamageTypeTags 6 个 + DamageTypes 4 个 + IAF 的龙息/龙冰/龙雷 3 个） |
| 弹反反击 | 主目标 x1.5，副目标 x0.8；反击冷却可跨不同长剑共享（挂在 Player 的 WeakHashMap 上） |
| 投射物反射 | 拦截 ProjectileImpactEvent + LivingAttackEvent；NBT 标记 DragonMaidWingGuardReflected / DragonMaidLongswordDeflector / DragonMaidDeflectedChargeDamage；翼卫基础反射率 0.5；反射回 IAF 投射物时走特殊处理 |
| 客户端表现 | LongswordGuardRender（184）、LongswordParryOverlay（144）、LongswordParryFeedback（44） |

**新增 4 个附魔（item/BladeEnchantments，124 行，用自定义 EnchantmentCategory）**

| 附魔 | 适用范围 | 效果 | 概率/数值表（等级 1-3，下标 0 为未附魔） |
| --- | --- | --- | --- |
| windstep | 太刀 | 蓄力时移速 | 0.4 / 0.6 / 0.9 / 1.2 |
| second_chance | 长剑 | 长剑持续格挡期间被打中仍可触发一次反击 | 0 / 0.125 / 0.25 / 0.35 |
| steady_guard | 长剑 | 延长弹反窗口 | +5 tick x 等级（上限 3 级） |
| deflection | 长剑 | 格挡时弹开投射物 | 0 / 0.15 / 0.3 / 0.5 |

**新增武器与龙铠物品（item/ModItems，+61 行）**

| 类别 | 数量 | 命名 |
| --- | --- | --- |
| 长刀（jian） | 12 | wooden/stone/copper/silver/iron/golden/diamond/netherite/dragonbone 的 _jian，+ dragonbone_jian_{fire,ice,lightning} |
| 太刀（katana） | 12 | 同上对应的 _katana |
| 龙装四件套 | 16 | kimono / xia / viking / frostsilver 各 4 件（head / neck / body / tail） |
| 饰品 | 1 | wing_guard_legend（CombatAccessoryItem.Kind.WING_GUARD） |

武器属性：长剑 base attack 3 / speed -2.8，太刀 1 / -1.6（均 extends SwordItem，下界合金带 fireResistant）。银质武器对亡灵额外 +2.0 伤害，且只能用 forge:ingots/silver 修复。

**其它战斗改动（修改类）**

| 类 | 变化量 | 要点 |
| --- | --- | --- |
| combat/HumanoidGoal | 547 -> 1275（+747） | 新增飞行战斗、骑乘战斗、空中三叉戟、骑乘直击、骑乘吐息、绕行守卫等状态机；是**改动量最大的单个类** |
| combat/DragonShield | 145 -> 320（+136） | 新增长剑格挡态、远程压制窗口（RANGED_WINDOW=60）、骑手格挡、盾牌禁用计时 |
| combat/HumanoidMeleeTiming | 60 -> 181（+117） | record 扩字段（axe/trident）；新增斧（4/8/12/16/20/31）与三叉戟（24/28/13/8/9）时间轴；Frame 增加 weight 字段（与旧构造兼容） |
| combat/DragonArmor | 45 -> 72 | 护甲合并上限 40（先天 + 装备 + 饰品合计） |
| animation/TailSweepState | 64 -> 186（+97） | 尾扫状态机扩展 |
| combat/HumanoidCombat / HumanoidIdleLook / HumanoidRoar / TailShieldBreak / TailSweepKnockback / SafeDragonCharges / MiniatureBalance / BreathProfile / BreathAnimationSync | 小改 | 与新武器/架势联动 |

### 2.2 B 组：飞行 / 载人 / 骑乘

**movement/HumanoidFlight（1093 行，全新，整个 1.2.7 最大的单文件）** 实现了人形龙的自主/载人飞行状态机。

| 子项 | 常量 / 机制 |
| --- | --- |
| 阶段 | TAKEOFF_TICKS=12、TAKEOFF_HOLD_TICKS=21、TOUCHDOWN_TICKS=27；Phase 枚举带 Access 接口 |
| 路径规划 | PLAN_INTERVAL=8 / CATCHUP_PLAN_INTERVAL=4、起飞点搜索半径 16、室内降落距离平方 256、室内降落半径 6 |
| 追赶逻辑 | 距离 > 12 启动、< 8 停止；速度 2.25-4.0；速度优势 0.55；刹车 5 tick |
| 垂直控制 | 载人上升 0.46 / 下降 0.52 / 响应 0.5 / 制动 0.6；自主升降 0.23 / 0.18 |
| 护送起飞 | ESCORT_LIFTOFF_HEIGHT=1.8、ESCORT_LIFT_CLEARANCE=4.0、LEDGE_RISE=1.25 |
| 吐息瞄准 | 身体转向 20 度/tick、头部 30 度/tick；BYPASS_ANGLES = +-45/+-90/+-135 用于绕开遮挡 |
| 骑乘输入 | setDescend / setRiderAttacks(breathe, strike) / setRiderGuard / clearRiderInput / emptyHands |

配套新增：

- **data/HumanoidGroundMount（111 行）**：地面载人上下车 + 重登恢复（ThreadLocal RestoringRider，配合 mixin/DragonMountRelogMixin），seatHeight / dismount 位置计算。
- **client/HumanoidRidingPose（232 行）**：把玩家骨骼按 6 个 PartPose 常量摆成「被龙抱着 / 抱龙」两种姿态（HELD_* / CARRIER_*，含俯仰-偏航-翻滚三轴），并隐藏被占用的手（RenderHandEvent）。
- **config/HumanoidRidingConfig（57 行）**：新增 client 配置 dragonmaid-client.toml，按成长阶段 III/IV/V 与性别分别选姿势，另有两个方向（dragon_carries_player / player_carries_dragon）+ piggyback。
- **movement/EscortFlightOrbit（121 行）**：8 位置/环、SECTOR=pi/4、预约 80 tick 的护送编队，避免多龙重叠；用 WeakHashMap 挂在玩家上。
- **client/HumanoidFallTimeline（120 行）**：掉落三阶段（ENTER 7 / BRACE 7 / LAND 9 / RECOVER 18），最小落差 1.5。
- **network/HumanoidFlightInputMessage**：record(vehicle, descend, breathe, strike, guard)，含 4 参数兼容构造。
### 2.3 C 组：开局伙伴选择（16 个新类，纯新增，无修改类）

| 类 | 行数 | 作用 |
| --- | --- | --- |
| client/StarterCompanionScreen | 590 | 完整的新建世界选项界面（物种/性别/成长阶段/知识/名字/初始形态/是否装进号角/饰品槽位/配色） |
| client/StarterPreviewLevel | 188 | 一个假的 Level 实现，用于在 UI 里实时渲染 3D 预览 |
| starter/StarterSettings | 246 | 设置模型 + normalize/Random 解析 + 存盘 + createEgg/create（真正造龙） |
| starter/StarterWorldData | 199 | SavedData，首次登录发伙伴/发蛋；含 EGG_CLAIM 标记防重复发放 |
| starter/StarterConfig | 75 | 上下限配置（min/maxDays、min/maxKnowledge）+ showEntry 开关 |
| client/StarterWorldOptions | 12 | 给 CreateWorldScreen 混入的接口 |
| mixin/client/StarterCreateWorldMixin | 181 | 注入 CreateWorldScreen，加 tab 与按钮 |
| mixin/client/StarterGridAccessor / StarterGridCellInvoker / StarterTabAccessor | 12 各 | 反射访问 GridLayout / GridLayoutTab 内部 |
| mixin/client/StarterPreviewLivingRenderMixin | 71 | 在预览 Level 里渲染生物 |
| mixin/client/StarterPreviewNameMixin | 27 | 预览生物名字 |

配套 160 个新语言 key 中的 starter.dragonmaid.* 约 60 条 + config.dragonmaid.starter* 约 12 条。

### 2.4 D 组：龙铠四件套

- **item/ServiceDragonArmorItem** 从 27 行扩到 66 行，新增内部枚举 **Style { KIMONO, XIA, VIKING, FROSTSILVER }**，并把旧的单参构造保留为兼容重载；新增 styleOf(ItemStack) / styleOf(ItemStack, dragon) / isSpecialtyArmor。
- **client/ServiceArmorResources** 383 -> 443：从单例 INSTANCE 改成按 Style 分实例（ServiceArmorResources.of(style)），内部 Snapshot/Binding/HumanModel 三个 record 都在变。
- **client/DragonArmorOrnaments（238 行，全新）**：从 .geo.json 烤出装饰网格（Mesh/Attachment/Face 三级），按装备槽渲染；对雷电龙有专门的 arm / forewing 骨骼解析（lightningArm / lightningForewing）。
- 资源侧新增 4 套 geo（male/female 各一）+ 各色系 ornaments geo + armor contract json：

      assets/dragonmaid/geo/{frostsilver,kimono,viking,xia}_{male,female}_armor.geo.json
      assets/dragonmaid/geo/{kimono,xia,viking}_*_ornaments.geo.json
      assets/dragonmaid/armor/<set>/<set>_armor_contract.json

- 贴图：textures/armor/<set>/ 下新增 48 张 PNG。
- 护甲合并上限 40（combat/DragonArmor 改动），语言 key config.dragonmaid.limitInnateArmorGrowth.tooltip 同步改写。

### 2.5 E 组：饰品 / 召回台

**召回台从纯装饰方块升级为可动方块实体**

| 变化 | 细节 |
| --- | --- |
| DragonRecallAltarBlock | 改为 implements EntityBlock；碰撞箱去掉中心块（只留两侧 0..4.5 与 11.5..16）；新增 newBlockEntity / getTicker |
| block/RecallAltarBlockEntity（57 行，全新） | GEM_HOVER_HEIGHT=1.0、gemYaw/gemHeight 悬浮动画、getRenderBoundingBox |
| client/RecallAltarRenderer（58 行，全新） | 渲染 block/dragon_recall_altar_gem 模型，GEM_PIVOT_Y=0.803125 |
| recall/RecallContent | 新增 BLOCK_ENTITIES DeferredRegister；setup() 改为 setup(IEventBus)；新增 ChunkEvent.Load 监听 attachExistingAltars（给已存在的老存档补挂 BlockEntity） |

**号角事务恢复（recall/RecallHornRecovery，235 行，全新）**

- 用 TicketType dragonmaid_horn_recovery + ChunkPos 强制加载，把「号角跨维度转移龙」做成可恢复的两阶段事务；
- 结果枚举对应 7 条新消息 key：horn_checking / horn_destination_loading / horn_recovery_failed / horn_recovery_ready / horn_recovery_world / horn_storage / horn_unverified；
- RecallHorn 424 -> 427，把散落在多处的 receipt 校验收敛成 matchesReceipt()，并把原先混用的 recall.busy 拆成更细的消息。

**饰品侧**：accessory/DragonHands 179 -> 255（+53）、DragonAccessories / DragonAccessoryBehavior 小改；新增 DragonAccessoryBehavior.WING_GUARD。

### 2.6 F 组：渲染 / 动画 / 客户端

**全新「连续翼膜」系统（1.2.7 渲染侧最大新增）**

| 类 | 行数 | 作用 |
| --- | --- | --- |
| client/ContinuousWingMesh | 573 | 从 JSON 解析双翼网格（Wing/Point/Face/Rig/Anchor/Bound），按骨骼绑定做线性混合蒙皮 |
| client/ContinuousWingRenderType | 64 | 自定义 RenderType |
| client/ContinuousWingResources | 67 | SimplePreparableReloadListener，按 ResourceLocation 缓存 |
| client/DragonVertexBatch | 180 | 手工顶点提交，含 Embeddium / Oculus 两条兼容分支（BufferFields / EmbeddiumWriter / OculusFormat） |
| assets/dragonmaid/wing_membranes/*.json | 18 个 | 3 物种 x 2 性别 x 阶段 3/4/5 的翼膜数据 |

**玩家武器视觉（第一人称）**

| 类 | 行数 | 作用 |
| --- | --- | --- |
| client/PlayerBladeMotion | 212 | 读 player_motions/player_first_person.motion.json，对 RightGrip / Scabbard / RightForearm / LeftForearm 四根骨骼做关键帧插值 |
| client/PlayerBladeAnimation | 180 | 播放控制 |
| client/KatanaSheathLayer | 143 | 刀鞘 RenderLayer（Style 内枚举） |
| client/PlayerKatanaPose | 87 | 太刀持握姿态 |
| client/TridentGrip | 47 | 三叉戟握持 |
| client/BladeMovementInput | 58 | 蓄力时限速 |
| client/BladeSweepParticle + SweepPoint/SweepSegment | 159 | 弧形刀光粒子（新粒子类型 blade_sweep） |
| client/KatanaChargeSparks | 64 | 蓄力火花 |
| client/HumanoidWallOcclusion | 122 | 骑乘时墙体遮挡处理 |

**大改的既有渲染类**

| 类 | 变化 |
| --- | --- |
| client/IceFemaleModel | 734 -> 1096（+519） |
| client/IceFemaleRenderer | 305 -> 644（+259），新增 ArmorDraw 内部类 |
| client/IceFemaleEquipmentLayer | 201 -> 316（+164），Piece 结构变化 |
| client/SharedAnimationPose | 347 -> 374，新增 Transition |
| client/ServiceArmorResources | 383 -> 443，按 Style 分裂 |
| client/TailArmorLayer | 61 -> 78 |
| client/ClientRegistration | 156 -> 175，注册 3 个 reload listener、blade_sweep 粒子、KatanaSheathLayer、DragonTridentRenderer |

**新增粒子/实体渲染**：DragonTridentRenderer（58，extends ThrownTridentRenderer）、RecallAltarRenderer、MiniatureLightningRenderer（小改）。

### 2.7 G 组：网络包

| 项 | 变化 |
| --- | --- |
| DragonNetwork.VERSION | **21 -> 23**（跨 2 个协议版本） |
| 新包 | HumanoidFlightInputMessage、PlayerKatanaDrawMessage、PlayerRiposteMessage |
| DragonHandsMessage | record 增加第 6 个字段 tridentAway（boolean），保留 5 参构造兼容；新增 of(dragon) |
| DragonNetwork | 注册项从 14 增至 17（3 个新包） |

**兼容性结论：1.2.7 与 1.2.0-beta.4 的客户端/服务端不可互通**（协议版本 21 不等 23，且包结构变化）。移植仓若已发版，升级需同步两侧。

### 2.8 H 组：配置

| 变化 | 细节 |
| --- | --- |
| 新增长剑配置组 | category longsword：guardDelaySeconds（0.0，范围 0-10）、parryWindowSeconds（0.75，0-10）、parryCooldownSeconds（1.0，0-10）、secondChanceTriggersCooldown（false） |
| 新增人形生命倍率 | humanoidHealthMultiplier（1.0，0.01-100），form 切换时按比例保留生命 |
| 新增吐息伤害倍率 | fireMultiplier / iceMultiplier / lightningMultiplier（各 1.0，0-100） |
| 新增开局伙伴配置组 | starterCompanion：minDays / maxDays / minKnowledge / maxKnowledge / showEntry |
| 新增 client 配置 | dragonmaid-client.toml（HumanoidRidingConfig），按阶段与性别选骑乘坐姿 |
| **配置架构重构** | config/GlobalBalanceConfig 从 87 行扩到 168 行：新增 Scope record，把 SPEC 镜像成 global / world 两套 spec，新增 dragonmaid-world-server.toml 与 dragonmaid-recall-world-server.toml，用 useWorldSettings 开关决定「本地存档覆盖全局」；starterCompanion 段被显式排除在 world 覆盖之外 |

注意：上游给所有新配置项都用了 worldRestart()，即**改配置需重进世界/重启服务器**。

### 2.9 I 组：语言 / 手册

| 项 | 变化 |
| --- | --- |
| en_us.json | 416 -> 576 key（+160，-0，12 条值改写） |
| zh_cn.json | 416 -> 576（同上，与 en_us 完全同步） |
| Patchouli 手册 | 新增 8 个条目文件（en_us + zh_cn 各 4）：wing_guard_legend、behavior/humanoid_riding、behavior/katana、behavior/longsword；改写 30 个既有条目 |
| 进度 | 新增 data/dragonmaid/advancements/book/wing_guard_legend.json |

新增 key 的分布（en_us）：

| 前缀 | 数量 | 内容 |
| --- | --- | --- |
| item.dragonmaid.* | 44 | 24 件刀/太刀 + 16 件龙铠 + wing_guard_legend |
| starter.dragonmaid.* | 60 | 开局伙伴 UI 全部文案 |
| config.dragonmaid.* | 34 | 4 个新配置组 + 类别/scope/文件标签 |
| enchantment.dragonmaid.* | 8 | 4 个附魔 + 4 个 .desc |
| tooltip.dragonmaid.wing_guard_legend* | 4 | 翼卫说明 3 段 |
| message.dragonmaid.recall.* | 7 | 号角恢复 7 种状态 |
| 其余杂项 | 3 | — |

12 条值改写中有 8 条是数值语义修正（innate armor 上限 40、饰品槽解锁规则、重伤交互说明、银/毒牙/龙息饰品作用范围从「仅人形」放宽到「人形 + 微型」）。

---
## 3. 资源增量

### 3.1 总量

| 范围 | 基线 | 目标 | 新增 | 修改 | 删除 |
| --- | --- | --- | --- | --- | --- |
| assets/ | 352 | 617 | **265** | 35 | 0 |
| data/ | 94 | 186 | **92** | 1 | 0 |
| 根 / META-INF | 6 | 6 | 0 | 4 | 0 |
| 合计 | 452 | 809 | **357** | 36 | 0 |

新增文件按扩展名：**199 个 .json + 158 个 .png**。全部为新增或修改，**没有任何资源被删除**（说明上游改名策略保守，也意味着移植时不必处理删除项）。

### 3.2 assets 新增目录树摘要（265）

| 目录 | 新增数 | 内容 |
| --- | --- | --- |
| assets/dragonmaid/textures/entity | 64 | 3 物种 x 2 性别 x 4 色系 x 成长阶段 4/5 的龙身贴图（部分带 .layers.png） |
| assets/dragonmaid/textures/armor | 48 | 4 套龙铠 x 各色系 |
| assets/dragonmaid/textures/item | 44 | 24 件刀/太刀 + 16 件龙铠图标 + 3 张刀鞘 + wing_guard_legend |
| assets/dragonmaid/models/item | 43 | 对应物品模型 + companion_jian_handheld / companion_katana_handheld |
| assets/dragonmaid/geo | 19 | 4 套 x male/female 龙铠 + 8 个 ornaments + 3 个刀鞘 |
| assets/dragonmaid/wing_membranes | 18 | **新目录**：翼膜网格数据 3 物种 x 2 性别 x 阶段 3-5 |
| assets/dragonmaid/animations | 11 | 6 个 shared（bridal_poses / wing_thrust / armed_tail_spin / flight_movement / hover_fall_land / waterfowl_blade_tail）+ 5 个 combat 分支 |
| assets/dragonmaid/patchouli_books | 8 | 4 个新条目 x en_us/zh_cn |
| assets/dragonmaid/armor | 4 | **新目录**：4 套 armor_contract.json |
| assets/dragonmaid/models/block | 2 | 召回台 base + gem |
| assets/dragonmaid/particles | 1 | blade_sweep.json |
| assets/dragonmaid/player_motions | 1 | **新目录**：player_first_person.motion.json |
| assets/dragonmaid/textures/block | 1 | recall_altar_emblems.png |
| assets/dragonmaid/textures/particle | 1 | blade_sweep.png |

三个**全新顶层资源目录**值得注意：wing_membranes/、armor/、player_motions/。它们是新渲染系统的数据契约，移植时必须连同读取代码一起搬。

### 3.3 data 新增（92）

| 目录 | 新增数 | 说明 |
| --- | --- | --- |
| data/dragonmaid/advancements | 42 | 25 个 combat 配方进度（刀/太刀）+ 18 个 misc（16 龙铠 + wing_guard_legend）+ 1 个 book/wing_guard_legend |
| data/dragonmaid/recipes | 41 | 同上 25 + 16 |
| data/dragonmaid/tags | 5 | items/{katanas,longswords,tridents,dragon_interaction_priority} + damage_type/longsword_physical_guard |
| data/dragonmaid/combat | 2 | **新目录**：armed_tail_geometry.json / waterfowl_tail_geometry.json（尾扫几何采样点） |
| data/iceandfire/tags/items | 1 | pixie_stolen_blacklist（配合 PixieStealMixin：强化号角不被妖精偷） |
| data/minecraft/tags/items | 1 | swords（把新刀剑并入原版剑 tag） |

data 修改仅 1 个：data/dragonmaid/patchouli_books/dragon_maid_guide/book.json（手册目录）。

### 3.4 修改的既有资源（36）

| 类别 | 数量 | 说明 |
| --- | --- | --- |
| patchouli 条目（en/zh） | 30 | 15 个条目 x 双语，内容跟随武器/饰品改动更新 |
| lang | 2 | en_us / zh_cn |
| 召回台模型 | 2 | models/block/dragon_recall_altar.json、models/item/dragon_recall_altar.json（适配方块实体） |
| 动画 | 1 | animations/shared/dragon_shared.sword_combo.animation.json（剑连击动画改写） |
| patchouli book.json | 1 | 目录 |

---

## 4. 依赖与元数据

### 4.1 META-INF/mods.toml

| 字段 | 1.2.0-beta.4 | 1.2.7 | 变化 |
| --- | --- | --- | --- |
| modLoader | javafml | javafml | 无 |
| loaderVersion | [47,) | [47,) | 无 |
| version | 1.2.0-beta.4 | **1.2.7** | 唯一实质变化 |
| license / authors / logoFile / description | 不变 | 不变 | 无 |
| 依赖 forge | [47,) | [47,) | 无 |
| 依赖 minecraft | [1.20.1] | [1.20.1] | 无 |
| 依赖 iceandfire | [2.1.13-1.20.1] | [2.1.13-1.20.1] | 无 |
| 依赖 citadel | [2.5.3,3) | [2.5.3,3) | 无 |
| 依赖 patchouli | [1.20.1-85-FORGE,) | [1.20.1-85-FORGE,) | 无 |
| 依赖 geckolib | [4.7.4,5) | [4.7.4,5) | 无 |
| 依赖 curios | 无 | 无 | 无 |

**结论：依赖声明一字未改。** 1.2.7 的所有新 API 用法都是 1.20.1 Forge 生态内的既有能力，没有引入新依赖，也没有升级 GeckoLib / IAF / Citadel / Patchouli。对移植的直接影响：**不存在「因为跨依赖版本而必须重写」的额外交付**，风险全部集中在 1.20.1-1.21.1 与 Forge-NeoForge 的 API 映射上。

### 4.2 Mixin 配置与 refmap

| 项 | 1.2.0-beta.4 | 1.2.7 | 变化 |
| --- | --- | --- | --- |
| dragonmaid.mixins.json | 48 server + 21 client = **69** | 57 server + 37 client = **94** | **+26 / -1** |
| compatibilityLevel | JAVA_17 | JAVA_17 | 无 |
| refmap | dragonmaid.refmap.json | dragonmaid.refmap.json | **内容变化**（必然，因新增 mixin） |
| defaultRequire | 1 | 1 | 无 |

**新增 26 个 mixin 及其目标类**（完整表见 analysis_127/mixin_changes.txt）：

| 目标类 | 新增 mixin |
| --- | --- |
| Player（原版） | PlayerHumanoidFlightMixin、PlayerLongswordSweepMixin、PlayerDragonPanelMixin |
| LivingEntity | RiderJumpAccessor |
| Camera | HumanoidRidingCameraMixin |
| CreateWorldScreen | StarterCreateWorldMixin |
| GridLayout / GridLayoutTab | StarterGridAccessor、StarterGridCellInvoker、StarterTabAccessor |
| EntityRenderer / LivingEntityRenderer | StarterPreviewNameMixin、StarterPreviewLivingRenderMixin、HumanoidRiderModelMixin、PlayerHumanoidRidingRendererMixin |
| EntityRenderDispatcher | HumanoidDragonCullingMixin |
| ClientLevel | ClientPredictionAccessor |
| Entity | KatanaMovementMixin |
| Explosion | LongswordExplosionMixin |
| ServerGamePacketListenerImpl | LongswordUseSequenceMixin |
| PlayerList | DragonMountRelogMixin |
| ModelDragonEgg | FireEggModelMixin |
| PixieAISteal（IAF） | PixieStealMixin |
| IafDragonFlightManager（IAF） | DragonFlightRestReturnMixin |
| ClientEvents（模组自身） | HumanoidMountClientMixin |
| RenderType | WingRenderTypeAccessor |
| Forge 配置界面 | ForgeConfigWorldLoadMixin |
| PlayerModel / PlayerRenderer | PlayerKatanaModelMixin |

注入类型统计：Inject 为主，另有 Redirect（7 处）、Shadow、ModifyVariable（2）、ModifyArg、Invoker、Accessor（4 个纯 Accessor 接口）。**没有 @Overwrite**，说明上游改造程度可控。

### 4.3 pack.mcmeta

两者完全相同：pack_format **15**（1.20.1 资源包格式），内容为 dragonmaid resources。**1.2.7 未升级 pack_format**，说明它仍然是纯 1.20.1 包。移植到 1.21.1 必须改为 34（资源包）与 48（数据包），且 data 目录需单数化。

---
## 5. 移植建议

### 5.1 三档分类

#### 档 1 · 可直接照搬（纯 1.20.1 通用 Java，只需改写命名空间引用）

判定标准：不依赖 Minecraft 版本 API 签名、不依赖 Forge 事件、不依赖渲染管线的纯逻辑/常量/数据结构。

| 类 | 理由 |
| --- | --- |
| combat/KatanaCharge | 纯常量 + 数学（progress / stage / radius / lightArmorMultiplier） |
| combat/BladeMotion（除 Frame 的 ResourceLocation 拼接） | 动作编号与 tick 时间轴表 |
| combat/AirTridentThrow | 常量 + 权重曲线 |
| combat/WaterfowlLeap（除 Vec3/AABB 调用） | 弹道解算纯数学（dragSum / MAX_LAUNCH_SPEED / MAX_JUMP_HEIGHT） |
| combat/KatanaSweep 的几何判定 | 弧形余弦判定 + 角点采样，只用到 Vec3/AABB/Mth |
| item/BladeEnchantments 的 4 张概率表与 parryBonusTicks 公式 | 数值可直接复用 |
| config 的默认值与取值范围 | 见 2.8 表格 |
| starter/StarterSettings 的字段语义、stage(days)、slots()、normalize() | 与版本无关的模型逻辑 |
| starter/StarterConfig.Limits | 纯 record |
| data/HumanoidHealth 的属性 UUID 与修正思路 | 属性系统在 1.21.1 仍可用（API 名变） |
| network/*Message 的 record 定义与字段 | 编解码需换 API，但字段契约可直接沿用 |
| combat/HumanoidMeleeTiming 的时间轴常量（AXE_* / TRIDENT_*） | 纯数值 |
| combat/RestReturnGoal.Spot 的 save/load 结构 | CompoundTag 结构可沿用 |

#### 档 2 · 需 API 映射（Forge 到 NeoForge / 1.20.1 到 1.21.1，逻辑不变）

| 类/子系统 | 需要的映射 |
| --- | --- |
| **26 个新 mixin + 18 个改动的 mixin** | 目标方法名/描述符全部要按 1.21.1 重找；refmap 重新生成；net.minecraftforge 包名改 net.neoforged；ObfuscationReflectionHelper 换包 |
| item/ModItems（+61 行） | DeferredRegister 保持但 ForgeRegistries 改 NeoForgeRegistries/BuiltInRegistries；CreativeModeTab.Output 的 m_246326_ 换成 accept |
| entity/ModEntities | 同上；EntityType.Builder 链式方法改名 |
| particle/ModParticles | 同上 |
| item/CompanionBladeItem | **1.21.1 的 SwordItem 构造与属性系统已改**：攻击力/攻速从构造参数移到 DataComponents.ATTRIBUTE_MODIFIERS；需重写但不是重设计 |
| item/ServiceDragonArmorItem（Style 枚举） | 继承 IAF 的 ItemDragonArmor，跟随 IAF 2.2 映射 |
| combat/DragonTridentFlight（416 行） | extends ThrownTrident：Entity/投射物 API、SynchedEntityData、EntityDataSerializers 名、ThrownTrident 内部字段访问方式 |
| combat/SpecialWeapons | ForgeMod.ENTITY_REACH 改 Attributes.ENTITY_INTERACTION_RANGE；ForgeRegistries.ITEMS 改 BuiltInRegistries.ITEM；ItemAttributeModifierEvent 签名变化；ItemTags 的 forge:ingots/silver 改 c:ingots/silver |
| combat/BladeCombat、LongswordParry、LongswordProjectiles、DragonShield | LivingHurtEvent / LivingAttackEvent / ProjectileImpactEvent / Explosion 事件与 DamageTypeTags 常量重映射 |
| **item/BladeEnchantments（4 个附魔）** | **1.21.1 附魔改为数据驱动**：EnchantmentCategory 已移除，Enchantment 构造与注册方式全变。逻辑可保留，但注册必须重写为 datapack JSON + Holder 读取（详见 5.3 风险 1） |
| network/DragonNetwork + 3 个新包 | SimpleChannel 在 1.21.1 必须换成 RegisterPayloadHandlersEvent + CustomPacketPayload；VERSION 语义保留 |
| config/*（DragonMaidConfig / CompanionBalanceConfig / RecallConfig / GlobalBalanceConfig） | ForgeConfigSpec.Builder 改 ModConfigSpec.Builder；ModLoadingContext.get().registerConfig 改 ModContainer#registerConfig（构造期）；.translation() 与 .worldRestart() 在 NeoForge 21.1 是否可用需现场编译核实 |
| recall/RecallContent | ForgeRegistries.BLOCK_ENTITY_TYPES 改 NeoForgeRegistries.BLOCK_ENTITY_TYPES；ChunkEvent.Load 仍在但包名变 |
| 所有 IAF 交互（13 个类） | EntityDragonBase / EntityMutlipartPart / IafItemRegistry / IafDamageRegistry / PixieAISteal / IafDragonFlightManager / TileEntityDragonforgeInput 逐个对照 docs/IAF-CE-MAPPING.md 落 |
| data/ 资源目录 | advancements 改 advancement、recipes 改 recipe、tags/items 改 tags/item；pack_format 15 改 34/48 |
| 全库 ResourceLocation 构造 | 1.21.1 构造器私有，必须用 fromNamespaceAndPath / parse（移植仓应已解决，新增类需遵守） |

#### 档 3 · 需重新实现（客户端渲染 / GeckoLib / 1.21.1 结构大改）

| 子系统 | 为什么必须重写 | 工作量 |
| --- | --- | --- |
| **连续翼膜渲染**（ContinuousWingMesh 573 + RenderType 64 + Resources 67 + DragonVertexBatch 180 + 18 个 wing_membranes JSON） | 1.21 的 VertexFormat.Mode / RenderType.create 签名 / BufferBuilder 全部变化；还内嵌 Embeddium 与 Oculus 两条第三方分支，属于上游已知的脆弱点 | 大 |
| **龙铠装饰网格**（DragonArmorOrnaments 238） | 从 .geo.json 手工烤 Mesh 并直接提交顶点；依赖 GeoBone/GeoCube/GeoVertex 的 GeckoLib 内部结构与 RenderType | 大 |
| **开局伙伴 UI**（StarterCompanionScreen 590 + StarterPreviewLevel 188 + StarterCreateWorldMixin 181 + 4 个 Grid/Tab accessor + StarterPreviewLivingRenderMixin 71） | **1.20.5+ 重写了 CreateWorldScreen**（tab 化布局），GridLayout/GridLayoutTab 的内部结构、tab 添加方式与 1.20.1 完全不同；StarterPreviewLevel 要实现的 Level 抽象方法集也变了 | **最大** |
| **玩家骑乘坐姿**（HumanoidRidingPose 232 + HumanoidRiderModelMixin 53 + PlayerHumanoidRidingRendererMixin 24 + HumanoidRidingCameraMixin 32 + 2 个新动画） | 1.21.1 的 EntityRenderer 构造改为接收 Context，PlayerRenderer 的 layer 注册 API 变化，PlayerModel 部件操作需重对 | 中-大 |
| **第一人称刀动画**（PlayerBladeMotion 212 + PlayerBladeAnimation 180 + KatanaSheathLayer 143 + PlayerKatanaPose 87 + player_first_person.motion.json） | 第一人称手部渲染在 1.21.1 走了新的 ItemInHandRenderer 路径；骨名 RightGrip/Scabbard/RightForearm/LeftForearm 需要按 1.21.1 玩家模型核对 | 中-大 |
| **龙身模型/渲染大改**（IceFemaleModel +519、IceFemaleRenderer +259、IceFemaleEquipmentLayer +164） | GeckoLib 4.7.4 到 4.9.2 的 model/renderer 契约差异 + 1.21.1 渲染管线 | 中-大 |
| **刀光/格挡 UI**（BladeSweepParticle 159 + LongswordGuardRender 184 + LongswordParryOverlay 144 + KatanaChargeSparks 64） | 粒子构造（TextureSheetParticle 签名）、GUI/HUD 叠加层、PoseStack 用法 | 中 |
| **飞行与剔除相关客户端**（HumanoidFallTimeline 120 + HumanoidWallOcclusion 122 + HumanoidDragonCullingMixin 61 + ClientPredictionAccessor） | EntityRenderDispatcher.shouldRender 签名、Camera setup、ClientLevel 内部 | 中 |
### 5.2 建议移植顺序（按「解锁内容量 / 依赖关系 / 风险」排）

| 阶段 | 内容 | 可验收点 | 依赖 |
| --- | --- | --- | --- |
| **P0** | 资源落地：357 个新文件（改目录单数 + pack_format）、160 x 2 语言 key、8 个手册条目、41 配方、42 进度、5 标签 | 游戏能启动；物品在创造栏可见（贴图/模型/名称正确）；手册条目可打开 | 无 |
| **P1** | 注册与物品基类：ModItems（32 武器 + 16 龙铠 + 1 饰品）、ModEntities（dragon_trident_flight）、ModParticles（blade_sweep）、ModRecipes（dragon_blood_blade）、SpecialWeapons 的 tag 判定、CompanionBladeItem/DragonBloodBladeItem（属性走 DataComponents） | 武器可合成、可持握、伤害/攻速符合 2.1 表；三个新 tag 生效 | P0 |
| **P2** | 附魔重写（BladeEnchantments 改 1.21.1 数据驱动，4 个附魔 + 说明书 tooltip） | 附魔台可获得、等级 1-3 生效、概率表可测 | P1 |
| **P3** | 战斗核心：BladeCombat（530）、LongswordParry（121）、LongswordProjectiles（380）、KatanaCharge/KatanaSweep、DragonShield（320）、HumanoidMeleeTiming（181）、BladeMotion/AirTridentThrow、DragonTridentFlight（416）+ 3 个战斗相关 mixin | 太刀蓄力横扫、长剑格挡/弹反/弹开投射物、空中三叉戟投掷、盾牌远程压制窗口全部可复现 | P1、P2 |
| **P4** | 战斗 AI：HumanoidGoal（1275）、HumanoidArmedSweep（494）、HumanoidWingThrust（165）、DragonKatanaOpener、RestReturnGoal、ArmedTailGeometry + 2 个 combat JSON | 龙会自主用新武器、翼助推、武禽式跳跃、尾扫几何命中 | P3 |
| **P5** | 飞行与载人：HumanoidFlight（1093）、HumanoidGroundMount（111）、EscortFlightOrbit、HumanoidFlightInputMessage、PlayerHumanoidFlightMixin、DragonMountRelogMixin、RiderJumpAccessor | 人形龙可自主起飞/跟随/降落；玩家可骑乘（地面 + 飞行）；重登不丢座 | P1 |
| **P6** | 召回台与号角事务：RecallAltarBlockEntity、RecallAltarRenderer、RecallHornRecovery（235）、RecallContent 改造、RecallHorn matchesReceipt 重构 | 老存档召回台能挂上 BE；号角跨维度转移在断线/崩服后可恢复 | P1 |
| **P7** | 龙铠四件套：ServiceDragonArmorItem.Style、ServiceArmorResources 按 Style 分裂、护甲合并上限 40、4 套 armor_contract + geo 接入 | 4 套龙铠可制作/装备/显示；护甲总值不超 40 | P1、P5 |
| **P8** | 开局伙伴选择（Starter* 5 类 + 590 行 Screen + CreateWorldScreen 改造 + 5 个 mixin + 预览 Level） | 新建世界时可选择初始伙伴；进世界后按设置发放龙或蛋 | P1、P5；**风险最高，建议最后做或单独排期** |
| **P9** | 客户端表现层：连续翼膜、龙铠装饰网格、骑乘坐姿、第一人称刀动画、刀光/格挡 UI、龙身模型大改 | 视觉与上游对齐（可用截图对比验收） | P3-P8 |

排序理由：P0-P2 是「数据 + 注册 + 附魔」，几乎不依赖客户端，且解锁物品可见性，便于逐项验收；P3-P4 是本次内容主体（106 个类），机制自洽；P5 独立但被 P8 依赖；P6 与 P7 互不依赖，可并行；P8/P9 风险最高放后面，避免阻塞主体交付。

### 5.3 风险点（按严重度）

**风险 1 · 附魔系统（阻断级，必须重写）**
1.21.1 把 Enchantment 改为数据驱动：EnchantmentCategory 已删除、Enchantment 需通过 Registry + Holder 使用、附魔定义本体是 datapack JSON。上游 BladeEnchantments 用 EnchantmentCategory.create 自定义了两个分类（dragonmaid_katana / dragonmaid_longsword）并直接 new Enchantment。移植需要在 1.21.1 用「附魔 JSON + 自定义 EnchantmentEffectComponents / value effects」重建同一套效果，代码里通过 Holder<Enchantment> 读取。**建议单独做一个探针工程先验证 4 个附魔的 JSON schema，再写正式代码。**

**风险 2 · 网络协议（阻断级）**
VERSION 从 21 跳到 23，且 1.21.1 强制 CustomPacketPayload。三件新包 + DragonHandsMessage 加字段意味着：客户端/服务端若只更新一侧会直接握手失败。同时 NeoForge 21.1 的 payload 注册发生在 mod 构造期的 RegisterPayloadHandlersEvent，而上游是在 DragonNetwork.register() 静态注册，调用时机要一起迁。

**风险 3 · CreateWorldScreen 改造（工作量最大）**
1.20.5 起 CreateWorldScreen 重写为 tab 布局，上游的 StarterGridAccessor / StarterTabAccessor / StarterGridCellInvoker 三个 accessor 与 StarterCreateWorldMixin（181 行）都是针对 1.20.1 的内部结构写的，**不能翻译式移植**，只能按 1.21.1 的 tab API 重做。加上 StarterCompanionScreen（590 行）与 StarterPreviewLevel（要实现约 40 个 Level 抽象方法，1.21.1 方法集已变），这块建议单独立项、按「新功能」而非「移植」估工。

**风险 4 · RenderType / VertexFormat 体系（重写）**
连续翼膜系统（573+180+64+67 行）依赖 1.20.1 的 RenderType.create 与手工 BufferBuilder 提交，还带 Embeddium/Oculus 两条 shader 兼容分支。1.21 把 VertexFormat 重构为 VertexFormat.Mode、BufferBuilder 语义变化，这段基本等于重写；且第三方兼容分支在 NeoForge 1.21.1 上的对应模组接口也不同，需要重新判断是否保留。

**风险 5 · 数据包目录单复数（高，但机械）**
1.2.7 的 92 个新 data 文件全部用 1.20.1 的复数目录（advancements / recipes / tags/items）。1.21.1 必须是 advancement / recipe / tags/item。**这类错误不会报错，只会静默不加载**，极易在验收时被误判成「代码没生效」。建议 P0 就用脚本统一改名并写一条自检（启动日志里 recipe/advancement 数量对得上）。

**风险 6 · IAF 类映射（中-高，但已有路径）**
13 个类直接触碰 IAF：EntityDragonBase、EntityMutlipartPart、IafItemRegistry（COPPER/DRAGONBONE/FIRE/ICE/LIGHTNING_TOOL_MATERIAL 与 DRAGONBONE_SWORD_*）、IafDamageRegistry（DRAGON_FIRE/ICE/LIGHTNING_TYPE）、PixieAISteal、IafDragonFlightManager、TileEntityDragonforgeInput。移植仓已有 docs/IAF-CE-MAPPING.md（33KB）与 com/github/alexthe666/iceandfire 桩，逐个对照即可，但要注意 IAF 2.2（NeoForge）与 2.1.13（Forge）的类**可能不同名**，尤其 TileEntityDragonforgeInput 与 PixieAISteal。

**风险 7 · GeckoLib 4.7.4 到 4.9.2 契约差异（中）**
上游依赖声明仍是 geckolib [4.7.4,5)，移植仓是 4.9.2。GeoBone / GeoCube / AdvancedModelBox / TabulaModel / BakedGeoModel / GeoVertex 在 4.9 仍存在，但模型加载与 RenderType 获取方式有调整；DragonArmorOrnaments 与 ContinuousWingMesh 直接用到了这些内部结构，是最容易在编译期通过、运行期崩的地方。建议先用一个最小 Geo 模型验证骨骼遍历 API。

**风险 8 · 物品属性系统（中，机械）**
1.21.1 的 SwordItem/ArmorItem 不再从构造参数取攻击力/攻速/护甲值，改走 DataComponents.ATTRIBUTE_MODIFIERS。24 件刀剑的 12 组不同 (damage, speed) 与 16 件龙铠的护甲值需要全部改写成 component 或 items/*.json 的 attribute_modifiers 字段。银质武器的 undeadBonus（+2）也要改成 attribute 或事件。

**风险 9 · 配置双层镜像机制（中）**
GlobalBalanceConfig 用一个遍历式的 copyDefinitions 把既有 SPEC 镜像成 global + world 两套，并注册两个额外的 SERVER 配置（dragonmaid-world-server.toml / dragonmaid-recall-world-server.toml）。这段代码强依赖 ForgeConfigSpec 的 UnmodifiableConfig / ValueSpec / ConfigValue 内部结构。NeoForge 的 ModConfigSpec **有同名类但不保证同签名**，这块可能要从「镜像」改成「显式复制字段」，工作量比看起来大。

**风险 10 · 低风险但容易漏的点**

- pack_format 15 改 34（资源）/ 48（数据），否则整包资源不加载；
- forge:ingots/silver 在 1.21.1 应改为 c:ingots/silver（NeoForge 约定）；
- MobType.UNDEAD 在 1.21.1 用 EntityTypeTags.UNDEAD 判亡灵（CompanionBladeItem.undeadBonus）；
- 上游给所有新配置项加了 worldRestart()，验收时要按「改配置、重进世界」测；
- 老的 Jade mixin（JadeDragonArmorMixin）**已删除**，移植仓若已有别继续保留，同时确认其职责是否被 ServiceDragonArmorMixin + DragonArmorOrnaments 完整替代；
- data/iceandfire/tags/items/pixie_stolen_blacklist.json 是给 IAF 用的跨模组 tag，路径在 1.21.1 要写成 tags/item；
- 召回台新增了 ChunkEvent.Load 的 attachExistingAltars 补救逻辑，移植时如果漏掉，老存档里的召回台会没有方块实体（表现为无悬浮宝石）。

---

## 6. 附录：机器清单与复现方法

### 6.1 产物位置

    D:\mc-model\work\_port_src\analysis_127\
      classes_new.txt          142 个新增类（含 package 路径）
      classes_changed.txt      142 个修改类
      classes_removed.txt      1 个删除类
      classes_same.txt         301 个未变化类
      new_by_package.txt       新增类按 package 分组
      classes_by_group.txt     新增/修改类按功能分组（A-K）
      assets_new.txt           assets 下新增/修改/删除文件
      assets_all_new.txt       assets+data 全部新增文件（357）
      data_new.txt             data 下新增/修改/删除文件
      lang_keys_new.txt        en_us + zh_cn 新增 key 合并
      lang_keys_new_en_us.txt  en_us 新增/改写/删除 key 明细
      lang_keys_new_zh_cn.txt  zh_cn 同上
      mixin_changes.txt        46 行 mixin 目标类与注入类型（26 新增）
      registry_changes.txt     注册项变化摘要（物品/附魔/实体/粒子/配方/BE/网络）
      summary.json             全部统计数字的机器可读版本
      diff_raw.json            原始 diff（类/资源 hash 对比结果）
      sig_diff.json            每个类的 diff 体量与新增声明样本
      decompile_120.log / decompile_127.log   Vineflower 日志

### 6.2 复现命令

反编译（每个约 1-2 分钟）：

    java -Xmx3G -jar D:\mc-model\work\_tools\decompiler\vineflower.jar -dgs=1 -rsy=1 ^
      D:\mc-model\work\_port_src\dragonmaid-1.2.7.jar D:\mc-model\work\_port_src\decompiled_127

重新生成全部清单（脚本都在 analysis_127\ 下）：

    set PYTHONUTF8=1
    python _diff.py     # 类/资源 hash 对比
    python _meta.py     # mods.toml / mixins / lang
    python _sig.py      # 声明级 diff
    python _final.py    # 汇总清单 + summary.json
    python _grp.py      # 功能分组

### 6.3 方法学声明（哪些是确定的，哪些是推断的）

| 结论 | 确定性 |
| --- | --- |
| 新增/修改/删除类的数量与名单 | **确定**（.class 文件 sha256 字节比对，不经反编译） |
| 资源新增/修改/删除数量 | **确定**（同上） |
| mods.toml 依赖未变 | **确定**（文件逐行对比） |
| mixin 条目数 69 增至 94、新增 26、删除 1 | **确定**（mixins.json 逐条对比） |
| 新增类/改动类的**功能归属**与**改动摘要** | **较可靠**（读反编译源码的方法签名、常量、注释字符串），但反编译为 SRG 名（m_XXXXXX_），个别私有方法语义靠上下文推断 |
| 「需 API 映射」与「需重新实现」的边界 | **判断**（基于 1.21.1 的已知 API 变化），实际工作量需在移植中校准 |
| 具体行数（如 HumanoidFlight 1093 行） | **确定**（反编译源码行数，非原始源码行数，仅作复杂度相对度量） |

未覆盖：未做 1.2.0-beta.4 之前的历史溯源（如某机制是哪个版本引入的）；未对每个类做逐行 diff（只到方法/常量级）；未验证任何代码在 1.21.1 下的实际编译结果。
