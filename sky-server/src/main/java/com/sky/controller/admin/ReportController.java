package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/report")
@Api(tags="数据统计图标展示")
public class ReportController {
    @Autowired
    private ReportService reportService;
    @GetMapping("/turnoverStatistics")
    @ApiOperation("营业额统计")
    public Result<TurnoverReportVO> turnoverStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("{},{}", begin, end);
        return Result.success(reportService.getTurnoverStatistics(begin, end));
    }
    //用户统计
    @GetMapping("/userStatistics")
    @ApiOperation("用户统计")
    public Result<UserReportVO> userStatistics
    (@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
    @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end)
    {
     log.info("{},{}", begin, end);
     return Result.success(reportService.getUserStatistics(begin, end));
    }
    /**
     *订单统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/ordersStatistics")
    @ApiOperation("订单统计")
    public Result<OrderReportVO> ordersStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd")LocalDate end)
    {
        log.info("统计从{}到{}的订单情况", begin, end);
        return Result.success(reportService.getOrdersStatistics(begin, end));
    }
    /**
     * 查询前10的订单排名
     * @return
     */
    @GetMapping("/top10")
    @ApiOperation("查询前10的订单排名")
    public Result<SalesTop10ReportVO> getTop10(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd")LocalDate end)
    {
        log.info("查询{}至{}的销量排名", begin, end);
        return Result.success(reportService.getTop10(begin, end));
    }
    /**
     * 导出数据
     * @param response
     */
    @GetMapping("/export")
    @ApiOperation("导出数据")
    //需要Response对象才可以导出数据
    public void exportData(HttpServletResponse response) {
        reportService.exportBusinessData(response);
    }
}
