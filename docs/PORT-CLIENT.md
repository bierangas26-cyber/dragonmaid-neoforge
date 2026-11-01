# Dragon Maid 移植 P3（客户端渲染）— 分析与对照表

> 本文件由 task-4 owner 产出。工程：`dragonmaid-port`。源码目录 `src/main/java/com/example/dragonmaid/client/`（55 个文件）。
> 依赖比对对象：`libs/geckolib-neoforge-1.21.1-4.9.2.jar`（javap 实测签名）+ `build/moddev/artifacts/neoforge-21.1.264-sources.jar`（MC 1.21.1 源码实测）。
> 未跑 gradle（构建归 task-2 owner）。

## 1. 结论先说

1. **客户端渲染层的主要阻塞不是 GeckoLib**，而是 (a) `com.example.dragonmaid.client` 里 13 个文件依赖 IAF（`DragonBaseEntity` 等，task-3 的范围），(b) 另有 21 个文件依赖 P1 尚未恢复的注册类（`DragonForms`/`ModItems`/`ModMenus`/`ModParticles`/`ModEntities`/`ServiceDragonArmorItem`/`HumanoidRoar`/`HumanoidLife`/`HumanoidWater`/`DragonNetwork`/`AccessoryMenu`/`KnowledgeMenu`/`RecallMenu`/`DragonAccessories`/`ModParticles`）。
2. **GeckoLib 4.7.4 → 4.9.2 的 API 变更比预期小**：`animation.*` / `cache.object.*` / `model.GeoModel` / `renderer.layer.GeoRenderLayer` / `GeoBone` 的这些方法与 4.7 基本一致（见 §3 逐条实测）。真正需要改的只有 4 处：`RenderUtils→RenderUtil`、`preRender/postRender` 颜色参数（4×float→1×int）、`core.molang.MolangParser` 整个删除、`JsonUtil.GEO_GSON` 删除。
3. **真正的大改是 MC 1.20.1 → 1.21.1 的 `VertexConsumer`**：整族方法重命名 + `endVertex()`/`defaultColor()`/`unsetDefaultColor()` 删除（见 §4）。所有手写顶点的类都必须改：`ArticulatedArmorMesh`、`FemaleChestplateMesh`、`IceFemaleRenderer`（内嵌 untinted 包装）、`IceFemaleModel`、`SleepPoseGrounding`、`TailArmorMesh`、`ServiceArmorResources`、`FemaleChestplateMaterial`。

## 2. 依赖版本实测

| 原 mod（1.20.1） | 本包（1.21.1） | 结论 |
|---|---|---|
| GeckoLib 4.7.4 | 4.9.2（`libs/geckolib-neoforge-1.21.1-4.9.2.jar`，262 个 class） | 包结构一致，无需改包名；只有 4 处特例 |
| Ice and Fire 2.1.13（`com.github.alexthe666`） | IAF CE 2.0（`com.iafenvoy`） | 客户端渲染层 13 个文件受牵连（task-3） |
| Patchouli 1.20.1-85 | Patchouli 1.21.1-93 | 只影响 `mixin/client/PatchouliBookTitleMixin`（不在本任务范围） |

## 3. GeckoLib 4.7.4 → 4.9.2 对照表（javap 实测）

| 旧（4.7.4，本工程现状） | 新（4.9.2） | 证据 | 需改文件 |
|---|---|---|---|
| `software.bernie.geckolib.util.RenderUtils` | `...util.RenderUtil`（去 s） | javap：`RenderUtil.translateToPivotPoint(PoseStack,GeoBone)` 等全部存在 | FemaleChestplateMesh, IceFemaleEquipmentLayer, IceFemaleRenderer |
| `preRender(PoseStack,T,BakedGeoModel,MultiBufferSource,VertexConsumer,boolean,float,int,int,float red,float green,float blue,float alpha)` | `preRender(...,int colour)`（4 个 float → 1 个打包 int） | javap 10 参版；旧调用见 IceFemaleRenderer:289（13 参） | IceFemaleRenderer, HumanoidDragonRenderer |
| `postRender(..., float r,float g,float b,float a)` | `postRender(..., int colour)` | javap | 同上 |
| `software.bernie.geckolib.core.molang.MolangParser` | **删除**；`loading.math.MolangQueries` / `MathParser` 取代 | jar 内无 `core/` 包 | ClientRegistration |
| `JsonUtil.GEO_GSON` | **删除**（`JsonUtil` 类还在） | jar 内 `util/JsonUtil` 存在，无 GEO_GSON 字段 | 若用到需自建 Gson |
| `GeoBone#getRotX/setRotX/getPosX/getPosY/getPosZ/getScale*/getPivot*/getChildBones/getCubes/isHidden/setHidden/setChildrenHidden/setTrackingMatrices/resetStateChanges/mark*AsChanged` | **全部保留** | javap 逐条核对 | 无需改 |
| `GeoModel#getModelResource(T)/getTextureResource(T)/getAnimationResource(T)/getAnimationResourceFallbacks(T)/getBakedModel(ResourceLocation)` | **全部保留** | javap | 无需改 |
| `GeoRenderer#applyRenderLayersForBone(PoseStack,T,GeoBone,RenderType,MultiBufferSource,VertexConsumer,float,int,int)` | **签名不变**（9 参） | javap | 无需改 |
| `GeoRenderLayer` 抽象方法 `preRender/render/renderForBone` | **不变** | javap | IceFemaleEquipmentLayer, TailArmorLayer 无需改签名 |
| `constant.DataTickets` / `constant.dataticket.DataTicket` | **保留** | jar 内存在 | 无需改 |
| `animation.{Animation,AnimationController,AnimationState,RawAnimation,PlayState,EasingType}` / `animation.state.BoneSnapshot` | 包名与方法名一致（`BoneSnapshot.getRotX/getOffsetX/getScaleX` 全在） | javap | 无需改 |
| **`Keyframe<T extends com.eliotlash.mclib.math.IValue>`** | **`Keyframe<T extends software.bernie.geckolib.loading.math.MathValue>`**（4.9 自带数值类型，不再依赖 mclib） | javap：泛型上界是 `MathValue`；jar 内已无 `com/eliotlash` | **SharedAnimationPose（已改）** |
| `com.eliotlash.mclib.math.{IValue, Constant}` | `software.bernie.geckolib.loading.math.MathValue` / `...loading.math.value.Constant` | jar 扫描 | SharedAnimationPose（已改） |
| `loading.object.BakedModelFactory` / `$VertexSet` / `GeometryTree` / `loading.json.raw.Model` / `FaceUV$Rotation` | **保留** | jar 内存在（嵌套类是 `BakedModelFactory$VertexSet`） | FemaleChestplateMesh / TailArmorMesh / ServiceArmorResources |

## 4. MC 1.20.1 → 1.21.1 `VertexConsumer` 对照表（源码实测）

1.21.1 的 `com.mojang.blaze3d.vertex.VertexConsumer` 抽象方法只有 6 个：
`addVertex(float,float,float)`、`setColor(int,int,int,int)`、`setUv(float,float)`、`setUv1(int,int)`、`setUv2(int,int)`、`setNormal(float,float,float)`。

| 旧（1.20.1） | 新（1.21.1） |
|---|---|
| `vertex(double x,double y,double z)` | `addVertex(float,float,float)`（另有 `addVertex(PoseStack.Pose,...)` / `addVertex(Matrix4f,...)` / `addVertex(Vector3f)`） |
| `color(int r,int g,int b,int a)` | `setColor(int r,int g,int b,int a)`；另有 `setColor(int packed)`、`setColor(float×4)`、`setWhiteAlpha(int)` |
| `uv(float u,float v)` | `setUv(float,float)` |
| `overlayCoords(int u,int v)` | `setOverlay(int packed)`（其默认实现转发到 `setUv1`） |
| `uv2(int u,int v)` | `setUv2(int,int)` / `setLight(int packedLight)`（默认实现转发到 `setUv2`） |
| `normal(float,float,float)` | `setNormal(float,float,float)`（另有 `setNormal(PoseStack.Pose,...)`） |
| `endVertex()` | **删除**（顶点在下一次 `addVertex` 时提交；收集器需自行 flush） |
| `defaultColor(...)` / `unsetDefaultColor()` | **删除** |
| `vertex(x,y,z, r,g,b,a, u,v, overlay, light, nx,ny,nz)`（14 参便捷重载） | `addVertex(x,y,z, int color, u,v, int overlay, int light, nx,ny,nz)`（11 参 default） |

顺带一条重要事实（决定 `ArticulatedArmorMesh.capture` 能工作）：
1.21.1 `ModelPart.Cube.compile()` 每个顶点只调 **一次** 11 参 `addVertex(...)`（`ModelPart.java:362`），
它内部依次调 `addVertex(3参)` → `setColor` → `setUv` → `setOverlay` → `setLight` → `setNormal`，
所以自定义收集器只需实现 6 个抽象方法 + 在下一个 `addVertex` 时把上一个顶点入库。

## 5. 55 个 client 文件分层（按可编译性）

判据：文件本身已排除（`excluded.txt`），且其 import 的工程内类是否仍被排除。

### T1 可立即移植（无 IAF、无被排除的工程依赖）

| 文件 | 需要改什么 | 状态 |
|---|---|---|
| `ArticulatedArmorMesh.java` | VertexConsumer 新 API（capture/render 全改） | **已移植，待编译** |
| `LegArmorMesh.java` | 无 API 变更（只调 ArticulatedArmorMesh） | **无需改，待编译** |
| `SwimPose.java` | 无 | **无需改，待编译** |
| `BreathPoseLayer.java` | `animation.Animation` 4.9.2 保留 | **无需改，待编译** |
| `SharedAnimationPose.java` | `IValue→MathValue`、`Constant` 换包，算法与对外签名未动 | **已移植，待编译** |
| `FemaleChestplateMaterial.java` | IAF CE：`DragonArmorType→DragonArmorMaterial` | **已移植，待编译** |
| `FemaleChestplateMesh.java` | `RenderUtils→RenderUtil`（4 处）+ 新顶点 API | **已移植，待编译** |
| `FemaleChestplateResources.java` | 无（`SimplePreparableReloadListener`/`getResource` 1.21.1 都在） | **无需改，待编译** |
| `ManagedLayeredTexture.java` / `FadingBreathParticle.java` | 无 GeckoLib；需确认 1.21.1 `AbstractTexture`/`TextureSheetParticle` 构造 | 待做 |

### T2 依赖 P1 注册（等 task-2）

`IceFemaleRenderer`（DragonForms）、`IceFemaleModel`（DragonForms/HumanoidRoar/HumanoidLife/HumanoidWater）、`IceFemaleAnimatable`（DragonForms）、`IceFemaleEquipmentLayer` + `TailArmorLayer` + `ServiceDragonArmorTextures`（ServiceDragonArmorItem）、`HumanoidDragonRenderer`/`HumanoidMotion`（DragonForms/Humanoid*）、`ClientRegistration`（ModItems/ModMenus/ModParticles/RecallClientContent）、`ClientEvents`（DragonAccessories/DragonForms/AccessoryMenuAction/DragonNetwork）、`AccessoryScreen`（DragonForms/AccessoryMenu）、`KnowledgeScreen`（DragonForms/KnowledgeMenu）、`RecallScreen`（RecallMenu/DragonNetwork/RecallRequestMessage/RecallRosterMessage）、`ExecutionPrompt`（DragonNetwork/*Message）、`ReinforcedHornRendering`（ModEntities）、`BreathVisuals`/`ClientBreathWaves`（ModParticles）、`ClientBreathAnimations`/`ClientTailSweepAnimations`（DragonForms + 网络消息）

### T3 依赖 IAF CE 2.0（等 task-3）

`ClientMouthEffects`（DragonBaseEntity/IceDragonEntity/LightningDragonEntity）、`FemaleDragonAssets`（4 个 IAF 类）、`HumanoidBannerLayer`、`HumanoidStatueMaterials`（IafRenderTypes）、`HumanoidStatueRenderer`（StoneStatueEntity/IafEntities）、`ServiceArmorResources`（DragonBaseEntity 等）、`NativeMetalArmor`（**ModelCopperArmor/ModelSilverArmor 在 CE 中已删除**）、`NativeMetalEquipment`、`DragonScreenTabs`（DragonForms + DragonBaseEntity）——`FemaleChestplateMaterial` 已由本任务改完（见 §8）

## 6. 下一步（给构建 owner 的接线顺序）

1. 先 un-exclude T1 批次 → build → 把 GeckoLib/vertex 相关错误回给我，我逐个修。
2. `IceFemaleRenderer`/`IceFemaleModel`/`IceFemaleAnimatable` 是渲染主干，等 P1 的 `DragonForms` 落地后我立刻接线（它们的 GeckoLib 侧改动已在本表列清）。
3. `ClientRegistration`（renderer 注册 + HUD/菜单绑定）最后做，因为它同时依赖 P1 的 ModMenus/ModItems 和 P3 的渲染类。
4. 资源侧：`assets/dragonmaid/` 已 100% 逐字节一致（geo 21 / animations 23 / textures 106），渲染层不需要动资源；若某个 geo 的 bone 名对不上，`GeoModel.crashIfBoneMissing()` 可临时置 false 来定位。

## 7. 踩坑预警

- `GeoReplacedEntityRenderer` 的 `preRender` 在 4.9.2 里 **没有 RenderType 参数**；`renderRecursively` 和 `applyRenderLayersForBone` 仍然有，不要一起删。
- 4.9.2 的 color 是 **int（ARGB）**，旧的 4 个 float 不要再传；`RenderUtil` 没有 `translateToPivotPoint` 以外的兼容层。
- 1.21.1 `VertexConsumer` 没有 `endVertex()`：收集型消费者（本工程 `ArticulatedArmorMesh.Capture`、`IceFemaleRenderer.untinted`）必须自己 flush 最后一个顶点，否则每个模型少 1 个顶点 = 三角形缺失。
- `IceFemaleRenderer.untinted` 旧实现还覆盖了 `overlayCoords/uv2/defaultColor/unsetDefaultColor`，新接口里这些要么改名（`setOverlay/setUv2`）要么删除；改写时只保留 6 个抽象方法 + 需要的 default 转发。
## 8. IAF CE 2.0 客户端侧实测事实（可直接给 P2 用）

| 旧（IAF 2.1.13） | CE 2.0 现状（扫 jar 实测） | 受影响文件 |
|---|---|---|
| `DragonArmorItem.DragonArmorType`（嵌套枚举） | **删**；改用 `com.iafenvoy.iceandfire.data.DragonArmorMaterial`（record + 静态单例），`DragonArmorItem.type` 为 public final | FemaleChestplateMaterial（**已改**） |
| `client.model.armor.ModelCopperArmor` / `ModelSilverArmor` | **未删除，只是换包改名**：`render.model.armor.CopperArmorModel` / `SilverArmorModel`，`createMesh(CubeDeformation,float)` 签名一致（pregen-dev 纠正 + 我方 javap 复核） | NativeMetalArmor（**已改**，import/类名替换；enum switch → record 恒等比较） |
| `client.render.IafRenderTypes` | **存在**：`com.iafenvoy.iceandfire.registry.IafRenderTypes`（只改 import） | HumanoidStatueMaterials |
| `enums.EnumDragonTextures(.Armor)` | **删**（CE 无此枚举） | ServiceDragonArmorTextures（需按 DragonArmorMaterial 重组纹理路径或降级） |
| `client.render.entity.RenderStoneStatue` | **删**，替代 `com.iafenvoy.iceandfire.render.entity.StoneStatueEntityRenderer` | HumanoidStatueRenderer |

`DragonArmorMaterial` 静态实例：`IRON / COPPER / SILVER / GOLD / DIAMOND / NETHERITE / DRAGON_STEEL_FIRE / DRAGON_STEEL_ICE / DRAGON_STEEL_LIGHTNING`。
旧枚举 → 新实例：前 5 个同名，FIRE/ICE/LIGHTNING → `DRAGON_STEEL_*`，CE 的 `NETHERITE` 无旧对应（返回 null）。

## 9. 待办（本任务剩余）

- [ ] 等 P1：接 `IceFemaleRenderer` / `IceFemaleModel` / `IceFemaleAnimatable` 主干（新 `preRender` 10 参 + int colour）
- [ ] 等 P1：`IndividualEquipmentLayer`/`TailArmorLayer`（ServiceDragonArmorItem）
- [ ] 等 P2：T3 的 13 个 IAF 客户端文件
- [ ] 最后：`ClientRegistration`（renderer 注册 + HUD/菜单绑定）
## 10. 迁移完成清单（本轮实做）

| 文件 | 改了什么 | 依据 |
|---|---|---|
| `ArticulatedArmorMesh` | 收集型 `VertexConsumer` 重写为 6 个抽象方法 + flush；渲染端 11 参 `addVertex` + 手打包 ARGB | 1.21.1 VertexConsumer 源码 + `ModelPart.Cube.compile()` |
| `SharedAnimationPose` | `Keyframe<IValue>`→`Keyframe<MathValue>`，`Constant` 换包；算法/对外签名未动 | javap `Keyframe`/`MathValue` |
| `FemaleChestplateMesh` | 4 处 `RenderUtils`→`RenderUtil`；`vertex(14参)`→`addVertex(11参)` | javap `RenderUtil` |
| `FemaleChestplateMaterial` | IAF CE `DragonArmorType`→`DragonArmorMaterial`（record+静态单例） | 扫 `iceandfire-2.0.jar` |
| `IceFemaleRenderer` | `preRender` 13→10 参（int colour）、`renderRecursively` 14→11 参、`untinted()` 按新接口重写、`RenderUtils`→`RenderUtil` | javap `GeoReplacedEntityRenderer`/`GeoRenderer` |
| `IceFemaleEquipmentLayer` | `renderCube` 9→6 参、`ModelPart.render` 8→5 参（packed colour）、`RenderUtil` | javap `GeoRenderer`；1.21.1 `ModelPart` |
| `ClientRegistration` | `core.molang.MolangParser`（已删）→ `MolangQueries.clearActor()` | javap `MolangQueries` |
| `ServiceDragonArmorTextures` | `EnumDragonTextures.Armor` + ICE/LIGHTNING/FIRE 三分支 → CE `getArmorTexture`，空槽跳过、0 层返回 null | javap CE jar |
| `ClientEvents` | 删除未使用的 `com.github.alexthe666.iceandfire.IceAndFire` import | 编译 |
| `ServiceArmorResources` | **零改动**：用到的 `BakedModelFactory`/`GeometryTree`/`BakedGeoModel.topLevelBones`/`GeoBone.getChildBones` 在 4.9.2 全部同名；IAF 只做 `instanceof` 选贴图目录（三个实体类 CE 都有） | javap + 全量 API 扫描 |
| `NativeMetalArmor` | IAF CE 未删、只换包改名：`CopperArmorModel`/`SilverArmorModel`，`createMesh` 同签名；`switch(DragonArmorType)` → record 恒等比较 | javap CE jar（pregen-dev 先发现、我方复核） |
| `TailArmorLayer` | `renderCube` 9→6 参；**`EnumDragonTextures.Armor` 已消除**：改用 CE `DragonArmorFeatureRenderer.getArmorTexture(stack, slot)` + `getItemBySlot`，空槽直接跳过 | javap CE jar（pregen-dev 提供 API 线索） |

**全客户端树 `RenderUtils` / `com.eliotlash` / `core.molang` / `1.20.1 VertexConsumer` 用法已清零**（最后一遍全目录扫描：0 处）。
无需改动即可编译（API 层）：`LegArmorMesh`、`SwimPose`、`BreathPoseLayer`、`FemaleChestplateResources`、`IceFemaleAnimatable`（javap 逐条核对：`DataTicket`/`AnimationState`/`GeoAnimatable`/`AnimationController` 4.9.2 全部同名）。

## 11. 当前唯一剩余阻塞（不在本任务 write scope 内）

渲染主干 `IceFemaleRenderer` / `IceFemaleModel` / `IceFemaleAnimatable` / `IceFemaleEquipmentLayer` / `ClientRegistration` / `ClientEvents` 的编译依赖落在：
`HumanoidRoar`、`HumanoidWater`（combat/data 包）、`NativeMetalArmor`/`NativeMetalEquipment`/`ServiceArmorResources`/`ServiceDragonArmorTextures`（IAF，P2）、`ModItems`/`ModMenus`/`ModParticles`/`RecallClientContent`（P1）。
这些类一旦 un-exclude，本任务已改好的客户端文件应当直接编译（API 迁移已做完）。

**IAF 硬点已清零**：`TailArmorLayer` 与 `ServiceDragonArmorTextures` 均已改用 CE `DragonArmorFeatureRenderer.getArmorTexture(stack, slot)`；客户端树里 `com.github.alexthe666.*` / `EnumDragonTextures` 实际 import **0 处**（仅注释提及）。

跨包前置（task-3 scope）：`mixin/client/ServiceDragonArmorMixin.java:45` 需对 `ServiceDragonArmorTextures.texture(dragon)` 加 null 判空（该方法现在按 CE 语义可能返回 null）。
CE 的替代贴图布局已实测：`assets/iceandfire/textures/entity/dragon_armor/armor_<part>_<material>.png`（part=head/neck/body/tail，material=copper/iron/silver/gold/diamond/netherite/dragon_steel_*），**不再按龙种分三套**。
等 pregen-dev 给出「旧 Armor 枚举值→CE 材质名」与「DragonArmorPart.fromSlot 槽位对照」即可收尾。
## 12. 客户端实测抓到的运行时 bug（编译期发现不了）

隔离客户端（`D:\mc-model\work\_probe_dm_client_min`，最小 mod 集 + 本 mod）跑到主菜单过程中命中两个真 bug：

### 12.1 GeckoLib 4.9 反序列化两层坑（已修，client scope）

| 层 | 现象 | 根因 | 修法 |
|---|---|---|---|
| 1 | `NullPointerException: Model.minecraftGeometry() is null` → `GeometryTree.fromModel` | `Model` 是 record，真实 JSON 键是 `format_version`/`minecraft:geometry`，且自带 `public static JsonDeserializer<Model> deserializer()`；`compat/GeoJson.GSON` 是裸 `new GsonBuilder().create()`，字段名对不上 → 全 null | 新增 `client/GeoModelJson.java` 注册 `Model.deserializer()` |
| 2 | `NullPointerException: Cannot load from double array because "array" is null` → `RenderUtil.arrayToVec` ← `BakedModelFactory$Builtin.constructBone` | 4.9 的 `constructBone` 对 `bone.pivot()/rotation()` **不判空**（4.7 会兜默认值），本 mod 的 bedrock geo 部分骨骼省略这两个字段 | `GeoModelJson.normalize(JsonObject)`：deepCopy 后给每个 bone 补 `pivot:[0,0,0]`、`rotation:[0,0,0]` |

调用点：`FemaleChestplateMesh.load`（第 85 行起）、`ServiceArmorResources`（第 199 行）。
`compat/GeoJson.java` 目前无调用者（grep `GeoJson.GSON` = 0），建议后续把 adapter 注册进去而非各自建 Gson。

### 12.2 KnowledgeUpgrades 非幂等（已修，lead 特批的 scope 例外）

```
IllegalArgumentException: Knowledge advancement already registered: 2
  at learning.KnowledgeUpgrades.register(:43) ← registerDefaults(:36)
  → DeferredWorkQueue → Failed to wait for future Common setup → broken mod state → 客户端到不了主菜单
```
全项目只有 `DragonMaid.java:91` 调 `registerDefaults`，但客户端日志里 `HELLO FROM COMMON SETUP` 出现两次、加载期两次资源重载 → 同一个 DeferredWorkQueue 任务被执行两次 → 第二次 `putIfAbsent` 命中抛错。
修法（最小）：`registerDefaults()` 先 `unregister(2)/unregister(3)` 再注册，使其幂等；`register()` 的「重复注册抛错」语义不变。
## 13. 客户端验收记录（task-4 门禁，已通过）

环境：`D:\mc-model\work\_probe_dm_client_min`（最小 mod 集：geckolib 4.9.2 + IAF CE 2.0 + uranus + jupiter + Patchouli + curios + architectury + 本 mod = 8 jar；mods 以硬链接从 client-live 取，**未修改 client-live/live-server**）。
jar（**验收用**）：`dragonmaid-neoforge-1.21.1-1.1.0-beta.2.jar` 6,812,359 B / 2026-11-01 10:47:09 / sha256 `679CBD46E7444EA4C8611781E1F71C25CB0896B3A948268A1F25936A456DE9E4`（excluded=0，0 编译错误）。
jar（**最终归档**）：同一 jar 重新打包 6,812,359 B / 2026-11-01 10:53:11 / sha256 `D73161076F5A99A1DC54EC38F7AF678B45299316DE46179B654EBBB9FEA177B6` —— 与验收版源码一致（字节大小相同，仅 zip 时间戳不同），验收结论对归档版同样有效（构建负责人 2026-11-01 10:53 确认）。

关键日志（`_probe_dm_client_min/launch-out.log`，全文 125 行）：
```
[10:48:47] Found mod file "dragonmaid-neoforge-1.21.1-1.1.0-beta.2.jar"
            Dragon Maid 1.1.0-beta.2 (dragonmaid)
[10:48:58] [dragonmaid] Dragon Maid 1.1.0-beta.2 - NeoForge 1.21.1 port bootstrap (assets stage)
[10:49:00] HELLO FROM CLIENT SETUP / MINECRAFT NAME >> Owner
[10:49:04] OpenAL initialized on device OpenAL Soft on ...
[10:49:04] Sound engine started            ← 主菜单
[10:49:04] Created: 1024x1024x4 minecraft:textures/atlas/blocks.png-atlas
```

| 验收项 | 结果 |
|---|---|
| mod 加载（mod 列表 + bootstrap 日志） | ✅ |
| 到主菜单（`Sound engine started` + 图集构建） | ✅ |
| `Mixin apply failed` | 0 |
| broken mod state / `Cowardly refusing` / `Failed to wait` | 0 |
| crash report | 无（`crash-reports` 目录不存在） |
| 本 mod 资源监听器异常（`FemaleChestplateResources` / `ServiceArmorResources` / `FemaleChestplateMesh`） | 0 |
| 本次运行 ERROR/FATAL 行 | **NONE** |

### 13.1 GeckoLib 4.9 反序列化的真正根因（三层现象，一个根因）

前两轮修的都是「症状」，最终根因是 **Gson 实例用错**：

| 现象 | 表层解释 | 真正根因 |
|---|---|---|
| `Model.minecraftGeometry() is null` | `Model` 是 record、键名不匹配 | 裸 Gson 没有 `Model` 适配器 |
| `RenderUtil.arrayToVec` NPE（pivot/rotation） | 4.9 不判空 + geo 省略字段 | 同上（`Bone.deserializer()` 未被使用） |
| `ModelProperties.textureWidth()` NPE | description 没映射成 modelProperties | 同上（`MinecraftGeometry.deserializer()` 未被使用） |

**正解**：GeckoLib 把「全配置 Gson」从 4.7 的 `JsonUtil.GEO_GSON` 搬到了
`software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter.GEO_GSON`（`FileLoader.loadModelFile` 内部用的就是它）。
`client/GeoModelJson.GSON` 现直接引用该实例；`normalize()` 仍保留 pivot/rotation/cubes 默认值作纵深防御。