package com.sky.controller.admin;

import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.UserOrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Slf4j
@ApiOperation("订单管理接口")
public class OrderController {
         @Autowired
    private UserOrderService userOrderService;
      @GetMapping("conditionSearch")
      @ApiOperation("订单搜索")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO)
      {
          log.info("订单搜索");
          PageResult pageResult=userOrderService.conditionSearch(ordersPageQueryDTO);
          return Result.success(pageResult);
      }
      @GetMapping("/statistics")
    @ApiOperation("各个状态下的订单统计")
    public Result<OrderStatisticsVO> statistics()
    {
        log.info("各个状态下的订单统计");
        OrderStatisticsVO orderStatisticsVO=userOrderService.statistics();
        return Result.success(orderStatisticsVO);
    }
   @GetMapping("/details/{id}")
    @ApiOperation("订单详情")
    public Result<OrderVO> getOrderDetail(@PathVariable("id") Long id)
    {
        log.info("订单详情");
        OrderVO orderVO=userOrderService.getOrderDetail(id);
        return Result.success(orderVO);
    }
    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO)
    {
        log.info("接单:{}", ordersConfirmDTO);
        userOrderService.confirm(ordersConfirmDTO);
        return Result.success();
    }
    @PutMapping("/rejection")
    @ApiOperation("拒单")
    public Result cancel(@RequestBody OrdersRejectionDTO ordersRejectionDTO) throws Exception
    {
        log.info("拒单:{}", ordersRejectionDTO);
        userOrderService.cancel(ordersRejectionDTO);
        return Result.success();
    }
    @PutMapping("/delivery/{id}")
    @ApiOperation("派单")
    public Result delivery(@PathVariable("id") Long id)
    {
        log.info("派单");
        userOrderService.delivery(id);
        return Result.success();
    }
    @PutMapping("/complete/{id}")
    @ApiOperation("完成订单")
    public Result complete(@PathVariable("id") Long id)
        {
        log.info("完成订单");
        userOrderService.complete(id);
        return Result.success();
    }
}
