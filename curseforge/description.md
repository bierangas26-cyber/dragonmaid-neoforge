# Dragon Maid Companion — 社区移植版（NeoForge 1.21.1）

> **这是非官方社区移植，不是原作者发布。**
> - 原 mod：[**[IAF]: Dragon Maid Companion**](https://www.curseforge.com/minecraft/mc-mods/iaf-dragon-maid-companion)（作者：**别思丶 / EvenFlowInShipGirl**）
> - 原版：Minecraft 1.20.1 / Forge
> - 本移植：Minecraft 1.21.1 / NeoForge
> - 移植仓库（源码 + Issue）：https://github.com/bierangas26-cyber/dragonmaid-neoforge

## 关于这个移植

把 *Dragon Maid Companion*（冰火传说附属：让驯服的龙化形为龙娘同伴）从 1.20.1 Forge 迁移到 **1.21.1 NeoForge**。

**已获得原作者许可发布**（免费、非盈利；原作者会在原 mod 页面同步本社区移植的下载链接）。原 mod 的贴图、模型、动画、音效、手册内容等全部资产版权归原作者所有。

## 与原版的差异（移植改了什么）

- 全量代码从 Forge 1.20.1 迁移到 NeoForge 21.1（Java 21）
- 依赖从原版 Ice and Fire 换成 **Ice and Fire Community Edition 2.2+**
- 1.21 数据包目录改名（advancements→advancement、recipes→recipe、loot_tables→loot_table 等），配方/进度的 NBT 字段格式升级
- 适配 1.21.1 组件化 API：龙魂号角（DataComponent）、石化状态、附魔书经验、召回台交互
- 修复了一批移植后失效的路径：龙界面按钮全灰、人形预览过小、龙角收放失效、召回台右键无反应、召回丢失、龙炉吐息 mixin、石化全流程
- 不含新增内容；原 mod 的内容与平衡完全保留

## 依赖（必须一起装）

| 依赖 | 版本 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.264+ |
| Ice and Fire Community Edition | **2.2+**（2.0/2.1.x 不支持） |
| uranus | 2.4.1+ |
| jupiter | 2.3.7+ |
| GeckoLib | 4.9.2 |
| Patchouli | 1.21.1-93+ |

> IAF CE 2.2 起不再需要 Citadel；客户端与服务端都要装同一套依赖与本 mod。

## 安装

1. 安装 NeoForge 21.1.264+（Minecraft 1.21.1）
2. 把上表所有 jar（含本 mod）放进 `mods/`
3. 启动即可（单人 / 服务器均需要；多人时服务端也必须装）

## 已知问题

- 龙甲只验证了 silver 系，其它材质/部位可能有问题
- 龙炉吐息加热链路未完整验证
- 人形化形的界面交互/逐形态外观尚未全部截图验证
- 创造模式下石化解药不消耗（原版设计如此）
- 多人断线重连未覆盖

## 反馈 / 支持

遇到 bug 请到移植仓库提 Issue（附 `latest.log` + 复现步骤 + 是否装了其它龙类 mod）：
https://github.com/bierangas26-cyber/dragonmaid-neoforge/issues

请勿把本移植的问题反馈给原作者（与原 mod 无关）。

## 免责声明

本移植按「现状」提供，不附带任何担保；因使用造成的存档损坏/崩溃等损失由使用者自行承担，请先备份存档。
若原作者发布官方 NeoForge 版本，请优先使用官方版本。

---

## English

**Unofficial community port** of *Dragon Maid Companion* by **别思丶 / EvenFlowInShipGirl**.

- Original mod: [**[IAF]: Dragon Maid Companion**](https://www.curseforge.com/minecraft/mc-mods/iaf-dragon-maid-companion) (Minecraft 1.20.1 / Forge)
- This port: Minecraft **1.21.1 / NeoForge** — published **with the original author's permission** (free, non-commercial).
- Source & issue tracker: https://github.com/bierangas26-cyber/dragonmaid-neoforge

What changed: full source migration to NeoForge 21.1 / Java 21, dependency switched to Ice and Fire Community Edition **2.2+**, 1.21 data-pack renames, component-based API adaptation (dragon horn, petrification, stored enchantments, altar interaction), plus a list of port-specific fixes (dragon GUI, preview scale, recall altar, forge breath mixin...). No new content; original gameplay is preserved.

**Requirements:** Minecraft 1.21.1 · NeoForge 21.1.264+ · Ice and Fire CE **2.2+** · uranus 2.4.1+ · jupiter 2.3.7+ · GeckoLib 4.9.2 · Patchouli 1.21.1-93+ (Citadel is no longer needed with IAF CE 2.2).

**Known issues:** only the silver dragon armor set was verified; dragon-forge breath heating not fully verified; humanoid GUI/visuals not exhaustively tested; petrification remedy is not consumed in creative (vanilla behavior); multiplayer reconnects untested.

All original textures/models/animations/sounds belong to the original author. This port is unofficial and not affiliated with the original author. If an official NeoForge version is released, please use that instead.

Need help? Open an issue on GitHub (with `latest.log` + reproduction steps). Please do **not** report port-specific bugs to the original author.
