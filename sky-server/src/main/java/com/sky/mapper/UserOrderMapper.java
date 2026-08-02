package com.sky.mapper;

import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import io.swagger.models.auth.In;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserOrderMapper {
    void insert(Orders orders);
    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    Page<Orders> historyOrders(Long currentId);

    List<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);
    @Select("select count(id) from orders where status = #{status}")
    Integer countStatus(Integer deliveryInProgress);

    Double sumByMap(Map map);

   List<GoodsSalesDTO> getTop10(LocalDateTime begin, LocalDateTime end);

    Integer countAllOrders(
            @Param("beginTime")LocalDateTime beginTime,
            @Param("endTime")LocalDateTime endTime);

    List<Orders> getStatusAndOrderTimeLT(Integer pendingPayment, LocalDateTime time);

    Integer getStatusAndVaildOrder(@Param("beginTime") LocalDateTime beginTime,
                                   @Param("endTime") LocalDateTime endTime,
                                   @Param("status") Integer completed);

    Integer countByMap(Map map);
    /**
     * 统计指定时间范围内的订单数据
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return Map 包含：orderCount, turnover, validOrderCount, avgPrice
     */
    Map<String,Object>getHourlyStatus(
            @Param("startTime") LocalDateTime startTime, @Param("endTime")
            LocalDateTime endTime);

/**
 * 查询今日因超时取消的订单
 * @param todayStart 开始时间
 * @param todayEnd 结束时间
 * @param cancelledStatus 已取消状态码
 * @return 订单列表
 */
List<Orders> selectCanceledOrderToday(
    @Param("todayStart") LocalDateTime todayStart,
    @Param("todayEnd") LocalDateTime todayEnd,
    @Param("cancelledStatus") Integer cancelledStatus
);

    void markStockRestored(@Param("status")Integer  status);

    List<Map<String, Object>> getDailyTurnOver(
            @Param("beginTime") LocalDateTime beginTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("status") Integer status);

    /**
     * @param beginDateTime
     * @param endDateTime
     * @param status
     * @return
     */
    List<Map<String, Object>> getDailyOrderStats(@Param("beginTime") LocalDateTime beginDateTime,
                                                 @Param("endTime")LocalDateTime endDateTime,
                                                 @Param("status") Integer status);

    List<Map<String, Object>> getDailyOrderData(@Param("beginTime") LocalDateTime beginDateTime,
                                                 @Param("endTime")LocalDateTime endDateTime,
                                                 @Param("status") Integer status);
    List<Map<String, Object>> getTodayTurnOver(@Param("beginTime") LocalDate beginDateTime,
                                               @Param("endTime")LocalDate endDateTime,
                                               @Param("status") Integer status);
}
