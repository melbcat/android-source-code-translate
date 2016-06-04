/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.os;

import android.annotation.MainThread;
import android.annotation.WorkerThread;

import java.util.ArrayDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>AsyncTask 可以适当而且很方便的用于 UI 线程。这个类允许执行后台操作，
 * 并且在无需操作 threads 和 handlers 的情况下，可以在 UI 线程上进行一些操作</p>
 *
 * <p>AsyncTask 被设计成一个 {@link Thread} 和 {@link Handler} 的辅助类，并不构成一个通用的线程框架。
 * AsyncTasks 应用于比较短的操作操作(最多几秒)。如果你需要保持线程长时间运行，我们强烈建议你使用
 * <code>java.util.concurrent</code> 提供的 APIs，比如 {@link Executor}，
 * {@link ThreadPoolExecutor} 和 {@link FutureTask}。</p>
 *
 * <p>一个被定义的异步任务运行在后台线程，其结果会反馈到 UI 线程。一个异步任务有三种类型定义，
 * <code>Params</code>, <code>Progress</code> 和 <code>Result</code>, 和其他的4步，
 * <code>onPreExecute</code>, <code>doInBackground</code>, 
 * <code>onProgressUpdate</code> and <code>onPostExecute</code></p>
 *
 * <div class="special reference">
 * <h3>开发指南</h3>
 * <p>对于更多使用任务和线程的信息，请阅读开发指南
 * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html">Processes and
 * Threads</a>部分。</p>
 * </div>
 *
 * <h2>用法</h2>
 * <p>AsyncTask 必须被子类继承以后才可以使用. 子类至少要重写 ({@link #doInBackground}) 方法, 
 * 通常也需要重写 ({@link #onPostExecute})方法。</p>
 *
 * <p>这是一个子类化的例子:</p>
 * <pre class="prettyprint">
 * private class DownloadFilesTask extends AsyncTask&lt;URL, Integer, Long&gt; {
 *     protected Long doInBackground(URL... urls) {
 *         int count = urls.length;
 *         long totalSize = 0;
 *         for (int i = 0; i < count; i++) {
 *             totalSize += Downloader.downloadFile(urls[i]);
 *             publishProgress((int) ((i / (float) count) * 100));
 *             // Escape early if cancel() is called
 *             if (isCancelled()) break;
 *         }
 *         return totalSize;
 *     }
 *
 *     protected void onProgressUpdate(Integer... progress) {
 *         setProgressPercent(progress[0]);
 *     }
 *
 *     protected void onPostExecute(Long result) {
 *         showDialog("Downloaded " + result + " bytes");
 *     }
 * }
 * </pre>
 *
 * <p>一旦创建，一个任务被执行:</p>
 * <pre class="prettyprint">
 * new DownloadFilesTask().execute(url1, url2, url3);
 * </pre>
 *
 * <h2>AsyncTask 的属性</h2>
 * <p>被一个异步任务所使用的三种类型如下:</p>
 * <ol>
 *     <li><code>Params</code>, 发送给要执行任务的参数类型.</li>
 *     <li><code>Progress</code>, 在后台运行期间发布的进度类型.</li>
 *     <li><code>Result</code>, 后台执行结果的类型.</li>
 * </ol>
 * <p>一个异步任务不总是需要所有的参数类型， 如果类型没有被使用，可以以将它设置为 {@link Void}:</p>
 * <pre>
 * private class MyTask extends AsyncTask&lt;Void, Void, Void&gt; { ... }
 * </pre>
 *
 * <h2>4步</h2>
 * <p>当一个异步任务被执行的时候，会经历以下4步:</p>
 * <ol>
 *     <li>{@link #onPreExecute()}, 在任务执行之前在 UI 线程中被调用。这一步通常被用来对任务进行设置，
 *     比如在用户界面上显示一个进度条。</li>
 *     <li>{@link #doInBackground}, 在 {@link #onPreExecute()} 方法执行完成之后立即被后台线程调用。
 *     这一步是用来执行后台的耗时操作。异步任务的参数被传递到这一步。执行的结果也在这一步返回传递到最后一步。
 *     这一步也可以使用 {@link #publishProgress} 来发布一个或多个进度单位。这些值将被发布到 UI 线程的
 *     {@link #onProgressUpdate} 这一步。</li>
 *     <li>{@link #onProgressUpdate}, {@link #publishProgress} 被调用后，该方法被UI线程调用。
 *     执行的时间是不确定的。这个方法是用来当后台操作仍然在执行时更新用户界面的进度条。
 *     举个例子，它可以用来更新进度条或者在一个文本框中显示日志。</li>
 *     <li>{@link #onPostExecute}, 在后台操作结束后由UI线程调用。后台计算的结果作为一个参数传递到这一步。</li>
 * </ol>
 * 
 * <h2>取消一个任务</h2>
 * <p>通过调用 {@link #cancel(boolean)}，一个任务可以随时被取消。调用此方法将导致之后被调用的 {@link #isCancelled()} 返回 true。
 * 调用此方法之后, {@link #doInBackground(Object[])} 返回之后会调用 {@link #onCancelled(Object)}，
 * 而不是 {@link #onPostExecute(Object)}。为了确保任务尽快取消，你应该在 {@link #doInBackground(Object[])} 中
 * 循环检查 {@link #isCancelled()} 的返回值, 如果可能的话（例如内循环）。</p>
 *
 * <h2>线程的规则</h2>
 * <p>为了能使这个类正常工作，有几个线程规则必须遵守:</p>
 * <ul>
 *     <li>AsyncTask 类必须在 UI 线程中被装载。这是 {@link android.os.Build.VERSION_CODES#JELLY_BEAN} 自动完成的。</li>
 *     <li>任务的实例必须在 UI 线程中被创建。</li>
 *     <li>{@link #execute} 方法必须在 UI 线程中被调用。</li>
 *     <li>不要手动调用 {@link #onPreExecute()}, {@link #onPostExecute},
 *     {@link #doInBackground}, {@link #onProgressUpdate} 者几个方法。</li>
 *     <li>任务只能被执行一次（如果被第二次执行将会抛出异常）。</li>
 * </ul>
 *
 * <h2>Memory observability</h2>
 * <p>在没有明确的同步下，以下操作是安全的方式下，AsyncTask 保证所有回调函数调用都是同步的。</p>
 * <ul>
 *     <li>在构造函数或 {@link #onPreExecute} 方法中设置成员字段, 在 {@link #doInBackground} 方法中参考他们。</li>
 *     <li>在 {@link #doInBackground} 方法中设置成员字段, 在 {@link #onProgressUpdate} 和
 *         {@link #onPostExecute} 方法中参考他们.</li>
 * </ul>
 *
 * <h2>执行的顺序</h2>
 * <p>当首次引入，AsyncTasks 在一个后台线程中连续被执行。在 {@link android.os.Build.VERSION_CODES#DONUT} 下开始, 
 * 这是改变了一个线程池允许多个任务并行操作。在 {@link android.os.Build.VERSION_CODES#HONEYCOMB} 下开始, 
 * 任务在一个线程上被执行,以避免在并行执行情况下常见的应用程序错误</p>
 * <p>如果你真的需要并行执行，你可以在 {@link #THREAD_POOL_EXECUTOR} 情况下，调用
 * {@link #executeOnExecutor(java.util.concurrent.Executor, Object[])}。</p>
 */
public abstract class AsyncTask<Params, Progress, Result> {
    private static final String LOG_TAG = "AsyncTask";

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE = 1;

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
        }
    };

    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<Runnable>(128);

    /**
     * 可用于并行执行任务的 {@link Executor}
     */
    public static final Executor THREAD_POOL_EXECUTOR
            = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
                    TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory);

    /**
     * 串行执行任务的 {@link Executor} ，这个序列对于一个具体的进程是全局的。
     */
    public static final Executor SERIAL_EXECUTOR = new SerialExecutor();

    private static final int MESSAGE_POST_RESULT = 0x1;
    private static final int MESSAGE_POST_PROGRESS = 0x2;

    private static volatile Executor sDefaultExecutor = SERIAL_EXECUTOR;
    private static InternalHandler sHandler;

    private final WorkerRunnable<Params, Result> mWorker;
    private final FutureTask<Result> mFuture;

    private volatile Status mStatus = Status.PENDING;
    
    private final AtomicBoolean mCancelled = new AtomicBoolean();
    private final AtomicBoolean mTaskInvoked = new AtomicBoolean();

    private static class SerialExecutor implements Executor {
        final ArrayDeque<Runnable> mTasks = new ArrayDeque<Runnable>();
        Runnable mActive;

        public synchronized void execute(final Runnable r) {
            mTasks.offer(new Runnable() {
                public void run() {
                    try {
                        r.run();
                    } finally {
                        scheduleNext();
                    }
                }
            });
            if (mActive == null) {
                scheduleNext();
            }
        }

        protected synchronized void scheduleNext() {
            if ((mActive = mTasks.poll()) != null) {
                THREAD_POOL_EXECUTOR.execute(mActive);
            }
        }
    }

    /**
     * 表明一个任务当前的状态。在任务的声明周期里，每个状态仅仅可以被设置一次。
     */
    public enum Status {
        /**
         * 表明这个任务至今还没有被执行
         */
        PENDING,
        /**
         * 表明任务正在运行中
         */
        RUNNING,
        /**
         * 表明 {@link AsyncTask#onPostExecute} 这个方法已经执行了
         */
        FINISHED,
    }

    private static Handler getHandler() {
        synchronized (AsyncTask.class) {
            if (sHandler == null) {
                sHandler = new InternalHandler();
            }
            return sHandler;
        }
    }

    /** @hide */
    public static void setDefaultExecutor(Executor exec) {
        sDefaultExecutor = exec;
    }

    /**
     * 创建一个新的异步任务。这个构造函数必须在 UI 线程中被调用。
     */
    public AsyncTask() {
        mWorker = new WorkerRunnable<Params, Result>() {
            public Result call() throws Exception {
                mTaskInvoked.set(true);

                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                //noinspection unchecked
                Result result = doInBackground(mParams);
                Binder.flushPendingCommands();
                return postResult(result);
            }
        };

        mFuture = new FutureTask<Result>(mWorker) {
            @Override
            protected void done() {
                try {
                    postResultIfNotInvoked(get());
                } catch (InterruptedException e) {
                    android.util.Log.w(LOG_TAG, e);
                } catch (ExecutionException e) {
                    throw new RuntimeException("An error occurred while executing doInBackground()",
                            e.getCause());
                } catch (CancellationException e) {
                    postResultIfNotInvoked(null);
                }
            }
        };
    }

    private void postResultIfNotInvoked(Result result) {
        final boolean wasTaskInvoked = mTaskInvoked.get();
        if (!wasTaskInvoked) {
            postResult(result);
        }
    }

    private Result postResult(Result result) {
        @SuppressWarnings("unchecked")
        Message message = getHandler().obtainMessage(MESSAGE_POST_RESULT,
                new AsyncTaskResult<Result>(this, result));
        message.sendToTarget();
        return result;
    }

    /**
     * 返回任务的当前状态
     *
     * @return 当前状态
     */
    public final Status getStatus() {
        return mStatus;
    }

    /**
     * 重写这个方法，在后台线程中执行计算。指定的参数是由 {@link #execute} 方法传递的。
     *
     * 这个方法中调用 {@link #publishProgress} 方法，在UI线程中更新数据。
     *
     * @param params 任务的参数.
     *
     * @return 返回一个此任务子类定义的结果 Result
     *
     * @see #onPreExecute()
     * @see #onPostExecute
     * @see #publishProgress
     */
    @WorkerThread
    protected abstract Result doInBackground(Params... params);

    /**
     * 在 {@link #doInBackground} 方法执行之前，运行在 UI 主线程中
     *
     * @see #onPostExecute
     * @see #doInBackground
     */
    @MainThread
    protected void onPreExecute() {
    }

    /**
     * <p>在 {@link #doInBackground} 方法执行之后，运行在UI线程。
     * 此方法的参数是 {@link #doInBackground} 的返回值。</p>
     * 
     * <p>如果这个任务被取消，这个方法将不再被调用。</p>
     *
     * @param result {@link #doInBackground} 方法的返回值.
     *
     * @see #onPreExecute
     * @see #doInBackground
     * @see #onCancelled(Object) 
     */
    @SuppressWarnings({"UnusedDeclaration"})
    @MainThread
    protected void onPostExecute(Result result) {
    }

    /**
     * 在 {@link #publishProgress} 被调用之后，运行在UI线程中。方法中的参数是已经
     * 被传送到 {@link #publishProgress} 中的参数
     *
     * @param values 指示进度的值
     *
     * @see #publishProgress
     * @see #doInBackground
     */
    @SuppressWarnings({"UnusedDeclaration"})
    @MainThread
    protected void onProgressUpdate(Progress... values) {
    }

    /**
     * <p>在 {@link #cancel(boolean)} 被调用之后，运行在UI线程中，并且此时 
     * {@link #doInBackground(Object[])} 已经结束</p>
     * 
     * <p>默认简单的实现直接调用了 {@link #onCancelled()} 方法，忽略了参数。
     * 如果你要重写此方法，不要调用 <code>super.onCancelled(result)</code>。</p>
     *
     * @param result 参数可能是 {@link #doInBackground(Object[])} 的计算结果，也可能是null
     * 
     * @see #cancel(boolean)
     * @see #isCancelled()
     */
    @SuppressWarnings({"UnusedParameters"})
    @MainThread
    protected void onCancelled(Result result) {
        onCancelled();
    }    
    
    /**
     * <p>应用程序最好重写 {@link #onCancelled(Object)} 这个方法。
     * 这个方法默认被 {@link #onCancelled(Object)} 的实现方法所调用。</p>
     * 
     * <p>在 {@link #cancel(boolean)} 方法被调用之后运行在UI线程中，此时，
     * {@link #doInBackground(Object[])} 已经完成执行。</p>
     *
     * @see #onCancelled(Object) 
     * @see #cancel(boolean)
     * @see #isCancelled()
     */
    @MainThread
    protected void onCancelled() {
    }

    /**
     * 在这个任务执行完成之前，如果它被取消，返回<tt>true</tt>。如果你正在对你的任务
     * 执行 {@link #cancel(boolean)} 方法，你应该在 {@link #doInBackground(Object[])} 方法中
     * 检查这个方法的返回值，这样可以尽快的结束这个任务。
     *
     * @return 如果任务在它完成前被取消，返回<tt>true</tt>
     *
     * @see #cancel(boolean)
     */
    public final boolean isCancelled() {
        return mCancelled.get();
    }

    /**
     * <p>尝试取消执行这个任务。如果任务已经执行结束、已经被取消、或是因为其他原因并不能被取消等，
     * 这个尝试将会是失败的。当调用此 <tt>cancel</tt> 方法时，此方法执行成功并且这个任务还没有执行，
     * 那么此任务将不再执行。如果任务已经开始，那 <tt>mayInterruptIfRunning</tt> 参数的值确定是否
     * 应该中断线程来停止这个任务。</p>
     * 
     * <p>调用这个方法将会导致，{@link #doInBackground(Object[])} 返回之后，{@link #onCancelled(Object)}
     * 方法在UI线程中国被调用。调用这个方法保证了 {@link #onPostExecute(Object)} 方法将不再被调用。
     * 在调用这个方法之后，你应该在 {@link #doInBackground(Object[])} 方法中定期检查 {@link #isCancelled()}
     * 方法的返回值，这样可以尽快的结束这个任务。</p>
     *
     * @param mayInterruptIfRunning 如果为<tt>true</tt>则正在执行的线程将会中断；否则正在执行的任务可以完成
     *
     * @return <tt>false</tt> 如果任务不能被取消，通常是因为它已经正常完成。
     *         <tt>true</tt> 否则
     *
     * @see #isCancelled()
     * @see #onCancelled(Object)
     */
    public final boolean cancel(boolean mayInterruptIfRunning) {
        mCancelled.set(true);
        return mFuture.cancel(mayInterruptIfRunning);
    }

    /**
     * 等待计算结束并返回结果
     *
     * @return 计算结果
     *
     * @throws CancellationException 如果计算被取消
     * @throws ExecutionException 如果计算抛出一个异常
     * @throws InterruptedException 当等待时，当前线程被中断
     */
    public final Result get() throws InterruptedException, ExecutionException {
        return mFuture.get();
    }

    /**
     * 等待计算结束并返回结果，最长时间为给定时间
     *
     * @param timeout 取消这个操作之前需等待的时间
     * @param unit 超时的时间单位
     *
     * @return 计算结果
     *
     * @throws CancellationException 如果计算被取消
     * @throws ExecutionException 如果计算抛出一个异常
     * @throws InterruptedException 当等待时，当前线程被中断
     * @throws TimeoutException 等待时间.
     */
    public final Result get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        return mFuture.get(timeout, unit);
    }

    /**
     * 用指定的参数执行此任务。这个方法将返回此任务本身，所以调用者可以拥有此任务的引用。
     * 
     * 
     * <p>注意：依据平台的版本，这个函数安排任务队列在一个后台单线程中或是线程池中。
     * 第一次引进时，AsyncTasks 被执行在一个后台单线程中。以 {@link android.os.Build.VERSION_CODES#DONUT}
     * 形式开始，它将变成一个可以允许多个任务并行操作线程池。以 {@link android.os.Build.VERSION_CODES#HONEYCOMB}
     * 形式开始，任务将在后台一个线程中执行，避免常见应用程序并行执行造成的错误。
     * 如果你真的想要并行执行，在{@link #THREAD_POOL_EXECUTOR} 下，你可以使用 {@link #executeOnExecutor} 方法；
     * 然而，看注释文档中的Warning部分。
     *
     * <p>这个方法必须被调用在UI线程中
     *
     * @param params 任务的参数
     *
     * @return AsyncTask 的一个实例
     *
     * @throws IllegalStateException 如果 {@link #getStatus()} 方法返回的是
     *         {@link AsyncTask.Status#RUNNING} 或 {@link AsyncTask.Status#FINISHED}
     *
     * @see #executeOnExecutor(java.util.concurrent.Executor, Object[])
     * @see #execute(Runnable)
     */
    @MainThread
    public final AsyncTask<Params, Progress, Result> execute(Params... params) {
        return executeOnExecutor(sDefaultExecutor, params);
    }

    /**
     * 用指定的参数执行此任务。这个方法将返回此任务本身，所以调用者可以拥有此任务的引用。
     * 
     * <p>这个方法通常使用 {@link #THREAD_POOL_EXECUTOR} 来允许多个任务并行运行在一个线程池中，
     * 然而你也可以使用你自己的 {@link Executor}。
     * 
     * <p><em>Warning:</em> 允许多个任务并行运行的线程池通常并不是自己想要的那样，
     * 因为他们的操作是没有定义顺序的。例如，例如，如果这些任务是用来修改共同之处
     * (比如单击一个按钮，写一个文件)，并不能保证是顺序修改的。没有认真工作，
     * 新版本的数据被旧版本所覆盖，导致的数据丢失和稳定性问题。这种变化是最好的串行执行的;
     * 保证这样的工作是序列化的而不管平台的版本，在 {@link #SERIAL_EXECUTOR} 下，你可以使用
     * 这个方法。
     *
     * <p>这个方法必须被调用子UI线程中
     *
     * @param exec 需要使用的 executor。作为一个方便的线程池，任务是宽松的，{@link #THREAD_POOL_EXECUTOR} 是可用的，
     * @param params 任务的参数
     *
     * @return AsyncTask 的一个实例
     *
     * @throws IllegalStateException 如果 {@link #getStatus()} 方法返回的是
     *         {@link AsyncTask.Status#RUNNING} 或 {@link AsyncTask.Status#FINISHED}
     *
     * @see #execute(Object[])
     */
    @MainThread
    public final AsyncTask<Params, Progress, Result> executeOnExecutor(Executor exec,
            Params... params) {
        if (mStatus != Status.PENDING) {
            switch (mStatus) {
                case RUNNING:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task is already running.");
                case FINISHED:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task has already been executed "
                            + "(a task can be executed only once)");
            }
        }

        mStatus = Status.RUNNING;

        onPreExecute();

        mWorker.mParams = params;
        exec.execute(mFuture);

        return this;
    }

    /**
     * 一个方便的 {@link #execute(Object...)} 版本，通过使用一个简单的 Runnable 对象。
     * 有关更多信息，请看 {@link #execute(Object[])}。
     *
     * @see #execute(Object[])
     * @see #executeOnExecutor(java.util.concurrent.Executor, Object[])
     */
    @MainThread
    public static void execute(Runnable runnable) {
        sDefaultExecutor.execute(runnable);
    }

    /**
     * 从 {@link #doInBackground} 方法执行开始到，在UI线程上发布更新，
     * 当后台计算仍然在运行时，这个方法一直被执行。每次调用这个方法将触发 
     * {@link #onProgressUpdate} 在UI线程上被执行。
     *
     * 如果任务被取消，{@link #onProgressUpdate} 将不再被触发。
     *
     * @param values 更新UI的进度值.
     *
     * @see #onProgressUpdate
     * @see #doInBackground
     */
    @WorkerThread
    protected final void publishProgress(Progress... values) {
        if (!isCancelled()) {
            getHandler().obtainMessage(MESSAGE_POST_PROGRESS,
                    new AsyncTaskResult<Progress>(this, values)).sendToTarget();
        }
    }

    private void finish(Result result) {
        if (isCancelled()) {
            onCancelled(result);
        } else {
            onPostExecute(result);
        }
        mStatus = Status.FINISHED;
    }

    private static class InternalHandler extends Handler {
        public InternalHandler() {
            super(Looper.getMainLooper());
        }

        @SuppressWarnings({"unchecked", "RawUseOfParameterizedType"})
        @Override
        public void handleMessage(Message msg) {
            AsyncTaskResult<?> result = (AsyncTaskResult<?>) msg.obj;
            switch (msg.what) {
                case MESSAGE_POST_RESULT:
                    // There is only one result
                    result.mTask.finish(result.mData[0]);
                    break;
                case MESSAGE_POST_PROGRESS:
                    result.mTask.onProgressUpdate(result.mData);
                    break;
            }
        }
    }

    private static abstract class WorkerRunnable<Params, Result> implements Callable<Result> {
        Params[] mParams;
    }

    @SuppressWarnings({"RawUseOfParameterizedType"})
    private static class AsyncTaskResult<Data> {
        final AsyncTask mTask;
        final Data[] mData;

        AsyncTaskResult(AsyncTask task, Data... data) {
            mTask = task;
            mData = data;
        }
    }
}
