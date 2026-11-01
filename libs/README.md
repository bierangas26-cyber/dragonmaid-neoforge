# libs/ —— 依赖 jar（不随仓库分发）

本工程用 `compileOnly fileTree(dir: 'libs', include: ['*.jar'])` 引入依赖，**这些第三方模组不随本仓库分发**，请自行下载后放进本目录。

## 需要的 jar（1.21.1 / NeoForge 21.1.264）

| 文件 | 说明 | 来源 |
|---|---|---|
| `iceandfire-2.0.jar` | Ice and Fire Community Edition 2.0（本移植的依赖基线，与原版 Ice and Fire 2.1.13 不同） | CurseForge / Modrinth 搜索 “Ice and Fire Community Edition” |
| `uranus-2.4.1-1.21.1-neoforge.jar` | IAF CE 的前置 | 同上（IAF CE 页面列出的依赖） |
| `jupiter-2.3.7-1.21.1-neoforge.jar` | IAF CE 的前置（配置库） | 同上 |
| `geckolib-neoforge-1.21.1-4.9.2.jar` | GeckoLib 4.9.2（动画） | Modrinth: GeckoLib |
| `Patchouli-1.21.1-93-NEOFORGE.jar` | Patchouli（手册） | Modrinth: Patchouli |
| `jei-1.21.1-neoforge-19.70.0.458.jar` | JEI（可选，仅编译参考） | Modrinth: JEI |
| `citadel-2.7.1-1.21.1.jar` | 仅个别模型基类参考，**运行时不装** | 如有需要 |

> 版本必须与上表一致或兼容；IAF CE 2.0 与 1.x 的包名/API 完全不同，不要混用。
