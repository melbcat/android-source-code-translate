/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

import java.util.List;
import java.util.Collection;

// BEGIN android-note
// removed security manager docs
// END android-note

/**
 * {@link Executor} 提供了管理结束的方法，同时可以通过一个 {@link Future}
 * 来追踪一个或多个异步任务的进度。
 *
 * <p>{@code ExecutorService} 可以被停止，导致将拒绝新的任务。两个不同的方法被
 * 提供来暂停 {@code ExecutorService}。{@link #shutdown} 方法允许先前被提交的
 * 任务在终止前可以继续执行，而 {@link #shutdownNow} 方法可以防止等待的任务启动
 * 并且试图阻止当前正在执行的任务。一旦终止，executor 将不再有正在执行的任务、
 * 等待执行的任务、新的可以提交的任务。不再使用的 {@code ExecutorService} 应该
 * 关闭，以允许资源回收。
 *
 * <p>{@code submit} 方法扩展自 {@link Executor#execute(Runnable)} 方法，
 * 通过创建并返回一个可以用于取消执行或等待完成的 {@link Future} 对象。
 * {@code invokeAny} 和 {@code invokeAll} 方法更多的是批量执行，执行一个任务
 * 集合，然后等待至少一个、或所有的完成。( {@link ExecutorCompletionService}
 * 类可以用来编写这些方法的自定义变体。)
 *
 * <p>{@link Executors} 类为这个包下的 executor 服务提供了工厂的方法。 
 *
 * <h3>示例</h3>
 *
 * 下面是线程池服务进行网络请求的例子。它使用 {@link Executors#newFixedThreadPool}
 * 工厂方法来进行初始配置。
 *
 *  <pre> {@code
 * class NetworkService implements Runnable {
 *   private final ServerSocket serverSocket;
 *   private final ExecutorService pool;
 *
 *   public NetworkService(int port, int poolSize)
 *       throws IOException {
 *     serverSocket = new ServerSocket(port);
 *     pool = Executors.newFixedThreadPool(poolSize);
 *   }
 *
 *   public void run() { // run the service
 *     try {
 *       for (;;) {
 *         pool.execute(new Handler(serverSocket.accept()));
 *       }
 *     } catch (IOException ex) {
 *       pool.shutdown();
 *     }
 *   }
 * }
 *
 * class Handler implements Runnable {
 *   private final Socket socket;
 *   Handler(Socket socket) { this.socket = socket; }
 *   public void run() {
 *     // read and service request on socket
 *   }
 * }}</pre>
 *
 * 下面的方法展示了关闭一个 {@code ExecutorService} 在两个阶段，首先调用
 * {@code shutdown} 方法来拒绝接受新任务，然后调用 {@code shutdownNow}，
 * 如果必要，取消任何拖延的任务：
 *
 *  <pre> {@code
 * void shutdownAndAwaitTermination(ExecutorService pool) {
 *   pool.shutdown(); // Disable new tasks from being submitted
 *   try {
 *     // Wait a while for existing tasks to terminate
 *     if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
 *       pool.shutdownNow(); // Cancel currently executing tasks
 *       // Wait a while for tasks to respond to being cancelled
 *       if (!pool.awaitTermination(60, TimeUnit.SECONDS))
 *           System.err.println("Pool did not terminate");
 *     }
 *   } catch (InterruptedException ie) {
 *     // (Re-)Cancel if current thread also interrupted
 *     pool.shutdownNow();
 *     // Preserve interrupt status
 *     Thread.currentThread().interrupt();
 *   }
 * }}</pre>
 *
 * <p>Memory consistency effects: Actions in a thread prior to the
 * submission of a {@code Runnable} or {@code Callable} task to an
 * {@code ExecutorService}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * any actions taken by that task, which in turn <i>happen-before</i> the
 * result is retrieved via {@code Future.get()}.
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface ExecutorService extends Executor {

    /**
     * 对于先前被提交的执行的任务有序的停止，新任务不再被接受。如果已经被
     * 通知，调用不再有其他的效果。
     *
     * <p>此方法不会等待以前提交的任务完成执行。用{@link #awaitTermination awaitTermination}
     * 来实现。
     */
    void shutdown();

    /**
     * 尝试停止所有正在执行的任务，以及等待的任务，并且返回返回一个
     * 等待执行的任务列表
     *
     * <p>此方法不会等待正在执行的任务终止。使用{@link #awaitTermination awaitTermination}
     * d来实现。
     *
     * <p>不能保证但尽最大努力，试图停止正在执行任务。例如：典型的实现可以通过 
     * {@link Thread#interrupt} 来取消，因而失败响应中断的任何任务都可能从来不会终止。
     *
     * @return 从未开始执行的任务列表
     */
    List<Runnable> shutdownNow();

    /**
     * 如果这个 executor 已经被终止，返回 {@code true}
     *
     * @return {@code true} 如果这个 executor 已经被终止
     */
    boolean isShutdown();

    /**
     * 如果所有的任务都已经终止，返回{@code true}。
     * 注意{@code isTerminated}从来不返回{@code true}，除非{@code shutdown}
     * {@code shutdownNow} 方法第一次调用。
     *
     * @return {@code true} 如果所有的任务都已经终止
     */
    boolean isTerminated();

    /**
     * 在一个终止请求、超时、或当前线程被中断之后，无论哪一个先发生都将处于阻塞状态，
     * 直到所有的任务已经完成执行。
     *
     * @param 最大的等待时间
     * @param 超时参数的时间单位
     * @return {@code true} 如果这个 executor 被终止，
     *         {@code false} 如果在终止之前超时
     * @throws InterruptedException 如果等待时发生中断
     */
    boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * 提交一个带返回值的任务去执行，并且返回一个代表任务结果的 Future
     * 对象。成功完成之后，Future 的 {@code get} 方法将返回任务的结果。
     *
     * <p>
     * 如果你想立刻阻塞正在等待的任务，你可以使用
     * {@code result = exec.submit(aCallable).get();}
     *
     * <p>注意：{@link Executors} 类包含了一组方法，可以转换一些常见的
     * closure-like 对象，例如把 {@link java.security.PrivilegedAction}
     * 转换到 {@link Callable}，这样就可以被提交。
     *
     * @param 要提交的任务
     * @return 一个 Future，代表待完成的任务
     * @throws RejectedExecutionException 如果任务无法被安排执行
     * @throws NullPointerException 如果任务为 null
     */
    <T> Future<T> submit(Callable<T> task);

    /**
     * 提交一个 Runnable 任务去执行，并且返回一个代表任务的 Future
     * 对象。成功完成之后，Future 的 {@code get} 方法将返回给定的结果。
     * 
     *
     * @param 要提交的任务
     * @param 返回的结果
     * @return 一个 Future，代表待完成的任务
     * @throws RejectedExecutionException 如果任务无法被安排执行
     * @throws NullPointerException 如果任务为 null
     */
    <T> Future<T> submit(Runnable task, T result);

    /**
     * 提交一个 Runnable 任务去执行，并且返回一个代表任务的 Future
     * 对象。成功完成之后，Future 的 {@code get} 方法将返回 {@code null}。
     *
     * @param 要提交的任务
     * @return 一个 Future，代表待完成的任务
     * @throws RejectedExecutionException 如果任务无法被安排执行
     * @throws NullPointerException 如果任务为 null
     */
    Future<?> submit(Runnable task);

    /**
     * 执行给定的任务集，当所有的都完成之后，返回一个 Futures 列表，保留
     * 他们的状态和结果。对于返回列表中的每一个元素的{@link Future#isDone}
     * 方法都是{@code true}。
     * 注意：完成的任务也可能会正常终止或抛一个异常。当操作正在执行时，
     * 如果给定的集合被修改，这个方法放回的结果并没有定义。
     *
     * @param 任务集合
     * @return 一个代表任务的 Futures 列表, 通过给定的任务列表迭代而产生的
     *         一个有序集合，并且都已完成。
     * @throws InterruptedException 如果等待中被终止，导致未完成的任务被取消
     * @throws NullPointerException 如果任务集合或其中的元素为 {@code null}
     * @throws RejectedExecutionException 如果任何一个任务都没被安排执行
     */
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
        throws InterruptedException;

    /**
     * 执行给定的任务集，当所有的都完成或由于超时结束之后，返回一个 Futures 列表，
     * 保留他们的状态和结果。对于返回列表中的每一个元素的{@link Future#isDone}
     * 方法都是{@code true}。没有被完成的任务被取消。
     * 注意：完成的任务也可能会正常终止或抛一个异常。当操作正在执行时，
     * 如果给定的集合被修改，这个方法放回的结果并没有定义。
     *
     * @param 任务集合
     * @param 最大的等待时间
     * @param 超时参数的时间单位
     * @return 一个代表任务的 Futures 列表, 通过给定的任务列表迭代而产生的
     *         一个有序集合 如果操作并没有超时，每个任务都将已经完成。如果
     *         超时，集合中的一些任务将不能完成。
     * @throws InterruptedException 如果等待中被终止，导致未完成的任务被取消
     * @throws NullPointerException 如果任务集合或其中的元素为 {@code null}、或超时
     * @throws RejectedExecutionException 如果任何一个任务都没被安排执行
     */
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                  long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * 执行给定的任务集，如果有一个执行，返回成功完成的结果
     * （i.e.，并没有抛一个异常）。正常的返回下，并没有完成的任务会被取消。
     * 当操作正在执行时，如果给定的集合被修改，这个方法返回的结果并没有定义。
     * 
     *
     * @param 任务集合
     * @return 返回其中一个任务的结果
     * @throws InterruptedException 如果等待中被终止
     * @throws NullPointerException 如果任务集合或其中的元素为 {@code null}
     * @throws IllegalArgumentException 任务集合是空的
     * @throws ExecutionException 如果没有任务成功完成
     * @throws RejectedExecutionException 如果任何一个任务都没被安排执行
     */
    <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException;

    /**
     * 执行给定的任务集，在超时之前，如果有一个执行，返回成功完成的结果
     * （i.e.，并没有抛一个异常）。正常的返回下，并没有完成的任务会被取消。
     * 当操作正在执行时，如果给定的集合被修改，这个方法返回的结果并没有定义。
     *
     * @param 任务集合
     * @param 最大的等待时间
     * @param 超时参数的时间单位
     * @return 返回其中一个任务的结果
     * @throws InterruptedException 如果等待中被终止
     * @throws NullPointerException 如果任务集合或其中的元素为 {@code null}、或超时
     * @throws TimeoutException 任何一个给定的任务成功完成之前，如果超时
     * @throws ExecutionException 如果没有任务成功完成
     * @throws RejectedExecutionException 如果任何一个任务都没被安排执行
     */
    <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                    long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;
}
