# 1.2.0 验收测试清单（ACCEPTANCE TEST）

适用版本：`1.2.0-beta.4-neoforge.1`（jar sha256 `048FE70382DF6E7BF0E10978CAF7AEB9EF9DE06E484C8E67C9FB58D3F1EBCD00`）

## 安装前提

| 依赖 | 版本 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.264+ |
| Ice and Fire Community Edition | **2.2+**（不要装 Citadel） |
| uranus | 2.4.1+ |
| jupiter | 2.3.7+ |
| GeckoLib | 4.9.2 |
| Patchouli | 1.21.1-93+ |
| Jade（可选） | 15.10+ |

## P0 必测（约 10 分钟）

| # | 测什么 | 操作 | 预期 |
|---|---|---|---|
| 1 | 启动/进世界 | 正常启动 | 无崩溃；`latest.log` 无 `Mixin apply ... failed` / `InjectionError` |
| 2 | 驯服 + 开界面 | 用龙食驯服龙 → **Shift+右键**龙 | 打开 176×214 龙界面，右侧 4 个按钮（Details/Knowledge Level/Accessories/Human form） |
| 3 | 装备界面 | 点 Accessories | 252×232 新界面：预览框、5 装备槽、2 手部槽、`Pick up:` 模式按钮、`Dragon storage · 0 / 27`、玩家 36 格、Knowledge Lv 标签；锁槽 hover 提示 Not unlocked yet |
| 4 | 知识界面 + 交书 | 点 Knowledge Level → 把书拖进左侧书槽 → Learn | 知识 Lv0→Lv1（60 本书=300 XP），进度条更新，Human form 变可用 |
| 5 | 人形变身 | 点 Human form | 龙变蓝发女仆龙人形；再点变回龙 |
| 6 | 龙蛋性别 | 火龙蛋放灵魂土+灵魂火孵化；冰龙蛋垫蓝冰孵化；雷龙蛋旁红石 15 孵化 | 火→雄、蓝冰→雌、红石15→雌 |
| 7 | 龙角/归档 | 龙角右键自己的龙收走 → `/dragonmaid get all` | 输出里出现该龙，带 `State: HORN, … (archive)` |

## P1 建议测（自动化难覆盖，需人判断）

| # | 测什么 | 备注 |
|---|---|---|
| 8 | 仙灵之愿：知识 Lv1 后戴饰品 → 龙变小 → 空手右键停肩；2/3 只飞行 | ✅ 2026-11-08 实机验证：迷你形态/停肩已确认（截图见 1.2-VERIFICATION）；2/3 只飞行需人判断 |
| 9 | 战斗手感：人形爪击/三连击/甩尾破盾/受击表现 | 需人判断 |
| 10 | 背包自动进食 + 拾取模式（全部/关闭/仅已有） | ✅ 2026-11-08 实机验证（自动进食 +30/块、ALL 模式拾取进槽；模式循环逻辑 GameTest 覆盖） |
| 11 | 多人：服务端 + 两个客户端，龙同步/重连 | ✅ 2026-11-08 双客户端实测：玩家/龙同步渲染、断开重连状态保留 |
| 12 | 旧存档升级（1.1 存档进 1.2） | 新世界已验；旧档会生成新的 `dragonmaid-sync-server.toml` 等配置 |

## 已知限制（不算 bug）

- IAF 1.20.1 原版 `eatFoodBonus` 为空实现（no-op），本移植的自动进食行为与上游一致（饥饿/回血/粒子/音效正常）
- 7 个 ForgeConfig* mixin 目标第三方模组无 1.21.1 版，静默跳过（不影响 NeoForge 自带配置界面）
- 龙甲只验证 silver 系

## 反馈

任何一项不符合预期：附 `latest.log` + 复现步骤（哪条龙/哪个界面/什么操作）提交到仓库 Issues。
