/*
 * Copyright (C) 2006 The Android Open Source Project
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

/**
 * Handy class for starting a new thread that has a looper. The looper can then be 
 * used to create handler classes. Note that start() must still be called.
 * 
 * 一个很方便的类，一个可以拥有 looper 的线程。在创建 handler 的类中 looper 可以被使用。
 * 注意: start() 方法仍然需要被调用
 */
public class HandlerThread extends Thread {
    int mPriority;
    int mTid = -1;
    Looper mLooper;

    public HandlerThread(String name) {
        super(name);
        mPriority = Process.THREAD_PRIORITY_DEFAULT;
    }
    
    /**
     * Constructs a HandlerThread.
     * @param name
     * @param priority The priority to run the thread at. The value supplied must be from 
     * {@link android.os.Process} and not from java.lang.Thread.
     *                 线程运行的优先级。这个值必须来自 {@link android.os.Process} 而不是来自 java.lang.Thread
     */
    public HandlerThread(String name, int priority) {
        super(name);
        mPriority = priority;
    }
    
    /**
     * Call back method that can be explicitly overridden if needed to execute some
     * setup before Looper loops.
     * 
     * 如果在 Looper loops 之前你需要执行一些操作，重写这个回调方法
     */
    protected void onLooperPrepared() {
    }

    @Override
    public void run() {
        mTid = Process.myTid();
        Looper.prepare();
        synchronized (this) {
            mLooper = Looper.myLooper();
            notifyAll();
        }
        Process.setThreadPriority(mPriority);
        onLooperPrepared();
        Looper.loop();
        mTid = -1;
    }
    
    /**
     * This method returns the Looper associated with this thread. If this thread not been started
     * or for any reason is isAlive() returns false, this method will return null. If this thread 
     * has been started, this method will block until the looper has been initialized.
     *
     * 这个方法返回当前线程所拥有的 Looper 对象。如果这个线程并没有启动或者由于一些原因 isAlive() 方法返回为 false，
     * 这个方法将返回 null，如果这个线程已经被启动，这个方法将被锁定直到 looper 对象已经被初始化
     * 
     * @return The looper.
     */
    public Looper getLooper() {
        if (!isAlive()) {
            return null;
        }
        
        // If the thread has been started, wait until the looper has been created.
        // 如果线程已经开始，wait 直到 looper 对象被创建
        synchronized (this) {
            while (isAlive() && mLooper == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
        return mLooper;
    }

    /**
     * Quits the handler thread's looper.
     * 退出 handler 线程的 looper 实例
     * <p>
     * Causes the handler thread's looper to terminate without processing any
     * more messages in the message queue.
     * 使线程中的 looper 对象不再工作，不再去处理消息队列中的消息
     * </p><p>
     * Any attempt to post messages to the queue after the looper is asked to quit will fail.
     * For example, the {@link Handler#sendMessage(Message)} method will return false.
     * 当 looper 退出的时，任何尝试发送消息到消息队列都会失败
     * 例如: {@link Handler#sendMessage(Message)} 方法将会返回 false
     * </p><p class="note">
     * Using this method may be unsafe because some messages may not be delivered
     * before the looper terminates.  Consider using {@link #quitSafely} instead to ensure
     * that all pending work is completed in an orderly manner.
     * 使用这个方法可能是不安全的，因为在 looper 被终止之前一些消息可能没有被处理。
     * 建议使用 {@link #quitSafely} 来确保所有待完成的消息都是以有序的方式完成
     * </p>
     *
     * @return True if the looper looper has been asked to quit or false if the
     * thread had not yet started running.
     *         如果 looper 已经被退出，则返回 true。如果线程至今都没有被启动，则返回 fasle
     *
     * @see #quitSafely
     */
    public boolean quit() {
        Looper looper = getLooper();
        if (looper != null) {
            looper.quit();
            return true;
        }
        return false;
    }

    /**
     * Quits the handler thread's looper safely.
     * 让 handler 线程的 looper 实例可以很安全的退出
     * <p>
     * Causes the handler thread's looper to terminate as soon as all remaining messages
     * in the message queue that are already due to be delivered have been handled.
     * Pending delayed messages with due times in the future will not be delivered.
     * 直到消息队列中所有剩下的消息已经被 handler 处理了，线程的 looper 实例才会终止。
     * 待延迟发送的一些消息将不再被处理
     * </p><p>
     * Any attempt to post messages to the queue after the looper is asked to quit will fail.
     * For example, the {@link Handler#sendMessage(Message)} method will return false.
     * 当 looper 退出的时，任何尝试发送消息到消息队列都会失败
     * 例如: {@link Handler#sendMessage(Message)} 方法将会返回 false
     * </p><p>
     * If the thread has not been started or has finished (that is if
     * {@link #getLooper} returns null), then false is returned.
     * Otherwise the looper is asked to quit and true is returned.
     * 如果线程至今没有被启动或是已经结束({@link #getLooper} 返回 null)，将返回 fasle
     * 否则，looper 退出，返回 true
     * </p>
     *
     * @return True if the looper looper has been asked to quit or false if the
     * thread had not yet started running.
     *         如果 looper 已经被退出，则返回 true。如果线程至今都没有被启动，则返回 fasle
     */
    public boolean quitSafely() {
        Looper looper = getLooper();
        if (looper != null) {
            looper.quitSafely();
            return true;
        }
        return false;
    }

    /**
     * Returns the identifier of this thread. See Process.myTid().
     *
     * 返回这个线程的标识符。可以去看 Process.myTid() 实现
     */
    public int getThreadId() {
        return mTid;
    }
}
