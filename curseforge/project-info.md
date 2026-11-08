# CurseForge 项目信息（上传时填）

## 名称（已改）

已完成（评审要求去掉版本号）：**Dragon Maid Companion: NeoForge Community Port**

## Slug 建议

dragon-maid-companion-neoforge

## Summary（短描述）

中文：Dragon Maid Companion 的 1.21.1 NeoForge 社区移植：让龙化形为龙娘，陪你冒险。非官方、免费发布。

英文：Unofficial NeoForge 1.21.1 port of Dragon Maid Companion - transform dragons into dragon maids. Free, with author permission.

## 分类

- Adventure
- Mobs
- Utility

## 游戏版本 / 加载器

- Game version: 1.21.1
- Mod loader: NeoForge

## 环境

- Client: required
- Server: required

## License

选择 Custom license，内容用仓库里的 LICENSE.md（非盈利 + 署名原作者 + 原资产版权归作者）。

## 依赖（Dependencies）

| 项目 | 类型 |
|---|---|
| Ice and Fire Community Edition | Required |
| uranus | Required |
| jupiter | Required |
| GeckoLib | Required |
| Patchouli | Required |

## 链接

- Source: https://github.com/bierangas26-cyber/dragonmaid-neoforge
- Issue tracker: https://github.com/bierangas26-cyber/dragonmaid-neoforge/issues

## 上传文件（最终版 · 1.2 内容版）

文件：dragonmaid-neoforge-1.21.1-1.2.0-beta.4-neoforge.1.jar
size：7,267,457 B
sha256：048FE70382DF6E7BF0E10978CAF7AEB9EF9DE06E484C8E67C9FB58D3F1EBCD00
依赖基线：IAF CE 2.2+（1.21.1 NeoForge），不再需要 Citadel，Jade 为可选
## 图片

images/ 下 3 张，建议顺序：
1. dm3-0-inworld.png（世界内人形龙娘）
2. dm3-5-dragonscreen.png（龙界面人形预览）
3. dm-1-after-shift-rightclick.png（界面按钮可用）

## 图标

用原 mod 的 logo（jar 内 assets/dragonmaid/dragonmaid.png）或截取龙娘头像。

## 已验证（发布前）

· 实机：化形 / 龙界面 / 女仆龙铠 / 饰品（护甲+6、回血 0.4/s）/ 龙魂号角收放 / 召回台真实召回（老龙+新龙）
  / 介错刀流程 / 指南书 / 石化（蛇发头→雕像→解药恢复，含名字/血量/主人保留）
· GameTest 9/9（数据包加载、号角收放 UUID、饰品护甲、附魔书经验、祭坛开菜单、石化→复原）
· 服务端 R1-R7 回归
· 24 个 main mixin 全部加载成功（0 失败）

## 已知限制（同步写进项目描述）

· 龙甲只验证了 silver 系（其它材质/部位未逐一确认）
· 龙炉吐息加热未做完整端到端验证（mixin 加载已修复，龙喷火→热炉这条链路待玩家反馈）
· 客户端渲染/动画/贴图、网络包客户端侧解码、多人断线重连未在自动化里覆盖
· 创造模式下石化解药不消耗（设计如此）

## 玩家 Bug 反馈

页面 Issue tracker 指向 GitHub Issues：遇到 bug 请附上 latest.log + 复现步骤 + 是否装了其它龙类 mod。
