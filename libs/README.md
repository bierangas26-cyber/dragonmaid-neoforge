# libs/ —— 依赖 jar（不随仓库分发）

本工程用 `compileOnly fileTree(dir: 'libs', include: ['*.jar'])` 引入依赖，**这些第三方模组不随本仓库分发**，请自行下载后放进本目录。

## 需要的 jar（1.21.1 / NeoForge 21.1.264）

| 文件 | 说明 | 来源 |
|---|---|---|
| `iceandfire-2.2.jar` | Ice and Fire Community Edition **2.2**（1.21.1 NeoForge 构建；本移植的当前依赖基线） | CurseForge 项目 1040076 / Modrinth “Ice and Fire Community Edition” |
| `uranus-2.4.1-1.21.1-neoforge.jar` | IAF CE 的前置 | 同上（IAF CE 页面列出的依赖） |
| `jupiter-2.3.7-1.21.1-neoforge.jar` | IAF CE 的前置（配置库） | 同上 |
| `geckolib-neoforge-1.21.1-4.9.2.jar` | GeckoLib 4.9.2（动画） | Modrinth: GeckoLib |
| `Patchouli-1.21.1-93-NEOFORGE.jar` | Patchouli（手册） | Modrinth: Patchouli |
| `jei-1.21.1-neoforge-19.70.0.458.jar` | JEI（可选，仅编译参考） | Modrinth: JEI |

> 版本必须与上表一致或兼容。
>
> **IAF CE 2.2 起不再需要 Citadel**（2.2 已把 Citadel 的类型并入自身/NeoForge PartEntity），本移植也不再依赖它。
>
> 从 IAF CE 2.0/2.1.x 升级时注意：2.2 把 `MultipartPartEntity` 从 `Entity` 改成了 NeoForge `PartEntity`，
> 本仓库的 `DragonPartInjuryMixin` 已按新 API 重写（见 `docs/IAFCE-2.2-COMPAT.md`）。
> 对应地，`neoforge.mods.toml` 里 `iceandfire` 的版本要求已提升到 `[2.2,)`。
