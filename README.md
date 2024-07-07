# BlockIndex 插件

BlockIndex 是一个 Minecraft Bukkit 插件，用于追踪和管理游戏中特定方块的位置和状态。通过与数据库的集成，BlockIndex 能够在服务器中为引用插件提供可靠的方块坐标索引功能。

## 功能特性

- **方块追踪**：插件可以追踪特定种类的方块，如箱子、熔炉、信标等。
- **数据上传**：当玩家在游戏中放置或破坏特定方块时，插件会将该方块的数据上传到数据库。
- **数据删除**：当特定方块被破坏或改变时，插件会从数据库中删除对应的数据。
- **区块扫描与同步**：在区块加载时，插件会扫描区块内的方块，并同步数据库中的方块信息。
- **数据库配置**：支持通过配置文件设置 MySQL 数据库连接。

## 安装步骤

1. **下载插件**：将 BlockIndex 插件下载到本地。
2. **放置插件**：将插件文件放置到服务器的 `plugins` 文件夹中。
3. **启动服务器**：启动 Minecraft 服务器，插件会自动生成配置文件。
4. **配置数据库**：在 `plugins/BlockIndex/mysql.yml` 文件中配置 MySQL 数据库连接信息。
5. **重启服务器**：重新启动服务器以应用配置。

## 配置文件

默认配置文件 `config.yml` 中包含需要追踪的方块列表，如下所示：

```yaml
trackedBlocks:
  - CHEST
  - FURNACE
  - BEACON
  - ENCHANTING_TABLE
  - CRAFTING_TABLE
  - BLAST_FURNACE
  - SMOKER
  - LOOM
  - ANVIL
  - CAMPFIRE
  - SOUL_CAMPFIRE
  - BREWING_STAND
  - BEEHIVE
  - BEE_NEST
  - LECTERN
  - ENDER_CHEST
  - BARREL
  - END_PORTAL_FRAME
  - FARMLAND
  - LODESTONE
  - COMPOSTER
  - CAULDRON
  - BELL
  - TRAPPED_CHEST
  - LIGHTNING_ROD
```
你可以根据需要编辑 config.yml 文件以添加或删除需要追踪的方块类型。

## 事件处理

插件注册了多种 Minecraft 事件监听器，以实现对方块操作的追踪：

- **BlockPlaceEvent**：监听方块放置事件，如果被追踪的方块被放置，将方块数据上传至数据库。如果数据库未连接，事件将被取消。
- **BlockBreakEvent**：监听方块破坏事件，如果被追踪的方块被破坏，将从数据库中删除对应的数据。如果数据库未连接，事件将被取消。
- **BlockFadeEvent**：监听方块褪色事件，特定处理耕地方块的状态变化（如耕地变为普通土块），并从数据库中删除对应的数据。
- **PlayerInteractEvent**：监听玩家交互事件，特定处理使用锄头耕地的情况。如果玩家使用锄头右击草地或泥土，稍后会检查是否变为耕地，并在必要时将数据上传至数据库。
- **BlockExplodeEvent**：监听方块爆炸事件，如果被追踪的方块在爆炸中被破坏，将从数据库中删除对应的数据。如果数据库未连接，事件将被取消。
- **ChunkLoadEvent**：监听区块加载事件，扫描并同步区块内的方块数据。加载时检查区块内所有被追踪的方块类型，并更新数据库中的方块信息。


## 常见问题

### 如何检查数据库连接状态？

在服务器控制台中，如果数据库连接成功，插件会显示 `BLOCKINDEX已启动` 的信息。如果连接失败，请检查 `mysql.yml` 文件中的数据库配置，并确保 MySQL 服务器正在运行。

### 如何添加新的追踪方块？

编辑 `config.yml` 文件，在 `trackedBlocks` 列表中添加新的方块类型（使用方块的枚举名称，如 `DIAMOND_BLOCK`）。保存文件后，重启服务器以应用新的配置。

### 插件是否支持多世界？

是的，插件在处理方块数据时会记录方块所在的世界 UUID，以确保在多世界环境下数据的准确性。

## 开发者

BlockIndex 插件由维嘉WKEA开发。欢迎提交反馈和贡献代码！

## 许可证

此插件遵GPL循许可证。
