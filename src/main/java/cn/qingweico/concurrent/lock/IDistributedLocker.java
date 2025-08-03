package cn.qingweico.concurrent.lock;

/**
 * 分布式锁接口,定义获取和释放分布式锁的基本操作
 *
 * @author zqw
 * @date 2025/8/2
 */
public interface IDistributedLocker {

    /**
     * 获取指定名称的锁(阻塞式)
     * <p>
     * 此方法将阻塞当前线程直到成功获取锁,或者线程被中断建议优先使用{@link #tryLock(String, long)}方法
     * </p>
     *
     * @param name      锁名称,不能为空
     * @param leaseTime 锁的最长持有时间(毫秒),必须大于0
     * @throws InterruptedException     如果当前线程在等待获取锁的过程中被中断
     * @throws IllegalArgumentException 如果name为空或leaseTime小于等于0
     * @see #tryLock(String, long)
     */
    void lock(String name, long leaseTime) throws InterruptedException;

    /**
     * 尝试获取锁(非阻塞式)
     * <p>
     * 此方法会立即返回获取锁的结果,不会阻塞当前线程
     * </p>
     *
     * @param name        锁名称,不能为空
     * @param leaseTimeMs 锁的最长持有时间(毫秒),必须大于0
     * @return {@code true} 如果成功获取锁,{@code false} 如果锁已被其他线程持有
     * @throws IllegalArgumentException 如果name为空或leaseTimeMs小于等于0
     */
    boolean tryLock(String name, long leaseTimeMs);

    /**
     * 尝试获取锁(带超时的阻塞式)
     * <p>
     * 此方法会在指定的等待时间内尝试获取锁,如果在等待时间内未能获取锁则返回false
     * </p>
     *
     * @param name        锁名称,不能为空
     * @param waitMs      获取锁的最大等待时间(毫秒),必须大于等于0
     * @param leaseTimeMs 锁的最长持有时间(毫秒),必须大于0
     * @return {@code true} 如果在等待时间内成功获取锁,否则返回{@code false}
     * @throws InterruptedException     如果当前线程在等待获取锁的过程中被中断
     * @throws IllegalArgumentException 如果name为空,或waitMs小于0,或leaseTimeMs小于等于0
     */
    boolean tryLock(String name, long waitMs, long leaseTimeMs) throws InterruptedException;

    /**
     * 释放指定名称的锁
     * <p>
     * 注意：锁只能由持有它的线程释放,无法跨线程解锁
     * </p>
     *
     * @param name 要释放的锁名称,不能为空
     * @throws IllegalArgumentException 如果name为空
     * @throws IllegalStateException    如果当前线程不持有该锁
     */
    void unlock(String name);
}
