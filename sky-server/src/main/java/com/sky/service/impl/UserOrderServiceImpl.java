package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.sky.WebSocket.WebSocketServer;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.UserOrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.RedisConstant.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.github.pagehelper.Page;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class UserOrderServiceImpl implements UserOrderService {
    @Autowired
    private UserOrderMapper userOrderMapper;
    @Autowired
    private UserOrderDetailMapper userOrderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;
    @Autowired
    @Qualifier("redisTemplate")
    private RedisTemplate redisTemplate;

    @Override
    @Transactional  // 添加事务注解
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        log.info("用户下单：{}", ordersSubmitDTO);

        // 1. 校验地址簿
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 2. 获取当前用户ID
        Long userId = BaseContext.getCurrentId();

        // 3. 从Redis获取购物车数据
        String key = Constant.SHOPPING_CART + userId;
        List<Object> objectList = redisTemplate.opsForHash().values(key);
        if (objectList == null || objectList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 4. 将LinkedHashMap转换为ShoppingCart对象
        List<ShoppingCart> cartList = objectList.stream()
                .map(obj -> {
                    if (obj instanceof ShoppingCart) {
                        return (ShoppingCart) obj;
                    } else {
                        // 如果是LinkedHashMap，手动转换
                        Map<String, Object> map = (Map<String, Object>) obj;
                        ShoppingCart cart = new ShoppingCart();
                        cart.setId(map.get("id") != null ? Long.valueOf(map.get("id").toString()) : null);
                        cart.setName((String) map.get("name"));
                        cart.setUserId(map.get("userId") != null ? Long.valueOf(map.get("userId").toString()) : null);
                        cart.setDishId(map.get("dishId") != null ? Long.valueOf(map.get("dishId").toString()) : null);
                        cart.setSetmealId(map.get("setmealId") != null ? Long.valueOf(map.get("setmealId").toString()) : null);
                        cart.setDishFlavor((String) map.get("dishFlavor"));
                        cart.setNumber(map.get("number") != null ? Integer.valueOf(map.get("number").toString()) : null);
                        cart.setAmount(map.get("amount") != null ? new BigDecimal(map.get("amount").toString()) : null);
                        cart.setImage((String) map.get("image"));
                        // LocalDateTime需要特殊处理
                        Object createTimeObj = map.get("createTime");
                        if (createTimeObj != null) {
                            try {
                                if (createTimeObj instanceof LocalDateTime) {
                                    cart.setCreateTime((LocalDateTime) createTimeObj);
                                } else if (createTimeObj instanceof java.time.LocalDateTime) {
                                    cart.setCreateTime((java.time.LocalDateTime) createTimeObj);
                                } else {
                                    // 其他类型暂时不设置，避免解析错误
                                    log.warn("createTime类型不支持: {}", createTimeObj.getClass().getName());
                                }
                            } catch (Exception e) {
                                log.warn("createTime转换失败，跳过该字段", e);
                            }
                        }
                        return cart;
                    }
                })
                .collect(Collectors.toList());

        // 5. 计算订单总金额
        BigDecimal totalAmount = cartList.stream()
                .map(cart -> cart.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 6. 创建订单
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());  // 设置收货人
        orders.setAddress(addressBook.getDetail());       // 设置收货地址
        orders.setUserId(userId);
        orders.setAmount(totalAmount);                    // 设置订单总金额

        userOrderMapper.insert(orders);

        // 7. 创建订单详情（这里改为遍历 cartList）
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (ShoppingCart cart : cartList) {  // 修改这里：使用 cartList 而不是 list
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);
        }

        userOrderDetailMapper.insertBatch(orderDetails);

        // 8. 清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        // 9. 构建返回结果
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);
        // 判断当前用户是否登录
        if(user==null||user.getOpenid()==null)
        {
            throw new OrderBusinessException(MessageConstant.USER_NOT_LOGIN);
        }

        //调用微信支付接口，生成预支付交易单
        try {
            JSONObject jsonObject = weChatPayUtil.pay(
                    ordersPaymentDTO.getOrderNumber(), //商户订单号
                    new BigDecimal(0.01), //支付金额，单位 元
                    "苍穹外卖订单", //商品描述
                    user.getOpenid() //微信用户的openid
            );

            if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
                throw new OrderBusinessException("该订单已支付");
            }

            OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
            vo.setPackageStr(jsonObject.getString("package"));

            return vo;
        } catch (Exception e) {
            log.error("微信支付失败：%s", e);
            throw new RuntimeException("微信支付未上线");
        }
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = userOrderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        userOrderMapper.update(orders);
        //通过WebSocket推送消息给客户端发送消息 如type， content， orderId
        Map<String, Object> map = new HashMap<>();
        map.put("type", 1);//1表示来单提醒，2表示客户催单
        map.put("orderId", ordersDB.getId());
        map.put("content","订单号"+outTradeNo);
        String jsonString = JSONObject.toJSONString(map);
        webSocketServer.sendToAllClient(jsonString);
    }

    /**
     * 历史订单查询
     *
     * @param page
     * @param pageSize
     * @param status
     */
    public PageResult pageQuery4User(Integer page, Integer pageSize, Integer status) {
        log.info("分页查询订单，页码：{}，页数：{}，状态：{}", page, pageSize, status);

        // 设置分页参数
        PageHelper.startPage(page, pageSize);

        // 构建查询条件
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        // 查询订单
        List<Orders> orderList = userOrderMapper.pageQuery(ordersPageQueryDTO);

        // 安全地获取分页信息
        Page<Orders> pageInfo = (Page<Orders>) orderList;
        long total = pageInfo.getTotal();

        List<OrderVO> list = new ArrayList<>();

        // 如果有订单，批量查询订单详情
        if (orderList != null && !orderList.isEmpty()) {
            // 收集所有订单ID
            List<Long> orderIds = orderList.stream()
                    .map(Orders::getId)
                    .collect(Collectors.toList());

            // 批量查询所有订单详情（需要在Mapper中新增此方法）
            List<OrderDetail> allOrderDetails = userOrderDetailMapper.getByOrderIds(orderIds);

            // 按订单ID分组
            Map<Long, List<OrderDetail>> orderDetailMap = allOrderDetails.stream()
                    .collect(Collectors.groupingBy(OrderDetail::getOrderId));

            // 组装VO
            for (Orders orders : orderList) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                // 从Map中获取对应的订单详情
                List<OrderDetail> orderDetails = orderDetailMap.getOrDefault(orders.getId(), new ArrayList<>());
                orderVO.setOrderDetailList(orderDetails);
                list.add(orderVO);
            }
        }

        return new PageResult(total, list);
    }
    /**
     * 订单详情
     *
     * @param id
     * @return
     */
    public OrderVO getOrderDetail(Long id) {
        //先根据用户id 查询订单
        Orders orders = userOrderMapper.getById(id);
        //获取用户查询订单的菜品信息或者套餐信息
        List<OrderDetail> orderDetailList = userOrderDetailMapper.getByOrderId(id);
        //封转vo 进行返回给后端
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * 催单
     *
     * @param id
     */
    public void reminder(Long id) {
        log.info("催单处理，订单id：{}", id);
        //首先先判断是否有订单
        Orders orders = userOrderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Map map = new HashMap();
        map.put("type", 2);//短信类型，1来单信息 2催单信息
        map.put("orderId", id);
        map.put("content", "订单号：" + orders.getNumber());
        String jsonString = JSONObject.toJSONString(map);
        webSocketServer.sendToAllClient(jsonString);
    }

    /**
     * 取消订单
     *
     * @param id
     */
    public void cancel(Long id) throws Exception {
        log.info("取消订单，订单id：{}", id);

        // 先判断是否有订单
        Orders order = userOrderMapper.getById(id);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 明确的状态判断：只有待支付和待接单可以取消
        if (order.getStatus() != Orders.PENDING_PAYMENT &&
                order.getStatus() != Orders.TO_BE_CONFIRMED) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders updateOrder = new Orders();
        updateOrder.setId(order.getId());
        updateOrder.setCancelReason("用户取消");
        updateOrder.setCancelTime(LocalDateTime.now());

        // 待接单状态下取消需要退款
        if (order.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            try {
                // 使用正确的金额构造方式
                BigDecimal refundAmount = new BigDecimal("0.01");
                BigDecimal orderAmount = new BigDecimal("0.01");

                weChatPayUtil.refund(
                        order.getNumber(),    // 商户订单号
                        order.getNumber(),    // 微信交易号（注意：这里应该是微信交易号，不是商户订单号）
                        refundAmount,         // 退款金额
                        orderAmount           // 订单金额
                );

                // ✅ 正确：同时设置订单状态和支付状态
                updateOrder.setStatus(Orders.CANCELLED);    // 订单状态：已取消
                updateOrder.setPayStatus(Orders.REFUND);    // 支付状态：已退款

            } catch (Exception e) {
                log.error("退款失败，订单号：{}", order.getNumber(), e);
                // 退款失败也继续取消订单，但记录状态
                updateOrder.setStatus(Orders.CANCELLED);
            }
        } else {
            // 待支付状态下直接取消，不需要退款
            updateOrder.setStatus(Orders.CANCELLED);        // 订单状态：已取消
            updateOrder.setPayStatus(Orders.UN_PAID);        // 支付状态：未支付
        }

        userOrderMapper.update(updateOrder);
        log.info("订单取消完成，订单id：{}", id);
    }

    /**
     * 再来一单
     *
     * @param id
     */
    public void repetition(Long id) {
        log.info("再来一单，订单id：{}", id);
        //查询当前用户ID
        Long userId = BaseContext.getCurrentId();
        //根据当前用户ID查询当前订单详情
        List<OrderDetail> orderDetailList = userOrderDetailMapper.getByOrderId(id);
        //将当前订单详情转换成购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            //将原本的订单详情中的菜品信息重新转换成购物车对象
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());
        //将购物车对象批量添加至数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 订单搜索
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = (Page<Orders>) userOrderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> orderVOList = getOrderVOList(page);
        return new PageResult(page.getTotal(), orderVOList);
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        List<OrderVO> orderVOList = new ArrayList<>();
        List<Orders> ordersList = page.getResult();
        if (ordersList != null && !ordersList.isEmpty()) {
            for (Orders orders : ordersList) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderList = userOrderDetailMapper.getByOrderId(orders.getId());
        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());
        return String.join("", orderDishList);
    }

    /**
     * 统计各个情况下订单的 数量
     *
     * @return
     */
    public OrderStatisticsVO statistics() {
        //根据状态，分别查询出待接单、待派送、派送中的订单数量
        Integer toBeConfirmed = userOrderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = userOrderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = userOrderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);
        //将查询出的数据封装到orderStatisticsVO中响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 接单
     *
     * @return
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        log.info("接单：{}", ordersConfirmDTO);
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        userOrderMapper.update(orders);
    }

    /**
     * 拒单
     *
     * @param ordersRejectionDTO
     */
    public void cancel(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        Orders orders = userOrderMapper.getById(ordersRejectionDTO.getId());
        //获取订单的状态
        Integer orderStatus = orders.getStatus();
        if (orderStatus == Orders.TO_BE_CONFIRMED || orders.getPayStatus() == Orders.PAID) {
            // 用户已经支付，需要退款
            String refund = weChatPayUtil.refund(
                    orders.getNumber(),
                    orders.getNumber(),
                    new BigDecimal("0.01"),
                    new BigDecimal("0.01"));
            log.info("申请退款：{}", refund);
        }
        //管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
        Orders order = new Orders();
        order.setId(ordersRejectionDTO.getId());
        order.setStatus(Orders.CANCELLED);
        order.setCancelReason(ordersRejectionDTO.getRejectionReason());
        order.setCancelTime(LocalDateTime.now());
        userOrderMapper.update(order);
        log.info("订单取消完成，订单id：{}", ordersRejectionDTO.getId());
    }

    /**
     * 派送订单
     *
     * @param id
     */
    public void delivery(Long id) {
        //先根据id查询订单
        Orders orders = userOrderMapper.getById(id);
        //判断订单是否存在并且设置状态为派送中
        if (orders == null || !orders.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders order = new Orders();
        order.setId(id);
        order.setStatus(Orders.DELIVERY_IN_PROGRESS);
        userOrderMapper.update(order);
    }

    public void complete(Long id) {
        //先根据id查询订单
        Orders orders = userOrderMapper.getById(id);
        //判断订单是否存在并且设置状态为已完成,注:只能将正在派送中的订单修改为已完成
        if (orders == null || !orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders order = new Orders();
        order.setId(id);
        order.setStatus(Orders.COMPLETED);
        order.setDeliveryTime(LocalDateTime.now());
        userOrderMapper.update(order);
    }
}

