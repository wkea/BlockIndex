package sunsky.io.blockindex;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import static sunsky.io.blockindex.SERVER_UUID.getPropertyValue;

public final class BlockIndex extends JavaPlugin implements Listener {
    public Boolean MysqlEnable = false;
    public UUID ServerUUID = null;
    private BlockDataAPI blockDataAPI;
    private DatabaseManager dbManager;

    private final Set<Material> trackedBlocks = new HashSet<>(Arrays.asList(
            Material.CHEST, Material.FURNACE, Material.BEACON, Material.ENCHANTING_TABLE, Material.CRAFTING_TABLE,
            Material.BLAST_FURNACE, Material.SMOKER, Material.LOOM, Material.ANVIL, Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE, Material.BREWING_STAND, Material.BEEHIVE, Material.BEE_NEST, Material.LECTERN,
            Material.ENDER_CHEST, Material.BARREL, Material.END_PORTAL_FRAME, Material.FARMLAND, Material.LODESTONE,
            Material.COMPOSTER, Material.CAULDRON, Material.BELL, Material.TRAPPED_CHEST, Material.LIGHTNING_ROD
    ));

    @Override
    public void onEnable() {
        // 插件启动逻辑
        saveDefaultConfig(); // 保存默认配置文件

        // 读取或初始化配置
        initializeTrackedBlocks();

        dbManager = new DatabaseManager(this);
        if (!dbManager.setupDatabaseConfig()) {
            getLogger().log(Level.SEVERE, "数据库尚未链接 请尝试配置 本插件目录下的mysql.yml");
            MysqlEnable = false;
        } else {
            ServerUUID = getPropertyValue("ServerUUID");
            MysqlEnable = true;
        }

        // 注册事件监听器
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("BLOCKINDEX已启动 可为其引用插件提供方块坐标索引");
    }
    private void initializeTrackedBlocks() {
        FileConfiguration config = getConfig();
        if (!config.contains("trackedBlocks") || config.getStringList("trackedBlocks").isEmpty()) {
            // 配置文件不存在或为空，使用默认值
            getLogger().info("未找到配置文件或配置为空，使用默认trackedBlocks");
            config.set("trackedBlocks", Arrays.asList(
                    "CHEST", "FURNACE", "BEACON", "ENCHANTING_TABLE", "CRAFTING_TABLE",
                    "BLAST_FURNACE", "SMOKER", "LOOM", "ANVIL", "CAMPFIRE",
                    "SOUL_CAMPFIRE", "BREWING_STAND", "BEEHIVE", "BEE_NEST", "LECTERN",
                    "ENDER_CHEST", "BARREL", "END_PORTAL_FRAME", "FARMLAND", "LODESTONE",
                    "COMPOSTER", "CAULDRON", "BELL", "TRAPPED_CHEST", "LIGHTNING_ROD"
            ));
            saveConfig();
        }
        trackedBlocks.clear();
        for (String blockName : config.getStringList("trackedBlocks")) {
            try {
                trackedBlocks.add(Material.valueOf(blockName));
            } catch (IllegalArgumentException e) {
                getLogger().warning("未知的Material类型: " + blockName);
            }
        }
    }

    @Override
    public void onDisable() {
        // 插件关闭逻辑
        try {
            if (dbManager != null) {
                dbManager.closeDatabase();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        getLogger().info("BLOCKINDEX已关闭");
    }


    public void uploadBlockData(String worlduid, int x, int y, int z, String blockType) {
        try {
            dbManager.uploadBlockData(worlduid, x, y, z, blockType);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteBlockData(String worlduid,int x, int y, int z) {
        try {
            dbManager.deleteBlockData(worlduid,x, y, z);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Material getBlockType(String worlduid,Location loc) {
        try {
            return dbManager.getBlockTypeAtLocation(UUID.fromString(worlduid),loc);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if(!dbManager.isConnected()){
            dbManager.setupDatabaseConfig();
            event.setCancelled(true);
        }
        Material material = event.getBlock().getType();
        if (trackedBlocks.contains(material)) {
            Location loc = event.getBlock().getLocation();
            uploadBlockData(event.getBlock().getWorld().getUID().toString(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), material.toString());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if(!dbManager.isConnected()){
            dbManager.setupDatabaseConfig();
            event.setCancelled(true);
        }
        Material material = event.getBlock().getType();
        if (trackedBlocks.contains(material)) {
            Location loc = event.getBlock().getLocation();
            deleteBlockData(event.getBlock().getWorld().getUID().toString(),loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }
    }
    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        if (event.getBlock().getType() == Material.FARMLAND) {
            if(!event.isCancelled()){
                deleteBlockData(event.getBlock().getWorld().getUID().toString(),event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ());
            }

        }
    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            ItemStack item = event.getItem();
            if (block != null && (block.getType() == Material.DIRT || block.getType() == Material.GRASS_BLOCK)
                    && item != null && item.getType().name().endsWith("_HOE")) {
                scheduleFarmlandCheck(block, event);
            }
        }
    }
    private void scheduleFarmlandCheck(Block block, PlayerInteractEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            new BukkitRunnable() {
                @Override
                public void run() {
                    // 检测方块是否变为耕地
                    if (block.getType() == Material.FARMLAND) {
                        uploadBlockData(block.getWorld().getUID().toString(), block.getX(), block.getY(), block.getZ(), block.getType().toString());
                    }
                }
            }.runTaskLater(this, 3L); // 延迟1秒 (20 ticks)

        });
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        if(!dbManager.isConnected()){
            dbManager.setupDatabaseConfig();
            event.setCancelled(true);
        }
        for (org.bukkit.block.Block block : event.blockList()) {
            Material material = block.getType();
            if (trackedBlocks.contains(material)) {
                Location loc = block.getLocation();
                deleteBlockData(event.getBlock().getWorld().getUID().toString(),loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            }
        }
    }
    public void scanAndSyncChunk(ChunkSnapshot chunkSnapshot, World world) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
           // long startTime = System.currentTimeMillis();
            for (int x = 0; x < 16; x++) {
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                    for (int z = 0; z < 16; z++) {
                        Material material = chunkSnapshot.getBlockType(x, y, z);

                        if (trackedBlocks.contains(material)) {
                            Location loc = new Location(world, chunkSnapshot.getX() * 16 + x, y, chunkSnapshot.getZ() * 16 + z);
                            String worldUID = world.getUID().toString();
                            if (material != getBlockType(worldUID, loc)) {
                                deleteBlockData(worldUID, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                                uploadBlockData(worldUID, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), material.toString());
                            }
                        }
                    }
                }
            }
           // long endTime = System.currentTimeMillis();
           // getLogger().info("区块 (" + chunkSnapshot.getX() + ", " + chunkSnapshot.getZ() + ") 本次扫描同步耗时 " + (endTime - startTime) + " 毫秒");
        });
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        ChunkSnapshot snapshot = chunk.getChunkSnapshot();
        scanAndSyncChunk(snapshot, chunk.getWorld());
    }


}
