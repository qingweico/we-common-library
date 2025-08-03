package cn.qingweico.collection;


import cn.qingweico.convert.Convert;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zqw
 * @date 2021/4/9
 */
public final class CollUtils {
    private CollUtils() {
    }

    public static <K, V> Map<K, V> map() {
        return map(mapSize(1 << 4));
    }

    public static <K, V> Map<K, V> map(int capacity) {
        return new HashMap<>(capacity);
    }

    public static <T> List<T> list() {
        return new ArrayList<>();
    }

    public static <T> LinkedList<T> newLinkedList() {
        return new LinkedList<>();
    }

    public static <T> Set<T> set() {
        return new HashSet<>();
    }

    public static <T> Queue<T> queue() {
        return new LinkedList<>();
    }

    public static int mapSize(int exceptedSize) {
        return (int) ((float) exceptedSize / 0.75F + 1.0F);
    }

    /**
     * 将List<Map>转换为以指定key-value对构成的Map
     *
     * @param retList 原始List<Map>数据
     * @param key     作为结果Map中key的字段名
     * @param value   作为结果Map中value的字段名
     * @return 转换后的Map, 不会返回null
     */
    public Map<String, Object> listToMap(List<Map<String, Object>> retList, String key, String value) {
        if (retList == null || retList.isEmpty()) {
            return new HashMap<>(0);
        }
        Map<String, Object> t = new HashMap<>(mapSize(retList.size()));
        t.putAll(retList.stream()
                .collect(Collectors.toMap(map ->
                                Convert.toString(map.get(key)), map -> map.get(value),
                        (oldK, newK) -> oldK)));
        return t;

    }
}
