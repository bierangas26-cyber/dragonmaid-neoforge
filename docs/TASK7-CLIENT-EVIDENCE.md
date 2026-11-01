# TASK7 — Dragon Maid P4 客户端侧验证（R3b/R4/R5/R6/R7）

> 执行：combatkeys-dev（task-7 交接自 pregen-dev，见 PORT-STATUS §8.1）
> 时间：2026-11-01 11:07–11:40（Asia/Shanghai）
> 环境：服务端 `_probe_dm`（25605 / RCON 25606，世界 `world_probe`）；客户端 `D:\mc-model\work\_probe_dm_client`（client-live 85 mods 硬链 + server-only mod 补齐，共 87 jar）
> jar：`dragonmaid-neoforge-1.21.1-1.1.0-beta.2.jar` 6,812,396 B / 11:20:36 / sha256 `CD0863CEF2289F1B4BB9461634327BF4B900B0B41F364BC9EB395CDC76E00742`（含 pregen 的 DragonMixin.refreshForm 客户端 NPE 修复）
> 纪律：键鼠自动化 + 截图 已获 Lead 授权，仅作用于该隔离客户端窗口；未触碰 client-live / live-server。

## 结果总览

| 项 | 结果 | 关键证据 |
|---|---|---|
| R3b 化形 | **PASS**（修复后无崩溃，人形模型正常渲染） | `data modify ... DragonMaid.Humanoid 1b` 后 4s：无新 crash report、`list` 玩家仍在线；截图 `shots/t7-02-humanoid-front.png`、`t7-03-humanoid-view.png` |
| R4 护卫跟随 | **PASS** | `Command=2`；玩家 tp 到 18 格外后，龙 Pos `(-71.73,69,-257.70)` → `(-58.90,69,-255.04)`（5s 内靠近 ~13 格） |
| R5 龙息 | **PASS**（判据需修正，见下） | 人形 + Command=2 + 玩家在线；husk 目标 4 格外：`DragonMaid.Combat.BreathCooldown` 0 → **60**（t=0.82s，= 配置 breathRecoveryTicks 默认 60），husk 20 HP → 死亡 |
| R6 龙甲渲染 | **服务器 PASS / 客户端 FAIL（贴图缺失）** | `item replace entity ... armor.head with iceandfire:dragonarmor_silver_head` → `Items: [{}, {count:1, id:"iceandfire:dragonarmor_silver_head"}, {}, {}, {}]`；客户端人形模型头部龙甲显示 **洋红/黑缺失贴图**（截图 `t7-05-r6-armor-d.png`、`t7-06-r7-fight.png` 均可见） |
| R7 护卫战斗（末影人） | **PASS** | 末影人 40 HP → t=16s 10 HP → t=18-22s 9 HP → t=24s 死亡；观察 30s 内 **无新 crash report**、玩家全程在线（未断线） |

## 重要发现 1：客户端崩溃（已修，已复验）

**R3b 路径**（切人形）在**旧 jar** 上必崩客户端：
```
Description: Ticking entity
java.lang.NullPointerException: Cannot invoke "DragonPartEntity.copyPosition(Entity)" because "this.headPart" is null
  at iceandfire/DragonBaseEntity.updateParts(DragonBaseEntity.java:440)
  at iceandfire/DragonBaseEntity.dragonmaid$refreshForm(...)      <- dragonmaid mixin
  at iceandfire/DragonBaseEntity.handler$dgm000$dragonmaid$syncForm(...)
  at net.minecraft.client.multiplayer.ClientLevel.tickNonPassenger(...)   <- 客户端
```
- 崩溃报告存档：`_probe_dm_client/shots/crash-R3b-humanoid-sync-2026-11-01_11.15.31-client.txt`（另有 11:20:23 同一栈第二份）
- 根因：`refreshForm()` 在 client 侧先 `removeParts()` 再 `updateParts()`，而 IAF CE 2.0 的 `updateParts()` 不判空
- 修复（pregen-dev，已含本 jar）：client 侧不做 part 增删；服务端 `removeParts()` 后用 `updateScale(...)` 重建
- **复验**：新 jar 上同样路径（Humanoid 0b→1b）→ 无新 crash report、玩家在线、人形模型渲染正常 ✅

## 重要发现 2：R5 判据修正（§8.1 需要更新）

§8.1 写「`FireBreathing` 出现 1b」——实测 **人形形态不会设置 IAF 的 `FireBreathing`**（那是龙形态字段，全程 0b）。
本 mod 的人形龙息正确观测点是 `DragonMaid.Combat.BreathCooldown`：
```
t=0.82s  BreathCooldown = 60   （= DragonMaidConfig breathRecoveryTicks 默认 60）
target   Husk 20.0f -> 死亡
```
建议 §8.1 改成：`data get entity @e[tag=dmtest,limit=1] DragonMaid.Combat.BreathCooldown` 出现 >0，或目标掉血/死亡；`FireBreathing` 仅用于龙形态。

## 重要发现 3：R6 客户端龙甲贴图缺失（需渲染层 owner 跟进）

服务器侧正确（Items 槽位有 silver head），但客户端人形模型上该龙甲显示 **洋红/黑棋盘（missing texture）**：
- 截图：`shots/t7-05-r6-armor-d.png`（近景）、`shots/t7-06-r7-fight.png`（战斗近景，头部洋红块明显）
- 可能原因（待查，属渲染层）：人形龙甲走 `IceFemaleEquipmentLayer` / `TailArmorLayer` 的 CE 龙甲贴图路径（`DragonArmorFeatureRenderer.getArmorTexture`），而“头部银色龙甲”在人形骨骼上找不到对应 UV/部位映射 → 取到空/未注册纹理。
- 未验证：其它材质（gold/diamond/dragonsteel_*）与其它部位（body/tail）是否同样缺失。

## 截图清单（`D:\mc-model\work\_probe_dm_client\shots\`）

| 文件 | 内容 |
|---|---|
| `t7-00-baseline.png` | 客户端窗口基线（世界已进入） |
| `t7-01-humanoid.png` | 切人形后（旧相机位） |
| `t7-02-humanoid-front.png` / `t7-03-humanoid-view.png` | 人形龙正面对镜（蓝发/角/翼/粉眼，渲染正常） |
| `t7-04-r5-a/b/c.png` | R5 龙息/战斗过程 |
| `t7-05-r6-armor*.png` | R6 龙甲（含洋红缺失贴图的近景/远景） |
| `t7-06-r7-fight.png` | R7 近身战斗（人形龙 + 洋红缺失贴图） |
| `crash-R3b-humanoid-sync-*.txt` | 旧 jar 的两个客户端崩溃报告（已修复复验） |

## 未验证 / 限制

1. **R3b 的按钮点击本身**：我用 RCON `data modify DragonMaid.Humanoid` 触发，与 GUI 按钮走**同一条同步路径**（entityData → 客户端 syncForm → refreshForm），但「在龙界面点中化形按钮」这一 UI 交互未按像素级验证（GUI 需要打开龙界面再定位按钮，本次未做）。
2. **R6 其它材质/部位**未覆盖（只测了 silver head）。
3. **R7「末影守望者」**：本次用原版 `minecraft:enderman`（40 HP）代替；若指某个 mod 的自定义 Boss，未测。
4. 客户端出现过 “Game Menu” 暂停（窗口失焦），最终截图已通过「点击 Back to Game + PrintWindow」修正；非崩溃。

## 环境变更（已/待恢复）

- `difficulty`：Peaceful → **Normal**（Peaceful 会秒删敌对生物，R5/R7 无法进行）
- `gamerule doDaylightCycle false` + `time set`（用于稳定光照）
- 测试实体：`tag=dmtest`（冰龙，人形，银头龙甲）、清理了 `dmtarget`/`dmwatcher`
- 服务端 / 客户端进程在收尾时按 PID 清理
## 已知问题（Lead 已定位，记录待复测，不重复报）

1. **SimpleChannel 编解码错位**（pregen 跟进）：`ResourceLocationException: Non [a-z0-9/._-] character in path of location: minecraft:\u0013min`，来源 `BreathImpactMessage.decode` / `BreathAnimationMessage.decode` → 化形/龙息同步失败。
2. **龙甲贴图旧路径**（已在 client 侧修复，见下）：`FileNotFoundException: iceandfire:textures/models/armor/armor_silver_metal_layer_1.png`（CE 2.0 已改名 `silver_layer_1.png`）。

### 紫黑（缺失贴图）具体部位 + 坐标

| 截图 | 紫黑区域 | 观察者/龙坐标 |
|---|---|---|
| `t7-05-r6-armor-d.png` | 龙**头顶板 + 后脑/颈部**大块方形 | player (-53, 68, -266) 近景；dragon (-52.33, 68, -266.66)，humanoid=1b，`Items[1]=iceandfire:dragonarmor_silver_head` |
| `t7-05-r6-armor-wide.png` | 同上（远景，头顶 + 左后颈） | 同上 |
| `t7-06-r7-fight.png` | **头部/头盔主体 + 上背**（近身战斗特写） | player (-53, 68, -275) → dragon 近身 |

结论：紫黑＝“银色龙甲”在人形模型上走的是 IAF 1.20.1 的旧贴图路径（CE 2.0 已无该文件）。

### 已修复（本次提交，client/ 范围）

- `client/NativeMetalArmor.java`：`textures/models/armor/armor_<mat>_metal_layer_N.png` → `textures/models/armor/<mat>_layer_N.png`（CE 2.0 命名，jar 内实证 `silver_layer_1.png` / `copper_layer_1.png` 存在）
- `client/FemaleChestplateMaterial.java`：SILVER/COPPER 两条路径同样改名（`armor_silver_metal_layer_1.png` → `silver_layer_1.png`，`armor_copper_metal_layer_1.png` → `copper_layer_1.png`）
- 说明：CE jar 里还包含 `resourcepacks/iaf_legacy/.../copper_layer_1.png`，但**没有** `armor_*_metal_*`；即旧名在 CE 2.0 彻底不存在。

### 复测计划（等新 jar）

1. R6：重新放 silver head/body 龙甲 → 截图确认紫黑消失；顺带补测 gold / diamond / dragonsteel_ice 等材质
2. R3b/R5：跟 pregen 的 SimpleChannel 修复一起复测（化形按钮 + 龙息同步）
3. R7：复测端影人战斗无 crash
## 复测结果（jar `7E5EFE36…` / 6,812,348 B / 11:36:30，含龙甲贴图修复 + SimpleChannel 修复）

| 项 | 结果 | 证据 |
|---|---|---|
| R3b 化形 | **PASS** | Humanoid 0b→1b 后 4s：无新 crash report（最新仍是 11:20:23 旧 jar 的两份）、`list` 玩家在线；人形模型正常 |
| R5 龙息 | **PASS** | `DragonMaid.Combat.BreathCooldown` 0 → **60** @ t=0.71s；无 `ResourceLocationException` / decode 报错（SimpleChannel 修复生效） |
| R6 龙甲渲染 | **PASS（紫黑消失）** | 银龙头甲：`Items[1]={count:1,id:"iceandfire:dragonarmor_silver_head"}`；截图 `shots/rt-r6-final.png` 头盔为白/银贴图，**无洋红/黑棋盘** |
| R7 末影人战斗 | **PASS** | 末影人 18s 内被击杀；无新 crash report；玩家全程在线 |

### 截图（复测，`D:\mc-model\work\_probe_dm_client\shots\`）
- `rt-r6-final.png` —— 人形龙 + 银色龙甲头盔（贴图正确，无紫黑）**这是 R6 通过的关键证据**
- `rt-r6-silver-b.png`、`rt-r5-breath.png`、`rt-r7-enderman.png` —— 过程图
- 对照（旧 jar 紫黑）：`t7-05-r6-armor-d.png`、`t7-06-r7-fight.png`

### 关于 SimpleChannel（pregen 已修，我方复测确认）
- 本轮客户端日志无 `ResourceLocationException`、无 payload decode 报错；R5 龙息与 R3b 化形同步均正常。
- 客户端日志：`D:\mc-model\work\_probe_dm_client\launch-out.log`（本轮）

### 结论
task-7 五项：**R3b / R4 / R5 / R6 / R7 全部 PASS**（R6 的服务器/客户端两侧都通过）。
遗留（非阻塞）：GUI 按钮的像素级点击未单独验证（用等价的 entityData 同步路径）；R6 只覆盖 silver head，未覆盖 gold/diamond/dragonsteel。