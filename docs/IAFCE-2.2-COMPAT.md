# IAF CE 2.2 兼容记录（2026-11-07）

## 为什么改

Ice and Fire Community Edition 在 **2026-11-05** 为 1.21.1 / NeoForge 发布了 **2.2**（CF 文件 `IceAndFireCE-2.2-1.21.1-neoforge.jar`，20,952,409 B）。
原 mod（Dragon Maid Companion）作者截至 2026-11-07 仍停留在 **1.1.0-beta.2**（CF 文件 9372171，6,759,417 B，与移植时取用的 jar 完全一致），所以本次不涉及内容更新，只处理依赖 API 变化。

## 实际破坏点

IAF CE 2.2 把 `com.iafenvoy.iceandfire.entity.MultipartPartEntity` 从

```
2.0: abstract class MultipartPartEntity extends Entity implements OwnableEntity
     + SynchedEntityData PARENT_UUID  + getParent()/getParentId()
2.2: abstract class MultipartPartEntity<T extends LivingEntity>
     extends net.neoforged.neoforge.entity.PartEntity<T> implements OwnableEntity
     父实体是 PartEntity 的 final 字段 → getParent()，不再有 getParentId()
```

旧 `DragonPartInjuryMixin` 还是 2.0 的写法：

```java
@Shadow(remap = false) public abstract Entity getParent();
@Shadow(remap = false) public abstract UUID getParentId();
```

加载即崩：

```
[modloading-worker-0/FATAL] [mixin/]: Mixin apply for mod dragonmaid failed
  dragonmaid.mixins.json:DragonPartInjuryMixin -> ...MultipartPartEntity:
  InvalidMixinException @Shadow method getParent ... was not located in the target class
  (No refMap loaded)
```

## 改法

`DragonPartInjuryMixin` 改为走 NeoForge `PartEntity` API（`src/main/java/com/example/dragonmaid/mixin/DragonPartInjuryMixin.java`）：

```java
@Mixin(remap = false, value = {MultipartPartEntity.class})
public abstract class DragonPartInjuryMixin extends Entity {
    public boolean canBeHitByProjectile() {
        Entity entity = (Object) this instanceof PartEntity<?> part ? part.getParent() : null;
        if (entity instanceof DragonBaseEntity dragon
            && HumanoidLife.isInjured(dragon)
            && dragon.getHealth() <= 0.0F) {
            return false;
        }
        return super.canBeHitByProjectile();
    }
}
```

- 用 `(Object) this` 中转是因为 mixin 源类的编译期父类仍是 `Entity`，不能直接写 `this instanceof PartEntity`。
- 删掉了 `getParentId()` 的客户端兜底搜索（2.2 的 PartEntity 持有直接引用，不需要 UUID 反查）。
- `neoforge.mods.toml`：`iceandfire` 版本范围 `[2.0,)` → `[2.2,)`。
- Citadel 不再是 IAF CE 的运行前置（2.2 jar 里已无 citadel 条目），`build.gradle` 的 GameTest 运行依赖中已移除。

## 验证证据

| 项 | 结果 |
|---|---|
| 对 IAF CE 2.2 编译 | ✅ 0 error（仅 deprecation 警告） |
| `gradle runGameTestServer` | ✅ `All 9 required tests passed :)`，BUILD SUCCESSFUL |
| 同上，但不装 Citadel | ✅ 9/9（证明不再需要 Citadel） |
| 全新开发服务端 `gradle runServer` | ✅ `Done (0.767s)`；mod 列表含 `Dragon Maid 1.1.0-beta.2-neoforge.2` 与 `Ice And Fire Community Edition 2.2`；24 main mixin 全部 apply，无 FATAL |
| `summon iceandfire:ice_dragon/fire_dragon/lightning_dragon` | ✅ 三系均可生成；`TamedDragon/AgeTicks/Gender/Variant` 等旧 NBT 键仍有效 |
| 开发客户端 `gradle runClient` | ✅ 主菜单 10 mods，无 mixin 失败 |
| 客户端连入开发服务端 | ✅ `Loaded 47 advancements`，世界正常渲染 |
| IAF CE 2.2 龙模型渲染 | ✅ 蓝宝石冰龙模型正常显示 |
| Patchouli 手册 | ✅ “Dragon Maid Guide 39th Edition” 正常打开 |

## 尚未覆盖（如实记录）

- 人形化形后的**外观渲染**尚未逐个形态截图核对（服务端 NBT 可置 `NeoForgeData.DragonMaid.Humanoid=1b`，但客户端同步/渲染路径建议进游戏点 GUI 化形按钮确认一次）。
- 多人断线重连、龙炉吐息加热链路仍未端到端验证（与 IAF 版本无关的既有缺口）。

## 复现命令

```bash
# 1) 取 IAF CE 2.2（1.21.1 NeoForge）
#    https://www.curseforge.com/minecraft/mc-mods/iceandfire-ce/files/9455119
#    放到 libs/iceandfire-2.2.jar
# 2) 编译 + GameTest
gradle compileJava
gradle runGameTestServer
```
