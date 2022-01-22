package com.example.distributedemo.service.impl;

import com.example.distributedemo.dao.OrderItemMapper;
import com.example.distributedemo.dao.OrderMapper;
import com.example.distributedemo.dao.ProductMapper;
import com.example.distributedemo.model.Order;
import com.example.distributedemo.model.OrderItem;
import com.example.distributedemo.model.Product;
import com.example.distributedemo.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 使用数据库行锁解决超卖问题
 *
 */
@Service("OrderServiceImpl2_dbupdate")
@Slf4j
public class OrderServiceImpl2_dbupdate implements OrderService {

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


    /**
     * 在代码中实现扣减库存操作
     */
    @Transactional(rollbackOn = Exception.class)
    @Override
    public Integer createOrder() throws Exception {
        // ------------------------ 在程序中计算库存的数量 ------------------------
        Product product = productMapper.selectByPrimaryKey(purchaseProductId);
        if (product == null) {
            throw new Exception("购买商品：" + purchaseProductId + "不存在");
        }

        // 商品当前库存
        Integer currentCount = product.getCount();

        // 重要: 校验库存
        if (purchaseProductNum > currentCount) {
            throw new Exception("商品" + purchaseProductId + "仅剩" + currentCount + "件，无法购买");
        }
        
        // 计算剩余库存
        // Integer leftCount = currentCount - purchaseProductNum;
        // 更新库存
        // product.setCount(leftCount);
        // product.setUpdateTime(new Date());
        // product.setUpdateUser("xxx");
        // productMapper.updateByPrimaryKeySelective(product);


        // 使用数据库行锁解决超卖
        productMapper.updateProductCount(purchaseProductNum, "xxx", new Date(), product.getId());


        // ------------------------ 创建订单 ------------------------
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
        return order.getId();
    }

}
