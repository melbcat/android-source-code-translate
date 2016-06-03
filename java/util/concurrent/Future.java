/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

/**
 * {@code Future} 代表着一个异步计算的结果。它的方法是提供去检查计算是否完成，
 * 等待其完成，并且去检索计算的结果。当计算已经完成时，结果仅仅可以通过使用 
 * {@code get} 方法来获取，必要时阻塞直到它已经准备好。通过 {@code cancel} 方法
 * 来取消。提供额外的方法来决定任务是否正常完成或被取消。一旦计算已经完成，不能被取消。
 * 如果你想使用 {@code Future} 是为了可删除而不是提供一个有用的结果，你可以声明
 * {@code Future<?>} 表单类型，并且返回 {@code null} 来作为底层任务的结果。
 *
 * <p>
 * <b>简单的使用</b> (注意,下面的类都是虚构的。) <p>
 *  <pre> {@code
 * interface ArchiveSearcher { String search(String target); }
 * class App {
 *   ExecutorService executor = ...
 *   ArchiveSearcher searcher = ...
 *   void showSearch(final String target)
 *       throws InterruptedException {
 *     Future<String> future
 *       = executor.submit(new Callable<String>() {
 *         public String call() {
 *             return searcher.search(target);
 *         }});
 *     displayOtherThings(); // do other things while searching
 *     try {
 *       displayText(future.get()); // use future
 *     } catch (ExecutionException ex) { cleanup(); return; }
 *   }
 * }}</pre>
 *
 * The {@link FutureTask} class is an implementation of {@code Future} that
 * implements {@code Runnable}, and so may be executed by an {@code Executor}.
 * For example, the above construction with {@code submit} could be replaced by:
 * {@link FutureTask} 类是 {@code Future} 的一个实现类，它也实现了 {@code Runnable}，
 * 因此可能被一个 {@code Executor} 所执行。例如，在上面例子中 {@code submit} 可能被这样替代：
 *  <pre> {@code
 * FutureTask<String> future =
 *   new FutureTask<String>(new Callable<String>() {
 *     public String call() {
 *       return searcher.search(target);
 *   }});
 * executor.execute(future);}</pre>
 *
 * <p>Memory consistency effects: Actions taken by the asynchronous computation
 * <a href="package-summary.html#MemoryVisibility"> <i>happen-before</i></a>
 * actions following the corresponding {@code Future.get()} in another thread.
 *
 * @see FutureTask
 * @see Executor
 * @since 1.5
 * @author Doug Lea
 * @param <V> 被 Future 的 {@code get} 方法返回的结果
 */
public interface Future<V> {

    /**
     * 视图取消执行的任务。如果任务已经完成、已经被取消或由于其他原因不能被取消，
     * 那这个尝试将会失败的。如果成功的话，并且当 {@code cancel} 被调用时，这个
     * 任务还没有开始，那这个任务应该从来都没有运行。如果任务已经开始，那么
     * {@code mayInterruptIfRunning} 参数决定了执行任务的这个线程是否应该被中断，
     * 视图去停止这个任务。
     *
     * <p>这个方法返回之后，随后调用 {@link #isDone} 将总是返回 {@code true}。
     * 如何这个方法返回 {@code true}，那调用 {@link #isCancelled} 将总是返回 {@code true}
     *
     * @param mayInterruptIfRunning {@code true} 表示执行这个任务的线程应该被中断;
     *  否则, 正在进行的任务可以完成
     * @return {@code false} 如果任务不能被取消，通常因为它已经完成了。
     * {@code true} 否则
     */
    boolean cancel(boolean mayInterruptIfRunning);

    /**
     * 如果任务在它正常完成之前被取消，则返回 {@code true}
     *
     * @return {@code true} 如果任务在它正常完成之前被取消
     */
    boolean isCancelled();

    /**
     * 如果这个任务已经完成了，则返回 {@code true}
     *
     * 完成可能是因为正常结束、一个异常、或是取消 -- 所有这些情况下，
     * 这个方法都返回 {@code true}
     *
     * @return {@code true} 如果这个任务已经完成
     */
    boolean isDone();

    /**
     * 如果是必要的，等待直到计算完成，然后得到它的结果
     *
     * @return 计算的结果
     * @throws CancellationException 如果计算被取消
     * @throws ExecutionException 如果计算抛出一个异常
     * @throws InterruptedException 在等待时，如果当前线程被中断
     */
    V get() throws InterruptedException, ExecutionException;

    /**
     * 如果是必要的，等待最多的给定时间完成计算，如果计算完成了，然后得到它的结果
     *
     * @param 最长等待时间
     * @param 超时的时间单位
     * @return 计算的结果
     * @throws CancellationException 如果计算被取消
     * @throws ExecutionException 如果计算抛出一个异常
     * @throws InterruptedException 在等待时，如果当前线程被中断
     * @throws TimeoutException 如果等待超时
     */
    V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;
}
