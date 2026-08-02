package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public interface UserOrderService {
    OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO);
    void paySuccess(String outTradeNo);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;
    PageResult pageQuery4User(Integer page, Integer pageSize, Integer status);

    OrderVO getOrderDetail(Long id);

    void reminder(Long id);

    void cancel(Long id) throws Exception;

    void repetition(Long id);

    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    OrderStatisticsVO statistics();

    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    void cancel(OrdersRejectionDTO ordersRejectionDTO)throws  Exception;

    void delivery(Long id);

    void complete(Long id);
}
