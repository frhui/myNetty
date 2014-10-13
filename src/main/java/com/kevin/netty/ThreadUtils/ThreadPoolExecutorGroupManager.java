package com.kevin.netty.ThreadUtils;


import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.apache.log4j.xml.DOMConfigurator;
import org.springframework.stereotype.Service;


import java.util.concurrent.atomic.AtomicInteger;


@Service
public class ThreadPoolExecutorGroupManager {

    private EventExecutorGroup EventExecutor[];
    private final AtomicInteger _index = new AtomicInteger();
    private static final int MAX_FLG = 100;

    public ThreadPoolExecutorGroupManager() {

    }

    /**
     * 初始化线程组
     *
     * @param poolArraySize
     */
    public void init(int poolArraySize) {
        int DEFAULT_THREAD_CORE_SIZE = 1;
        init(poolArraySize, DEFAULT_THREAD_CORE_SIZE, ThreadPoolExecutorGroupManager.class + "");
    }


    /**
     * 初始化线程组
     *
     * @param poolArraySize  线程组数量
     * @param threadCoreSize 每个线程池数量
     */
    public void init(int poolArraySize, int threadCoreSize, String groupNmae) {
        EventExecutor = new EventExecutorGroup[poolArraySize];
        for (int i = 0; i != poolArraySize; ++i) {
            EventExecutor[i] = new DefaultEventExecutorGroup(
                    threadCoreSize,
                    new PriorityThreadFactory(groupNmae + "+#+" + i, Thread.NORM_PRIORITY));
        }
    }


    /**
     * 获取下一个线程池
     *
     * @return EventExecutorGroup
     */
    public EventExecutorGroup nextThreadPool() {
        int index = _index.getAndIncrement();
        if (index >= MAX_FLG) {
            _index.set(0);
        }
        return EventExecutor[Math.abs(index) % EventExecutor.length];
    }


    public static void main(String[] args) {
        DOMConfigurator.configure("res/log4j.xml");
        ThreadPoolExecutorGroupManager executorGroupManager = new ThreadPoolExecutorGroupManager();
        executorGroupManager.init(2, 3, "excutors");
        for (int i = 0; i != 102; ++i) {
            System.out.println("-->>>>" + executorGroupManager.nextThreadPool());
        }
    }


}
