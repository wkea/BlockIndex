package sunsky.io.blockindex;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private Connection connection;
    private boolean isConnected = false;
    private Plugin plugin;

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 判断数据库是否连接
     * @return 是否连接
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * 设置数据库配置并连接数据库
     * @return 配置是否成功
     */
    public boolean setupDatabaseConfig() {
        File configFile = new File(plugin.getDataFolder(), "mysql.yml");
        if (!configFile.exists()) {
            plugin.saveResource("mysql.yml", false);
            return false;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String url = config.getString("database.url");
        String user = config.getString("database.user");
        String password = config.getString("database.password");

        if (url == null || user == null || password == null) {
            plugin.getLogger().log(Level.SEVERE, "数据库配置文件格式错误");
            return false;
        }

        try {
            connectDatabase(url, user, password);
            if (!checkTableExists("block_data")) {
                initializeDatabase();
            }
            createIndexes();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "数据库连接失败: " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * 连接数据库
     * @param url 数据库URL
     * @param user 数据库用户名
     * @param password 数据库密码
     * @throws SQLException 如果连接失败
     */
    private void connectDatabase(String url, String user, String password) throws SQLException {
        try {
            connection = DriverManager.getConnection(url, user, password);
            isConnected = true;
        } catch (SQLException e) {
            isConnected = false;
            throw e;
        }
    }

    /**
     * 检查指定的表是否存在
     * @param tableName 表名
     * @return 表是否存在
     * @throws SQLException 如果数据库操作失败
     */
    private boolean checkTableExists(String tableName) throws SQLException {
        String query = "SHOW TABLES LIKE ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, tableName);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    /**
     * 初始化数据库表
     * @throws SQLException 如果数据库操作失败
     */
    private void initializeDatabase() throws SQLException {
        String createTableQuery = "CREATE TABLE block_data (" +
                "uid VARCHAR(36) NOT NULL," +
                "x INT NOT NULL," +
                "y INT NOT NULL," +
                "z INT NOT NULL," +
                "block_type VARCHAR(255) NOT NULL," +
                "PRIMARY KEY (uid, x, y, z)" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(createTableQuery);
        }
    }

    /**
     * 创建索引以提高查询效率
     * @throws SQLException 如果数据库操作失败
     */
    private void createIndexes() throws SQLException {
        // 创建 serverUUID + 坐标组合索引
        if (!indexExists("idx_server_coords", "block_data")) {
            String createIndexQuery1 = "CREATE INDEX idx_server_coords ON block_data (uid, x, y, z)";
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(createIndexQuery1);
            }
        }

        // 创建 serverUUID + 方块类型 + 坐标组合索引
        if (!indexExists("idx_server_blocktype_coords", "block_data")) {
            String createIndexQuery2 = "CREATE INDEX idx_server_blocktype_coords ON block_data (uid, block_type, x, y, z)";
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(createIndexQuery2);
            }
        }

        // 创建方块类型索引（单独）
        if (!indexExists("idx_block_type", "block_data")) {
            String createIndexQuery3 = "CREATE INDEX idx_block_type ON block_data (block_type)";
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(createIndexQuery3);
            }
        }
    }

    /**
     * 检查索引是否存在
     * @param indexName 索引名
     * @param tableName 表名
     * @return 索引是否存在
     * @throws SQLException 如果数据库操作失败
     */
    private boolean indexExists(String indexName, String tableName) throws SQLException {
        String query = "SHOW INDEX FROM " + tableName + " WHERE Key_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, indexName);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    /**
     * 关闭数据库连接
     * @throws SQLException 如果关闭失败
     */
    public void closeDatabase() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    /**
     * 上传方块的类型和坐标
     * @param uid 服务器UID
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @param blockType 方块类型
     * @throws SQLException 如果数据库操作失败
     */
    public void uploadBlockData(String uid, int x, int y, int z, String blockType) throws SQLException {
        if (!isConnected) {
            throw new SQLException("Database is not connected");
        }
        String query = "INSERT INTO block_data (uid, x, y, z, block_type) VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE block_type = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uid);
            stmt.setInt(2, x);
            stmt.setInt(3, y);
            stmt.setInt(4, z);
            stmt.setString(5, blockType);
            stmt.setString(6, blockType);
            stmt.executeUpdate();
        }
    }

    /**
     * 根据坐标查询方块类型
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @return 方块类型，如果没有找到则返回null
     * @throws SQLException 如果数据库操作失败
     */
    public String getBlockType(int x, int y, int z) throws SQLException {
        if (!isConnected) {
            throw new SQLException("Database is not connected");
        }
        String query = "SELECT block_type FROM block_data WHERE x = ? AND y = ? AND z = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, x);
            stmt.setInt(2, y);
            stmt.setInt(3, z);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("block_type");
            } else {
                return null;
            }
        }
    }

    /**
     * 删除指定坐标的方块数据
     * @param uid 服务器uid
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @throws SQLException 如果数据库操作失败
     */
    public void deleteBlockData(String uid, int x, int y, int z) throws SQLException {
        if (!isConnected) {
            throw new SQLException("Database is not connected");
        }
        String query = "DELETE FROM block_data WHERE uid = ? AND x = ? AND y = ? AND z = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uid);
            stmt.setInt(2, x);
            stmt.setInt(3, y);
            stmt.setInt(4, z);
            stmt.executeUpdate();
        }
    }
    public void countBlockTypesInRange(Location location, int range) throws SQLException {
        if (!isConnected) {
            throw new SQLException("Database is not connected");
        }

        // 记录方法开始时间
        long startTime = System.currentTimeMillis();

        int minX = location.getBlockX() - range;
        int maxX = location.getBlockX() + range;
        int minY = location.getBlockY() - range;
        int maxY = location.getBlockY() + range;
        int minZ = location.getBlockZ() - range;
        int maxZ = location.getBlockZ() + range;

        String query = "SELECT block_type, COUNT(*) as count FROM block_data " +
                "WHERE x BETWEEN ? AND ? AND y BETWEEN ? AND ? AND z BETWEEN ? AND ? " +
                "GROUP BY block_type";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, minX);
            stmt.setInt(2, maxX);
            stmt.setInt(3, minY);
            stmt.setInt(4, maxY);
            stmt.setInt(5, minZ);
            stmt.setInt(6, maxZ);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String blockType = rs.getString("block_type");
                int count = rs.getInt("count");
                plugin.getLogger().info("附近有 " + count + " 个 " + blockType + " 类型的方块");
            }
        }

        // 记录方法结束时间
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        plugin.getLogger().info("countBlockTypesInRange 方法执行耗时: " + duration + " 毫秒");
    }
    /**
     * 根据 UID 和坐标范围查询指定类型的方块
     * @param uid 服务器UID
     * @param location 中心坐标
     * @param range 范围
     * @param material 方块类型
     * @return 方块类型的列表
     * @throws SQLException 如果数据库操作失败
     */
    public List<String> getBlockTypesInRange(String uid, Location location, int range, Material material) throws SQLException {
        if (!isConnected) {
            throw new SQLException("Database is not connected");
        }
        int minX = location.getBlockX() - range;
        int maxX = location.getBlockX() + range;
        int minY = location.getBlockY() - range;
        int maxY = location.getBlockY() + range;
        int minZ = location.getBlockZ() - range;
        int maxZ = location.getBlockZ() + range;

        String query = "SELECT block_type FROM block_data WHERE uid = ? AND block_type = ? " +
                "AND x BETWEEN ? AND ? AND y BETWEEN ? AND ? AND z BETWEEN ? AND ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uid);
            stmt.setString(2, material.toString());
            stmt.setInt(3, minX);
            stmt.setInt(4, maxX);
            stmt.setInt(5, minY);
            stmt.setInt(6, maxY);
            stmt.setInt(7, minZ);
            stmt.setInt(8, maxZ);
            ResultSet rs = stmt.executeQuery();
            List<String> blockTypes = new ArrayList<>();
            while (rs.next()) {
                blockTypes.add(rs.getString("block_type"));
            }
            return blockTypes;
        }
    }




    public List<Location> getBlockLocationsInRange(UUID serverUUID, Location location, Material material, int range) throws SQLException {
        if (!isConnected) {
            throw new SQLException("Database is not connected");
        }

        int minX = location.getBlockX() - range;
        int maxX = location.getBlockX() + range;
        int minY = location.getBlockY() - range;
        int maxY = location.getBlockY() + range;
        int minZ = location.getBlockZ() - range;
        int maxZ = location.getBlockZ() + range;

        String query = "SELECT x, y, z FROM block_data WHERE uid = ? AND block_type = ? " +
                "AND x BETWEEN ? AND ? AND y BETWEEN ? AND ? AND z BETWEEN ? AND ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, serverUUID.toString());
            stmt.setString(2, material.toString());
            stmt.setInt(3, minX);
            stmt.setInt(4, maxX);
            stmt.setInt(5, minY);
            stmt.setInt(6, maxY);
            stmt.setInt(7, minZ);
            stmt.setInt(8, maxZ);
            ResultSet rs = stmt.executeQuery();

            List<Location> locations = new ArrayList<>();
            while (rs.next()) {
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                locations.add(new Location(location.getWorld(), x, y, z));
            }
            return locations;
        }
    }

    public int countBlocksInRange(UUID serverUUID, Location location, int range) throws SQLException {
        if (!isConnected) {
            throw new SQLException("Database is not connected");
        }

        int minX = location.getBlockX() - range;
        int maxX = location.getBlockX() + range;
        int minY = location.getBlockY() - range;
        int maxY = location.getBlockY() + range;
        int minZ = location.getBlockZ() - range;
        int maxZ = location.getBlockZ() + range;

        String query = "SELECT COUNT(*) as count FROM block_data WHERE uid = ? " +
                "AND x BETWEEN ? AND ? AND y BETWEEN ? AND ? AND z BETWEEN ? AND ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, serverUUID.toString());
            stmt.setInt(2, minX);
            stmt.setInt(3, maxX);
            stmt.setInt(4, minY);
            stmt.setInt(5, maxY);
            stmt.setInt(6, minZ);
            stmt.setInt(7, maxZ);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            } else {
                return 0;
            }
        }
    }


    public Material getBlockTypeAtLocation(UUID serverUUID, Location location) throws SQLException {
        if (!isConnected) {
            throw new SQLException("Database is not connected");
        }

        String query = "SELECT block_type FROM block_data WHERE uid = ? AND x = ? AND y = ? AND z = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, serverUUID.toString());
            stmt.setInt(2, location.getBlockX());
            stmt.setInt(3, location.getBlockY());
            stmt.setInt(4, location.getBlockZ());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Material.valueOf(rs.getString("block_type"));
            } else {
                return null;
            }
        }
    }




    public Location getNearestBlockLocation(UUID serverUUID, Material material, Location location) throws SQLException {
        if (!isConnected) {
            throw new SQLException("Database is not connected");
        }

        String query = "SELECT x, y, z, " +
                "SQRT(POW(x - ?, 2) + POW(y - ?, 2) + POW(z - ?, 2)) AS distance " +
                "FROM block_data WHERE uid = ? AND block_type = ? " +
                "ORDER BY distance ASC LIMIT 1";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, location.getBlockX());
            stmt.setInt(2, location.getBlockY());
            stmt.setInt(3, location.getBlockZ());
            stmt.setString(4, serverUUID.toString());
            stmt.setString(5, material.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                return new Location(location.getWorld(), x, y, z);
            } else {
                return null;
            }
        }
    }

}
