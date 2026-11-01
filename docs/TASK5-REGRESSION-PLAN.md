# TASK-5 (P4) 回归执行计划 — 准备稿 v3

> 状态：task-5 任务板仍 blocked（依赖 task-4）；本文件为解阻前准备，owner: pregen-dev。
> 依据：P0-P1-EVIDENCE.md、IAF CE 2.0 decompiled_iaf 源码、iceandfire-2.0.jar javap、最终 jar 实测。

## 1. 资产复核（已实测，针对最终 jar）

jar：`build/libs/dragonmaid-neoforge-1.21.1-1.1.0-beta.2.jar`，6,812,454 B，sha256 `943783E496633FE3E52DADD705A18158373AFEDAF5CFD2F5587EE1B9595753B6`。

| 项 | 结果 |
|---|---|
| 原 jar 内容文件（assets/ + data/ + pack.mcmeta + dragonmaid.png） | 359 |
| 逐字节一致 | **358** |
| 不一致 | **1：pack.mcmeta（有意变更：pack_format 15→34 + supported_formats 15..48，1.21.1 必须）** |
| 缺失 | 0 |
| 新增内容文件 | 64（P0 的 1.21 目录别名副本，只在 build 产物里） |

复现：`_port_src/task5_asset_check.py`。

## 2. 「龙存活」问题（源码级结论 + 复验命令）

### 2.1 已定位的事实
1. `[IceAndFire] Not a list: null` **不是消失根因**：来自 CE `DragonBaseEntity.readAdditionalSaveData()` 的
   `ItemStack.OPTIONAL_CODEC.listOf().parse(..., compound.get("Items"))`；裸 summon 无 `Items` 标签 → NbtOps 报错 →
   `resultOrPartial(LOGGER::error)` 打 ERROR 后 `.orElse(List.of())` 吞掉。任何无 Items NBT 的龙都会出现，与存活无关，**不作为失败判据**。
2. **最可疑原因：console /summon 不带坐标 → 落点 (0,0,0) 在地下**（world_probe 是 1.21.1 自定义高度）→ 生成即窒息死亡。
   P1 证据里活下来的那次 summon 是带坐标的（`2 -60 2`）。
3. CE 龙年龄/阶段公式（源码 `DragonBaseEntity`）：
   `getAgeInDays() = clamp(AgeTicks/24000, 0, isTame ? maxTamedDragonAge : 128)`，`maxTamedDragonAge` 默认 128；
   `getDragonStage()`：ageDays≥100→5，≥75→4，≥50→3，≥25→2，否则 1。
   → 化形解锁需要 **stage≥3 + knowledge≥300**（`DragonForms.unlocked`）；服务端用 `AgeTicks:2400000`（100 天，stage 5）或 ≥1200000（50 天，stage 3）。
4. CE 龙 NBT 键：`TamedDragon`、`Owner`（TamableAnimal `putUUID` → NBT int array `[I;a,b,c,d]`）、`Variant`、`AgeTicks`、`Gender`、`Hunger`、`Command`、`DeathStage`、`Flying/Hovering/Sleeping/FireBreathing`、`Items`、`BrushedTime`。
5. CE 指令语义：`Command=0` 游荡/回家、`1` 坐、**`2` 跟随（本 mod 的「护卫」判据 `Command==2`，见 GuardEscort.owner）**。
6. CE 冰龙颜色（`IafDragonColors`）：`blue` / `white` / `sapphire` / `silver`；闪电龙：`electric` / `amethyst`。回归用 `Variant:"sapphire"`。

### 2.2 生成方式（三路线）
```
S1（首选）显式坐标 + 完整 NBT summon（/summon 会执行 readAdditionalSaveData，NBT 全生效）：
  summon iceandfire:ice_dragon 0 120 0 {Tags:["dmtest"],TamedDragon:1b,Owner:[I;1,2,3,4],Variant:"sapphire",AgeTicks:2400000,Gender:0b,Hunger:90,Command:2,DragonMaid:{Humanoid:1b,Knowledge:400,KnowledgeLevel:3}}
  判据：连续 60 tick entity 仍存在；TamedDragon=1b；DragonMaid.Humanoid=1b。

S2（官方孵化）龙蛋 item：iceandfire:dragonegg_{red,green,bronze,gray,blue,white,sapphire,silver,electric,amethyst,copper,black}
  放置/`summon iceandfire:dragon_egg 0 120 0 {Color:"red",DragonAge:0,OwnerUUID:"<uuid>"}`；eggBornTime 默认 7200，
  孵化条件：火龙蛋脚下燃烧方块 / 冰龙蛋在水中（500 tick 概率变 EGG_IN_ICE）/ 闪电龙蛋下雨+见天；
  孵化后自动 setTame(true,true)+owner=蛋主（适合从 0 验证「驯服」）。

S3（官方刷怪蛋）iceandfire:spawn_egg_{fire,ice,lightning}_dragon（DeferredSpawnEggItem，走 finalizeSpawn）：需要在线玩家/客户端，
  客户端验收（task-4/5 客户端）用。
```

## 3. 回归清单（R1-R7，命令 + 判定）

| # | 步骤 | 命令 | 通过判据 |
|---|---|---|---|
| R1 | 生成（服务端） | S1 summon（坐标必须显式；保底 S2 蛋） | entity 连续存活 ≥60 tick，无新增 ERROR（Items 噪音除外） |
| R2 | 驯服 | S1 NBT `TamedDragon:1b` + `Owner`；`data get entity @e[tag=dmtest,limit=1] TamedDragon` | 返回 1b；Owner 有值 |
| R3 | 化形（服务端） | S1 NBT `DragonMaid:{Humanoid:1b,Knowledge:400,KnowledgeLevel:3}`（readAdditionalSaveData TAIL → setHumanoid(true)） | `data get entity … DragonMaid` => Humanoid=1b；后续 tick 无 crash |
| R3b | 化形（客户端按钮） | 客户端打开龙界面按钮 → `AccessoryMenuAction.transform` → `DragonForms.toggle` | 提示 message.dragonmaid.human_form；服务端数据同 R3（task-4 客户端环境） |
| R4 | 护卫 | S1 `Command:2` | **无在线玩家时只能验「Command 语义 + 无异常」**：`GuardEscort.owner()` 需要真实 Player 实体，护送跟随需客户端/玩家在线阶段验（记录此限制） |
| R5 | 龙息 | S1（人形+Command2）+ `summon zombie 3 120 3 {NoAI:1b}` | **无在线玩家时 HumanoidGoal 的护卫呼吸不激活** → 服务端可验「FireBreathing 字段存在/NBT 可读」；完整呼吸需玩家在线（客户端阶段） |
| R6 | 龙甲渲染 | 客户端：give 龙甲 + 给龙穿 | 服务端先验 `getItemBySlot(HEAD)`；客户端无 NPE、CE `DragonArmorFeatureRenderer.getArmorTexture` 有纹理（task-4 客户端） |
| R7 | 原崩溃复现 | 护卫状态打末影守望者 | 服务端无 crash；日志留证 |

## 4. 自动回归脚本（已就绪，dry-run 通过）

`_port_src/task5_runner.py`（python，vision-venv 跑）：
```
python task5_runner.py --dry-run                       # 只打印命令序列，不连服务器
python task5_runner.py --start --suite spawn           # 起 _probe_dm（等待 Done）→ 跑 spawn 套件
python task5_runner.py --attach --suite all --out <evidence.md>
python task5_runner.py --stop                          # RCON stop + 验证 25605/25606 关闭
```
- 预检（start/preflight）：jar 存在且 size==6,812,454、sha256==943783E4…；`_probe_dm` 存在；jar 已装进 `_probe_dm/mods/`；25605/25606 未被占用（避免双开）。
- 套件：`spawn`（显式坐标裸 summon 存活 A/B）、`nbt`（完整 NBT：驯服/化形/Command/Health）、`breath`（僵尸目标 + FireBreathing/血量）、`cleanup`。
- 证据：RCON transcript + 日志扫描（自动过滤 CE `Not a list: null` 噪音）+ 日志尾部，写 `--out` markdown。
- 起服方式：直接 `java.exe @user_jvm_args.txt @libraries/.../win_args.txt nogui`（cwd=_probe_dm），日志 probe.log；记录 PID；结束用 RCON `stop` 并确认端口关闭。

## 5. 基础设施与协调
- 测试服 `D:\mc-model\work\_probe_dm`：mods=live-server 79 hardlinks + 最终 jar；端口 25605 / RCON 25606 / password probepw / level=world_probe。
- RCON 协议客户端：脚本内置（参考 `_port_src/rcon.py`）。
- **一次只允许一个测试服占用**：起服前与构建负责人对时间；起服到我停服期间不双开；只杀自己记录的 PID。
- 证据最终并入 `PORT-STATUS.md`（task-5 write scope：仅该文件）。

## 6. 解阻后执行顺序
1. 与构建负责人确认 `_probe_dm` 空闲窗口（预计 15-20 分钟：起服 1-2 min + 套件 2 min + 停服 1 min + 余量）。
2. `python task5_runner.py --start --suite spawn` → 判定「y=0 落石窒息」假设成立/不成立。
3. 成立 → 用 S1 NBT 跑 `--suite nbt`（驯服+化形+Command）；不成立 → 改 S2 蛋孵化路线。
4. 服务端部分跑完 → 客户端阶段（task-4 环境）补 R3b/R4/R5/R6；R7 崩溃复现（需玩家在线）一起。
5. 汇总写 PORT-STATUS.md：每项 PASS/FAIL + 命令 + 日志行号 + 限制说明。