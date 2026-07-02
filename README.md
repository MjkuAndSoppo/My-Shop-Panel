# My Shop Panel

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.x-red)](https://files.minecraftforge.net/)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

轻量级 Minecraft Forge 经济模组。独立货币 + 玩家自由市场 + OP 官方商店，统一通过 **报价终端** 操作。

---

## 特性

- **独立 GUI 系统** — 完全脱离原版容器界面，自绘实现，与任何模组零冲突
- **MSPP 点数** — 独立货币体系，支持负数余额（透支/欠款）
- **玩家自由市场** — 上架/浏览/购买/撤单，完整自由交易闭环
- **世界商店** — OP 可配置买入/卖出双模式官方商店，支持有限/无限库存
- **报价终端** — 所有功能的唯一入口物品，右键/潜行/左键/I 键四种交互
- **冗余仓库** — 离线玩家下架物品安全寄存，上线后取回
- **三步提交** — Prepare → Confirm → Commit，取消即回滚，超时/掉线零风险
- **透支警告** — 负余额操作前高亮弹窗确认
- **黑名单机制** — 可配置禁止上架的物品

---

## 快速开始

### 安装

1. 从 [Releases](https://github.com/your/repo/releases) 下载 `my_shop_panel-1.0.0.jar`
2. 放入 `.minecraft/mods/` 目录（客户端和服务端均需安装）
3. 启动游戏

### 第一笔交易

```
创造模式 → My Shop Panel 标签页 → 取「报价终端」
右键终端 → 玩家市场 → 上架物品 → 选择背包物品 → 定价 → 确认
```

背包中有报价终端时，按 **I 键** 可快捷打开主菜单。

---

## 主要功能

### 报价终端

| 操作 | 效果 |
|------|------|
| 右键空气 | 打开主菜单 |
| Shift + 右键 | 打开 OP 管理面板（需 OP 权限） |
| 左键其他玩家 | 发起交易请求 |
| 按 I 键 | 快捷打开主菜单（无需手持） |

### 玩家市场

```
上架：背包网格选品 → 输入标价 & 数量 → 确认上架（物品从背包扣除）
购买：浏览全部挂单 → 点击他人挂单 → 弹窗确认（余额不足高亮透支警告）
撤单：切换到「我的挂单」→ 点击撤下（物品退回背包/掉落地面）
```

### 世界商店

```
玩家视角：
  SELLING 条目 → 左键购买 → 选择数量 → 确认扣款
  BUYING  条目 → 左键出售 → 选择数量 → 获得 MSPP
               右键 → 1.3x 价格买回

管理视角（需 OP）：
  潜行右键终端 → OP 管理 → 新增/删除/调库存/切模式
  或 /MSPEdit true → 主菜单出现「编辑」按钮
```

---

## 指令

### `/msp` — 点数管理

```
/msp pay  <玩家> <金额>    转账（所有人可用）
/msp get  <玩家>           查看余额
/msp set  <玩家> <金额>    设置余额（可为负）
/msp add  <玩家> <金额>    增加余额
/msp cut  <玩家> <金额>    扣减余额（可扣为负）
```

### `/MSPB` — 服务器报价管理（需 OP）

```
/MSPB up <价格> [数量]     上架手中物品（小数价格）
/MSPB dn <展示ID>          下架报价（离线则进冗余仓库）
```

### `/MSPEdit` — 编辑模式（需 OP）

```
/MSPEdit true               开启编辑模式
/MSPEdit false              关闭编辑模式
```

### `/MSPBlacklist` — 黑名单（需 OP）

```
/MSPBlacklist add <注册名>     添加黑名单
/MSPBlacklist remove <注册名>  移除黑名单
/MSPBlacklist list              查看黑名单
```

### `/MSPtest` — 测试指令（需 OP）

```
/MSPtest OnlineMode true      模拟在线
/MSPtest OnlineMode false     模拟离线
```

---

## 配置文件

| 文件 | 位置 | 说明 |
|------|------|------|
| `msp_quote.toml` | `config/` | 世界商店条目（可编辑模式修改） |
| `msp_blacklist.toml` | `config/` | 报价黑名单 |
| `admin_shop_config.json` | `config/my_shop_panel/` | OP 商店条目定义 |
| `msp_points.dat` | `<世界>/data/` | 玩家 MSPP 余额 |
| `player_market_data.dat` | `<世界>/data/` | 玩家市场挂单 |
| `redundant_warehouse.dat` | `<世界>/data/` | 冗余仓库 |

---

## 架构

```
net/my_shop_panel/
├── MyShopPanel.java              # 模组主类
├── command/
│   ├── MSPPCommands.java         # /msp 指令
│   ├── MSPBCommands.java         # /MSPB 指令
│   ├── MSPEditCommands.java      # /MSPEdit 指令
│   ├── MSPBlacklistCommands.java # /MSPBlacklist 指令
│   └── MSPTestCommands.java      # /MSPtest 指令
├── economy/
│   ├── MSPPointsSavedData.java   # 点数持久化（WorldSavedData）
│   ├── ClientBalanceData.java    # 客户端余额缓存
│   ├── EditModeData.java         # 编辑模式状态
│   └── SimulateOfflineData.java  # 离线模拟
├── item/
│   ├── QuotationTerminalItem.java # 报价终端物品
│   ├── TerminalEventHandler.java  # 左键玩家事件
│   ├── TerminalKeyMapping.java    # I 键绑定
│   └── TerminalKeyHandler.java    # 按键处理
├── network/
│   ├── NetworkHandler.java        # SimpleChannel 总线
│   └── packet/                    # 网络包（C2S/S2C）
├── screen/
│   ├── BaseStoreScreen.java      # 所有界面的磨砂底基类
│   ├── MainMenuScreen.java       # 主菜单
│   ├── PlayerMarketScreen.java   # 玩家市场
│   ├── ListingSetupScreen.java   # 上架物品
│   ├── AdminShopScreen.java      # OP 管理
│   ├── AdminShopEditScreen.java  # OP 编辑
│   ├── AdminShopListingScreen.java # OP 条目列表
│   ├── AdminTransactionDialog.java # 交易弹窗
│   ├── ConfirmDialog.java        # 通用确认弹窗
│   └── RedundantWarehouseScreen.java # 冗余仓库
└── shop/
    ├── PlayerMarketListing.java   # 挂单数据模型
    ├── PlayerMarketSavedData.java # 挂单持久化
    ├── AdminShopEntry.java        # 商店条目模型
    ├── AdminShopConfig.java       # 商店配置 JSON
    ├── TransactionService.java    # 三步提交事务服务
    ├── RedundantWarehouseSavedData.java # 冗余仓库持久化
    ├── WarehouseItem.java         # 仓储物品模型
    ├── MarketBlacklist.java       # 黑名单管理
    ├── ShopLang.java              # 文本常量
    └── ShopUtils.java             # 工具方法
```

---

## 构建

需要 JDK 17 和 ForgeGradle。

```bash
git clone https://github.com/your/repo.git
cd MyShopPanel
./gradlew build
```

输出：`build/libs/my_shop_panel-1.0.0.jar`

---

## 数据安全

- 所有经济数据通过 `WorldSavedData` 持久化，每个存档独立存储
- 冗余仓库确保离线玩家物品不丢失
- 三步提交机制防止网络波动导致数据不一致
- 物品交付失败自动掉落地面，永不消失

---

## 许可

MIT License — 自由使用、修改、分发。
