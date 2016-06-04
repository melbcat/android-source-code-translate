/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

/**
 * 可能会返回一个结果或是抛出一个异常的任务。实现者定义一个不带参数的方法 {@code call}
 *
 * <p>{@code Callable} 接口和 {@link java.lang.Runnable} 很相似，因为
 * 这两个类都被设计类的实例会在另一个线程中执行。然而 {@code Runnable} 
 * 并没有返回一个结果或是抛出一个受检异常。
 *
 * <p>{@link Executors} 类包含了一些实用的方法从常见的形式转换到 {@code Callable} 类
 *
 * @see Executor
 * @since 1.5
 * @author Doug Lea
 * @param <V> {@code call} 方法返回值结果的类型
 */
public interface Callable<V> {
    /**
     * 计算结果，如果不能执行，则抛出一个异常
     *
     * @return 计算后的结果
     * @throws 如果不能计算结果，抛出异常
     */
    V call() throws Exception;
}
