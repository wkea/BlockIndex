package sunsky.io.blockindex;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
     * @param uid 用户ID
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
}
