# Dragon Maid Companion — NeoForge 1.21.1 社区移植（非官方）

> **原作者 / 原 mod**：Dragon Maid Companion 1.1.0-beta.2（作者：**别思丶**）
> **本仓库**：1.20.1 Forge → **Minecraft 1.21.1 / NeoForge 21.1.264** 的社区移植
> **性质**：非官方、**非盈利**、仅供个人与免费整合包使用。

## 下载

| 渠道 | 链接 | 状态 |
|---|---|---|
| GitHub Release（最新） | [v1.2.0-beta.4-neoforge.1](https://github.com/bierangas26-cyber/dragonmaid-neoforge/releases/tag/v1.2.0-beta.4-neoforge.1) | ✅ 追平原版 1.2.0-beta.4 内容 |
| GitHub 中文下载目录 | [download-cn/](download-cn/)（含中文安装说明 + sha256） | ✅ 可用 |
| GitHub Release（旧） | [v1.1.0-beta.2-neoforge.2](https://github.com/bierangas26-cyber/dragonmaid-neoforge/releases/tag/v1.1.0-beta.2-neoforge.2) | ✅ 保留（IAF CE 2.2 兼容版） |
| CurseForge | [Dragon Maid Companion: NeoForge 1.21.1](https://www.curseforge.com/minecraft/mc-mods/dragon-maid-companion-neoforge) | ⏳ 审核中（新项目首次提交需人工审核） |

> **neoforge.2 变更**：适配 Ice and Fire Community Edition **2.2**（2026-11-05 发布）。
> IAF CE 2.2 把 `MultipartPartEntity` 改成了 NeoForge `PartEntity`，旧 mixin 会导致启动即崩，详见 [docs/IAFCE-2.2-COMPAT.md](docs/IAFCE-2.2-COMPAT.md)。

最终发布 jar（1.2.0-beta.4-neoforge.1）：`dragonmaid-neoforge-1.21.1-1.2.0-beta.4-neoforge.1.jar`
size `7,261,374 B` / sha256 `5602FC0E1FDD19913CD1D1EB283C1010E2478888CF1A9ACCDE686E3374DD3E9E`

## 授权说明

本移植已获得原作者许可，并同意**公开发布**（含 CurseForge 等平台，免费分发）。原作者会在其原 mod 页面同步本社区移植的下载链接。使用本仓库即表示你同意：

1. **必须署名原作者**（别思丶）与原 mod（Dragon Maid Companion）；
2. 标注为**社区移植（非官方）**，不得让人误以为是作者官方发布；
3. **保持免费分发**：不得收费（含付费整合包、赞助解锁、付费下载）；
4. 原 mod 的贴图/模型/动画/音效等资产版权归原作者所有。

若原作者发布官方 NeoForge 版本，请优先使用官方版本。本移植的更新会与作者版本保持同步。

---

## 当前状态

| 项 | 结果 |
|---|---|
| 加载 / 启动（NeoForge 1.21.1 服务端） | ✅ PASS（`Done` 正常，无本 mod 相关异常） |
| 资产一致性 | ✅ 359/359 内容文件逐字节一致（唯一有意偏离：`pack.mcmeta` 升级为 1.21.1 格式，见 [docs/asset-sha256.md](docs/asset-sha256.md)） |
| R1 生成 / 存活 | ✅ PASS |
| R2 驯服（NBT） | ✅ PASS |
| R3 化形（服务端状态） | ✅ PASS |
| R3b 化形（客户端按钮/同步/人形渲染） | ✅ PASS |
| R4 护卫跟随 | ✅ PASS |
| R5 龙息（战斗） | ✅ PASS |
| R6 龙甲渲染（silver 头部） | ✅ PASS |
| R7 护卫战斗崩溃回归 | ✅ PASS（无复现） |
| 日志横切（dragonmaid 相关 ERROR/WARN） | ✅ 0 |

详细验证记录（含命令、日志行）：[docs/PORT-STATUS.md](docs/PORT-STATUS.md)、[docs/TASK7-CLIENT-EVIDENCE.md](docs/TASK7-CLIENT-EVIDENCE.md)。

### 尚未覆盖 / 已知限制

- 龙甲的**其它材质**（gold / diamond / dragonsteel_*）与 **body / tail** 部位的贴图未逐一验证（本轮只验了 silver 头部）；
- 长时间运行、多人服务器压力、与其它整合包的组合兼容性未做长跑验证；
- IAF CE 2.0 **已删除的 API** 对应功能（如石化、龙炉等）在本移植中标记为**不支持**，详见 [docs/IAF-CE-MAPPING.md](docs/IAF-CE-MAPPING.md)；
- 仅适配 **IAF Community Edition 2.0**，与旧版 Ice and Fire 2.x 不兼容。

---

## 环境要求

| 项 | 版本 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.264（其它 21.1.x 理论兼容，未逐一验证） |
| Java | 21 |
| Gradle（仅构建） | 8.8+ |

### 运行时依赖（必须一起装）

- **Ice and Fire Community Edition 2.2+**（`iceandfire-2.2.jar`；2.0 / 2.1.x 不支持）
- **uranus** 2.4.1+ / **jupiter** 2.3.7+（IAF CE 的前置，2.2 起不再需要 Citadel）
- **GeckoLib** 4.9.2（NeoForge 1.21.1 版）
- **Patchouli** 1.21.1-93+
- JEI（可选）

> 这些 jar **不随本仓库分发**，请自行下载后放到 `libs/`（编译用）与游戏 `mods/`（运行用），清单见 [libs/README.md](libs/README.md)。

---

## 构建

```bash
# 1) 把依赖 jar 放进 libs/（见 libs/README.md）
# 2) 用 JDK 21 构建
export JAVA_HOME=/path/to/jdk21      # Windows: set JAVA_HOME=D:\path\to\jdk21
gradle build
# 产物：build/libs/dragonmaid-neoforge-1.21.1-<version>.jar
```

本工程没有内置 Gradle Wrapper，用系统 Gradle 8.8+ 即可；构建脚本见 [build.gradle](build.gradle)。

## 安装

1. 安装 Minecraft 1.21.1 + NeoForge 21.1.264；
2. 把依赖（IAF CE 2.0 / uranus / jupiter / GeckoLib / Patchouli）与本 mod 的 jar 一起放进 `mods/`；
3. 启动游戏即可。

---

## 目录结构

```
src/main/java/        移植后的 Java 源码（1.21.1 API）
src/main/resources/   assets / data（来自原 mod，逐字节保留）
docs/                 移植记录、验证证据、IAF CE API 对照
libs/                 依赖 jar 放置处（不提交）
build.gradle          构建脚本（moddev 2.0.141）
excluded.txt          1.21.1 下不参与编译的源文件清单
```

## 致谢

- 原作者 **别思丶** —— 原 mod 与全部美术/动画资产；
- Ice and Fire Community Edition 团队 —— 1.21.1 的 IAF 基础；
- GeckoLib / Patchouli —— 动画与手册库。

---

## English summary

Unofficial **1.21.1 / NeoForge 21.1.264** port of *Dragon Maid Companion* 1.1.0-beta.2 by **别思丶**. Ported with the author's permission for **non-commercial** use only. Original assets remain the author's property; attribution and non-commercial terms are mandatory. Requires Ice and Fire Community Edition 2.0 (+ uranus/jupiter), GeckoLib 4.9.2 and Patchouli. Verified: mod load, entity spawn, taming, transformation (server+client), escort follow, breath combat, dragon armor rendering, escort-combat crash regression (10/10 checks). See `docs/PORT-STATUS.md` for evidence.
