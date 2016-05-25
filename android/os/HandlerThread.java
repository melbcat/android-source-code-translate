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
 * 这是一个方便创建新线程并拥有 looper 的类，可以利用这个 looper 创建 Handler
 * 注意: start() 方法仍然还没有被调用
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
     * 构造一个 HandlerThread 对象
     * @param name
     * @param priority 线程运行的优先级。这个值必须来自 {@link android.os.Process} 而不是来自 java.lang.Thread
     */
    public HandlerThread(String name, int priority) {
        super(name);
        mPriority = priority;
    }
    
    /**
     * 如果在 Looper 调用 loops 方法之前你需要执行一些操作，请重写这个回调方法
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
     * 这个方法返回当前线程所拥有的 Looper 对象。如果这个线程并没有启动或者由于一些原因 isAlive() 方法返回为 false，
     * 这个方法将返回 null，如果这个线程已经被启动，这个方法将被锁定直到 looper 对象已经被初始化
     * 
     * @return The looper.
     */
    public Looper getLooper() {
        if (!isAlive()) {
            return null;
        }
        
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
     * 退出当前线程中正在运行的 looper 实例
     * <p>
     * 使线程中的 looper 对象不再工作，不再去处理消息队列中的消息
     * </p><p>
     * 当 looper 退出的时，任何尝试发送消息到消息队列都会失败
     * 例如: {@link Handler#sendMessage(Message)} 方法将会返回 false
     * </p><p class="note">
     * 使用这个方法可能是不安全的，因为在 looper 被终止之前一些消息可能没有被处理。
     * 建议使用 {@link #quitSafely} 来确保所有待完成的消息都是以有序的方式完成
     * </p>
     *
     * @return 如果 looper 已经被退出，则返回 true。如果线程至今都没有被启动，则返回 fasle
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
     * 安全退出当前线程中正在运行的 looper 实例
     * <p>
     * 直到消息队列中所有剩下的消息已经被 handler 处理了，线程的 looper 实例才会终止。
     * 待延迟发送的一些消息将不再被处理
     * </p><p>
     * 当 looper 退出的时，任何尝试发送消息到消息队列都会失败
     * 例如: {@link Handler#sendMessage(Message)} 方法将会返回 false
     * </p><p>
     * 如果线程至今没有被启动或是已经结束({@link #getLooper} 返回 null)，将返回 fasle
     * 否则，looper 退出，返回 true
     * </p>
     *
     * @return 如果 looper 已经被退出，则返回 true。如果线程至今都没有被启动，则返回 fasle
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
     * 返回这个线程的标识符。可以去看 Process.myTid() 具体实现
     */
    public int getThreadId() {
        return mTid;
    }
}