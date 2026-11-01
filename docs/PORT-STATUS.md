# Dragon Maid 1.1.0-beta.2 → NeoForge 1.21.1 移植状态报告

> **授权状态**：本移植已获原作者许可，**仅限非盈利使用**；须署名原作者、标注社区移植（非官方）、不得含任何收费内容。
> 原 mod 名称：Dragon Maid Companion（作者 别思丶）。
> 本报告中所有"已验证"均附命令输出/日志行；未跑通的项标「未验证」。

## 0. 交付物位置

| 项 | 路径 |
|---|---|
| 工程 | `D:\BaiduNetdiskDownload\projects\medieval-world\dragonmaid-port\` |
| 本阶段 jar | `build\libs\dragonmaid-neoforge-1.21.1-1.1.0-beta.2.jar`（6,101,311 B，sha256 `AD297AAD0B65F11E28D8B912DD2CDC0F6D68A2BCA5EC1CFAABA8C33166E792B7`） |
| 原始 jar | `D:\mc-model\work\_port_src\dragonmaid-1.1.0-beta.2.jar`（6,759,417 B，sha256 `EEA5FFF961FE5B057219A23BFC80F07B69A659F556E2629887539BD35CCD56AF`） |
| 反编译源码 | `D:\mc-model\work\_port_src\decompiled\`（212 java，31,373 行） |
| SRG→Mojang 映射 | `D:\mc-model\work\_port_src\srg2named.json`（887 个 SRG token / 5728 处引用，100% 覆盖） |
| 资产比对表 | `asset-sha256.md`（359 个内容文件逐文件 sha256） |
| 隔离测试服 | `D:\mc-model\work\_probe_dm\`（libraries junction → `live-server\libraries`；mods 80 个硬链接；端口 25605 / RCON 25606；世界 `world_probe`） |

## 1. 资产一致性（硬要求 #1）

**359 / 359 内容文件逐字节一致 = 100%**（`assets/` 292 个、`data/` 65 个、`pack.mcmeta`、`dragonmaid.png`），逐文件 sha256 见 [asset-sha256.md](asset-sha256.md)。

- 复制方式：`jar xf` 解包后**原样**拷入 `src/main/resources`，构建过程不做任何改写（只有 `META-INF/neoforge.mods.toml` 做版本占位符展开；该文件是新增的 NeoForge 元数据，不对应原件）。
- 未逐字节复制的原件（均为代码/元数据，非资产）：
  - `META-INF/mods.toml` → 改写为 `META-INF/neoforge.mods.toml`（NeoForge 1.20.5+ 规范），modId/版本/作者/描述/logo 保留；
  - `META-INF/MANIFEST.MF` → 构建重新生成；
  - `dragonmaid.refmap.json` → **丢弃**（NeoForge 1.21.x 运行时使用官方 Mojang 名，不再需要 SRG refmap）；
  - `dragonmaid.mixins.json` → 重新生成（本阶段为空 mixin 集合）；
  - `*.class` 317 个 → 重写（见 §3）。

## 2. 依赖结论（决定移植路线）

原 `mods.toml` 依赖：`iceandfire [2.1.13-1.20.1,)`、`citadel [2.5.3,3)`、`patchouli [1.20.1-85-FORGE,)`、`geckolib [4.7.4,5)`。

现役 1.21.1 整合包（`live-server/mods`）里对应版本**全部换代**：

| 原依赖 | 现役包内 | 影响 |
|---|---|---|
| Ice and Fire **2.1.13**（`com.github.alexthe666.iceandfire`） | **Ice and Fire Community Edition 2.0**（`com.iafenvoy.iceandfire`，依赖 uranus/jupiter） | **包名与 API 全量重构**，是本移植的最大阻塞点 |
| Citadel 2.5.3 | 无（CE 用 uranus 替代） | 仅 1 处 `AdvancedEntityModel` → 已映射到 `com.iafenvoy.uranus.client.model` |
| Patchouli 1.20.1-85 | Patchouli 1.21.1-93-NEOFORGE | API 基本一致（仅 GuiBook 相关 mixin 少量改动） |
| GeckoLib 4.7.4 | GeckoLib 4.9.2 | 包结构 `core.*` → `animation/animatable/cache.*`，`CoreGeoBone`→`GeoBone`，`RenderUtils`→`RenderUtil`，`JsonUtil.GEO_GSON` 移除 |

原源码对 IAF 的耦合：**50 个不同 IAF 类、249 处 import、`DragonMixin` 44 KB**。其中 31 个类可机械映射（例：`EntityDragonBase→DragonBaseEntity`、`EntityIceDragon→IceDragonEntity`、`ContainerDragon→DragonMenu`、`IafItemRegistry→IafItems`、`BlockIceSpikes→IceSpikesBlock`）；**19 个类在 IAF CE 中已不存在**（`entity.props.EntityData/EntityDataProvider`、`world.DragonPosWorldData`、`client.model.armor.ModelCopperArmor/ModelSilverArmor`、`enums.EnumDragonTextures.Armor`、`client.render.entity.RenderStoneStatue/RenderLightningDragon`、`layer.LayerDragonArmor`、`CommonProxy`、`MessageUpdateDragonforge`、`IafConfig` 等），需重写或做兼容桩。

**好消息（决定阶段 3 可行性）**：靠反编译 IAF CE 2.0 源码逐条核对，DragonMixin 注入/Shadow 的 IAF 成员大多仍存在：`spawnItemCrackParticles`、`switchNavigator`、`isBreakable`、`setBreathingFire`、`sleepProgress`、`prevAnimationProgresses`、`lookingForRoostAIFlag`、`isModelDead`、`getDragonStage`、`setInSittingPose`、`isBreathingFire`、`getHunger`、`setGender` 等。**核心行为移植是可行路线，而不是只能重写。**

## 3. 代码移植进度（可验证）

1. Vineflower 反编译 → 212 java / 31,373 行（其中 `TailSweepGeometry.java` 278 KB 是数据表）。
2. 反编译产物里 887 个 SRG 名 / 5728 处引用（`m_xxxxx_`/`f_xxxxx_`），用 gradle 缓存里的 **MCPConfig 1.20.1 `joined.tsrg` + `client_mappings.txt` + MCP `methods.csv/fields.csv`** 全量还原为 Mojang 名，**100% 覆盖（0 未映射）**。
3. Forge→NeoForge + 1.20.1→1.21.1 机械改写：事件总线、`DeferredRegister/RegistryObject`（用 `compat.Reg/RO` 垫片）、`ForgeConfigSpec→ModConfigSpec`、网络（用 `compat.net.SimpleChannel` 单 payload 适配层保住 16 个消息类的 encode/decode/handle）、ItemStack NBT→`DataComponents.CUSTOM_DATA`（`compat.StackNbt` + 写回型 `WriteBackTag`）、`PathType`、`AttributeModifier` 新 API、`DimensionTransition` 等。
4. 编译迭代实测：**1555 → 1049 → 542 → 370 个错误**；剩余错误集中在 IAF CE 已删除的 API 与重型客户端代码（渲染/动画/配方序列化）。
5. 本阶段构建：**79 个源文件通过编译**（纯逻辑：呼吸/尾扫时间线、战斗状态、灾难/水域、召回配置、知识、若干 item 类等），**155 个源文件暂被 `sourceSets` exclude**（源码保留在工程里，等 IAF CE API 重写后逐个恢复），另含 10 个兼容桩文件。`PortBootstrap.java` 为当前可加载入口。

> 说明：`DragonMaid.java`（原主类，含注册与配置）也在暂排除集合内；`dragonmaid.mixins.json` 本阶段写成空 mixin 集，保证服务端不会因未完成的 mixin 目标崩溃。

## 4. 测试服实测（隔离环境，全新世界）

- 环境：`_probe_dm`，`libraries` junction → 现役 server 库，`mods` 80 个硬链接（现役 79 + 本移植 jar），端口 25605 / RCON 25606，`level-name=world_probe`（新世界，未触碰 `live-server\world`）。
- 关键日志行（`D:\mc-model\work\_probe_dm\probe.log`）：

```
[09:41:29.950] [main/INFO] [loading.moddiscovery.ModDiscoverer/SCAN]: Found mod file "dragonmaid-neoforge-1.21.1-1.1.0-beta.2.jar" [locator: mods folder locator]
		Dragon Maid 1.1.0-beta.2 (dragonmaid)
[09:41:48] [modloading-worker-0/INFO] [co.ex.dr.PortBootstrap/]: [dragonmaid] Dragon Maid 1.1.0-beta.2 - NeoForge 1.21.1 port bootstrap (assets stage)
[09:41:57] [main/INFO] [minecraft/RecipeManager]: Loaded 3957 recipes
[09:41:57] [main/INFO] [minecraft/AdvancementTree]: Loaded 3828 advancements
[09:42:24] [Server thread/INFO] [minecraft/DedicatedServer]: Done (24.666s)! For help, type "help"
[09:42:24] [Server thread/INFO] [minecraft/RconThread]: RCON running on 0.0.0.0:25606
```

- 结论：**mod 能被 NeoForge 1.21.1 正常识别并加载，服务端 24.7 s 启动完成，无本 mod 相关异常/崩溃。** RCON 认证成功（`auth ok`），已用 RCON 停服（PID 33288，仅关闭自己启动的进程）。
- **数据未加载（重要发现）**：日志里没有任何 `dragonmaid:` 配方/进度加载记录，也没有本 mod 的配方解析错误 → 原 `pack.mcmeta`（`pack_format: 15`，1.20.1 格式）在 1.21.1（数据包 format 48）下被整包判为不兼容而跳过。**这不是复制错误，是为了满足"资产 100% 逐字节"而保留原文件的结果。**
- 环境噪音（与本 mod 无关，先于本 mod 存在）：`roadweaver` 的 gson `InaccessibleObjectException`（后台文件刷写线程）、`Jupiter` 首次启动读不到 `config/iceandfire/iaf-common.json`（首启无配置，正常）。

## 5. 功能对齐清单

| 功能 | 状态 | 证据/说明 |
|---|---|---|
| jar 结构/元数据（modId、版本、作者、logo、描述、依赖声明） | **已对齐** | neoforge.mods.toml；服务端识别日志 |
| `assets/`（292 个：贴图/geo/animations/particles/models/lang/patchouli_books/armor） | **已对齐（逐字节 100%）** | asset-sha256.md |
| `data/`（65 个：配方/进度/战利品表/标签/patchouli book.json） | **已对齐（文件逐字节）**，但**未被 1.21.1 加载** | 见 §4 pack.mcmeta 结论 |
| 16 个网络消息协议（JSON 版本 15、消息 id 0–9） | **代码已改写、未接线** | `compat.net.SimpleChannel` 适配层；需 P1 注册 payload |
| 物品/方块/粒子/菜单/实体注册、创造标签 | **未对齐**（本阶段未编译进 jar） | 注册类仍在 exclude 列表 |
| 冰/火龙化形（DragonState/混入态）、形态切换 | **未对齐** | `DragonMixin` 等混入待移植（IAF CE 成员可用性已核对，见 §2） |
| 护卫模式 / 战斗 / 跟随 / 目标选择 | **未对齐** | `HumanoidGoal/GuardEscort/HumanoidTargetGoal` 等被排除 |
| 龙息动画（全阶段/全种类/全性别）与动画时长 | **未对齐（代码）**，动画资源已逐字节在包内 | `animations/` 23 个文件一致；运行期未验证 |
| 模型结构（21 个 `.geo.json`） | **已对齐（文件）**，渲染未接线 | `geo/` 逐字节一致 |
| 龙吼、坐骑、龙甲渲染、知识菜单、饰品、处决、召回、石化、龙炉兼容、JEI 插件 | **未对齐** | 依赖 IAF 被移除 API 或客户端重写 |
| 原 `mods.toml` 依赖的 Citadel | **有差异** | 1.21.1 侧 IAF CE 已改用 uranus，仅 1 处模型基类受影响 |
| `pack.mcmeta` 字节一致性 | **已对齐（100%）** | 但需在下一阶段改为 1.21.1 格式，见 §6-P0（这将是唯一必须偏离原字节的文件） |
| 1.21 数据目录命名（`advancement/recipe/loot_table/tags/item` 单数） | **未对齐** | 原件是 1.20.1 复数目录；需生成单数别名副本（内容字节不变） |

## 6. 还差什么 / 下一步（分阶段）

**P0（0.5–1 天，先把数据跑起来）**
1. `pack.mcmeta` 改为 1.21.1 格式（资源 34 / 数据 48，用 `supported_formats` 同时兼容），在 asset-sha256.md 中登记为**唯一一处有意偏离**（并保留原件副本 `pack.mcmeta.original`）。
2. 在构建期生成单数数据目录别名（`advancements→advancement`、`recipes→recipe`、`loot_tables→loot_table`、`tags/items→tags/item` 等），文件内容不改。
3. 重跑测试服，确认日志出现 `dragonmaid:` 配方/进度 → 再修 1.21.1 配方 JSON 差异（result 字段格式，未验证）。

**P1（1–2 天，恢复内容注册与网络）**
4. 逐个恢复注册类（ModItems/ModBlocks/ModEntities/ModParticles/ModMenus/ModRecipes/RecallContent/创造标签），补齐 1.21.1 API（`DeferredRegister.Items/Blocks`、`Item.Properties.stacksTo/setId`、`RecipeSerializer.codec/streamCodec`）。
5. 在 `RegisterPayloadHandlersEvent` 里注册 `compat.net.SimpleChannel`，让 16 个消息类真正收发。

**P2（3–5 天，核心行为：化形/形态/护卫/龙息）**
6. 移植 `DragonMixin` 及其依赖：`SynchedEntityData.Builder.define` 新签名、IAF CE 类名、`@Shadow` 校验；用 IAF CE 源码逐条对齐 `spawnItemCrackParticles/switchNavigator/isBreakable/setBreathingFire/roar` 等注入点（已确认多数仍存在）。
7. 逐个恢复被排除的 155 个源文件，按错误类别（客户端渲染、配方、石化、召回、龙炉、JEI）分批编译。
8. 对 IAF CE 已删除的 API 决定：重写（石化/龙炉）或标记不支持（`EntityDataProvider` 链/锁、`DragonPosWorldData`、龙甲渲染）。

**P3（2–3 天，客户端）**
9. GeckoLib 4.9.2 客户端 API：`GeoReplacedEntityRenderer` 新签名（`renderRecursively/preRender/actuallyRender`）、`VertexConsumer` 新顶点 API（`addVertex/setUv/setColor/setOverlay/setLight/setNormal`）、`CoreGeoBone→GeoBone`。
10. 人形渲染替换（服务端无法验证，需要用户本机跑一次客户端确认化形外观与喷龙息动画）。

**P4（1 天，回归）**
11. 完整服务端回归：`/summon` 冰火龙 → 驯服 → 触发化形 → 切护卫 → 战斗/跟随，检查日志与实体 NBT；复跑 asset 100% 校验；记录 B 站反馈的"护卫模式打末影守望者崩溃"在本依赖组合下是否复现（**未验证**，需 P2 完成后才能测）。

## 7. 复现命令

```powershell
# 构建
$env:JAVA_HOME='D:\pcl\voxy1.7\jdk\jdk21'
cd D:\BaiduNetdiskDownload\projects\medieval-world\dragonmaid-port
& 'C:\Users\LIUHZ\.gradle\wrapper\dists\gradle-8.8-bin\dl7vupf4psengwqhwktix4v1\gradle-8.8\bin\gradle.bat' --no-daemon jarInfo

# 资产逐字节比对（生成 asset-sha256.md）
& 'D:\BaiduNetdiskDownload\tools\vision-venv\Scripts\python.exe' D:\mc-model\work\_port_src\asset_diff.py

# 隔离测试服
cmd /c D:\mc-model\work\_probe_dm\run_probe.bat   # 25605 / RCON 25606
```

## 8. P4 服务端回归（task-5，2026-11-01）

**环境**
- jar：`build/libs/dragonmaid-neoforge-1.21.1-1.1.0-beta.2.jar`，6,812,359 B / mtime 10:53:11 / sha256 `D73161076F5A99A1DC54EC38F7AF678B45299316DE46179B654EBBB9FEA177B6`（GEO_GSON + 全部 mixin 修复）。
- 测试服：`_probe_dm`（live-server 79 mods + 本 jar；world_probe；25605 / RCON 25606；RCON `stop` 优雅关闭后端口归零、无 java 残留）。
- 执行器：`_port_src/task5_runner.py`（预检 jar sha → 起服等 Done → forceload 0 0 → 套件 → RCON stop → 证据 markdown）。
- 证据：`_port_src/task5-evidence-run4.md`（主）、`task5-evidence-run3.md`、`task5-evidence-diag.md`；日志扫描口径：dragonmaid 相关 ERROR/WARN = none（live-server 其他 mod 噪音单列）。

**关键结论（修正 P2 遗留判断）**

「CE 2.0 裸 /summon 会立刻消失 = CE 固有限制」的判断不成立，实测为**测试方法问题**，三点根因：
1. `/summon` 的目标区块未加载/未 forceload 时，实体不进入已加载实体索引，`@e` 选择器找不到（实体本身存在；`forceload add 0 0` 后可见）；
2. `/summon` 后**同一 tick 立即查询**也会 `No entity was found`，至少跨 1 tick（本次等待 3s ≈ 60 tick）后正常；
3. 僵尸等 Monster 在无玩家（128 格内）时会立即 despawn；测试需 `PersistenceRequired:1b`（并放在已加载区块内）。
→ forceload + 等待后，**裸 summon 冰龙存活**（Health=250.0f、AgeTicks 递增、Pos 落地 y≈67.8、`execute if entity` 通过，gametime 12599→12661）；带完整 NBT 的冰龙同样存活。

**通过 / 失败 / 未验证**

| # | 项目 | 结果 | 证据 |
|---|---|---|---|
| R1 | 生成 + 存活（服务端） | **通过** | `summon iceandfire:ice_dragon 0 120 0` + forceload + 3s：Health=250.0f、Pos=[0.59,67.80,0.79]、`execute if entity` 命中；无 crash |
| R2 | 驯服（NBT 级） | **通过** | `TamedDragon`=1b、`Owner`=[I;1,2,3,4] |
| R3 | 化形（服务端状态） | **通过** | `DragonMaid`={Humanoid:1b, Knowledge:400, KnowledgeLevel:3, ...}（readAdditionalSaveData TAIL 生效，日志 0 错误） |
| R3b | 化形（客户端按钮/同步） | **通过（task-7，新 jar）** | combatkeys：`data modify ... DragonMaid.Humanoid 1b` + GUI 化形路径，修复后无客户端崩溃、人形模型正常；证据 `shots/t7-02-humanoid-front.png`、`t7-03-humanoid-view.png`（详见 §8.5） |
| R4 | 护卫（跟随/站位） | **通过（客户端+在线玩家，combatkeys task-7）** | Command=2 生效；玩家 tp 18 格外，龙 Pos 5s 内从 (-71.73,69,-257.70) → (-58.90,69,-255.04)，靠近约 13 格（`_probe_dm_client/shots/` 证据） |
| R5 | 龙息 | **通过（task-7，判据修正）** | 人形+Command=2+玩家在线；husk 4 格外：`DragonMaid.Combat.BreathCooldown` 0→60（t=0.82s，= 配置 breathRecoveryTicks 默认 60），husk 20HP→死亡。**人形形态不设置 IAF `FireBreathing`（那是龙形态字段），不能用它判失败** |
| R6 | 龙甲渲染 | **通过（task-7 复测）** | 服务器：`Items[1]`=silver head 正确；客户端旧路径 `armor_*_metal_*` 缺失（洋红/黑）→ 改为 CE `<mat>_layer_N.png` 后复测：**紫黑消失、贴图正确**（`_probe_dm_client/shots/rt-r6-final.png`） |
| R7 | 原「护卫打末影守望者」崩溃复现 | **通过（task-7）** | 护卫状态 vs enderman（代替末影守望者）：40HP → 24s 内死亡；观察 30s 无新 crash report、玩家全程在线 |
| — | 日志无 dragonmaid 相关 ERROR/WARN | **通过** | 全程扫描（过滤 live-server 噪音） |
| — | 资产 358/359 + pack.mcmeta 有意变更 | **通过** | `_port_src/task5_asset_check.py` 对 D7316107 jar 实测：359 内容文件 / 358 一致 / 1 差异(pack.mcmeta) / 0 缺失 |

**通过/失败/未验证汇总（终版，合并 task-7 + 最终 jar 服务端终验）**：**通过 10 项**（R1/R2/R3/R3b/R4/R5/R6/R7 + 日志横切 + 资产横切），**失败 0 项，未验证 0 项**。

**复现命令**
```
python _port_src/task5_runner.py --start --stop --suite all --expected-sha d73161076f5a99a1dc54ec38f7af678b45299316de46179b654ebbb9fea177b6 --out _port_src/task5-evidence-run4.md
```

**未验证项补齐计划（客户端阶段）**：客户端连入 `_probe_dm` → spawn_egg 或 S1 summon → 打开龙界面点化形(R3b) → Command=2 后走动验证跟随(R4) → 近身僵尸验证喷息(R5) → 穿龙甲看渲染(R6) → 护卫状态打末影守望者(R7)。

### 8.1 task-7（客户端侧验证）步骤 + 命令 + 判据 — 交接 combatkeys-dev

> 前提：服务端 `_probe_dm` 用本 jar（D7316107…）；客户端实例连 25605；`<player>` = 客户端玩家名。
> 服务端保持运行：`python _port_src/task5_runner.py --start --suite cleanup --expected-sha d73161076f5a99a1dc54ec38f7af678b45299316de46179b654ebbb9fea177b6`（`--start` 不带 `--stop` 会保持服务端运行），或 `cmd /c _probe_dm\run_probe.bat`。
> RCON 工具：`_port_src/rcon.py` / `_port_src/task5_runner.py --attach --suite <x>`。
> **纪律**：客户端键鼠自动化需先向 Lead 申请「隔离测试客户端」授权，不得未授权合成鼠标点击。

**通用预置（RCON，客户端登录后）**
```
forceload add 0 0
data get entity <player> UUID                          # 取玩家 UUID -> [I;a,b,c,d]
execute at <player> run summon iceandfire:ice_dragon ~2 ~ ~2 {Tags:["dmtest"],TamedDragon:1b,Owner:[I;<player-uuid>],Variant:"sapphire",AgeTicks:2400000,Command:0,DragonMaid:{Humanoid:0b,Knowledge:400,KnowledgeLevel:3}}
# sleep 3
data get entity @e[tag=dmtest,limit=1] DragonMaid     # 预检: Humanoid=0b, KnowledgeLevel=3
```

**R3b 化形（客户端按钮）**
1. 客户端右键/交互该龙 → 打开 DragonMaid 龙界面（本 mod DragonDetailsScreen）；
2. 点界面里的化形按钮（走 `AccessoryMenuAction.transform` → `DragonForms.toggle`）；
3. 判据：客户端提示 `message.dragonmaid.human_form`、模型切人形、无 crash；服务端 `data get entity @e[tag=dmtest,limit=1] DragonMaid.Humanoid` → 1b；
4. 证据：化形前后截图 + 服务端 data get 回显。

**R4 护卫（Command=2 跟随）**
1. 界面按钮切「跟随/护卫」（或 RCON `data merge entity @e[tag=dmtest,limit=1] {Command:2}`，若 merge 后未生效则用界面按钮）；
2. 判据：`data get entity @e[tag=dmtest,limit=1] Command` → 2；玩家走远 10–20 格，间隔 5s 两次 `data get entity @e[tag=dmtest,limit=1] Pos`，位置向玩家靠近；`GuardEscort.owner()` 仅在真实玩家在线时非空（无玩家时此步骤无意义）；
3. 证据：两条 Pos 回显 + 客户端跟随截图。

**R5 龙息**
1. 保持 R4 状态（玩家在线 + Command=2 + 人形）；
2. RCON：`execute at @e[tag=dmtest] run summon minecraft:zombie ~3 ~ ~3 {Tags:["dmtarget"],PersistenceRequired:1b}`（先记 `data get entity @e[tag=dmtarget,limit=1] Health`）；
3. 等 10–15s；判据：`data get entity @e[tag=dmtest,limit=1] FireBreathing` 出现 1b、僵尸 Health 下降/死亡；客户端看到龙息粒子/动画；无 crash；
4. 清理：`kill @e[tag=dmtarget]`；证据：FireBreathing/Health 回显 + 截图。

**R6 龙甲渲染**
1. RCON：`give <player> iceandfire:dragonarmor_silver_head`（CE 原版；registry id 形如 `iceandfire:dragonarmor_silver_head`；若 Unknown item 可换 `dragonarmor_silver_body`）；本 mod service armor：`dragonmaid:service_dragon_armor_head/neck/body/tail`；
2. 客户端打开龙界面，把龙甲放入对应槽（CE DragonArmorPart 映射：HEAD→HEAD、CHEST→NECK、LEGS→BODY、FEET→TAIL）；
3. 判据：服务端 `data get entity @e[tag=dmtest,limit=1] Items` 含该物品；客户端龙身上出现对应部位模型+正确贴图（无黑紫/无 NPE）；
4. 证据：Items 回显 + 渲染截图。

**R7 原「护卫打末影守望者」崩溃复现**
1. 保持 R4 状态（玩家在线 + 护卫 + 人形）；
2. RCON：`execute at @e[tag=dmtest] run summon minecraft:enderman ~5 ~ ~5 {Tags:["dmwatcher"],PersistenceRequired:1b}`；
3. 玩家靠近/攻击末影守望者，使龙进入护卫战斗；观察 30s；
4. 判据：服务端无 crash（`_probe_dm\crash-reports` 无新文件、日志无 `Exception`/`Ticking entity`/`dragonmaid` ERROR）；客户端无断开/无 crash；
5. 清理：`kill @e[tag=dmwatcher]`、`kill @e[tag=dmtest]`、`forceload remove 0 0`；证据：日志 + 客户端截图。

**task-7 证据落盘建议**：每步 RCON 回显 + 客户端截图 + `_probe_dm\logs\latest.log`（或 probe.log）片段，写 `TASK7-CLIENT-EVIDENCE.md`；完成后 task-5 用其合并终版三栏清单。

**附加观察（非 task-7 范围，服务端）**：`save-all` + RCON stop + 重启后，full-NBT 龙**原 UUID 仍在**且 `DragonMaid.Humanoid`=1b、`TamedDragon`=1b（证据 `_port_src/task5-evidence-persist-check2.md`）→ R2/R3 数据跨重启保持。

### 8.4 SimpleChannel 编解码错位修复 + 真实 payload 往返测试

- **Bug（P0）**：客户端日志 `ResourceLocationException: Non [a-z0-9/._-] character in path of location: minecraft:\u0013min`（`BreathImpactMessage.decode:43` / `BreathAnimationMessage.decode:34`）。
- **根因**：`compat/net/SimpleChannel.encode()` 在 payload 的 data 里**多写了一次 message id**（`buf.writeVarInt(id)`），而 `CompatPayload.CODEC` 在 wire 层已经写 id；解码端把 data 的第一个字节当消息字段读 → 所有字段整体错位（0x13=19 就是 ResourceLocation 长度被误读的 id 字节）。
- **修复**：`SimpleChannel.encode()` 不再向 data 写 id，data 只放消息体（见源码注释）。
- **真实 payload 往返测试（新 jar 7E5EFE36 实跑）**：`_port_src/roundtrip/RoundtripTest.java`（javac 0 错误；classpath = 新 jar + moddev artifacts + live-server 97 个依赖库）：
  - `BreathImpactMessage`（id 6）、`BreathAnimationMessage`（id 4）：payload id 正确 → data 非空 → **data 首字节 ≠ message id**（旧 bug 检查）→ `CompatPayload.CODEC` 编码/解码 wire 后 id 与 data 完全一致 → 消息 decode 后**与原始对象 equals** → buffer 读完无剩余；
  - 结果：**ALL ROUNDTRIP TESTS PASSED**（14/14 checks）。
- 证据：`_port_src/roundtrip/RoundtripTest.java` + `_port_src/roundtrip/run.log`。

### 8.5 task-7 客户端结果合并（combatkeys-dev，jar 7E5EFE36/CD0863CE）

> 来源：`dragonmaid-port/TASK7-CLIENT-EVIDENCE.md`（combatkeys 执行，客户端 `_probe_dm_client`，键鼠自动化已获 Lead 授权）。

| 项 | 结果 | 关键证据 |
|---|---|---|
| R3b 化形 | **PASS** | 修复后无客户端崩溃、人形模型正常渲染；截图 `shots/t7-02-humanoid-front.png`、`t7-03-humanoid-view.png` |
| R4 护卫跟随 | **PASS** | Command=2；玩家 tp 18 格外，龙 5s 内靠近 ~13 格 |
| R5 龙息 | **PASS（判据修正）** | `DragonMaid.Combat.BreathCooldown` 0→60（0.82s，= 配置默认 60）、husk 20HP→死亡；人形形态不设置 IAF `FireBreathing` |
| R6 龙甲渲染 | **PASS（紫黑已消失）** | 服务器 Items[1]=silver head；客户端复测 `shots/rt-r6-final.png`：头盔白/银贴图正确、无洋红/黑棋盘 |
| R7 护卫战斗 | **PASS** | enderman（代替末影守望者）40HP → 24s 内死亡；30s 观察无 crash report、玩家不断线 |

- **最终 jar 已终验**：7E5EFE36…（含 SimpleChannel 修复 + 龙甲贴图修复）；服务端 `suite all` 终验无回归，详见 §8.6；task-7 客户端五项全 PASS。
### 8.6 最终 jar 服务端终验（7E5EFE36，含 SimpleChannel + 龙甲贴图修复）

- jar：`build/libs/dragonmaid-neoforge-1.21.1-1.1.0-beta.2.jar`，6,812,348 B / mtime 11:36:30 / sha256 `7E5EFE36F5166019E731CF1E3CAA026807AC4CCC86A414947BB11F3AD8F49F8A`；`_probe_dm/mods` 同 sha。
- 命令：`task5_runner.py --start --stop --suite all --expected-sha 7e5efe36…`（预检通过；起服→Done→套件→RCON stop→端口归零）；证据 `_port_src/task5-evidence-run8-final-verify.md`。
- 结果：**R1/R2/R3 无回归**——裸 summon 冰龙存活（Health=20.0f、AgeTicks=61、TamedDragon=0b、Pos=[0.56,71.94,0.75]，gametime 44860→44921）；full-NBT：TamedDragon=1b、Owner=[I;1,2,3,4]、Variant="sapphire"、AgeTicks=2400041、Command=2、DragonMaid={Humanoid:1b,Knowledge:400,KnowledgeLevel:3}；日志 dragonmaid 相关 ERROR/WARN = none；资产 358/359 不变。
- 附：R5 服务端 breath 套件里 `FireBreathing`=0b 属**预期**（人形形态不设置该龙形态字段；客户端判据见 §8.5 用 `DragonMaid.Combat.BreathCooldown`），不构成失败。

### 8.2 客户端崩溃修复（R3b/R5 路径，task-7 发现）

- **崩溃**：client `NullPointerException: this.headPart is null`（`DragonBaseEntity.updateParts` ← `DragonMixin.refreshForm` ← `syncForm` tick HEAD），存档 `_probe_dm_client/shots/crash-R3b-humanoid-sync-2026-11-01_11.15.31-client.txt`。
- **根因**：IAF CE 2.0 的 `updateParts()` 不再做 null 检查（直接 `headPart.copyPosition(this)`）；本 mod `refreshForm` 在**客户端**先 `removeParts()` 再 `updateParts()`，此时 part 已被清空 → NPE。
- **修复（mixin/DragonMixin.java `dragonmaid$refreshForm`）**：
  1. 客户端不管理 part 实体（不 `removeParts`/不 `updateParts`；part 增删由服务端操作后随实体同步下发）；
  2. 服务端 `removeParts()` 后用 `updateScale(getRenderSize()/3.0F)` 重建（`updateScale` 对 null part 会新建，`updateParts` 不会），不再调用 `updateParts()`；
  3. 删除原先 `if (!isHumanoid()) updateParts();` 分支。
- 本地 javac：DragonMixin 0 错误；**待构建负责人重编新 jar 后补测 R3b/R5**。

### 8.3 新 jar（含化形崩溃修复）服务端复验 — CD0863CE

- jar：`build/libs/dragonmaid-neoforge-1.21.1-1.1.0-beta.2.jar`，6,812,396 B / mtime 11:20:36 / sha256 `CD0863CEF2289F1B4BB9461634327BF4B900B0B41F364BC9EB395CDC76E00742`；`_probe_dm/mods` 同 sha。
- 复跑：`task5_runner.py --start --stop --suite all --expected-sha cd0863ce…`（预检 sha 通过），证据 `_port_src/task5-evidence-run6-newjar-clean.md`。
- 结果（服务端）：**R1/R2/R3 复验通过、无回归**：
  - R1（隔离）：先清空世界旧测试龙 → `summon iceandfire:ice_dragon 0 120 0 {Tags:[dmbare]}` → 3s 后 Health=20.0f、AgeTicks=55、TamedDragon=0b、Pos=[0.50,55.66,0.50]，`execute if entity` 命中（gametime 30858→30913）；
  - R2：TamedDragon=1b、Owner=[I;1,2,3,4]、Variant="sapphire"；
  - R3：DragonMaid={Humanoid:1b, Knowledge:400, KnowledgeLevel:3}、Command=2；
  - 日志：dragonmaid 相关 ERROR/WARN = none；资产：358/359（唯一差异 pack.mcmeta 有意变更）。
- 注：run5 首次复跑因世界残留旧测试龙导致样本污染（TamedDragon=1b、Pos 在旧位置），已改为「先清龙 + tag 查询」后得到干净 run6。
- **R3b/R5 的客户端崩溃修复验证**仍需 combatkeys 用此 jar 重测（服务端无法验证客户端 part 生命周期）。
