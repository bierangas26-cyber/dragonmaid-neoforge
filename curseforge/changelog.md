# Changelog

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
