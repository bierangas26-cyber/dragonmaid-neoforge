# Changelog

## v1.1.0-beta.2-neoforge.2 (2026-11-07)

**适配 Ice and Fire Community Edition 2.2**（IAF CE 于 2026-11-05 为 1.21.1 NeoForge 发布的 2.2 版）。

### 兼容性修复

- **`MultipartPartEntity` 类型重构**：IAF CE 2.2 把它从普通 `Entity` 改成 NeoForge 的 `PartEntity`，并删除了同步字段 `getParentId()`。旧 mixin 还按 2.0 的方式 `@Shadow getParent()/getParentId()`，加载时会直接 `InvalidMixinException ... @Shadow method getParent was not located` 导致游戏启动失败。已改为走 `PartEntity#getParent()`（直接父实体引用），去掉 UUID 兜底查询；龙部位受伤判定逻辑不变。
- **依赖要求提升**：`neoforge.mods.toml` 中 `iceandfire` 版本范围由 `[2.0,)` 改为 `[2.2,)`。
- **不再需要 Citadel**：IAF CE 2.2 已不再依赖 Citadel，本移植同步移除该运行前置。

### 验证

- 编译：对 IAF CE 2.2 全量重编译，0 错误。
- GameTest：`runGameTestServer` **9/9 通过**（数据包加载、龙角收放、饰品护甲、附魔书经验、祭坛开菜单、石化→复原、NBT key 回归），且在不装 Citadel 的环境下同样 9/9。
- 服务端：全新 1.21.1 NeoForge 21.1.264 服务端 + IAF CE 2.2 + uranus 2.4.1 + jupiter 2.3.7 + GeckoLib 4.9.2 + Patchouli 93，正常启动（`Done`），24 个 main mixin 全部 apply，无 FATAL。
- 客户端：同一依赖组合启动客户端（10 mods），进入主菜单并连入服务器；IAF CE 2.2 的龙模型正常渲染，Patchouli 手册可正常打开。

### 兼容性说明

- 本版本**需要 IAF CE 2.2 或更高**；IAF CE 2.0 / 2.1.x 不再受支持（其 `MultipartPartEntity` 结构与新 mixin 不兼容）。
- 其余依赖版本不变：uranus 2.4.1+、jupiter 2.3.7+、GeckoLib 4.9.2、Patchouli 1.21.1-93+。
- 原 mod（Dragon Maid Companion）作者截至 2026-11-07 仍停留在 **1.1.0-beta.2**，本版本不涉及内容更新，只是依赖兼容。
## v1.1.0-beta.2-neoforge.1 (2026-11-01)

首个 1.21.1 NeoForge 移植版本。

### 移植与修复（相对原 1.20.1 版）

- 全量代码迁移到 NeoForge 1.21.1（212 个源文件，0 编译错误）
- 资产 359/359 逐字节一致（仅 `pack.mcmeta` 升级为 1.21.1 格式）
- 数据包目录改为 1.21 单数命名（advancement / recipe / loot_table / tags/item）
- 配方 `result.item` → `result.id`；进度 `display.icon.item` → `icon.id`

### 功能修复

- **龙详情界面按钮全灰**：`IafProxy.getReferencedMob()` 是遗留 stub → 改用 IAF CE 的 `DragonMenu.getDragon()`
- **人形预览过小**：新增 `DragonPreviewScaleMixin`（人形预览放大 7×）
- **龙魂号角收放失效**：IAF CE 改为 DataComponent 存储 → 重写为 `DragonHornComponent` 收/放（UUID/属性/名字保留）
- **召回台右键无反应**：1.20.5+ Block 交互 API 拆分 → 补 `useWithoutItem` / `useItemOn`（并修复 tooltip 签名）
- **召回号角收据丢失**：`StackNbt.getTag()` 返回副本 → 改用 `getOrCreateTag()` 回写；并加与普通号角的互斥检查
- **石化雕像数据**：旧 `IAFStoneStatueNBT` → IAF CE `STONE_STATUS` 组件
- **附魔书知识升级无效**：1.21 用 `STORED_ENCHANTMENTS` 组件 → 已适配（之前所有附魔书判非法、0 经验）
- **龙炉吐息 mixin 签名错误**：`DragonForgeBreathMixin` 的两个 `@Redirect` handler 参数顺序与 IAF CE 的 lambda 签名不符 → 首次加载 `IafDragonDestructionManager` 时 mixin apply **FATAL**；已按 javap 实证签名修正（龙区域吐息路径恢复，24 个 main mixin 全部加载成功）
- **石化全流程验证**：蛇发女妖之头（gorgon head）→ 龙变石像 → 石化解药恢复（名字/血量/主人保留）；创造模式不消耗解药（设计如此）

### 召回系统全面修复（5 层，本轮重点）

1. **事件监听器根本没注册**：`NeoForge.EVENT_BUS.register(RecallTracking.class)` 传 Class 对象 → 扫不到任何 `@SubscribeEvent`；改用 `@EventBusSubscriber`（跨区块追踪、脏数据 flush、实体加入事件恢复全部生效）
2. **令牌死锁**：`RecallLedger.observe()` else 分支从不补 stamp → 老存档/召唤龙永远没有 recall token；新增 `stampIfMissing` 补写（已有 token 不覆盖）
3. **找回失败**：`RecallService.findLoaded()` 在 token 缺失时直接判 missing；改为 UUID 匹配时补 stamp 后接受
4. **检查点崩溃**：`RecallPersistence.checkpoint()` 依赖已失效的 mixin accessor（且 `EntityStorage` 根本不是 `ChunkStorage` 子类）→ 改用 `SimpleRegionStorage.synchronize() + read()` 公开 API
5. **1.21.1 NBT key 改名**：实体持久数据从 `ForgeData` 改为 `NeoForgeData`，旧代码读写全部对不上 → 修复后石化/召回令牌校验通过

召回台现在可正常：列出伙伴 → 显示距离/费用 → 召回（龙出现在祭坛旁，UUID/属性保留）；失败不扣经验。

### 自动化测试

- 引入 NeoForge GameTest：`gradlew runGameTestServer`，**9/9 通过**（数据包加载、龙角收放、饰品护甲、附魔书经验、祭坛开菜单、石化→复原、NBT key 回归）
- 报告见仓库 `DM-GAMETEST-REPORT.md`

### 验证

- 服务端：R1-R7 回归全过（生成/驯服/化形/护卫/龙息/龙甲/崩溃回归）
- 客户端：化形、龙界面、女仆龙铠、饰品效果、号角收放、召回台真实召回、介错流程均实机验证
- GameTest：9/9

已知限制见项目描述页。
