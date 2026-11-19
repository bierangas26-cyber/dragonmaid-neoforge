# 基线报告：Dragon Maid Companion → NeoForge 1.21.1（1.2.0-beta.4-neoforge.1）

> 报告性质：**只读侦察**。本文结论均来自读文件 / 列目录 / 查环境，**未运行 gradle、未修改任何源码**。
> 因此"编译 0 error""15/15 通过"这类数字均引自仓库内已有文档（`docs/1.2-VERIFICATION.md`、`docs/GAMETEST-REPORT.md`），不是本轮实测。
> 生成时间：2026-11-19。

## 0. 一句话摘要

| 项 | 值 |
|---|---|
| 仓库分支 / HEAD | `main` / `bbeba7c`（工作区干净，无未提交改动） |
| 版本 | `mod_version=1.2.0-beta.4-neoforge.1`；目标 MC 1.21.1 / NeoForge 21.1.264 / Java 21 |
| 源码规模 | **342 个 .java / 45,504 行 / 35 个包**；其中 mixin 70 个（48 服务端 + 22 客户端） |
| 资源规模 | 451 个文件（assets 352 / data 95 / 根 3 / META-INF 1） |
| 事件钩子 | 77 处 `@SubscribeEvent`、39 个 `@EventBusSubscriber` 类 |
| 构建 | 无 Gradle wrapper；用本机解压好的 **Gradle 8.8** + **JDK 21**；`gradle build` |
| 测试 | **15 条 GameTest**，`gradle runGameTestServer`，退出码 = 失败数 |
| 当前最大风险 | `libs/` 只有 README（**0 个依赖 jar**）→ 现在直接构建必失败；且 IAF 必须是 **CE 2.2**，本机测试服目录里只有 2.0 |

> 仓库已从旧路径 `D:/BaiduNetdiskDownload/projects/medieval-world/dragonmaid-port` 迁到 `dragonmaid-neoforge-1.21.1`；
> `docs/` 里大量历史记录仍写着旧路径，读文档时注意换算。

---

## 1. 构建环境

### 1.1 Gradle

| 项 | 事实 | 证据 |
|---|---|---|
| Gradle Wrapper | **没有**。仓库根无 `gradlew` / `gradlew.bat` / `gradle/` 目录 | `Get-ChildItem <repo> -Filter gradlew*` 为空；根目录只有 4 个文件 + 5 个目录 |
| `where gradle` / `Get-Command gradle` | **无结果**（PATH 里没有 gradle） | 实测 |
| 本机可用 gradle | `C:/Users/LIUHZ/.gradle/wrapper/dists/gradle-8.8-bin/dl7vupf4psengwqhwktix4v1/gradle-8.8/bin/gradle.bat` | 唯一的 `gradle.bat`（已解压）。`wrapper/dists` 下另有 `gradle-9.1.0-bin` / `gradle-9.4.1-bin`，**未解压出 bin**，且 moddev 2.0.141 对 Gradle 9 未经验证 |
| 历史构建命令 | docs 里固定用这条 8.8 路径 + `--no-daemon` | `docs/PORT-STATUS.md` §7、`docs/GAMETEST-REPORT.md` §1.1、`docs/1.2-VERIFICATION.md` §复现方式 |

### 1.2 JDK

| 项 | 值 | 说明 |
|---|---|---|
| 当前 `JAVA_HOME` | `D:/jdk11/` | **错的**，1.21.1 不能用 JDK 11 编译（`options.release = 21`） |
| 工程指定 JDK | `gradle.properties`: `org.gradle.java.installations.paths=D:/pcl/voxy1.7/jdk/jdk21` | 已实测该路径存在 `bin/java.exe` |
| `build.gradle` | `java.toolchain.languageVersion = JavaLanguageVersion.of(21)`、`options.release = 21` | 强制 21 |
| 其它 | `D:/jdk17/` 也在，但不满足 21 | —— |

**建议**：构建前显式 `$env:JAVA_HOME="D:/pcl/voxy1.7/jdk/jdk21"`（历史文档就这么做），只靠 `installations.paths` 兜底不稳。

### 1.3 代理与 Gradle JVM 参数

`gradle.properties` 已写死本地代理（7897）：

```
systemProp.http.proxyHost=127.0.0.1 / proxyPort=7897
systemProp.https.proxyHost=127.0.0.1 / proxyPort=7897
systemProp.http.nonProxyHosts=localhost|127.*|[::1]（https 同）
org.gradle.jvmargs=-Xmx2G -XX:MaxMetaspaceSize=768m
org.gradle.daemon=false      # 所以 --no-daemon 冗余但无害
org.gradle.parallel=false
org.gradle.caching=true
```

代理不通时首次 `createMinecraftArtifacts` 会卡在下载阶段 —— 但本机 `~/.gradle/caches/modules-2/files-2.1/net.neoforged/` 里**已有 `neoforge/21.1.264` 与 `moddev-gradle` 缓存**，大概率可省掉大部分网络请求。

### 1.4 依赖坐标与仓库源

`build.gradle` 用的是 **ModDevGradle 2.0.141**（`plugins { id "net.neoforged.moddev" version "2.0.141" }`），不是 ForgeGradle。

| 类别 | 坐标 / 写法 |
|---|---|
| NeoForge | `neoForge { version = project.neoforge_version }` → **21.1.264**（`gradle.properties`） |
| Minecraft | 1.21.1（同上） |
| 第三方 API | `compileOnly fileTree(dir: "libs", include: ["*.jar"])` —— 全部靠 `libs/` 里的裸 jar，**不写 maven 坐标** |
| GameTest 运行期 | `gametestModJars().each { runtimeOnly files(it) }`，见下 |
| 仓库源 | `pluginManagement`：`https://maven.neoforged.net/releases` + `gradlePluginPortal()` + `mavenCentral()`；项目仓库：`mavenCentral()` + `flatDir { dirs "libs" }` |

**`libs/` 需要的 8 个 jar**（`libs/README.md` 定义，全部被 `.gitignore` 的 `libs/*.jar` 排除，不随仓库分发）：

| jar | 版本 | 必需性 |
|---|---|---|
| `iceandfire-2.2.jar` | IAF **Community Edition 2.2**（1.21.1 NeoForge） | **硬必需**，且必须是 2.2（见 §5.2） |
| `uranus-2.4.1-1.21.1-neoforge.jar` | 2.4.1 | 必需（IAF 前置） |
| `jupiter-2.3.7-1.21.1-neoforge.jar` | 2.3.7 | 必需（配置库） |
| `geckolib-neoforge-1.21.1-4.9.2.jar` | 4.9.2 | 必需（动画） |
| `Patchouli-1.21.1-93-NEOFORGE.jar` | 93 | 必需（手册） |
| `jei-1.21.1-neoforge-19.70.0.458.jar` | 19.70.0.458 | 可选（编译参考） |
| `Jade-1.21.1-NeoForge-15.10.6.jar` | 15.10.6 | 可选（Jade 插件 / 蛋性别提示） |
| `citadel-2.7.1-1.21.1.jar` | 2.7.1 | **已废弃**：IAF CE 2.2 起不需要 Citadel（gametest 名单里也已删掉） |

**GameTest 运行期 jar 解析顺序**（`gametestModJars()`）：

1. `libs/` 下按固定文件名找；
2. 找不到再去 `providers.gradleProperty("gameTestModsDir")`，默认 `D:/mc-model/work/_probe_dm/mods`（可用 `-PgameTestModsDir=<dir>` 覆盖）；
3. 再找不到就 `logger.lifecycle("[dragonmaid] GameTest runtime mod not found, skipping: ...")` **静默跳过** —— 随后 FML 会因 required 依赖缺失拒绝加载本 mod，GameTest 一条都注册不上。

名单：`geckolib-neoforge-1.21.1-4.9.2.jar`、`iceandfire-2.2.jar`、`uranus-2.4.1-1.21.1-neoforge.jar`、`jupiter-2.3.7-1.21.1-neoforge.jar`、`architectury-13.0.11-neoforge.jar`、`Patchouli-1.21.1-93-NEOFORGE.jar`。

### 1.5 当前构建可行性（关键结论）

- **`libs/` 里现在 0 个 jar**（只有 `README.md`）→ `compileOnly fileTree("libs")` 为空集。IAF CE / GeckoLib / Patchouli 的 import 全部找不到 → `compileJava` 会炸出成千条 `package does not exist`。**必须先补齐 `libs/`。**
- 本机可直接复用的副本（无需联网）：
  - 旧仓库 `D:/BaiduNetdiskDownload/projects/medieval-world/dragonmaid-port/libs/` **8 个 jar 齐全**，其中 `iceandfire-2.2.jar` 为 20,952,409 B；
  - 单独的 2.2 jar：`D:/mc-model/work/_port_src/iaf22/IceAndFireCE-2.2-1.21.1-neoforge.jar`（同尺寸，sha256 `1E888CAAF18771B9AA591D832EE7FE20EB0013B66C6D85096676F2843E4D6833`）；
  - 其余 jar 在 `D:/mc-model/work/_probe_dm/mods/` 也有。
- **注意**：`D:/mc-model/work/_probe_dm/mods` 里只有 `iceandfire-2.0.jar`（20,774,271 B），**没有 2.2**。若只依赖这个默认回退目录，GameTest 会跳过 IAF 并最终因依赖缺失失败。
- `build/` 与 `run/` 都不存在（`.gitignore` 排除），是干净检出：首次构建要跑 `createMinecraftArtifacts`，首次测试要生成 `run/gametest` 世界。

### 1.6 构建 / 运行命令（推荐写法）

```powershell
$env:JAVA_HOME = "D:/pcl/voxy1.7/jdk/jdk21"
$G = "C:/Users/LIUHZ/.gradle/wrapper/dists/gradle-8.8-bin/dl7vupf4psengwqhwktix4v1/gradle-8.8/bin/gradle.bat"
cd D:/BaiduNetdiskDownload/projects/dragonmaid-neoforge-1.21.1

# 0) 先补齐 libs/*.jar（见 §1.5），再执行下面任意一条

# 编译（只验类型）
& $G compileJava --no-daemon --console=plain

# 打包（产物 build/libs/dragonmaid-neoforge-1.21.1-1.2.0-beta.4-neoforge.1.jar）
& $G build --no-daemon --console=plain

# 只要 jar + 打印大小
& $G jarInfo --no-daemon --console=plain
& $G buildInfo --no-daemon --console=plain     # 额外打印 excluded.txt 条目数

# GameTest（退出码 = 失败的 required 测试数，0 = BUILD SUCCESSFUL）
& $G runGameTestServer --no-daemon --console=plain

# 开发服务端 / 客户端
& $G runServer --no-daemon --console=plain
& $G runClient --no-daemon --console=plain
```

自定义任务（`build.gradle` 已定义）：`jarInfo`、`buildInfo`；`jar` 任务的 manifest 会写 `MixinConfigs: dragonmaid.mixins.json`。

`processResources` 还有两段构建期改写（对当前源树基本是 no-op，但仍生效）：把 `data/<ns>/{advancements,recipes,loot_tables,predicates,item_modifiers,functions,structures}` 与 `tags/{blocks,items,entity_types,fluids,game_events,functions}` 复制成 1.21 单数别名；再对生成的 `recipe/`、`advancement/` 别名副本做 `"result":{"item"}` / `"icon":{"item"}` → `"id"` 的正则替换（**原始 1.20.1 复数文件保持逐字节不变**，这是"资产 100% 一致"策略的配套机制）。

---

## 2. 源码结构

### 2.1 包树与类数（342 个 .java）

| 包 | 类数 | 职责 |
|---|---:|---|
| `com.example.dragonmaid.client` | 61 | 客户端渲染/模型/GUI/粒子/输入 |
| `com.example.dragonmaid.mixin` | 48 | 服务端 mixin |
| `com.example.dragonmaid.combat` | 45 | 战斗、护卫、龙息、甩尾、迷你化战斗 |
| `com.example.dragonmaid.mixin.client` | 22 | 客户端 mixin |
| `com.example.dragonmaid.network` | 18 | 单向消息 record（encode/decode/handle） |
| `com.example.dragonmaid.compat` | 17 | Forge→NeoForge 语义垫片层（见 §3.6） |
| `com.example.dragonmaid.item` | 16 | 物品 + `ModItems` 注册 |
| `com.example.dragonmaid.recall` | 13 | 龙角召回、账本、持久化、客户端内容 |
| `com.example.dragonmaid.data` | 10 | `DragonState/DragonForms/DragonEvents/HumanoidLife/...` |
| `com.example.dragonmaid.gametest` | 10 | GameTest 9 类 + `GameTestSupport` |
| `com.example.dragonmaid.accessory` | 9 | 饰品/装备特效 |
| `com.example.dragonmaid.movement` | 8 | 人形门 / 栖息飞行 |
| `com.example.dragonmaid.compat.net` | 7 | `SimpleChannel` 单 payload 适配层 |
| `com.example.dragonmaid.recipe` | 7 | 配方 + `ModRecipes` |
| `com.example.dragonmaid.menu` | 6 | 菜单 + `ModMenus` |
| `com.example.dragonmaid`（根） | 5 | `DragonMaid` / `PortBootstrap` / `Config` / `DragonMaidConfig` / `BuildFeatures` |
| `com.example.dragonmaid.petrification` | 5 | 石化 |
| `com.example.dragonmaid.learning` | 4 | 知识升级 |
| `command` / `compat.jade` / `egg` | 3 / 3 / 3 | 命令、Jade 插件、龙蛋性别 |
| `advancement` / `animation` / `config` / `entity` / `inventory` / `com.github.alexthe666.iceandfire.entity.props` / `iafcompat` | 各 2 | —— |
| `block` / `brewing` / `compat.jei` / `particle` / `com.github.alexthe666.iceandfire.message` / `com.github.alexthe666.iceandfire.world` | 各 1 | —— |

统计口径补充：**70 个 `@Mixin`**、**77 处 `@SubscribeEvent`**、**39 个 `@EventBusSubscriber` 类**。最大文件：`combat/TailSweepGeometry.java`（240 KB，纯数据表）、`recall/RecallService.java`（68 KB）、`recall/RecallDragonPetrification.java`（52 KB）、`mixin/DragonMixin.java`（49 KB）。

### 2.2 关键系统入口

| 系统 | 位置 | 说明 |
|---|---|---|
| **@Mod 主类** | `com/example/dragonmaid/DragonMaid.java`（`MODID="dragonmaid"`） | 构造函数里完成全部注册（见 §6.2） |
| 第二个 @Mod | `com/example/dragonmaid/PortBootstrap.java` | 同 modid，只打一行日志，是早期"资产阶段"遗留（GAP-AUDIT #11，两个构造器都会跑） |
| 物品/方块 | `item/ModItems`、根类里的 `BLOCKS`（`compat.Reg<Block>`）、`recall/RecallContent`（祭坛方块） | `compat.Reg/RO` 垫片包住 `DeferredRegister/DeferredHolder` |
| 实体 | `entity/ModEntities` | 只有 1 个自建 item 实体；龙实体来自 IAF CE |
| 菜单 | `menu/ModMenus` | Knowledge / Accessories / Recall / Dragon |
| 粒子 | `particle/ModParticles` | 12 个 particle type |
| 配方 | `recipe/ModRecipes` | 自定义 serializer + `StrengthPotionIngredient` |
| 网络 | `network/DragonNetwork` + `compat/net/SimpleChannel` + `network/*Message`(18) | id 0–9、12–16 共 15 条注册；协议版本 `"21"`；单个 `CompatPayload`（RL `dragonmaid:compat`）承载全部消息 |
| 网络注册事件 | `DragonNetwork.registerPayloads(RegisterPayloadHandlersEvent)`（在 `DragonMaid` 构造函数里 `modEventBus.addListener`） | 只 bootstrap 一次（`payloadRegistered` 幂等） |
| 客户端初始化 | `client/ClientRegistration`（`@EventBusSubscriber(modid=..., value=Dist.CLIENT, bus=Bus.MOD)`） | 粒子 provider、菜单屏、EntityRenderer（`EntityRenderersEvent.AddLayers`，27 个女性龙渲染器）、`RegisterClientReloadListenersEvent`、`FMLClientSetupEvent` 里的 item properties |
| 客户端游戏事件 | `client/ClientEvents`（8 个 `@SubscribeEvent`） | clientTick、渲染帧前后、登出、Level 卸载 |
| 客户端↔服务端桥 | `compat/ClientBridge.java` | 7 处反射 + `catch (Throwable) {}` 静默吞异常（GAP-AUDIT #15，脆弱但当前可用） |
| 命令 | `command/CompanionCommands`（`RegisterCommandsEvent`） | `/dragonmaid ...`（get/set state、hunger、inventory 等） |
| mixin 配置 | `src/main/resources/dragonmaid.mixins.json` | `package: com.example.dragonmaid.mixin`，`compatibilityLevel: JAVA_21`，`required: true`，`injectors.defaultRequire: 1`；`mixins` 48 条 + `client` 22 条，与源文件**逐一对齐**（70 = 70） |
| mixin 目录 | `src/main/java/com/example/dragonmaid/mixin/`（48）+ `mixin/client/`（22） | 客户端 mixin 必须写在 `client` 数组里 |
| 配置 | `DragonMaidConfig`（server-synced，`dragonmaid-sync-server.toml`）、`config/GlobalBalanceConfig`、`config/CompanionBalanceConfig`、`recall` 的 recall-server/common | 见 §3.3 的坑：**禁止重复注册同名 spec** |

### 2.3 资源结构（451 文件）

| 区域 | 文件数 | 备注 |
|---|---:|---|
| `assets/dragonmaid/` | 352 | `animations` 30、`geo` 21、`models/item` 39、`textures/entity` 64、`textures/item` 32、`patchouli_books`（en_us 56 + zh_cn 55）、`particles` 11 等 |
| `data/dragonmaid/` | 92 | 目录已是 1.21 **单数**：`advancement`(4+25+1+27)、`loot_table`(1+1)、`recipe`(28)、`structure`(1)、`tags/item`(4) |
| `data/minecraft/tags/block/` | 2 | `mineable/` 1 + 1；旧版 `tags/blocks` 已在二轮审计中改单数 |
| 根 | 3 | `pack.mcmeta`、`dragonmaid.png`、`dragonmaid.mixins.json` |
| `META-INF` | 1 | `neoforge.mods.toml`（含 `[[mixins]]`，见 §4.3） |
| 其它 | 1 | `assets/forgeconfigscreens/lang/` —— 第三方配置界面兼容的遗留资源 |

`docs/asset-sha256.md`（64 KB）是上游 359 个内容文件的逐文件 sha256 对照表；二轮审计结论：上游 446 个 assets/data 条目按 1.21 单数化映射后 **0 缺失、0 大小差异**（`docs/1.2-VERIFICATION.md`）。

---

## 3. 移植约定（从 PORT-STATUS / PORT-GAP-AUDIT / IAF-CE-MAPPING / PORT-CLIENT + 代码实证提炼）

### 3.1 Forge → NeoForge 命名/API 映射惯例

| Forge 1.20.1 | 本仓写法（NeoForge 1.21.1） | 证据 |
|---|---|---|
| `@Mod.EventBusSubscriber` | `net.neoforged.fml.common.EventBusSubscriber(modid=..., bus=Bus.MOD / Bus.GAME, value=Dist.CLIENT)` | 39 个类，例：`client/ClientRegistration` |
| `FMLCommonSetupEvent` / `FMLClientSetupEvent` | 同名，走 MOD bus；`event.enqueueWork(...)` 包延迟工作 | `DragonMaid#commonSetup` |
| `DeferredRegister` + `RegistryObject` | 仍用 `DeferredRegister`/`DeferredHolder`，但经 `compat/Reg`+`compat/RO` 垫片暴露 `RO.get()/getId()/isPresent()`，少改上游代码 | `compat/Reg.java`、`compat/RO.java` |
| `ForgeConfigSpec` | `ModConfigSpec` + `modContainer.registerConfig(ModConfig.Type.SERVER, SPEC, "<file>.toml")` | `DragonMaid` 构造函数 |
| `ItemStack.getTag()/getOrCreateTag()` | `DataComponents.CUSTOM_DATA`；读 = `StackNbt.getTag()`（**副本**），写 = `StackNbt.getOrCreateTag()`（返回会回写的 `WriteBackTag`） | `compat/StackNbt.java`、`compat/WriteBackTag.java` |
| `entity.getPersistentData()`（ForgeData） | 仍用 `getPersistentData()`（1.21 原生 API，语义同） | GAP-AUDIT 修复记录（ForgeData→NeoForgeData） |
| `SimpleChannel` 网络 | `compat/net/SimpleChannel` + 单个 `CompatPayload(int id, byte[] data)`；wire 层只写一次 id | `compat/net/SimpleChannel.java` 注释记录了"多写一次 id"导致的 `ResourceLocationException` 事故 |
| `AttributeModifier(name, amount, op)` | `AttributeModifier(ResourceLocation, double, Operation)` | GAP-AUDIT §5 |
| `MultipartPartEntity`（IAF 2.0） | NeoForge `PartEntity<T>`（IAF CE 2.2） | `mixin/DragonPartInjuryMixin.java` |
| 旧式 `new ResourceLocation(...)` | `ResourceLocation.fromNamespaceAndPath(...)`、`ResourceKey` 等 1.21 写法 | 全库 import |

### 3.2 mixin 写法约定

1. **位置**：服务端 `com.example.dragonmaid.mixin`，客户端 `com.example.dragonmaid.mixin.client`。
2. **三处登记缺一不可**：源文件 → `dragonmaid.mixins.json` 的 `mixins`/`client` 数组 → `META-INF/neoforge.mods.toml` 的 `[[mixins]] config="dragonmaid.mixins.json"`。
   第三处是**开发环境**专用：`MixinConfigs` 只写在 jar manifest 里，而 `runServer/runClient/runGameTestServer` 把 `build/classes` 当 mod 加载，读不到 manifest → 所有 mixin 不生效（症状：`IceDragonEntity cannot be cast to DragonState`）。见 `docs/GAMETEST-REPORT.md` §1.4。
3. **第三方目标必须 `@Mixin(remap = false, value = {...})`**（IAF CE / GeckoLib / Patchouli / Jade 都是 `remap=false`）。
4. **`this` 中转**：mixin 类编译期父类仍是 `Entity`，访问目标类成员要写 `(DragonBaseEntity)(Object)this`（`DragonMixin` 里有 43 处；`docs/IAF-CE-MAPPING.md` §7 第 9 条）。
5. **可选目标用 `@Pseudo` + `require = 0`**：`injectors.defaultRequire = 1`，目标缺失 = 启动硬崩。7 个 `ForgeConfig*` 与 `JadeDragonArmorMixin` 就是按 `@Pseudo` 静默跳过处理的。
6. **不要绑 lambda 名**：`DragonForgeBreathMixin` 绑了 `lambda$destroyAreaBreath$0/$1`，上游一重编就改名（GAP-AUDIT #17，已知脆弱项）。
7. **`@Shadow` 字段被删就改方法**：`Entity.fireTicks` 在 1.21.1 变成 `getRemainingFireTicks()/setRemainingFireTicks(int)`（`IAF-CE-MAPPING.md` §5 第 2 条）。
8. **`@Unique` 死方法要清**：`DragonHornMixin` 里整套 1.20.1 抓龙实现已无人调用（GAP-AUDIT #9）。

### 3.3 1.21 资源与数据路径单数化约定

- 数据目录：`data/<ns>/advancement|recipe|loot_table|predicate|item_modifier|function|structure`；tag：`data/<ns>/tags/item|block|entity_type|fluid|game_event|function`。
- 原版 tag：`data/minecraft/tags/block/...`（不是 `tags/blocks`）—— 否则挖掘工具等级判定不生效（1.2 二轮审计修过）。
- `pack.mcmeta` 必须是 1.21 格式（资源 34 / 数据 48），这是相对上游**唯一有意的字节偏离**（`docs/PORT-STATUS.md` §6-P0）。
- 配方/进度 JSON 里 `result` / `icon` 的堆叠写法在 1.21 是 `{"id": ...}` 而非 `{"item": ...}`。
- 构建期仍保留"复数→单数别名 + item→id 重写"的兜底逻辑（`build.gradle` 的 `DATA_DIR_RENAMES` / `TAG_DIR_RENAMES`），改资源时**直接写单数**，别依赖兜底。

### 3.4 IAF CE 2.2 兼容层位置

| 文件 | 作用 |
|---|---|
| `iafcompat/IafCompat.java` | CE 桥：召唤水晶 / 食物 / NoAI / 菜单 / 火焰 tick / 喂食 |
| `iafcompat/EntityDataCompat.java` | 替代 IAF 2.x 的 citadel `EntityData`/`EntityDataProvider`（用 `getPersistentData()` + 1.21 原生冻结 tick） |
| `compat/IafCompatConfig.java` | `IafConfig` → `IafCommonConfig.INSTANCE.dragon.*.getValue()` 垫片 |
| `compat/IafProxy.java` | CE 无 `CommonProxy` 的替代入口 |
| `mixin/DragonPartInjuryMixin.java` | CE 2.2 `PartEntity` 改写（2.0/2.1 写法会 `InvalidMixinException`） |
| `com/github/alexthe666/iceandfire/*`（4 个） | 已删 API 的**编译期桩**，仅让旧代码过编译；当前无调用者 |
| `com/eliotlash/mclib/math/{IValue,Constant}.java` | GeckoLib 4.9 移除 mclib 后的编译期桩；无调用者 |

版本门槛：`neoforge.mods.toml` 的 `iceandfire` 版本范围是 **`[2.2,)`**，`2.0/2.1.x` 会启动即崩（`docs/IAFCE-2.2-COMPAT.md`）。

### 3.5 GeckoLib 4.9 / 渲染约定（PORT-CLIENT 提炼）

- `RenderUtils` → `RenderUtil`；`preRender/postRender` 颜色参数从 4×float 变 **1 个打包 int**。
- `core.molang.MolangParser` 删除 → 用 `loading.math.MolangQueries`（本仓 `ClientRegistration.clearWorldState()` 改用 `MolangQueries.clearActor()`）。
- `Keyframe<T extends MathValue>`：`com.eliotlash.mclib.math.IValue` → `software.bernie.geckolib.loading.math.MathValue`。
- **geo JSON 必须用 GeckoLib 自己的 Gson**：`KeyFramesAdapter.GEO_GSON`（`client/GeoModelJson.GSON` 直接引它）；裸 `new Gson()` 会得到 `Model.minecraftGeometry() == null` → NPE（两层症状一个根因，`PORT-CLIENT.md` §13.1）。
- 1.21.1 `VertexConsumer` 只有 6 个抽象方法：`addVertex / setColor / setUv / setUv1 / setUv2 / setNormal`；**`endVertex()` 已删除**，收集型消费者要自己 flush 最后一个顶点。
- `PartEntity` 的 `getParent()` 是 NeoForge 直接字段访问，不再有 `getParentId()`。

### 3.6 垫片层（`compat/`）的设计约定

新增/移植上游代码时，凡遇到"Forge 有、NeoForge 没有"的语义，**先看 `compat/` 里有没有现成垫片**，没有再加一个，而不是到处改上游调用点：

`Reg` / `RO` / `StackNbt` / `WriteBackTag` / `StackCodec` / `NetBuf` / `DistExecutor` / `FMLContext` / `ClientBridge` / `GeoJson` / `Enchants` / `CompanionRelations` / `ForgeCooperation` / `HumanoidForge` / `HumanoidMeleeAnimationEvent` / `IafProxy` / `IafCompatConfig` + `compat/net/*`（7 个）。

---

## 4. 测试资产

### 4.1 GameTest 清单（15 条，全部 required；9 个测试类 + 1 个 support）

统一注解：类上 `@GameTestHolder("dragonmaid")` + `@PrefixGameTestTemplate(false)`；方法上 `@GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = ...)`，模板即 `dragonmaid:gametest_empty`。

| # | 测试类 :: 方法 | 覆盖点 | timeoutTicks |
|---|---|---|---:|
| 1 | `DataLoadGameTests.recipesLoaded` | 19 个 `dragonmaid:*` 配方加载；`kaishaku_blade` / `reinforced_dragon_horn` 用自定义 serializer | 100 |
| 2 | `DataLoadGameTests.rootAdvancementLoaded` | `dragonmaid:root` + closer_companion / maid_for_hunting / trinity_of_dragons | 100 |
| 3 | `DataLoadGameTests.mercyKillingTagLoaded` | 物品 tag `dragonmaid:mercy_killing_tools` 含 kaishaku_blade / test_execution_blade | 100 |
| 4 | `RecallHornGameTests.captureAndReleaseKeepsUuid` | 强化龙角收龙（写 DRAGON_HORN 组件）→ 放龙，**UUID 不变** | 300 |
| 5 | `AccessoryGameTests.wardingEmblemRaisesArmor` | 知识 300 + Lv1 解锁槽 → 装 warding_emblem → `ARMOR` 至少 +(clamp(stage,1,5)+1)，移除后回基准 | 200 |
| 6 | `KnowledgeUpgradeGameTests.enchantedBookYieldsLearningExperience` | `STORED_ENCHANTMENTS` 附魔书 → `bookKind==REGULAR`、`bookLearningExperience>0`；空书被拒 | 100 |
| 7 | `AltarInteractionGameTests.altarOpensRecallMenu` | 放 `dragonmaid:dragon_recall_altar` → `useWithoutItem` 返回可消费、`player.containerMenu instanceof RecallMenu`、坐标正确 | 200 |
| 8 | `PetrificationGameTests.statueRoundTripKeepsDragon` | 石化 → 雕像标记含 uuid+Token → 龙离场 + ledger active → `petrification_remedy` 复原，UUID 不变 | 300 |
| 9 | `PetrificationGameTests.savedSnapshotKeepsRecallToken` | 存档快照保留 recall token（ForgeData→NeoForgeData 回归） | 100 |
| 10 | `MiniatureGameTests.faerieWishTogglesMiniature` | 仙灵之愿迷你化开关 | 200 |
| 11 | `BackpackGameTests.bagAutoFeedsHungryDragon` | 饥饿 0 时背包自动进食（每次 1 个、+30） | 200 |
| 12 | `BackpackGameTests.pickupModeCycleAndFiltering` | 拾取模式 OFF→MATCHING→ALL→OFF 循环与过滤 | 200 |
| 13 | `BackpackGameTests.knowledgeGatesBagSlots` | 知识等级门控背包槽位 | 200 |
| 14 | `HumanoidWaterGameTests.humanoidIceDragonSwimsAndDives` | 人形冰龙入水游泳/下潜 | 200 |
| 15 | `HumanoidWaterGameTests.dryHumanoidFallsBackToVanillaTravel` | 离开水后回落到原版 `travel` | 200 |

共享夹具 `gametest/GameTestSupport.java`：

- `TEMPLATE = "gametest_empty"`、`CENTRE = (2,2,2)`；
- `mockPlayer()` 用 `helper.makeMockServerPlayerInLevel()`（必须"真的注册进 level"，否则 `OwnableEntity#getOwner()` 查不到 → 所有龙交互被拒）；
- `spawnTamedDragon()`、`releasePlayer()`（测试必须清场，避免污染下一条）。

### 4.2 运行方式

```powershell
& "C:/Users/LIUHZ/.gradle/wrapper/dists/gradle-8.8-bin/dl7vupf4psengwqhwktix4v1/gradle-8.8/bin/gradle.bat" `
  runGameTestServer --no-daemon --console=plain
```

- `build.gradle` 已定义 `gameTestServer` run：`type = "gameTestServer"`、`gameDirectory = run/gametest`、`systemProperty("neoforge.enabledGameTestNamespaces", "dragonmaid")`、`-Xms1G -Xmx2G -Dfile.encoding=UTF-8`。**支持 `runGameTestServer`，无需额外配置。**
- **退出码 = 失败的 required 测试数**（0 → `BUILD SUCCESSFUL`），可直接当 CI 闸门。
- 结构模板：`src/main/resources/data/dragonmaid/structure/gametest_empty.nbt`（5×5×5 全空气，gzip NBT，DataVersion 3955）。NeoForge 21.1.x **没有 `@EmptyTemplate`**，必须真给 .nbt，写坏了会 `Missing test structure` → 整个 game test server 崩。
- 参考耗时（docs）：首次要下载 MC 资源约 6 分钟；热跑 28–35 s（测试本体 1.1–1.8 s）；只改测试代码约 45–50 s。
- 历史结果：2026-11-01 基线 9/9；2026-11-08 增至 15/15；已发布 jar 对应 9/9（源码与 jar 的差异仅在测试类/注释）。

### 4.3 服务端启动验证

```powershell
& $G runServer --no-daemon --console=plain
```

- run 配置：`gameDirectory = run/server`、`--nogui`、`-Xms1G -Xmx2G -Dfile.encoding=UTF-8`。
- 判据（从历史证据提炼）：日志出现 `Done (…s)! For help, type "help"`；无 `Mixin apply ... failed` / `InvalidMixinException` / `No refMap loaded`；`run/server/crash-reports/` 为空；日志里 `dragonmaid` 相关 ERROR/WARN = 0；`config/` 下生成 4 个 toml（`dragonmaid-sync-server.toml`、`dragonmaid-common.toml`、`dragonmaid-recall-common.toml`、`dragonmaid-recall-server.toml`）。
- 线上验收还用了 RCON（`/summon`、`data get entity`、`forceload`）。要点（`docs/PORT-STATUS.md` §8 的关键教训）：**必须先 `forceload add` 且跨 ≥1 tick 再查询**，否则 `@e` 找不到实体，会误判成"召唤后立即消失"。

### 4.4 GameTest 覆盖不到的部分（必须人测）

客户端渲染/模型/贴图/动画、GUI 布局与手感、真实网络解码（测试里用 `ChannelAttributes.setPayloadSetup` 假装已协商，只能证明"服务端发了"）、`RecallService` 的 PENDING 时序、`RecallLedger/PetrificationLedger` 落盘 round-trip、喂食之外的 knowledge 来源（`docs/GAMETEST-REPORT.md` §5）。

---

## 5. 已知缺口 / TODO

代码里的 `TODO/FIXME` 只有 **3 行**，所以缺口主要来自审计文档与已知限制。按"会不会咬人"分档。

### 5.1 A 档：结构性遗留（当前不阻塞，但改代码时会撞上）

| # | 问题 | 位置 | 影响 |
|---|---|---|---|
| A1 | **两个 `@Mod` 同 modid 同时生效**：`PortBootstrap`（只打日志）+ `DragonMaid`（真注册） | `PortBootstrap.java` | 两个构造器都执行；误导排查（GAP-AUDIT #11，未修） |
| A2 | 网络 7 条消息走 `ClientBridge` 反射 + `catch (Throwable) {}` | `compat/ClientBridge.java` | 客户端侧失败**完全静默**（GAP-AUDIT #15，建议改静态调用） |
| A3 | 死代码/桩：`compat/FMLContext.get()` 恒 null、`StackNbt.getTagElement/isSameItemSameTags` 无调用、`BasicDragonPetrification.placeStatue()` 无人调用、`DragonHornMixin` 的 `@Unique` 抓龙实现从不被调 | 多处 | GAP-AUDIT #9/#12/#13/#14 |
| A4 | 编译期桩 `com/github/alexthe666/*`（4 个）+ `com/eliotlash/mclib/*`（2 个）**全库无调用者** | 见 §3.4 | 可以删；删之前确认没有 mixin 引用旧描述符 |
| A5 | 文档数字口径差异：`1.2-VERIFICATION` 记 478 classes / 已发布 jar 482 classes，当前源文件 342 个 | —— | 342 源文件编译出 478 个 class 属正常（含 record/内部类） |

### 5.2 B 档：功能未对齐 / 只部分对齐

| # | 缺口 | 证据 | 状态 |
|---|---|---|---|
| B1 | IAF **2.2 是硬门槛**：`MultipartPartEntity` 从 `Entity` 变 `PartEntity`，旧 `@Shadow getParentId()` 会 `InvalidMixinException`（加载即崩） | `docs/IAFCE-2.2-COMPAT.md` | 已按 2.2 改；但 `libs/` 与默认 gametest 目录里只有 2.0 → **本机现在无法编译** |
| B2 | `RecallHorn` 仍用自建 legacy NBT（`EntityTag` / `DragonHornEntityID` / `DragonMaidRecall`），没迁 IAF `DRAGON_HORN` 组件 | GAP-AUDIT #10 | 自洽但与 CE 原生 UI/释放语义不一致；迁移需同时迁 `RecallLedger` 的 HORN 快照（中等改造，未排期） |
| B3 | 绑定龙的三龙宝石 tooltip 读 `custom_data.Dragon.CustomName`，CE 放在 `CRYSTAL_DRAGON_DATA` 组件 | GAP-AUDIT #8 | 纯外观；CE key 约定未确认，未修 |
| B4 | 龙焰锻造 mixin 绑 lambda 名，且 handler 形参比约定少一个 `LivingEntity` | GAP-AUDIT #17 | 存疑；需实机让龙对龙炉喷一次并看 `InvalidInjectionException` |
| B5 | 龙甲只逐一验过 **silver 头**；gold / diamond / dragon_steel_* 与 body / tail 未验 | `README.md`、`1.2-VERIFICATION.md` §已知限制 | 未验证 |
| B6 | 7 个 `ForgeConfig*` 客户端 mixin 的目标模组没有 1.21.1 NeoForge 版 | `1.2-VERIFICATION.md` | `@Pseudo` 静默跳过，不影响 NeoForge 自带配置界面 |
| B7 | IAF CE 已删 API 的替代：`EntityData/EntityDataProvider`（citadel capability）→ 自建 `EntityDataCompat`；`CommonProxy`、`MessageUpdateDragonforge` 标不支持；龙甲枚举拆成 `DragonArmorPart` + `DragonArmorMaterial`；`getSummoningCrystal()` → `DragonType.getCrystalItem()`；`isAiDisabled()` → `isNoAi()` | `docs/IAF-CE-MAPPING.md` §2/§5 | 已改写；龙炉同步完全交给 CE |
| B8 | 上游 `eatFoodBonus(ItemStack)` 本身是空实现（`// FIXME :: ?`） | `inventory/DragonBagFeeding.java:81` | 本移植不调用该钩子，自动进食走自实现，行为等价 |
| B9 | 代码级 TODO：`hurtAndBreak` 在 1.21.1 需要 `ServerLevel`，**破坏音效 TODO**；进食粒子/声音/饥饿由自实现接管 | `combat/HumanoidCombat.java:99`、`inventory/DragonBagFeeding.java:24` | 已知，不影响可玩性 |

### 5.3 C 档：验证面缺口（文档明确"未做"）

- 多人长跑 / 服务器压力 / 与其它整合包组合兼容性未验证；
- 旧存档升级（1.1 存档进 1.2）未验证，只验了新世界；
- 人形化形后的**逐形态外观**未逐个截图核对（只验了知识 Lv1 冰龙）；
- 装备槽**拖拽**是人工路径，自动化未覆盖；
- 龙甲其它材质/部位（见 B5）；
- 龙息/爪击/甩尾的"手感"需人判断；
- 龙炉吐息加热链路未端到端验证。

### 5.4 与 PORT-STATUS 旧状态的**关键差异**（务必以本仓为准）

`docs/PORT-STATUS.md` 记录的是 1.1.0-beta.2 阶段：155 个源文件被 `sourceSets` exclude、只剩 79 个文件编译、`DragonMaid.java` 在排除列表里、mixin 配置为空集。
**现状已完全不同**：

- `excluded.txt` 是 **0 字节**（无排除项），342 个源文件全部参与编译；
- `DragonMaid.java` 是真入口（含全部注册），`PortBootstrap` 才是遗留；
- `dragonmaid.mixins.json` 有 **48 + 22 = 70 条** mixin；
- 版本已到 `1.2.0-beta.4-neoforge.1`，依赖基线是 IAF CE **2.2**。

读历史文档做决策时，先对照这一条。

---

## 6. 给移植执行的建议清单

### 6.1 新增上游代码放哪些包

| 上游内容类型 | 本仓落点 |
|---|---|
| 业务逻辑（战斗/行为/数据） | 对应镜像包：`combat/`、`data/`、`movement/`、`accessory/`、`recall/`、`petrification/`、`learning/`、`advancement/`、`brewing/`、`command/` |
| 物品/方块/实体/菜单/粒子/配方 | `item/`、`block/`、`entity/`、`menu/`、`particle/`、`recipe/`（注册入口统一在 `DragonMaid` 构造函数） |
| 客户端渲染/模型/GUI/输入 | `client/`（**只能被 `Dist.CLIENT` 事件引用**） |
| mixin | 服务端 `mixin/`，客户端 `mixin/client/`（+ 两处登记） |
| 网络消息 | `network/`，一个 record + `encode/decode/handle`，再到 `network/DragonNetwork#register` 里 `registerMessage(id, ...)` 并指定方向 |
| IAF CE 桥 / 已删 API 适配 | `iafcompat/`（例：`IafCompat`、`EntityDataCompat`） |
| Forge 语义垫片 | `compat/`（先查已有，再新增） |
| GameTest | `gametest/`，复用 `GameTestSupport.TEMPLATE` 与 `mockPlayer()/spawnTamedDragon()` |
| 资源 | `assets/dragonmaid/...`、`data/dragonmaid/...`（**单数目录**） |
| 不要 | 往 `com/github/alexthe666/*` 或 `com/eliotlash/*` 加新文件 —— 那只是编译期桩 |

### 6.2 哪些地方需要注册（逐条清单）

1. **主类构造函数** `DragonMaid(IEventBus, ModContainer)`：`ModItems` / `ModRecipes` / `ModEntities` / `ModParticles.TYPES` / `ModMenus.MENUS` / `BLOCKS` / `CREATIVE_MODE_TABS` 的 `.register(bus)`；`DragonNetwork.register()` + `modEventBus.addListener(DragonNetwork::registerPayloads)`；`modContainer.registerConfig(...)`；`RecallContent.setup(modContainer)`；`GlobalBalanceConfig.register()`；`NeoForge.EVENT_BUS.register(this)`。
2. **创造模式标签**：新物品要加进 `DRAGON_MAID_TAB` 的 `displayItems`（否则物品存在但创造栏看不到）。
3. **网络**：新消息 → `network/DragonNetwork#register` 里 `registerMessage(id, ...)` + 方向；id 段目前占用 0–9、12–16（10/11 空缺，复用前先确认历史兼容）。
4. **mixin**：源文件 + `dragonmaid.mixins.json`（`mixins` 或 `client`）+ 已存在的 `[[mixins]]` 声明。
5. **客户端**：`client/ClientRegistration`（`RegisterParticleProvidersEvent` / `RegisterMenuScreensEvent` / `EntityRenderersEvent.AddLayers` / `RegisterClientReloadListenersEvent` / `FMLClientSetupEvent` 的 item properties）与 `client/ClientEvents`（游戏事件）。
6. **命令**：`command/CompanionCommands.register(RegisterCommandsEvent)`。
7. **资源**：`assets/<ns>/lang/{en_us,zh_cn}.json`、`models/item`、`textures/item`、`data/<ns>/recipe`。
8. **配置**：新配置项挂到已有 spec（`DragonMaidConfig` / `GlobalBalanceConfig` / `CompanionBalanceConfig`），**不要新注册同名 config 文件**（`Config.java` 注释记录了 "config file conflict" 硬崩）。
9. **GameTest**：新测试类加 `@GameTestHolder("dragonmaid") + @PrefixGameTestTemplate(false)`，模板用 `GameTestSupport.TEMPLATE`。

### 6.3 最容易踩的 5 个点（按"会浪费多少时间"排序）

1. **`libs/` 空 + IAF 版本错配**（最先撞上）
   `compileOnly fileTree("libs")` 现在是空集 → 全库上千条 `package does not exist`；而默认 GameTest 回退目录 `D:/mc-model/work/_probe_dm/mods` 里只有 `iceandfire-2.0.jar`，**2.0 没有 `PartEntity`**，`DragonPartInjuryMixin` 直接编译失败。
   → 动手第一件事：把 8 个 jar 放进 `libs/`（含 **`iceandfire-2.2.jar`**，可从旧仓库 `.../medieval-world/dragonmaid-port/libs/` 或 `D:/mc-model/work/_port_src/iaf22/` 复制）。

2. **dev 环境的 mixin 不生效（manifest 陷阱）**
   只在 `jar.manifest` 写 `MixinConfigs` 对 `runServer/runClient/runGameTestServer` **完全无效**（它们加载 `build/classes` 目录）。症状是诡异的 `ClassCastException: IceDragonEntity cannot be cast to DragonState`，而不是 mixin 报错。
   → 已在 `neoforge.mods.toml` 加了 `[[mixins]]`，**不要删**；新建 mixin 时也别忘登记 json 数组。

3. **1.21 组件语义的"副本 vs 活引用"（编译 0 报错，功能静默失效）**
   `StackNbt.getTag()` 返回的是 `CUSTOM_DATA` 的**副本**，往里写等于丢弃（历史事故：龙角收进去的龙再也放不出）；必须用 `getOrCreateTag()`（`WriteBackTag` 回写）。附魔书要用 `DataComponents.STORED_ENCHANTMENTS`（不是 NBT `StoredEnchantments`，否则学习系统对所有书判定非法 / 给 0 经验）。
   → 改物品 NBT / 附魔 / 组件代码前，先读 `compat/StackNbt.java` 与 `compat/WriteBackTag.java`。

4. **数据包会"静默不加载"**
   目录必须是 1.21 单数（`advancement`/`recipe`/`loot_table`/`tags/item`/`tags/block`），`pack.mcmeta` 必须是 1.21 格式（资源 34 / 数据 48），`result`/`icon` 用 `"id"`。不满足时**没有报错**，只是整包被跳过，日志里只有"没有 `dragonmaid:` 配方"—— GameTest `DataLoadGameTests` 就是专门抓这个的。
   → 新增任何 data JSON，先跑 `runGameTestServer`，看 `recipesLoaded` / `rootAdvancementLoaded` 是否还绿。

5. **客户端类 / mixin 目标的双重雷区**
   (a) 客户端类只能在 `Dist.CLIENT` 的事件或 `ClientRegistration/ClientEvents` 里引用，否则 dedicated server 加载时 `NoClassDefFoundError`（历史事故）。
   (b) `dragonmaid.mixins.json` 的 `injectors.defaultRequire = 1`：**任何目标方法/类不存在 = 启动硬崩**。可选目标必须 `@Pseudo` / `require = 0`；绑 lambda 名（`lambda$destroyAreaBreath$0`）的注入点在依赖重编后必然失效。
   → 新 mixin 先在 `docs/IAF-CE-MAPPING.md` 的方法核对表里确认目标存在（41 个目标已核对过，可作模板）。

**另外三条同等重要的"第 6/7/8 点"**（不计入前 5，但会咬人）：

- **GeckoLib 4.9 的 Gson**：必须用 `KeyFramesAdapter.GEO_GSON`（`client/GeoModelJson`），裸 Gson 会 `Model.minecraftGeometry() is null` → NPE，且报错点离真因很远（曾连修两轮"症状"）。
- **1.21.1 `VertexConsumer` 没有 `endVertex()`**：所有手写顶点/收集器（`ArticulatedArmorMesh`、`IceFemaleRenderer.untinted`）必须自己 flush 最后一个顶点，否则模型每个面少三角形。
- **`runClient` 的自动连接端口**：`build.gradle:32` 写死 `--quickPlayMultiplayer localhost:25615`，但 `runServer` 没设端口、仓库里也没有 `run/server/server.properties` 模板 → 首次要先手写 `server-port=25615` + `online-mode=false`，否则客户端一连就断（**推断项**：基于配置对照，未实跑验证）。

---

## 7. 附：本轮核对到的环境事实（可复现）

```
Get-Command gradle                 -> (空)
where.exe gradle                   -> (空)
$env:JAVA_HOME                     -> D:/jdk11/
D:/pcl/voxy1.7/jdk/jdk21/bin/java.exe -> 存在
<repo>/gradlew*                    -> (无)
<repo>/gradle/                     -> (不存在)
~/.gradle/wrapper/dists/           -> gradle-8.8-bin(已解压) / gradle-9.1.0-bin / gradle-9.4.1-bin
~/.gradle/caches/.../net.neoforged/neoforge/ -> 21.1.248, 21.1.264
<repo>/libs/*.jar                  -> 0 个（只有 README.md）
<repo>/build/, <repo>/run/         -> 不存在
D:/mc-model/work/_probe_dm/mods/iceandfire-2.2.jar -> 不存在（只有 iceandfire-2.0.jar）
D:/.../medieval-world/dragonmaid-port/libs/iceandfire-2.2.jar -> 存在（20,952,409 B）
git status --porcelain             -> 侦察开始时为空（本次仅新增本文档；之后出现的 docs/UPSTREAM-1.2.7-DELTA.md 属并行子代理）
```

> 免责声明：以上除"文件存在性/路径/计数"外，构建与测试结论均来自仓库内文档，本轮**未执行 gradle**（按任务要求，构建由主代理串行执行）。
