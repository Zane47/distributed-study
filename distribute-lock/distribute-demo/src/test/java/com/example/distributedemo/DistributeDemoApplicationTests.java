package com.example.distributedemo;

import com.example.distributedemo.service.OrderService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DistributeDemoApplicationTests {

    // 1. 代码中实现扣除库存 -> 导致超卖问题
    // @Qualifier("orderServiceImpl1_bug")
    // 2. 扣除库存逻辑放到数据库中
    // @Qualifier("OrderServiceImpl2_dbupdate")

    @Qualifier("OrderServiceImpl4_synchronized")
    @Autowired
    private OrderService orderService;

    @Test
    public void concurrentOrder() throws InterruptedException {
        // Thread.sleep(60000);

        CountDownLatch countDownLatch = new CountDownLatch(5);

        // 等5个线程
        CyclicBarrier cyclicBarrier = new CyclicBarrier(5);

        ExecutorService es = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            es.execute(() -> {
                try {
                    // 所有线程都在这里等待, 等到某一时刻到达同时往下运行
                    // 保证创建订单方法是并发执行的
                    cyclicBarrier.await();
                    Integer orderId = orderService.createOrder();
                    System.out.println("订单id：" + orderId);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        // 等5个线程都执行完了之后再执行,
        // 和具体业务无关, 与数据库连接池有关
        // 如果不加闭锁, 主线程结束后数据库连接池就关闭了, 所有的新开的线程就无法获取到数据库连接
        // 所以添加闭锁, 等5个线程执行完之后再关闭主线程
        countDownLatch.await();
        es.shutdown();
    }

}
