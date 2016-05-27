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
 * ����һ�����㴴�����̲߳�ӵ�� looper ���࣬����������� looper ���� Handler
 * ע��: start() ������Ȼ��û�б�����
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
     * ����һ�� HandlerThread ����
     * @param name
     * @param priority �߳����е����ȼ������ֵ�������� {@link android.os.Process} ���������� java.lang.Thread
     */
    public HandlerThread(String name, int priority) {
        super(name);
        mPriority = priority;
    }
    
    /**
     * ����� Looper ���� loops ����֮ǰ����Ҫִ��һЩ����������д����ص�����
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
     * ����������ص�ǰ�߳���ӵ�е� Looper �����������̲߳�û��������������һЩԭ�� isAlive() ��������Ϊ false��
     * ������������� null���������߳��Ѿ������������������������ֱ�� looper �����Ѿ�����ʼ��
     * 
     * @return The looper.
     */
    public Looper getLooper() {
        if (!isAlive()) {
            return null;
        }
        
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
     * �˳���ǰ�߳����������е� looper ʵ��
     * <p>
     * ʹ�߳��е� looper �����ٹ���������ȥ������Ϣ�����е���Ϣ
     * </p><p>
     * �� looper �˳���ʱ���κγ��Է�����Ϣ����Ϣ���ж���ʧ��
     * ����: {@link Handler#sendMessage(Message)} �������᷵�� false
     * </p><p class="note">
     * ʹ��������������ǲ���ȫ�ģ���Ϊ�� looper ����ֹ֮ǰһЩ��Ϣ����û�б�����
     * ����ʹ�� {@link #quitSafely} ��ȷ�����д���ɵ���Ϣ����������ķ�ʽ���
     * </p>
     *
     * @return ��� looper �Ѿ����˳����򷵻� true������߳�����û�б��������򷵻� fasle
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
     * ��ȫ�˳���ǰ�߳����������е� looper ʵ��
     * <p>
     * ֱ����Ϣ����������ʣ�µ���Ϣ�Ѿ��� handler �����ˣ��̵߳� looper ʵ���Ż���ֹ��
     * ���ӳٷ��͵�һЩ��Ϣ�����ٱ�����
     * </p><p>
     * �� looper �˳���ʱ���κγ��Է�����Ϣ����Ϣ���ж���ʧ��
     * ����: {@link Handler#sendMessage(Message)} �������᷵�� false
     * </p><p>
     * ����߳�����û�б����������Ѿ�����({@link #getLooper} ���� null)�������� fasle
     * ����looper �˳������� true
     * </p>
     *
     * @return ��� looper �Ѿ����˳����򷵻� true������߳�����û�б��������򷵻� fasle
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
     * ��������̵߳ı�ʶ��������ȥ�� Process.myTid() ����ʵ��
     */
    public int getThreadId() {
        return mTid;
    }
}