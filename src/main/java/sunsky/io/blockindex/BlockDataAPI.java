package sunsky.io.blockindex;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class BlockDataAPI {

    private final DatabaseManager databaseManager;

    public BlockDataAPI(Plugin plugin) {
        this.databaseManager = new DatabaseManager(plugin);
        if (!this.databaseManager.setupDatabaseConfig()) {
            plugin.getLogger().severe("Failed to setup database configuration.");
        }
    }
    /**
     * 更新/应用指定坐标的索引数据
     * @param UUID 地图UUID
     * @param X 坐标
     * @param Y 坐标
     * @param Z 坐标
     * @param blockType 类型
     * 当区块重新加载，或方块被重新放置时将重新被索引
     */
    public void uploadBlockData(String UUID, int X, int Y, int Z, String blockType) {
        try {
            databaseManager.uploadBlockData(UUID, X, Y, Z, blockType);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     * 删除指定坐标的索引数据
     * @param UUID 地图UUID
     * @param X 坐标
     * @param Y 坐标
     * @param Z 坐标
     * 当区块重新加载，或方块被重新放置时将重新被索引
     */
    public void deleteBlockData(String UUID ,int X, int Y, int Z) {
        try {
            databaseManager.deleteBlockData(UUID,X, Y, Z);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询指定范围内的某个方块类型的坐标
     * @param serverUUID 服务器UUID
     * @param location 中心坐标
     * @param material 方块类型
     * @param range 范围
     * @return 坐标列表
     */
    public List<Location> getBlockLocationsInRange(UUID serverUUID, Location location, Material material, int range) {
        try {
            return databaseManager.getBlockLocationsInRange(serverUUID, location, material, range);
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error querying block locations in range: " + e.getMessage());
            return null;
        }
    }

    /**
     * 统计某个坐标范围内的所有方块条目数
     * @param serverUUID 服务器UUID
     * @param location 中心坐标
     * @param range 范围
     * @return 方块条目数
     */
    public int countBlocksInRange(UUID serverUUID, Location location, int range) {
        try {
            return databaseManager.countBlocksInRange(serverUUID, location, range);
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error counting blocks in range: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 根据坐标读取该坐标的方块类型
     * @param serverUUID 服务器UUID
     * @param location 坐标
     * @return 方块类型
     */
    public Material getBlockTypeAtLocation(UUID serverUUID, Location location) {
        try {
            return databaseManager.getBlockTypeAtLocation(serverUUID, location);
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error getting block type at location: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取与坐标最近的同类型方块坐标
     * @param serverUUID 服务器UUID
     * @param material 方块类型
     * @param location 中心坐标
     * @return 最近的同类型方块坐标
     */
    public Location getNearestBlockLocation(UUID serverUUID, Material material, Location location,int range) {
        Bukkit.getLogger().severe("参数: " + location.toString());
        try {
            return databaseManager.getNearestBlockLocation(serverUUID, material, location, range);
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error getting nearest block location: " + e.getMessage());
            return null;
        }
    }
}
