/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

/**
 * 可以执行被提交的 {@link Runnable} 任务的对象。这个接口提供了一种
 * 解耦任务提交，每个任务如何运行的机制，包含线程的使用，时序安排等。
 * {@code Executor} 通常被使用去替代显示的创建线程。例如：代替调用
 * {@code new Thread(new RunnableTask()).start()} 为每一组任务，你可以使用：
 *
 * <pre>
 * Executor executor = <em>anExecutor</em>;
 * executor.execute(new RunnableTask1());
 * executor.execute(new RunnableTask2());
 * ...
 * </pre>
 *
 * 然而，{@code Executor} 接口并不严格要求执行是异步的。在最简单的情况下，
 * executor 可以直接运行被提交的任务在调用者的线程中：
 *
 *  <pre> {@code
 * class DirectExecutor implements Executor {
 *   public void execute(Runnable r) {
 *     r.run();
 *   }
 * }}</pre>
 *
 * 最典型的，任务在不是调用者线程的那个线程中执行的。下面的 executor 为
 * 每个任务生成一个新的线程。
 *
 *  <pre> {@code
 * class ThreadPerTaskExecutor implements Executor {
 *   public void execute(Runnable r) {
 *     new Thread(r).start();
 *   }
 * }}</pre>
 *
 * 许多的 {@code Executor} 实现中，对于任务如何执行、何时执行的安排
 * 做了一些限制。下面 executor 连续提交的任务到了另一个executor，说明了一个
 * 复合的 executor。
 *
 *  <pre> {@code
 * class SerialExecutor implements Executor {
 *   final Queue<Runnable> tasks = new ArrayDeque<>();
 *   final Executor executor;
 *   Runnable active;
 *
 *   SerialExecutor(Executor executor) {
 *     this.executor = executor;
 *   }
 *
 *   public synchronized void execute(final Runnable r) {
 *     tasks.add(new Runnable() {
 *       public void run() {
 *         try {
 *           r.run();
 *         } finally {
 *           scheduleNext();
 *         }
 *       }
 *     });
 *     if (active == null) {
 *       scheduleNext();
 *     }
 *   }
 *
 *   protected synchronized void scheduleNext() {
 *     if ((active = tasks.poll()) != null) {
 *       executor.execute(active);
 *     }
 *   }
 * }}</pre>
 *
 * 这个包下的 {@link ExecutorService}是 {@code Executor} 的一个实现接口，
 * 提供了更多扩展的接口。{@link ThreadPoolExecutor} 类提供了可扩展线程池。
 * {@link Executors} 为这些 Executors 提供了方便的工程方法。
 *
 * <p>Memory consistency effects: 在一个优先线程下，提交一个 {@code Runnable} 
 * 对象到一个到 {@code Executor}，由于 
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * 的机制，也许它可能在另一个线程中执行。
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Executor {

    /**
     * 执行在未来执行给定的命令。命令可能在一个新的线程、一个线程池或是
     * 在调用线程中执行，主要裁决于 {@code Executor} 的判定。
     *
     * @param 可运行的任务对象命令
     * @throws RejectedExecutionException 如果这个任务并不能被执行
     * @throws NullPointerException 如果任务命令为null
     */
    void execute(Runnable command);
}
