package cn.qingweico.utils.filter;

/**
 * {@link Filter} interface
 *
 * @param <T>
 *         the type of Filtered object
 * @author zqw
 */
public interface Filter<T> {

    /**
     * Does accept a filtered object?
     *
     * @param filteredObject
     *         filtered object
     * @return Accept a filtered object or not.
     */
    boolean accept(T filteredObject);
}
