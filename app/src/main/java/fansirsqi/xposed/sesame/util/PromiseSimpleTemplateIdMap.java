package fansirsqi.xposed.sesame.util;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PromiseSimpleTemplateIdMap {
    // 线程安全的存储
    private static final Map<String, String> idMap = new ConcurrentHashMap<>();

    // 只读视图
    private static final Map<String, String> readOnlyIdMap = Collections.unmodifiableMap(idMap);

    // 获取只读 Map
    public static Map<String, String> getMap() {
        return readOnlyIdMap;
    }

    // 添加条目
    public static void add(String key, String value) {
        idMap.put(key, value);
    }

    // 移除条目
    public static void remove(String key) {
        idMap.remove(key);
    }
}
