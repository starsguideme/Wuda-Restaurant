package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserOrderDetailMapper {
    void insertBatch(List<OrderDetail> orderDetail);
    
    List<OrderDetail> getByOrderId(@Param("orderId") Long orderId);
    
    List<OrderDetail>getOrderId(@Param("orderId") Long orderId);

    List<OrderDetail> getByOrderIds(@Param("orderIds") List<Long> orderIds);
}
