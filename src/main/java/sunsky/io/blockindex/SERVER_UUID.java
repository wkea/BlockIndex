package sunsky.io.blockindex;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

public class SERVER_UUID {

    /**
     * 读取 server.properties 文件中的指定键的值
     *
     * @param key 键名
     * @return 对应的值，如果键名不存在则返回 null
     */
    public static UUID getPropertyValue(String key) {
        Properties properties = new Properties();
        File propertiesFile = new File("server.properties");

        // 检查 server.properties 文件是否存在
        if (!propertiesFile.exists()) {
            System.err.println("server.properties 文件不存在");
            return null;
        }

        // 加载 server.properties 文件
        try (InputStream input = new FileInputStream(propertiesFile)) {
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // 获取指定键的值
        return UUID.fromString(properties.getProperty(key));
    }
}
