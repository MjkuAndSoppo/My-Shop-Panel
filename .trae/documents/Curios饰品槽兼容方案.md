# Curios API 饰品槽兼容方案

> 目标：兼容 Curios API，让玩家把「报价终端」放进自定义饰品槽后，按 `I` 键也能打开商店主菜单。
> 软依赖：未安装 Curios 时，一切行为与现在完全一致。

***

## 一、已确认的决策

| 决策点     | 结论                                                      | 来源       |
| ------- | ------------------------------------------------------- | -------- |
| 接入方式    | 引入 Curios API 依赖（`compileOnly` api + `runtimeOnly` 完整包） | 用户选择「推荐」 |
| I 键判定范围 | 背包/主副手 **+** 饰品槽（只做加法，原有判定不动）                           | 用户选择「推荐」 |

***

## 二、现状分析

### 2.1 相关物品

* 物品注册 ID：`my_shop_panel:quotation_terminal`

  * 见 [MyShopPanel.java](file:///e:/project%20666/MyShopPanel/src/main/java/com/example/myshoppanel/MyShopPanel.java#L57-L58)

* 物品类：`QuotationTerminalItem extends Item`，`new Properties().stacksTo(1)`

  * 见 [QuotationTerminalItem.java](file:///e:/project%20666/MyShopPanel/src/main/java/com/example/myshoppanel/item/QuotationTerminalItem.java#L23-L25)

  * 不可堆叠（天然适合饰品槽，一个槽放一个）

* 已有贴图：`src/main/resources/assets/my_shop_panel/textures/item/quotation_terminal.png`（槽位图标直接复用，无需新素材）

### 2.2 I 键打开的唯一校验入口

* [C2S\_OpenTerminalPacket.java](file:///e:/project%20666/MyShopPanel/src/main/java/com/example/myshoppanel/network/packet/C2S_OpenTerminalPacket.java#L29-L57)

* 服务端 `hasTerminal(player)` 目前只扫描：主手、副手、`player.getInventory().items`（背包主区）。

* 触发条件：`ServerConfig.isRequireTerminalForHotkey() && !hasTerminal(player)` → 提示 `my_shop_panel.hint.need_terminal` 并拒绝开菜单。

* 因此「饰品槽里的终端也算持有」只需**扩充** **`hasTerminal`**，其余流程零改动。

### 2.3 项目既有的软依赖范式（风格参考）

* [ShopUtils.java](file:///e:/project%20666/MyShopPanel/src/main/java/com/example/myshoppanel/shop/ShopUtils.java#L37-L49)：ProjectE 用 `Class.forName` 反射。

* 本次采用 Curios 官方推荐写法（`ModList.get().isLoaded("curios")` 守卫 + 独立 compat 类隔离），因为用户已同意引入依赖；独立类保证未装 Curios 时 JVM 不会加载到 Curios 类型。

### 2.4 构建与元数据现状

* [build.gradle](file:///e:/project%20666/MyShopPanel/build.gradle#L114-L147)：`repositories` 与 `dependencies` 均为空模板（只有 `minecraft` 依赖）。

* [gradle.properties](file:///e:/project%20666/MyShopPanel/gradle.properties)：无 `curios_version`。

* [mods.toml](file:///e:/project%20666/MyShopPanel/src/main/resources/META-INF/mods.toml#L52-L73)：仅 `forge`、`minecraft` 两条 `mandatory=true` 依赖。

* 资源目录：`data/my_shop_panel/` 下只有 `recipes/` 与 `def.json`，**无** **`curios/`**；`data/curios/` 也不存在。

* 语言文件共 **9** 个：`en_us`、`en_gb`、`en_au`、`zh_cn`、`zh_hk`、`zh_tw`、`ja_jp`、`ko_kr`、`ru_ru`。

### 2.5 Curios 5.x（1.20.1）机制要点（已核实官方文档）

* 槽位定义：`data/<namespace>/curios/slots/<identifier>.json`，字段 `size`/`order`/`icon`/`add_cosmetic` 等。

* 实体分配：`data/<namespace>/curios/entities/<任意文件名>.json`，字段 `entities`/`slots`，语义为「把列出的槽位关联到列出的实体」，**合并语义**（不会挤掉其他模组的槽）。

* 物品白名单：`data/curios/tags/items/<slot_id>.json`（命名空间固定 `curios`），Curios 默认校验器 `curios:tag` 读取该标签 → 只有标签内物品能进该槽。

* 本地化键：`curios.identifier.<slot_id>`。

* 服务端查询（Forge 1.20.1）：`CuriosApi.getCuriosInventory(LivingEntity)` → `Optional<ICuriosItemHandler>`；`ICuriosItemHandler#getCurios()` → `Map<String, ICurioStackHandler>`。

* 依赖坐标：`top.theillusivec4.curios:curios-forge:<version>`（`:api` classifier 供编译期），maven `https://maven.theillusivec4.top/`；1.20.1 最新为 `5.14.1+1.20.1`。

***

## 三、改动清单（7 项）

1. `gradle.properties` — 新增 `curios_version`
2. `build.gradle` — 新增 Curios maven 仓库 + 编译/运行依赖
3. `META-INF/mods.toml` — 新增 curios 可选依赖条目
4. **新建** `compat/curios/CuriosIntegration.java` — 饰品槽查询封装
5. `C2S_OpenTerminalPacket.java` — `hasTerminal` 追加饰品槽判定
6. **新建** 3 个 datapack JSON（槽位 / 实体分配 / 物品白名单标签）
7. 9 个 lang 文件 — 新增 `curios.identifier.terminal`

***

## 四、具体改动

### 4.1 `gradle.properties`

在 `## Mod Properties` 之前（环境属性区）追加：

```properties
# Curios API 版本（1.20.1 分支，5.x）
curios_version=5.14.1+1.20.1
```

### 4.2 `build.gradle`

`repositories` 块（替换现有空注释内容）：

```groovy
repositories {
    // Illusive Soulworks（Curios API 官方 maven）
    maven {
        name = "Illusive Soulworks maven"
        url = "https://maven.theillusivec4.top/"
    }
}
```

`dependencies` 块（保留 `minecraft` 行，追加两行）：

```groovy
dependencies {
    minecraft "net.minecraftforge:forge:${minecraft_version}-${forge_version}"

    // Curios API：compileOnly 只用 api 包编译；runtimeOnly 完整包供 dev 环境 runClient/runServer 加载
    compileOnly fg.deobf("top.theillusivec4.curios:curios-forge:${curios_version}:api")
    runtimeOnly fg.deobf("top.theillusivec4.curios:curios-forge:${curios_version}")
}
```

> 说明：`runtimeOnly` 只影响开发环境与运行测试，**不会**被你发布的 jar 携带；玩家是否安装 Curios 由 mods.toml 的可选依赖控制。

**（必需）为 run 配置开启 mixin refmap 重映射** —— Curios 使用 mixin，dev 环境若不开启此项，mixin 会按 SRG 名（如 `f_19803_`）解析，在 official mappings 下找不到目标字段而启动崩溃（`InvalidAccessorException`）。在 `runs { configureEach { ... } }` 中追加：

```groovy
            property 'mixin.env.remapRefMap', 'true'
            property 'mixin.env.refMapRemappingFile', "${projectDir}/build/createSrgToMcp/output.srg"
```

> 依据 Curios 官方文档 *Setting Up the Development Environment*。启动日志出现 `Remapping refMap curios.refmap.json using ...output.srg` 即表示生效。

### 4.3 `src/main/resources/META-INF/mods.toml`

在现有 `minecraft` 依赖块之后追加（`mandatory=false` + `ordering=AFTER` 表示可选且在 Curios 之后加载）：

```toml
# Curios API（可选依赖）：未安装时模组正常加载，I 键只认背包/主副手
[[dependencies.${mod_id}]]
    modId="curios"
    mandatory=false
    versionRange="[5.14.1,)"
    ordering="AFTER"
    side="BOTH"
```

### 4.4 新建 `src/main/java/com/example/myshoppanel/compat/curios/CuriosIntegration.java`

```java
package com.example.myshoppanel.compat.curios;

import com.example.myshoppanel.item.QuotationTerminalItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Curios API 软依赖集成层。
 *
 * <p>本类只在 Curios 已加载时被调用（调用方用 {@code ModList.get().isLoaded("curios")} 守卫），
 * 未安装 Curios 时本类不会被 JVM 加载，因此可以直接引用 Curios 的 API 类型而不会触发
 * NoClassDefFoundError / 专服 dist 问题。
 */
public final class CuriosIntegration {

    private CuriosIntegration() {}

    /** 玩家任意一个饰品槽中是否插着报价终端 */
    public static boolean hasTerminal(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> {
                    // handler.getCurios() = Map<String, ICurioStackHandler>（槽位 id -> 子库存）
                    for (var slotHandler : handler.getCurios().values()) {
                        for (ItemStack stack : slotHandler.getStacks()) {
                            if (stack.getItem() instanceof QuotationTerminalItem) {
                                return true;
                            }
                        }
                    }
                    return false;
                })
                .orElse(false);
    }
}
```

> 用 `var` 遍历子库存，避免依赖 `ICurioStackHandler` 的具体包路径，降低 API 版本漂移风险。

### 4.5 `C2S_OpenTerminalPacket.java`

新增 import：

```java
import com.example.myshoppanel.compat.curios.CuriosIntegration;
import net.minecraftforge.fml.ModList;
```

`hasTerminal` 方法在背包循环之后、`return false` 之前追加一行并更新注释：

```java
    /** 玩家身上（背包 / 主手 / 副手 / Curios 饰品槽）是否持有报价终端 */
    private static boolean hasTerminal(ServerPlayer player) {
        if (player.getMainHandItem().getItem() instanceof QuotationTerminalItem) return true;
        if (player.getOffhandItem().getItem() instanceof QuotationTerminalItem) return true;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof QuotationTerminalItem) return true;
        }
        // 软依赖：仅当 Curios 已加载时才查询饰品槽
        if (ModList.get().isLoaded("curios") && CuriosIntegration.hasTerminal(player)) return true;
        return false;
    }
```

> `import` 语句本身不会加载类；`isLoaded` 返回 false 时短路，`CuriosIntegration` 永不被触碰。

### 4.6 新建 3 个 datapack JSON

**a. 槽位定义** — `src/main/resources/data/my_shop_panel/curios/slots/terminal.json`

```json
{
  "size": 1,
  "order": 100,
  "icon": "my_shop_panel:gui/terminal_slot",
  "add_cosmetic": false
}
```

> `icon` 省略 `textures/` 前缀与 `.png` 后缀，指向 `assets/my_shop_panel/textures/gui/terminal_slot.png`（16×16 槽位图标）。

**b. 实体分配** — `src/main/resources/data/my_shop_panel/curios/entities/player.json`

```json
{
  "entities": ["player"],
  "slots": ["terminal"]
}
```

> 只把 `terminal` 槽挂到玩家身上；合并语义不会影响其他模组给玩家加的槽。

**c. 物品白名单标签** — `src/main/resources/data/curios/tags/items/terminal.json`

```json
{
  "replace": false,
  "values": ["my_shop_panel:quotation_terminal"]
}
```

> **命名空间必须是** **`curios`**（不是 `my_shop_panel`）——Curios 的默认校验器 `curios:tag` 固定读 `data/curios/tags/items/<slot_id>.json`。
> 效果：只有报价终端能放进该槽（这正是「给终端用的自定义饰品槽」的预期）。若不希望限制，可删掉此文件（任意物品都能放入）。

### 4.7 9 个语言文件新增键

在各自 JSON 内追加 `curios.identifier.terminal`（放在文件末尾 `}` 之前，注意与前一项的逗号）：

| 文件           | 值                  |
| ------------ | ------------------ |
| `en_us.json` | `"Terminal Slot"`  |
| `en_gb.json` | `"Terminal Slot"`  |
| `en_au.json` | `"Terminal Slot"`  |
| `zh_cn.json` | `"终端饰品槽"`          |
| `zh_hk.json` | `"終端飾品槽"`          |
| `zh_tw.json` | `"終端飾品槽"`          |
| `ja_jp.json` | `"ターミナルスロット"`      |
| `ko_kr.json` | `"터미널 슬롯"`         |
| `ru_ru.json` | `"Слот терминала"` |

***

## 五、验证步骤

1. 编译：`.\gradlew compileJava --console=plain`（沙箱受限时加 `dangerouslyDisableSandbox`），确认无编译错误、依赖解析成功。
2. 客户端：`.\gradlew runClient`，确认 dev 环境同时加载了 Curios。
3. 服务端：`.\gradlew runServer`，确认移动端/专服不因 Curios 类型引用崩溃（未装 Curios 时也应正常启动——类隔离生效）。
4. 游戏内功能验证（需安装 Curios）：

   * 打开 Curios 饰品栏，确认出现「终端饰品槽」，且只能放入报价终端。

   * 把终端放进饰品槽、背包/手上清空 → 保持 `requireTerminalForHotkey=true`（默认）→ 按 `I` 应能打开主菜单。

   * 通过 `/msp openmod f` 关闭终端要求后再按 `I`，仍应能打开（回归）。

   * 把终端从饰品槽取出 → 按 `I` 应提示 `my_shop_panel.hint.need_terminal`。
5. 未安装 Curios 的回归：不装 Curios 时按 `I` 的行为与改动前完全一致。

***

## 六、注意事项 / 风险

1. **Curios 是可选依赖**：`runtimeOnly` 仅为开发环境测试；发布 jar 不携带 Curios，玩家自行安装。mods.toml 的 `mandatory=false` 保证未装 Curios 时模组照常加载。
2. **类隔离是硬要求**：`CuriosIntegration` 必须始终由 `ModList.get().isLoaded("curios")` 守卫后才可被调用，否则未装 Curios 的环境会 `NoClassDefFoundError`。
3. **仅做加法**：`hasTerminal` 原有逻辑（背包/主副手）保持不变，饰品槽是新增判定分支，不影响既有行为。
4. **本次不涉及**：饰品槽中终端能否「右键打开」（Curios 槽位有自己的交互逻辑，不在本次范围）；`TerminalEventHandler` 右键实体转账逻辑不变。
5. **API 方法名**：以 `compileOnly` 的 `:api` 包为准，若 `getStacks()` 在某小版本改签名，按 IDE 提示改用 `getStacks()` 的等价方法（`ICurioStackHandler` 的标准读取方法）。

