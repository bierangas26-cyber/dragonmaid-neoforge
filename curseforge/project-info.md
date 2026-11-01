# CurseForge 项目信息（上传时填）

## 名称（二选一）

1. `Dragon Maid Companion: NeoForge 1.21.1`
2. `Dragon Maid Companion (Community Port) [1.21.1]`

## Slug 建议

`dragon-maid-companion-neoforge`

## Summary（短描述）

中文：`Dragon Maid Companion 的 1.21.1 NeoForge 社区移植：让龙化形为龙娘，陪你冒险。非官方、免费发布。`

英文：`Unofficial NeoForge 1.21.1 port of Dragon Maid Companion - transform dragons into dragon maids. Free, with author permission.`

## 分类

- Adventure
- Mobs
- Utility

## 游戏版本 / 加载器

- Game version: `1.21.1`
- Mod loader: `NeoForge`

## 环境

- Client: required
- Server: required

## License

选择 **Custom license**，内容用仓库里的 `LICENSE.md`（非盈利 + 署名原作者 + 原资产版权归作者）。

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
- Issue tracker: 同上（Issues）

## 上传文件（最终版）

```
文件：dragonmaid-neoforge-1.21.1-1.1.0-beta.2.jar
size：6,813,545 B
sha256：0DA42438688A9E2EC510E7BF97ED23E81F169DECC0BCB972D2DFD5D09DA96219
mtime：2026-11-01 18:20:54
```

- Release type: **Beta**
- Changelog：见 `changelog.md`

## 图片

`images/` 下 3 张，建议顺序：
1. `dm3-0-inworld.png`（世界内人形龙娘）
2. `dm3-5-dragonscreen.png`（龙界面人形预览）
3. `dm-1-after-shift-rightclick.png`（界面按钮可用）

## 图标

用原 mod 的 logo（jar 内 assets/dragonmaid/dragonmaid.png）或截取龙娘头像。

## 已验证（发布前）

```
· 实机：化形 / 龙界面 / 女仆龙铠 / 饰品（护甲+6、回血 0.4/s）/ 龙魂号角收放 / 召回台真实召回（老龙+新龙）/ 介错刀流程 / 指南书
· GameTest 9/9（数据包加载、号角收放 UUID、饰品护甲、附魔书经验、祭坛开菜单、石化→复原）
· 服务端 R1-R7 回归
```

## 已知限制（同步写进项目描述）

```
· 龙甲只验证了 silver 系（其它材质/部位未逐一确认）
· 龙炉相关交互未完整验证
· 客户端渲染/动画/贴图、网络包客户端侧解码、多人断线重连未在自动化里覆盖
```