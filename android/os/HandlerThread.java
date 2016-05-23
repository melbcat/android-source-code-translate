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
 * һ���ܷ�����࣬һ������ӵ�� looper ���̡߳��ڴ��� handler ������ looper ���Ա�ʹ�á�
 * ע��: start() ������Ȼ��Ҫ������
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
     *                 �߳����е����ȼ������ֵ�������� {@link android.os.Process} ���������� java.lang.Thread
     */
    public HandlerThread(String name, int priority) {
        super(name);
        mPriority = priority;
    }
    
    /**
     * Call back method that can be explicitly overridden if needed to execute some
     * setup before Looper loops.
     * 
     * ����� Looper loops ֮ǰ����Ҫִ��һЩ��������д����ص�����
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
     * ����������ص�ǰ�߳���ӵ�е� Looper �����������̲߳�û��������������һЩԭ�� isAlive() ��������Ϊ false��
     * ������������� null���������߳��Ѿ������������������������ֱ�� looper �����Ѿ�����ʼ��
     * 
     * @return The looper.
     */
    public Looper getLooper() {
        if (!isAlive()) {
            return null;
        }
        
        // If the thread has been started, wait until the looper has been created.
        // ����߳��Ѿ���ʼ��wait ֱ�� looper ���󱻴���
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
     * �˳� handler �̵߳� looper ʵ��
     * <p>
     * Causes the handler thread's looper to terminate without processing any
     * more messages in the message queue.
     * ʹ�߳��е� looper �����ٹ���������ȥ������Ϣ�����е���Ϣ
     * </p><p>
     * Any attempt to post messages to the queue after the looper is asked to quit will fail.
     * For example, the {@link Handler#sendMessage(Message)} method will return false.
     * �� looper �˳���ʱ���κγ��Է�����Ϣ����Ϣ���ж���ʧ��
     * ����: {@link Handler#sendMessage(Message)} �������᷵�� false
     * </p><p class="note">
     * Using this method may be unsafe because some messages may not be delivered
     * before the looper terminates.  Consider using {@link #quitSafely} instead to ensure
     * that all pending work is completed in an orderly manner.
     * ʹ��������������ǲ���ȫ�ģ���Ϊ�� looper ����ֹ֮ǰһЩ��Ϣ����û�б�����
     * ����ʹ�� {@link #quitSafely} ��ȷ�����д���ɵ���Ϣ����������ķ�ʽ���
     * </p>
     *
     * @return True if the looper looper has been asked to quit or false if the
     * thread had not yet started running.
     *         ��� looper �Ѿ����˳����򷵻� true������߳�����û�б��������򷵻� fasle
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
     * �� handler �̵߳� looper ʵ�����Ժܰ�ȫ���˳�
     * <p>
     * Causes the handler thread's looper to terminate as soon as all remaining messages
     * in the message queue that are already due to be delivered have been handled.
     * Pending delayed messages with due times in the future will not be delivered.
     * ֱ����Ϣ����������ʣ�µ���Ϣ�Ѿ��� handler �����ˣ��̵߳� looper ʵ���Ż���ֹ��
     * ���ӳٷ��͵�һЩ��Ϣ�����ٱ�����
     * </p><p>
     * Any attempt to post messages to the queue after the looper is asked to quit will fail.
     * For example, the {@link Handler#sendMessage(Message)} method will return false.
     * �� looper �˳���ʱ���κγ��Է�����Ϣ����Ϣ���ж���ʧ��
     * ����: {@link Handler#sendMessage(Message)} �������᷵�� false
     * </p><p>
     * If the thread has not been started or has finished (that is if
     * {@link #getLooper} returns null), then false is returned.
     * Otherwise the looper is asked to quit and true is returned.
     * ����߳�����û�б����������Ѿ�����({@link #getLooper} ���� null)�������� fasle
     * ����looper �˳������� true
     * </p>
     *
     * @return True if the looper looper has been asked to quit or false if the
     * thread had not yet started running.
     *         ��� looper �Ѿ����˳����򷵻� true������߳�����û�б��������򷵻� fasle
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
     * ��������̵߳ı�ʶ��������ȥ�� Process.myTid() ʵ��
     */
    public int getThreadId() {
        return mTid;
    }
}
