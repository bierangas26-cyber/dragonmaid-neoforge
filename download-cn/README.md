# 中文下载 / 安装说明（Dragon Maid Companion 社区移植版）

本目录直接放**打包好的 mod jar**，方便国内网络直接下载（GitHub Release 也有一份）。

## 文件

| 文件 | 说明 |
|---|---|
| `dragonmaid-neoforge-1.21.1-1.2.0-beta.4-neoforge.1.jar` | 当前版本（1.2 内容版，适配 IAF CE 2.2） |
| `sha256.txt` | 校验值（下载后建议核对） |

```
文件名：dragonmaid-neoforge-1.21.1-1.1.0-beta.2-neoforge.2.jar
大小：7,267,457 B
sha256：048FE70382DF6E7BF0E10978CAF7AEB9EF9DE06E484C8E67C9FB58D3F1EBCD00
```

> 本 jar 对应原版 **1.2.0-beta.4** 内容（龙的背包/双手、仙灵之愿、8 件战斗饰品、人形近战重做、分次学习、孵化性别等）。

## 必装依赖（版本必须对，缺一个都启动不了）

| 依赖 | 版本 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.264+ |
| Ice and Fire Community Edition（冰火传说 CE） | **2.2+**（2.0 / 2.1.x 不行） |
| uranus | 2.4.1+ |
| jupiter | 2.3.7+ |
| GeckoLib | 4.9.2 |
| Patchouli | 1.21.1-93+ |
| Jade（可选） | 15.10+，可看蛋孵化性别预期 |

> ⚠️ IAF CE 2.2 起**不需要 Citadel**，别装旧 Citadel。
> 单人游戏：客户端装齐即可；多人服务器：服务端也装同一套。

## 安装步骤

1. 用启动器装好 **Minecraft 1.21.1 + NeoForge 21.1.264+**（Java 21）
2. 把上面所有依赖的 jar + 本目录的 `dragonmaid-...-neoforge.2.jar` 放进 `.minecraft/mods/`
3. 启动游戏，主菜单「Mods」里能看到 `Dragon Maid` 即成功
4. 进游戏后：驯服冰/火/雷龙 → 右键龙打开龙界面 → 人形化形

## 不兼容 / 已知问题

- 只支持 **1.21.1 / NeoForge**，不支持 1.20.1 Forge 与 Fabric
- 只支持 **IAF CE 2.2+**（本版本针对 2.2 的 `PartEntity` 重构做了适配，2.0/2.1 会崩）
- 龙甲只验证了 silver 系；龙炉吐息加热链路未完整验证
- 人形化形的界面交互/逐形态外观未全部截图验证
- 创造模式下石化解药不消耗（原版设计如此）
- 多人断线重连未覆盖

## 出问题怎么办

到移植仓库提 Issue（附 `latest.log` + 复现步骤 + mod 列表）：
https://github.com/bierangas26-cyber/dragonmaid-neoforge/issues

**请不要把本移植的问题反馈给原作者**（原作者只负责 1.20.1 原版）。

## 版权 / 免责

- 原 mod：**[IAF]: Dragon Maid Companion**（作者 **别思丶 / EvenFlowInShipGirl**，1.20.1 Forge）
  原 mod 页面：https://www.curseforge.com/minecraft/mc-mods/iaf-dragon-maid-companion
- 贴图 / 模型 / 动画 / 音效 / 手册内容版权归原作者所有；本仓库只包含移植代码与逐字节保留的原资产。
- 本移植已获原作者许可，**免费、非盈利**发布；禁止售卖或放进付费整合包。
- 若原作者发布官方 NeoForge 版本，请优先使用官方版。

## 其他下载渠道

- GitHub Release：https://github.com/bierangas26-cyber/dragonmaid-neoforge/releases/tag/v1.1.0-beta.2-neoforge.2
- CurseForge（审核中）：https://www.curseforge.com/minecraft/mc-mods/dragon-maid-companion-neoforge
