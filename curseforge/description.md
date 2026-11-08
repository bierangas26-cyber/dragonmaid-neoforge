# Dragon Maid Companion — Community Port (NeoForge 1.21.1)

> **This is an unofficial community port, not an official release by the original author.**
> - Original mod: [**[IAF]: Dragon Maid Companion**](https://www.curseforge.com/minecraft/mc-mods/iaf-dragon-maid-companion) by **别思丶 / EvenFlowInShipGirl** (Minecraft 1.20.1 / Forge)
> - This port: Minecraft **1.21.1 / NeoForge** — currently tracking original **1.2.0-beta.4**
> - Port source & issue tracker: https://github.com/bierangas26-cyber/dragonmaid-neoforge

## About This Port

An unofficial community port of *Dragon Maid Companion* (an Ice and Fire addon that turns tamed dragons into humanoid companions) to **Minecraft 1.21.1 / NeoForge**.

**Published with the original author's permission.** The original author publicly acknowledges and links this port in the changelog of the original mod's **1.2.0-beta.4** release ("社区移植：在 1.21.1 再次相见"). The port is **free and non-commercial**; all original textures, models, animations, sounds and guide content remain the property of the original author.

## What's new in this version (ported from 1.2.0-beta.4)

- **Dragon inventory + hands**: one row per knowledge level (up to 3), main-hand weapon / off-hand shield at knowledge level 2, three pickup modes, automatic feeding, growth scales go into the bag first
- **Faerie's Wish**: shrink a dragon-form companion back to wyrmling appearance (real age/knowledge kept), perch on your shoulder, ranged support (focused breath / lightning bolts), 1 slows falls / 2 gliding / 3 flight
- **8 combat accessories + combinations**: Eye of Majesty, Thornfeather Charm, Venomflame Heart, Frosty Tail Fin, Wild Pendant, Rending Claw, Guardian Thornfeather Emblem, Hunter King Pendant
- **Reworked humanoid melee**: claw combos + three-hit sword combo, tail sweep breaks shields, knockback resistance at stages 3–5, selectable animated/instant melee
- **Step-by-step learning**: stops at the next knowledge threshold, keeps overflow, 2500 cap, enchantment books can be submitted early
- **Companions / hatching / travel**: escort & patrol tuning, gem binding modes, critical-injury notification with XYZ, hatching environment decides sex (fire/ice/lightning conditions), Jade shows the expected sex
- **Config / commands / guide**: global balance config, in-game config screen, CN/EN switch, starter guide toggle, companion info / growth / hunger / teleport commands and egg hatching countdown, updated guide book
- Fixes: dynamic-tree obstacles, rider position sync, growth past stage 5, long-range water recall, ice-hole pathing, floating idle pose, client config sync

## Requirements (must be installed together)

| Dependency | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.264+ |
| Ice and Fire Community Edition | **2.2+** (2.0 / 2.1.x not supported) |
| uranus | 2.4.1+ |
| jupiter | 2.3.7+ |
| GeckoLib | 4.9.2 |
| Patchouli | 1.21.1-93+ |
| Jade (optional) | 15.10+ — shows the expected egg sex |

> IAF CE 2.2 no longer needs Citadel. Client and server must have the same mods installed.

## Installation

1. Install NeoForge 21.1.264+ for Minecraft 1.21.1 (Java 21)
2. Put all jars listed above (including this mod) into `mods/`
3. Launch — single player works out of the box; for multiplayer the server needs the same set

## Known issues

- 1.2 client-side GUI (equipment screen, knowledge screen, human-form transform) was verified in-game; miniature/shoulder-perch and accessory visuals were verified by static checks only — please report anything odd
- `HumanoidWater` keeps the 1.1 water-exit behaviour (the 1.2 rewrite is a known gap)
- IAF CE 2.2 removed `eatFoodBonus`: the automatic-feeding meal bonus hook is missing (hunger/heal/particles/sounds still work)
- Only the silver dragon armor set was verified; dragon-forge breath heating chain not fully verified; multiplayer reconnect not covered

## Support

Report bugs on the port repository (with `latest.log`, reproduction steps and your mod list):
https://github.com/bierangas26-cyber/dragonmaid-neoforge/issues

Do **not** report port-specific problems to the original author — the original mod is 1.20.1 / Forge only.

## Disclaimer

This port is provided "as is", without warranty of any kind. Back up your world. If the original author releases an official NeoForge version, please use that instead.

---

# Dragon Maid Companion — 社区移植版（1.21.1 / NeoForge）

> **这是非官方社区移植，不是原作者发布。**
> - 原 mod：[**[IAF]: Dragon Maid Companion**](https://www.curseforge.com/minecraft/mc-mods/iaf-dragon-maid-companion)（作者：**别思丶 / EvenFlowInShipGirl**，Minecraft 1.20.1 / Forge）
> - 本移植：Minecraft **1.21.1 / NeoForge**，当前追平原版 **1.2.0-beta.4**
> - 移植仓库（源码 + Issue）：https://github.com/bierangas26-cyber/dragonmaid-neoforge

## 已获公开授权

原作者在原 mod **1.2.0-beta.4** 更新日志中公开致谢并链接本移植（「社区移植：在 1.21.1 再次相见」）。本移植**免费、非盈利**，原 mod 全部资产版权归原作者所有。

## 本版更新（移植自 1.2.0-beta.4）

- 🎒 **龙的背包 + 主副手**：知识每级一排（最多 3 排）、知识 2 级开主手/副手、三种拾取模式、自动进食、掉鳞进背包
- 🧚 **仙灵之愿**：迷你化保留真实年龄/知识、可停肩、肩载远程协战（吐息/雷弹）、1 只缓降 / 2 只滑翔 / 3 只飞行
- 💎 **8 件战斗饰品 + 组合**：威仪眼坠、棘羽护符、毒焰之心、寒潮尾桨、狂野吊坠、裂阵爪饰、护佑棘羽纹章、猎王吊坠
- ⚔️ **人形近战重做**：爪击/三连击、甩尾破盾、3-5 阶击退抗性、动画/直接攻击可选
- 📚 **学习可分次完成**：门槛停止、溢出保留、2500 上限、附魔书可提前交
- 🐉 **相处/孵化/旅途**：守护归队、宝石绑定模式、重伤通知（XYZ）、孵化环境决定性别（+Jade 显示）
- 🛠️ **配置/指令/手册**：全局配置、主菜单配置界面、中英切换、首次送手册、伙伴查询/成长饥饿修改/传送/蛋倒计时指令、手册更新
- 🔧 修复：动态树、乘客位置、五阶后成长、水中远距离归队、冰洞寻路、浮水待机、客户端配置同步

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
| Jade（可选） | 15.10+，可查看蛋孵化性别预期 |

> IAF CE 2.2 起不再需要 Citadel；客户端与服务端都要装同一套。

## 安装

1. 安装 Minecraft 1.21.1 + NeoForge 21.1.264+（Java 21）
2. 把上表所有 jar（含本 mod）放进 `mods/`
3. 启动即可；多人游戏服务端也要装同一套

## 已知问题

- 1.2 客户端界面（装备页/知识页/人形变身）已实机验证；迷你化/肩载与饰品外观为静态校验，若有异常请反馈
- `HumanoidWater` 的 1.2 出水/水面逻辑暂保留 1.1 行为（已知缺口）
- IAF CE 2.2 删除 `eatFoodBonus`：自动进食的进食加成钩子缺失（饥饿/回血/粒子/音效正常）
- 龙甲仅验证 silver 系；龙炉吐息加热链路未完整验证；多人断线重连未覆盖

## 反馈 / 支持

遇到 bug 请到移植仓库提 Issue（附 `latest.log` + 复现步骤 + mod 列表）：
https://github.com/bierangas26-cyber/dragonmaid-neoforge/issues

**请不要把本移植的问题反馈给原作者**（原 mod 仅支持 1.20.1 Forge）。

## 免责声明

本移植按「现状」提供，不附带任何担保，请先备份存档。若原作者发布官方 NeoForge 版本，请优先使用官方版本。
