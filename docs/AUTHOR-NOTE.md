# 给原作者的说明（可直接复制发送）

---

大佬你好，1.21.1 NeoForge 的移植版本已经做出来了，仓库和构建产物都准备好了，你可以先看看有没有原则性问题。

**移植信息**

- 原 mod：Dragon Maid Companion 1.1.0-beta.2（1.20.1 / Forge）
- 目标环境：Minecraft 1.21.1 / NeoForge 21.1.264 / Java 21
- 依赖基线：Ice and Fire Community Edition 2.0（+uranus/jupiter）、GeckoLib 4.9.2、Patchouli
- 授权：仅非盈利使用，README 与 LICENSE 里写明了原作者署名与禁止收费

**资源一致性**

- 359 个内容文件里 358 个与原包逐字节一致；唯一有意改动是 `pack.mcmeta`（1.21.1 需要新格式），原件已备份
- 贴图 / geo 模型 / 动画 / 音效 / 手册内容全部原样保留，没有重画或替换

**验证情况（10 项全过）**

| 项 | 结果 |
|---|---|
| 加载 / 启动 | PASS |
| 生成 / 存活 | PASS |
| 驯服（NBT） | PASS |
| 化形（服务端状态） | PASS |
| 化形（客户端按钮 + 人形渲染） | PASS（客户端崩溃已修） |
| 护卫跟随 | PASS |
| 龙息战斗 | PASS |
| 龙甲渲染（silver 头部） | PASS |
| 护卫战斗崩溃回归 | PASS（未复现） |
| 日志无本 mod ERROR/WARN | PASS |

**尚未覆盖**（会如实标注，不吹）

- 龙甲其它材质（gold / diamond / dragonsteel）与 body / tail 部位未逐一验
- 长时间跑服 / 多人压力未测
- IAF CE 2.0 删掉的 API 对应功能（石化、龙炉等）标记为不支持

**想请你确认的**

1. 这样非盈利发布 / 放 GitHub 是否可以？
2. 有没有哪些功能你希望优先补齐（比如龙甲全材质、某个 UI 页面）？
3. 如果你后面要出官方 NeoForge 版，我这边随时停用这个移植。

仓库地址与编译产物我可以直接发你。

---

## 维护者备注（不用发）

- 仓库：https://github.com/bierangas26-cyber/dragonmaid-neoforge-1.21.1
- Release jar：dragonmaid-neoforge-1.21.1-1.1.0-beta.2.jar（6,812,348 B）
- 源码 zip：`D:\BaiduNetdiskDownload\projects\dragonmaid-neoforge-1.21.1-source.zip`（6.5 MB）
