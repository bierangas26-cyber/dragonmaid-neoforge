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

### 验证

- 服务端：R1-R7 回归全过（生成/驯服/化形/护卫/龙息/龙甲/崩溃回归）
- 客户端：化形、龙界面、女仆龙铠、饰品效果、号角收放、召回台界面、介错流程均实机验证

已知限制见项目描述页。
