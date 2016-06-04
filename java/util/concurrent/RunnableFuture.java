/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

/**
 * {@link Future} 就是 {@link Runnable}。{@code run} 方法的成功执行，
 * {@code Future} 的完成并且允许得到它的结果。
 * 
 * @see FutureTask
 * @see Executor
 * @since 1.6
 * @author Doug Lea
 * @param <V> Future类的 {@code get} 方法返回值的类型
 */
public interface RunnableFuture<V> extends Runnable, Future<V> {
    /**
     * 设置这个 Future 的计算结果，直到它已经被取消。
     */
    void run();
}
