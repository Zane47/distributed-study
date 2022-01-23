package com.example.distributedemo.service.impl;

import com.example.distributedemo.dao.OrderItemMapper;
import com.example.distributedemo.dao.OrderMapper;
import com.example.distributedemo.dao.ProductMapper;
import com.example.distributedemo.model.Order;
import com.example.distributedemo.model.OrderItem;
import com.example.distributedemo.model.Product;
import com.example.distributedemo.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ReentrantLock可重入锁
 * <p>
 * 自己管理事务的提交
 */
@Service("OrderServiceImpl5_ReentrantLock")
@Slf4j
public class OrderServiceImpl5_ReentrantLock implements OrderService {

    @Resource
    private OrderMapper orderMapper;
    @Resource
    private OrderItemMapper orderItemMapper;
    @Resource
    private ProductMapper productMapper;
    //购买商品id
    private int purchaseProductId = 100100;
    //购买商品数量
    private int purchaseProductNum = 1;

    private Lock lock = new ReentrantLock();

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private TransactionDefinition transactionDefinition;


    /**
     *
     */
    // @Transactional(rollbackOn = Exception.class)
    @Override
    public Integer createOrder() throws Exception {
        Product product = null;

        lock.lock();
        try {
            TransactionStatus transaction1 = platformTransactionManager.getTransaction(transactionDefinition);
            product = productMapper.selectByPrimaryKey(purchaseProductId);
            if (product == null) {
                platformTransactionManager.rollback(transaction1);
                throw new Exception("购买商品：" + purchaseProductId + "不存在");
            }


            // 商品当前库存
            Integer currentCount = product.getCount();
            // 当前线程名称
            System.out.println(Thread.currentThread().getName() + "库存数: " + currentCount);

            // 重要: 校验库存
            if (purchaseProductNum > currentCount) {
                platformTransactionManager.rollback(transaction1);
                throw new Exception("商品" + purchaseProductId + "仅剩" + currentCount + "件，无法购买");
            }

            // 使用数据库行锁解决超卖
            productMapper.updateProductCount(purchaseProductNum, "xxx", new Date(), product.getId());

            platformTransactionManager.commit(transaction1);
        } finally {
            lock.unlock();
        }


        // ------------------------ 创建订单 ------------------------


        // 插入的时候也新建事务
        TransactionStatus transaction2 = platformTransactionManager.getTransaction(transactionDefinition);

        Order order = new Order();
        order.setOrderAmount(product.getPrice().multiply(new BigDecimal(purchaseProductNum)));
        order.setOrderStatus(1);//待处理
        order.setReceiverName("xxx");
        order.setReceiverMobile("13311112222");
        order.setCreateTime(new Date());
        order.setCreateUser("xxx");
        order.setUpdateTime(new Date());
        order.setUpdateUser("xxx");
        orderMapper.insertSelective(order);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(order.getId());
        orderItem.setProductId(product.getId());
        orderItem.setPurchasePrice(product.getPrice());
        orderItem.setPurchaseNum(purchaseProductNum);
        orderItem.setCreateUser("xxx");
        orderItem.setCreateTime(new Date());
        orderItem.setUpdateTime(new Date());
        orderItem.setUpdateUser("xxx");
        orderItemMapper.insertSelective(orderItem);
        platformTransactionManager.commit(transaction2);
        return order.getId();
    }

}
