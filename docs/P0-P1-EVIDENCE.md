# P0 + P1 evidence - Dragon Maid NeoForge 1.21.1 port

Date: 2026-11-01
Owner: dragonmaid-port (task-2)
Probe server: D:\mc-model\work\_probe_dm (mods = live-server 79 hardlinks + port jar), port 25605 / RCON 25606
Jar under test: build/libs/dragonmaid-neoforge-1.21.1-1.1.0-beta.2.jar (6,562,763 B)

## P0 - 1.21.1 data pack format

1. `src/main/resources/pack.mcmeta` upgraded from 1.20.1 `pack_format: 15` to
   `pack_format: 34` + `supported_formats: {min_inclusive:15, max_inclusive:48}`
   (34 = 1.21.1 resource packs, 48 = 1.21.1 data packs; the range also accepts the old 1.20.1 value).
2. 1.21 data folder names are generated as byte-identical copies during `processResources`:
   `advancements->advancement`, `recipes->recipe`, `loot_tables->loot_table`, `predicates->predicate`,
   `item_modifiers->item_modifier`, `functions->function`, `structures->structure`,
   `tags/{blocks,items,entity_types,fluids,game_events,functions} -> tags/{block,item,entity_type,fluid,game_event,function}`.
   Build log: `[dragonmaid] emitted 31 1.21 data-pack alias entries`.
   The original 1.20.1 files stay byte-identical in the jar.
3. 1.21.1 ItemStack JSON uses `id` instead of 1.20.1 `item` inside recipe result / advancement icon.
   Only the generated 1.21 alias copies are rewritten: build log
   `[dragonmaid] rewrote 23 1.21 alias json files (item->id)`.

### Verification (probe.log)
```
[10:15:06] [main/INFO] [minecraft/RecipeManager]: Loaded 3976 recipes
[10:15:06] [main/INFO] [minecraft/AdvancementTree]: Loaded 3867 advancements
```
Baseline before this mod's data loaded: 3957 recipes / 3828 advancements.
Delta: **+19 recipes / +39 advancements** = exactly the mod's
`data/dragonmaid/{recipes,advancements}` content (19 recipes + 20 advancements + 19 recipe-advancements).
No `dragonmaid:` parse errors remain in the log (earlier state had 19 "Parsing error ... No key id" lines).

## P1 - registries + network wiring

Restored and compiled (excluded.txt 155 -> 44 at the accepted build):
- items: `ModItems` (25 items), block: `RecallContent.block()` + `ModItems.DRAGON_RECALL_ALTAR`,
  entity: `ModEntities.REINFORCED_HORN`, particles: `ModParticles` (11 types),
  menus: `ModMenus` (knowledge / accessories / recall), recipes: `ModRecipes` (5 serializers),
  creative tab: `DragonMaid.DRAGON_MAID_TAB`, configs: server + common.
- network: all 16 legacy message classes registered through the `compat.net.SimpleChannel` payload adapter
  (`DragonNetwork.register()` + `modEventBus.addListener(DragonNetwork::registerPayloads)`,
  one bidirectional `dragonmaid:compat` payload dispatches by message id 0..11).
- 1.21 API ports done for this layer: `DeferredRegister/DeferredHolder` via `compat.Reg/RO`,
  `DataComponents.CUSTOM_DATA` NBT via `compat.StackNbt/WriteBackTag`,
  `AbstractContainerScreen.renderBackground`, `AdvancementHolder`, `LivingDamageEvent.Pre`,
  `DimensionTransition`, `PathfindingContext`, `LevelTickEvent.Post/ServerTickEvent.Post`,
  register-aware ItemStack codecs (`compat.StackCodec`), 1.21.1 recipe serializer `codec()/streamCodec()`.

### Verification (probe.log)
```
[10:14:30.318] Found mod file "dragonmaid-neoforge-1.21.1-1.1.0-beta.2.jar"
               Dragon Maid 1.1.0-beta.2 (dragonmaid)
[10:15:00] [DragonMaid] HELLO FROM COMMON SETUP
[10:15:00] [DragonMaid] DIRT BLOCK >> minecraft:dirt
[10:15:15] [DedicatedServer]: Done (5.606s)! For help, type "help"
[10:15:15] [RconThread]: RCON running on 0.0.0.0:25606
```
No Duplicate registration / Missing registry / unknown registry errors for dragonmaid.

### Entity registry check (RCON, after fixing the RCON packet type)
```
> summon dragonmaid:reinforced_dragon_horn 2 -60 2
  => Summoned new Air
[10:17:32] [ServerLevel/WARN]: Tried to add entity dragonmaid:reinforced_dragon_horn but it was marked as removed already
> execute unless entity @e[type=dragonmaid:reinforced_dragon_horn] run say NO_DM_ENTITY
  => (log) [Rcon] NO_DM_ENTITY
```
Interpretation: `dragonmaid:reinforced_dragon_horn` **is registered and resolvable by /summon** (command accepted,
entity constructed). It removes itself immediately because it is an empty item-display entity
(`ReinforcedHornItemEntity` is an ItemEntity subclass; the summon used no NBT/item -> "Air").

## Reproduce

```powershell
$env:JAVA_HOME='D:\pcl\voxy1.7\jdk\jdk21'
cd D:\BaiduNetdiskDownload\projects\medieval-world\dragonmaid-port
& 'C:\Users\LIUHZ\.gradle\wrapper\dists\gradle-8.8-bin\dl7vupf4psengwqhwktix4v1\gradle-8.8\bin\gradle.bat' --no-daemon buildInfo
cmd /c D:\mc-model\work\_probe_dm\run_probe.bat   # 25605 / RCON 25606
```

## Note on asset fidelity after P0

`pack.mcmeta` is intentionally **not** byte-identical any more (1.21.1 format required, see above);
all other original content files stay byte-identical. See asset-sha256.md for the regenerated table.
The 1.21 alias copies and the `item->id` rewrites exist only in the build output (not in the original files).

## Update 2026-11-01 10:21 - task-3/task-4 batch merge (green build #2)

- excluded.txt: 155 -> 26 (P1 layer + pregen batch1/2/3 combat + combatkeys client tree merged)
- compile: BUILD SUCCESSFUL, 0 errors
- jar: 6,754,957 B, sha256 933DC1FDD0F64F3E39298BE96F525B49F12ED2455421719020D59CF54CD51C8B
- probe server: Done (3.894s), RCON running, dragonmaid errors = 0, Loaded 3976 recipes
- still excluded: 8 client render entry files + 17 server mixins + brewing + compat leftovers (task-3 continues)

## Update 2026-11-01 10:43 - P2 mixin gate PASSED (excluded=0, strict require)

Final gate build: jar 6,812,454 B, mtime 10:43:18, sha256 943783E496633FE3E52DADD705A18158373AFEDAF5CFD2F5587EE1B9595753B6
- excluded.txt = 0 (all 234 source files compile; 25 server mixins + 11 client mixins in dragonmaid.mixins.json)
- mixins.json injectors.defaultRequire restored to 1 (strict); verbose removed
- probe server (full live-server mod set): Done (2.946s), RCON running, 0 mixin apply failures / 0 injection warnings / 0 dragonmaid WARN-ERROR
- applied and logged: DragonMixin, DragonInjuryMixin, DragonRecallMixin, DragonVariantMixin (Fire/Ice/Lightning), DragonChargeMixin,
  DragonDetailsMenuMixin, DragonMenuAccessor, DragonPartInjuryMixin, DragonLogicMixin, DragonForge*, Gorgon*, IceDragonWaterMixin,
  HumanoidIdleLookBodyMixin, RecallEntityMixin, RecallTameMixin, RecallEntityManagerAccessor, RecallEntityStorageAccessor,
  LivingDragonArmorMixin (all into their CE/vanilla targets)

### A/B: /summon iceandfire dragons (why both runs vanish)
With our mod ENABLED:  summon ice_dragon -> "Summoned new Ice Dragon", next command "No entity was found"
With our mod DISABLED: summon ice_dragon -> "Summoned new Ice Dragon", next command "No entity was found"
Both runs log the identical CE error "[IceAndFire] Not a list: null".
=> IAF CE 2.0 dragons cannot be kept alive via a bare /summon (no NBT/age) in this pack; this is a CE-side summon limitation,
   NOT caused by our mixins (baseline reproduces it). Our mixin apply path is verified at class-load + tick level without errors.

### Fixes that made the gate pass
- build owner: dragonmaid.mixins.json generated (36 entries), all annotations remap=false, DragonMixin inherited @Shadow removal (2),
  dragonmaid$parts(CallbackInfo) signature, RecallEntityStorageAccessor target EntityStorage->ChunkStorage
- pregen-dev: 6 mixin fixes (updateParts/breakBlocks split handlers, CE @At paths, stimulateFire->breathFireAtPos, Gorgon statue descriptor,
  DragonPosWorldData stub -> CE real class)
- combatkeys-dev: GeoModelJson deserializer + normalize(pivot/rotation/cubes) for GeckoLib 4.9.2; KnowledgeUpgrades idempotent

## FINAL JAR (2026-11-01 10:53)

- path: D:\BaiduNetdiskDownload\projects\medieval-world\dragonmaid-port\build\libs\dragonmaid-neoforge-1.21.1-1.1.0-beta.2.jar
- size: 6,812,359 B
- mtime: 2026-11-01 10:53:11
- sha256: D73161076F5A99A1DC54EC38F7AF678B45299316DE46179B654EBBB9FEA177B6
- excluded = 0, BUILD SUCCESSFUL, 0 errors
- same source as the 10:47 build (6,812,359 B / sha 679CBD46...): size identical, only jar zip entry timestamps differ
  (build is not bit-reproducible; content/source is the same). _probe_dm/mods holds this exact file (sha D7316107...).
- content: all 234 ported source files + 36 mixin entries (strict) + GeoModelJson(KeyFramesAdapter.GEO_GSON) +
  KnowledgeUpgrades idempotent + pregen 6 mixin fixes + 1.21 data aliases + band-identical assets

## CORRECTION 2026-11-01 11:02 - /summon dragon "vanishing" was a test-method artifact (supersedes the A/B conclusion above)

Independently re-verified on _probe_dm (jar D7316107):
  > forceload add 0 0
    => Marked chunk [0, 0] in Overworld to be force loaded
  > summon iceandfire:ice_dragon 0 120 0 {PersistenceRequired:1b}
    => Summoned new Ice Dragon
  # waited 4 s (>1 tick), then:
  > execute if entity @e[type=iceandfire:ice_dragon] run say DRAGON_ALIVE
    => (log) [Rcon] DRAGON_ALIVE
  > data get entity @e[type=iceandfire:ice_dragon,limit=1] Health
    => Ice Dragon has the following entity data: 20.0f
Root causes of the earlier false negative (all three confirmed):
1. the target chunk was not force-loaded, so a freshly summoned entity was not in the loaded-entity index yet;
2. the query was issued in the same tick as the summon (needs at least 1 tick; we used 3-4 s);
3. hostile mobs despawn without a nearby player unless PersistenceRequired:1b is set.
=> DRAGONS DO SURVIVE /summon in this pack. The earlier statement "CE 2.0 bare /summon limitation" is WRONG and is withdrawn.
   No mod/mixin defect is involved either way; the mixin gate conclusion is unchanged.
   (pregen-dev task-5 run4 independently reached the same result: Health=250.0f, Pos=[0.59,67.80,0.79], gametime advanced.)

## FINAL JAR #2 (2026-11-01 11:20, includes client crash fix in DragonMixin.refreshForm)

- path: D:\BaiduNetdiskDownload\projects\medieval-world\dragonmaid-port\build\libs\dragonmaid-neoforge-1.21.1-1.1.0-beta.2.jar
- size: 6,812,396 B
- mtime: 2026-11-01 11:20:36
- sha256: CD0863CEF2289F1B4BB9461634327BF4B900B0B41F364BC9EB395CDC76E00742
- excluded = 0, BUILD SUCCESSFUL, 0 errors
- change since D7316107: pregen-dev fix - DragonMixin.refreshForm no longer calls removeParts/updateParts on the client,
  server rebuilds parts via updateScale(getRenderSize()/3.0F) after removeParts (CE 2.0 updateParts is not null-safe).
  Fixes client crash R3b-humanoid-sync (NPE in headPart.copyPosition).
- _probe_dm/mods holds this exact file (same sha).

## FINAL JAR #3 (2026-11-01 11:36, includes SimpleChannel fix + dragon armor texture paths)

- path: D:\BaiduNetdiskDownload\projects\medieval-world\dragonmaid-port\build\libs\dragonmaid-neoforge-1.21.1-1.1.0-beta.2.jar
- size: 6,812,348 B
- mtime: 2026-11-01 11:36:30
- sha256: 7E5EFE36F5166019E731CF1E3CAA026807AC4CCC86A414947BB11F3AD8F49F8A
- excluded = 0, BUILD SUCCESSFUL, 0 errors
- changes since CD0863CE:
  * pregen-dev: compat/net/SimpleChannel.java fix for BreathAnimation/BreathImpact message payload handling (client sync)
  * combatkeys-dev: client/NativeMetalArmor.java + client/FemaleChestplateMaterial.java dragon armor texture paths
    (textures/models/armor/armor_<mat>_metal_layer_N.png -> CE 2.0 textures/models/armor/<mat>_layer_N.png) fixes purple/black armor render (R6)
- _probe_dm/mods holds this exact file (same sha).

## Client verification (task-7, jar 7E5EFE36 / 6,812,348 B / 2026-11-01 11:36:30)

Evidence owner: combatkeys-dev, full log TASK7-CLIENT-EVIDENCE.md (123 lines).

- R3b 化形: PASS - Humanoid 0b -> 1b (GUI sync equivalent) then 4 s: no new crash report, player online, humanoid model renders.
- R5 龙息: PASS - DragonMaid.Combat.BreathCooldown 0 -> 60 at 0.71 s (= configured breathRecoveryTicks 60); no ResourceLocationException.
- R6 龙甲: PASS (purple/black fixed) - silver head -> Items[1]; screenshot _probe_dm_client/shots/rt-r6-final.png shows correct white/silver helmet texture.
- R7 末影人: PASS - enderman killed in 18 s; no new crash; player not disconnected.
- SimpleChannel roundtrip (pregen-dev, same jar): BreathImpact(id6) / BreathAnimation(id4) payload id, first data byte != id, CODEC roundtrip,
  decode equals, buffer fully consumed -> 14/14 checks PASS.
- Old-jar comparison screenshots for R6 purple/black: t7-05-r6-armor-d.png, t7-06-r7-fight.png.

## FINAL SERVER ACCEPTANCE (task-5, jar 7E5EFE36) - PASS, task-5 complete

Command: task5_runner.py --start --stop --suite all --expected-sha 7e5efe36... (start -> Done -> suite -> RCON stop, ports back to 0)

Results (evidence: _port_src/task5-evidence-run8-final-verify.md; PORT-STATUS 8.6 + 8.5):
- R1 spawn: PASS (bare summon ice_dragon Health=20, AgeTicks=61 after >1 tick with forceload)
- R2 tame: PASS (TamedDragon=1b, Owner present)
- R3 form: PASS (Humanoid=1b, Knowledge=400, Command=2)
- no regression on R1/R2/R3; dragonmaid ERROR/WARN = none in log
- assets 358/359 byte identical (only pack.mcmeta intentionally upgraded)
- task-7 client items: 5/5 PASS (R3b/R5/R6/R7 + R6 purple-black fixed) - see Client verification section
- SimpleChannel roundtrip: 14/14 PASS
- test server released: 25605/25606 free, no java
