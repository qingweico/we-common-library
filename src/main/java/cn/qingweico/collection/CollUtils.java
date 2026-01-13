package cn.qingweico.collection;


import cn.hutool.core.lang.UUID;
import cn.qingweico.convert.StringConvert;
import cn.qingweico.supplier.RandomDataGenerator;

import java.util.*;
import java.util.function.Supplier;
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

    /**
     * {@link com.google.common.collect.Maps#newHashMapWithExpectedSize(int)}
     */
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
    public static Map<String, Object> listToMap(List<Map<String, Object>> retList, String key, String value) {
        if (retList == null || retList.isEmpty()) {
            return new HashMap<>(0);
        }
        Map<String, Object> t = new HashMap<>(mapSize(retList.size()));
        t.putAll(retList.stream()
                .collect(Collectors.toMap(map ->
                                StringConvert.toString(map.get(key)), map -> map.get(value),
                        (oldK, newK) -> oldK)));
        return t;

    }

    /**
     * 使用随机生成的数据填充指定的Map
     *
     * @param retMap 要填充的目标Map, 键值类型均为Object
     * @param size   要生成的键值对数量(必须大于0)
     */
    public static void fillMap(Map<Object, Object> retMap, int size) {
        if (size <= 0 || retMap == null) {
            return;
        }
        Supplier<?>[] generators = new Supplier[]{
                RandomDataGenerator::address,
                () -> RandomDataGenerator.address(true),
                RandomDataGenerator::birthday,
                () -> RandomDataGenerator.birthday(true),
                RandomDataGenerator::phone,
                () -> RandomDataGenerator.phone(true)
        };

        for (int i = 0; i < size; i++) {
            int generatorIndex = RandomDataGenerator.rndInt(generators.length);
            retMap.put(UUID.fastUUID().toString(true), generators[generatorIndex].get());
        }
    }


    /**
     * 使用指定的键值生成器向Map中填充指定数量的条目
     *
     * @param retMap        要填充的目标Map(非空)
     * @param size          要生成的键值对数量(必须大于0)
     * @param keySupplier   Key Supplier
     * @param valueSupplier value Supplier
     * @param <K>           Map键的类型
     * @param <V>           Map值的类型
     */
    public static <K, V> void fillMap(Map<K, V> retMap, int size,
                                      Supplier<? extends K> keySupplier,
                                      Supplier<? extends V> valueSupplier) {
        if (size <= 0 || retMap == null) {
            return;
        }
        for (int i = 0; i < size; i++) {
            retMap.put(keySupplier.get(), valueSupplier.get());
        }
    }
}
