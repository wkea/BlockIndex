package sunsky.io.blockindex;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Level;

public final class BlockIndex extends JavaPlugin {
    public Boolean MysqlEnable = false;

    private DatabaseManager dbManager;

    @Override
    public void onEnable() {
        // 插件启动逻辑
        dbManager = new DatabaseManager(this);
        if (!dbManager.setupDatabaseConfig()) {
            getLogger().log(Level.SEVERE, "数据库尚未链接 请尝试配置 本插件目录下的mysql.yml");
            MysqlEnable = false;
        }else {
            MysqlEnable = true;
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
    }

    public void uploadBlockData(String uid, int x, int y, int z, String blockType) {
        try {
            dbManager.uploadBlockData(uid, x, y, z, blockType);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getBlockType(int x, int y, int z) {
        try {
            return dbManager.getBlockType(x, y, z);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
