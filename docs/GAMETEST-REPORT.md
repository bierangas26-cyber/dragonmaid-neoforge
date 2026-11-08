# DM GameTest 报告 — NeoForge 1.21.1 headless 自动化测试

生成时间：2026-11-01 18:2x（Asia/Shanghai）
项目：`D:\BaiduNetdiskDownload\projects\medieval-world\dragonmaid-port`
执行者：子代理（GameTest 专项），与另一个并行子代理共用同一工作区（见 §7 冲突说明）

---

## 0. 一句话结论

**NeoForge GameTest 在这台机器上已经跑通，headless、无需进游戏**：最近一次完整验证（2026-11-01 18:22）是
`9 GAME TESTS COMPLETE IN 1.123 s` / `All 9 required tests passed :)` / `BUILD SUCCESSFUL in 26s`。
其中 8 条是本套测试，第 9 条（`savedSnapshotKeepsRecallToken`）由并行的另一个子代理追加（见 §7）。
覆盖：数据包加载（配方/进度/标签）、召回号角收放龙（UUID 不变）、饰品护甲加成、附魔书知识经验、
祭坛开界面、石化→复原 round-trip。

> **2026-11-08 更新**：测试增至 **15 条**（新增 `MiniatureGameTests`、3 条 `BackpackGameTests`、2 条 `HumanoidWaterGameTests`），最近一次 `runGameTestServer` = `15 GAME TESTS COMPLETE` / `All 15 required tests passed :)` / `BUILD SUCCESSFUL in 26s`。本报告正文记录 2026-11-01 的 9 条基线，供环境搭建复现用。

---

## 1. 环境怎么配的（4 处改动，都可回退）

### 1.1 用哪条 gradle

项目**没有 gradlew**，用现成的 8.8 发行版 + 项目自带 JDK 21：

```powershell
$env:JAVA_HOME='D:\pcl\voxy1.7\jdk\jdk21'
cd D:\BaiduNetdiskDownload\projects\medieval-world\dragonmaid-port
C:\Users\LIUHZ\.gradle\wrapper\dists\gradle-8.8-bin\dl7vupf4psengwqhwktix4v1\gradle-8.8\bin\gradle.bat `
    runGameTestServer --no-daemon --console=plain
```

- 任务名 `runGameTestServer` 由下面的 run 配置生成（ModDevGradle 2.0.141）。
- 退出码 = **失败的 required 测试数**；0 → `BUILD SUCCESSFUL`。所以这条命令可以直接当 CI 闸门。

### 1.2 `build.gradle` — 新增 gameTestServer run

```groovy
neoForge {
    runs {
        server { /* 原有，未改 */ }
        gameTestServer {
            type = "gameTestServer"
            gameDirectory = file('run/gametest')
            systemProperty("neoforge.enabledGameTestNamespaces", "dragonmaid")
            jvmArguments.addAll '-Xms1G', '-Xmx2G', '-Dfile.encoding=UTF-8'
        }
    }
}
```

### 1.3 `build.gradle` — 把依赖 mod 放进 game-test 运行期 classpath（关键）

mod 的 `neoforge.mods.toml` 把 `iceandfire / geckolib / patchouli` 声明为 **required**，
而依赖原本只有 `compileOnly fileTree('libs')` → 运行期没有这些 mod，FML 会直接拒绝加载 dragonmaid，
GameTest 一个都注册不上。新增：

```groovy
dependencies {
    compileOnly fileTree(dir: 'libs', include: ['*.jar'])   // 原有
    gametestModJars().each { runtimeOnly files(it) }        // 新增
}

List<File> gametestModJars() {
    def names = ['geckolib-neoforge-1.21.1-4.9.2.jar', 'iceandfire-2.0.jar', 'citadel-2.7.1-1.21.1.jar',
                 'uranus-2.4.1-1.21.1-neoforge.jar', 'jupiter-2.3.7-1.21.1-neoforge.jar',
                 'architectury-13.0.11-neoforge.jar', 'Patchouli-1.21.1-93-NEOFORGE.jar']
    def roots = [file('libs'), file(providers.gradleProperty('gametestModsDir')
            .getOrElse('D:/mc-model/work/_probe_dm/mods'))]
    names.collect { n -> roots.collect { new File(it, n) }.find { it.isFile() } }.findAll()
}
```

- 只**引用** jar（libs/ 或线上测试服 mods 目录），**没有复制、没有修改任何 jar**。
- `architectury` libs 里没有，是从 `D:\mc-model\work\_probe_dm\mods` 只读引用的；可用
  `-PgameTestModsDir=<别的目录>` 覆盖。
- `libs/*.jar`、`excluded.txt` **均未改动**。

### 1.4 `META-INF/neoforge.mods.toml` — 补 `[[mixins]]`（否则 dev 里 mixin 根本不生效）

**这是本次最关键的一个坑**：项目只在 `build.gradle` 的 jar manifest 里写了 `MixinConfigs`，
而 manifest 只对**真 jar** 生效。dev 运行是把 `build/classes` 目录当 mod（Mod List 里显示 `main`），
因此 **`DragonMixin` 等 37 个 mixin 一个都没加载**，表现是所有龙的测试都抛
`IceDragonEntity cannot be cast to com.example.dragonmaid.data.DragonState`。

```toml
[[mixins]]
    config="dragonmaid.mixins.json"
```

加了之后 Mod List 里出现 mixin 应用日志、`DragonState` 接口生效，龙相关测试立刻从"全炸"变成可测。
（生产 jar 的 manifest 保留不动，两处声明重复不会冲突。）

### 1.5 结构模板：`data/dragonmaid/structure/gametest_empty.nbt`

NeoForge **21.1.x 没有 `@EmptyTemplate`**（`net.neoforged.neoforge.gametest` 包里只有
`GameTestHolder / PrefixGameTestTemplate / GameTestHooks / BlockPosValueConverter`），
所以必须真给一个 .nbt。用脚本生成了一个 5×5×5 全空气模板（gzip NBT，DataVersion 3955）：

```json
{"DataVersion":3955,"size":[5,5,5],"palette":[{"Name":"minecraft:air"}],"blocks":[],"entities":[]}
```

> 踩坑记录：第一版生成器漏了每个 compound 结尾的 TAG_End，服务端报
> `ReportedNbtException: Loading NBT data / EOFException` → `Missing test structure` → **整个 game test server 崩溃**（不是测试失败）。

---

## 2. 测试清单（8 条，全部 required）

代码目录：`src/main/java/com/example/dragonmaid/gametest/`（7 个文件，含 1 个 support 类）

| # | 测试类 :: 方法 | 断言什么 | 结果 |
|---|---|---|---|
| 1 | `DataLoadGameTests.recipesLoaded` | 19 个 `dragonmaid:*` 配方全部出现在服务端 `RecipeManager`；`kaishaku_blade` 用 `dragonmaid:kaishaku_blade` 序列化器、`reinforced_dragon_horn` 用自己的序列化器 | PASS |
| 2 | `DataLoadGameTests.rootAdvancementLoaded` | `dragonmaid:root` + closer_companion / maid_for_hunting / trinity_of_dragons 均加载 | PASS |
| 3 | `DataLoadGameTests.mercyKillingTagLoaded` | 物品标签 `dragonmaid:mercy_killing_tools` 含 kaishaku_blade 与 test_execution_blade | PASS |
| 4 | `RecallHornGameTests.captureAndReleaseKeepsUuid` | 已驯服冰龙 → 强化龙角 `interactLivingEntity` 收龙：返回 SUCCESS、龙 `isRemoved()`、角获得 **DRAGON_HORN 组件**且 uuid/type 正确；再 `useOn`（点击上表面）放龙：返回 SUCCESS、组件被清空、龙回到 level 且 **UUID 不变** | PASS |
| 5 | `AccessoryGameTests.wardingEmblemRaisesArmor` | 知识 300 + 等级 1 解锁第一格 → 装 `warding_emblem` → `DragonAccessories.refresh` 后 `ARMOR` 至少 +（stage 钳制 1..5 + 1）；移除后回到基准值 | PASS |
| 6 | `KnowledgeUpgradeGameTests.enchantedBookYieldsLearningExperience` | 造 STORED_ENCHANTMENTS=sharpness 3 的附魔书 → `bookKind==REGULAR`、反射调 `bookLearningExperience(book,1) > 0`；空书仍被拒（0） | PASS |
| 7 | `AltarInteractionGameTests.altarOpensRecallMenu` | 放 `dragonmaid:dragon_recall_altar` → `blockState.useWithoutItem(...)` 返回可消费结果、`player.containerMenu instanceof RecallMenu`、菜单记录的祭坛坐标正确 | PASS |
| 8 | `PetrificationGameTests.statueRoundTripKeepsDragon` | 生产门面 `DragonPetrification`：buildStatue 的 `DragonMaidPetrification` 标记含 Dragon uuid+Token → addStatue 成功、龙离场、PetrificationLedger 记录 active → 玩家手持 `petrification_remedy` 调 restore → 龙回来且 UUID 相同、ledger 不再 active | PASS |

**并发提示**：另外一个并行子代理在同一目录里加过 `ZProbePetrificationTests`（诊断用，已自行删除），
并给我这份 `PetrificationGameTests` 追加了第 9 条 `savedSnapshotKeepsRecallToken`（ForgeData→NeoForgeData
回归）+ 改了 `RecallTracking/RecallService/RecallPersistence`。见 §7。

---

## 3. 原始输出（最新一次运行，`build/gametest-final.log`）

```
[18:22:22] [main/INFO] [ne.ne.ne.ga.GameTestHooks/]: Enabled Gametest Namespaces: [dragonmaid]
[18:22:24] [Server thread/INFO] [minecraft/GameTestServer]: Started game test server
[18:22:24] [Server thread/INFO] [minecraft/GameTestServer]: 9 tests are now running at position 11035031, -59, 10527847!
[18:22:25] [Server thread/INFO] [minecraft/GameTestRunner]: Running test batch 'defaultBatch:0' (9 tests)...
[18:22:25] [Server thread/INFO] [minecraft/GameTestServer]: ========= 9 GAME TESTS COMPLETE IN 1.123 s ======================
[18:22:25] [Server thread/INFO] [minecraft/GameTestServer]: All 9 required tests passed :)
[18:22:26] [Server thread/INFO] [minecraft/GameTestServer]: Game test server shutting down

BUILD SUCCESSFUL in 26s
```

失败/跳过数：**failed required = 0，skipped = 0**（`required` 全为默认 true）。

### 3.0 上一轮（只有我这 8 条测试时的）原始输出

```
[18:17:25] [Server thread/INFO] [minecraft/GameTestServer]: 8 tests are now running at position -13989061, -59, 354373!
[18:17:25] [Server thread/INFO] [minecraft/GameTestRunner]: Running test batch 'defaultBatch:0' (8 tests)...
[18:17:26] [Server thread/INFO] [minecraft/GameTestServer]: ========= 8 GAME TESTS COMPLETE IN 1.826 s ======================
[18:17:26] [Server thread/INFO] [minecraft/GameTestServer]: All 8 required tests passed :)
BUILD SUCCESSFUL in 34s
```

### 3.1 耗时分档

| 场景 | 墙钟 |
|---|---|
| 第一次跑（要下载 3888 个 MC 资源） | **6 min 07 s** |
| 热跑（资源已缓存、无代码改动） | **28–35 s**（其中 8 条测试本身 1.1–1.8 s，服务端起停约 14 s，gradle 配置约 10 s） |
| 只改测试代码（重编译 + 热跑） | 约 45–50 s |

### 3.2 过程中真实出现过的失败（都已修，留档）

| 现象 | 根因 | 修法 |
|---|---|---|
| `Missing test structure: dragonmaid:gametest_empty` → server 崩溃 | 自造 NBT 少了 TAG_End | 重写生成器 + 加了自校验解析器 |
| `IceDragonEntity cannot be cast to DragonState` | dev 里 mixin 未加载（manifest 不生效） | mods.toml 加 `[[mixins]]` |
| `Payload neoforge:advanced_open_screen may not be sent to the client!` | 假连接没有走 NeoForge 握手，`NetworkRegistry.checkPacket` 拒绝自定义 payload | 测试里给连接塞入 `NetworkPayloadSetup`（`ChannelAttributes.setPayloadSetup`），把 `neoforge:advanced_open_screen`、`dragonmaid:compat` 标为已协商 |
| `no accessory slot was unlocked` | 知识等级被 `Knowledge.normalizeLevel` 归一化：等级 1 需要原始知识 ≥300 | 先 `setKnowledge(300)` 再 `setKnowledgeLevel(1)` |
| `addStatue refused to place the statue` | ① IAF 的 `buildStatueEntity` 不复制坐标，雕像落在 (0,0,0) 未加载区块；② 用 `BasicDragonPetrification` 直连会被 `RecallDragonPetrification.preventDuplicateDragon` 取消（龙的 RecallLedger 仍是 WORLD） | ① 测试里先 `moveTo` 到龙的位置；② 改走生产门面 `DragonPetrification`（= `RecallDragonPetrification`，gorgon mixin 用的就是它） |

---

## 4. 测试代码怎么写的（要点，便于后续扩展）

- `@GameTestHolder("dragonmaid") + @PrefixGameTestTemplate(false)` 标在类上，方法用
  `@GameTest(template = "gametest_empty", timeoutTicks = ...)` → 模板 id 就是 `dragonmaid:gametest_empty`。
- **假玩家必须真的注册进 level**：`GameTestHelper.makeMockServerPlayerInLevel()`（deprecated 但可用，
  内部走 `PlayerList.placeNewPlayer` + `EmbeddedChannel`）。理由：龙的主人判定走
  `OwnableEntity#getOwner() → level().getPlayerByUUID(...)`，没注册的 `ServerPlayer` 永远查不到，
  `dragon.isOwnedBy(player)` 恒为 false。
- 测试结束要清场：`discard()` 实体、`PlayerList.remove(player)`，否则残留会污染下一条测试。
- 需要读 private API 时用反射（`bookLearningExperience`），没有为了测试改生产代码的可见性。

---

## 5. 必须人测 / 现阶段测不了的

1. **客户端渲染与动画**（人形模型、龙甲贴图、呼吸粒子、GUI 布局）：GameTest 跑在 dedicated server，
   没有客户端。这部分只能靠 `_probe_dm_client` 实机截图。
2. **网络包真正到达客户端**：测试里是用 `ChannelAttributes.setPayloadSetup` 假装"已协商"，
   能证明服务端**发了**正确的包（`RecallRosterMessage` 等），不能证明客户端**解对了**。
   客户端解码仍需实机（或单独的 encode/decode round-trip 单测）。
3. **祭坛的交互手感 / 距离限制 / 权限**（`stillValid` 的距离与菜单刷新节奏）只做了"能开"。
4. **多 MH 的召回计费/等待队列**（`RecallService.request`+`tick` 的异步 PENDING 流程）没有覆盖，
   需要按 tick 推进的时序测试。
5. **跨存档持久化**（`RecallLedger.flush`/`PetrificationLedger` 落盘再读回）只覆盖了内存态。
6. **附魔书之外的 knowledge 来源**（喂食/知识材料）未覆盖。

---

## 6. 后续可扩展点（按性价比）

1. `@BeforeBatch(batch="recall")` 建一个"已驯服龙 + 假玩家"的公共场景，减少每条测试的建龙开销。
2. `RecallHorn` 的 **failure path**：受伤状态、非主人、手上不是龙角、角里已有龙、右键非上表面。
3. `KnowledgeUpgrades.tryUpgrade` 的等级 1→2→3 门槛（需要 1000/2500 知识与 1/2 本书）与诅咒书分支。
4. `RecallService.request` 的完整时序：用 `helper.runAfterDelay` 推进 tick，断言 `PENDING→SUCCESS/FAIL`。
5. `PetrificationLedger` 落盘 round-trip：`save`→`load`→`active` 一致。
6. 把 game test 接进 CI：`gradle runGameTestServer` 的退出码直接当闸门（0=全过）。

---

## 7. ⚠️ 并发冲突（必须让 Lead 知道）

本次执行期间，**另一个子代理在同一个工作区、同一批文件上并行改动**：

- 18:12 出现 `gametest/ZProbePetrificationTests.java`（诊断用，后被其自行删除）；
- 18:18 起它给 `PetrificationGameTests.java` **追加了第 9 条测试** `savedSnapshotKeepsRecallToken`，
  并修改了生产代码 `recall/RecallTracking.java`、`RecallService.java`、`RecallPersistence.java`
  （ForgeData → NeoForgeData 的 1.21.1 重命名修复）；
- 18:18–18:22 之间它把 `RecallTracking.java` 改成了**中间态**，我的一次验证跑因此 `compileJava FAILED`；
  18:22 它落笔后我再跑，**9/9 全绿**（即 §3 顶部那次）。以下错误现场留档，说明冲突的影响面：

  ```
  src/main/java/com/example/dragonmaid/recall/RecallTracking.java:55: 错误: 未命名变量、未预置功能（默认构造下）
      public static void stamp(CompoundTag snapshot, RecallLedger.Entry entry) {
  src/main/java/com/example/dragonmaid/recall/RecallTracking.java:341: 错误: 需要 class、interface、enum 或 record
  }
  3 个错误
  ```

  → 这是**对方改到一半的语法错误**（多了一个 `}`，方法被解析成字段初始化），不是本套测试的问题；
  对方落笔后同样的命令 `BUILD SUCCESSFUL in 26s`、9/9 通过。以后再遇到，先看 `RecallTracking.java` 括号配平。

**建议**：GameTest 相关目录/`build.gradle`/`neoforge.mods.toml` 同一时间只派一个人写，否则
gradle 与 `build/gametest-*.log` 会互相踩（我这轮就被覆盖过一次日志）。
