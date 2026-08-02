package com.sky.service.impl;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.UserMapper;
import com.sky.mapper.UserOrderMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    private UserOrderMapper userOrderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private WorkspaceService workspaceService;

    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate beginTime, LocalDate endTime) {
        log.info("营业额统计：{}到{}", beginTime, endTime);

        LocalDateTime beginDateTime = LocalDateTime.of(beginTime, LocalTime.MIN);
        LocalDateTime endDateTime = LocalDateTime.of(endTime, LocalTime.MAX);

        List<Map<String, Object>> dailyData = userOrderMapper.getDailyTurnOver(beginDateTime, endDateTime, Orders.COMPLETED);

        Map<LocalDate, Double> turnoverMap = dailyData.stream()
                .collect(Collectors.toMap(
                        m -> ((java.sql.Date) m.get("date")).toLocalDate(),
                        m -> ((Number) m.get("turnover")).doubleValue()
                ));

        List<LocalDate> dateList = generateDateList(beginTime, endTime);
        List<Double> turnoverList = dateList.stream()
                .map(date -> turnoverMap.getOrDefault(date, 0.0))
                .collect(Collectors.toList());

        return TurnoverReportVO.builder()
                .dateList(dateList.stream().map(LocalDate::toString).collect(Collectors.joining(",")))
                .turnoverList(turnoverList.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        log.info("用户统计：{}到{}", begin, end);

        // 1. 一次性查询所有数据
        Map<String, Object> params = new HashMap<>();
        params.put("begin", LocalDateTime.of(begin, LocalTime.MIN));
        params.put("end", LocalDateTime.of(end, LocalTime.MAX));
        // 开始从数据库中查找数据并封装到List集合中
        List<Map<String, Object>> dailyStats = userMapper.getDailyUserStats(params);

        // 2. 生成日期开始日期与结束日期的列表
        List<LocalDate> dateList = generateDateList(begin, end);
        List<Integer> totalUserList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();

        // 3. 内存中组装数据
        Map<LocalDate, Map<String, Integer>> statsMap = dailyStats.stream()
                .collect(Collectors.toMap(
                        stat -> ((java.sql.Date) stat.get("date")).toLocalDate(),
                        stat -> Map.of(
                                "total", ((Number) stat.get("total_count")).intValue(),
                                "new", ((Number) stat.get("new_count")).intValue()
                        )
                ));

        // 4. 以日期为索引，从内存组装中获取对应的数据
        for (LocalDate date : dateList) {
            Map<String, Integer> dayStats = statsMap.get(date);
            if (dayStats != null) {
                totalUserList.add(dayStats.get("total"));
                newUserList.add(dayStats.get("new"));
            } else {
                totalUserList.add(0);
                newUserList.add(0);
            }
        }
        //最后将得到的日期列表，总用户列表，新增用户列表封装成String类型并使其成UserReportVO对象最后返回
        String dateListString = dateList.stream()
                .map(LocalDate::toString)
                .collect(Collectors.joining(","));
        String totalUserListString = totalUserList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String newUserListString = newUserList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return UserReportVO.builder()
                .dateList(dateListString)
                .totalUserList(totalUserListString)
                .newUserList(newUserListString)
                .build();
    }

    //用于确定开始日期与结束日期之间的日期列表
    private List<LocalDate> generateDateList(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate current = begin;
        while (!current.isAfter(end)) {
            dateList.add(current);
            current = current.plusDays(1);
        }
        return dateList;
    }

    @Override
    public OrderReportVO getOrdersStatistics(LocalDate beginTime, LocalDate endTime) {
        log.info("订单统计：{}到{}", beginTime, endTime);
        LocalDateTime beginDateTime = LocalDateTime.of(beginTime, LocalTime.MIN);
        LocalDateTime endDateTime = LocalDateTime.of(endTime, LocalTime.MAX);
        List<LocalDate> dateList = generateDateList(beginTime, endTime);
        List<Map<String, Object>> dailyData = userOrderMapper.getDailyOrderStats(beginDateTime, endDateTime, Orders.COMPLETED);
        Map<LocalDate, Map<String, Integer>> statsMap = dailyData.stream()
                .collect(Collectors.toMap(
                        m -> ((java.sql.Date) m.get("date")).toLocalDate(),
                        m -> Map.of(
                                "orderCount", ((Number) m.get("order_count")).intValue(),
                                "validCount", ((Number) m.get("valid_count")).intValue()
                        )
                ));
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        for (LocalDate date : dateList) {
            Map<String, Integer> dayStast = statsMap.getOrDefault(date, Map.of("orderCount", 0, "validCount", 0));
            orderCountList.add(dayStast.get("orderCount"));
            validOrderCountList.add(dayStast.get("validCount"));
        }
        int totalOrderCount = orderCountList.stream().mapToInt(Integer::intValue).sum();
        int validOrderCount = validOrderCountList.stream().mapToInt(Integer::intValue).sum();
        double orderCompletionRate = totalOrderCount > 0 ? (double) validOrderCount / totalOrderCount : 0.0;
        return OrderReportVO.builder()
                .dateList(dateList.stream().map(LocalDate::toString).collect(Collectors.joining(",")))
                .orderCountList(orderCountList.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .validOrderCountList(validOrderCountList.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();

    }

    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {
        log.info("查询销量排名前10商品：{}到{}", begin, end);
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = userOrderMapper.getTop10(beginTime, endTime);

        // 处理空数据情况
        if (salesTop10 == null || salesTop10.isEmpty()) {
            return SalesTop10ReportVO.builder()
                    .nameList("")
                    .numberList("")
                    .build();
        }

        List<String> names = salesTop10.stream()
                .map(GoodsSalesDTO::getName)
                .collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");

        List<Integer> numbers = salesTop10.stream()
                .map(GoodsSalesDTO::getNumber)
                .collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    @Override
    public void exportBusinessData(HttpServletResponse response) {
        LocalDate BeginTime=LocalDate.now().minusDays(30);
        LocalDate EndTime=LocalDate.now().minusDays(1);
        log.info("将过去一个月的数据导入到Excel中");
        List<Map<String, Object>> orderData = userOrderMapper.getDailyOrderData(
                LocalDateTime.of(BeginTime,LocalTime.MIN)
                ,LocalDateTime.of(EndTime,LocalTime.MAX), Orders.COMPLETED);
        List<Map<String, Object>> userData = userMapper.getDailyNumber(
                LocalDateTime.of(BeginTime,LocalTime.MIN)
                ,LocalDateTime.of(EndTime,LocalTime.MAX));
        Map<LocalDate, Map<String, Object>> orderMap = orderData.stream()
                .collect(Collectors.toMap(
                        m -> ((Date) m.get("date")).toLocalDate(),
                        m -> m
                ));
        Map<LocalDate, Integer> newUserMap = userData.stream()
                .collect(Collectors.toMap(
                        m -> ((java.sql.Date) m.get("date")).toLocalDate(),
                        m -> ((Number) m.get("new_users")).intValue()
                ));
        //查询概览的营业信息
        // ✅ 从已有数据算概览（0 次 SQL）
        double totalTurnover = orderMap.values().stream()
                .mapToDouble(m -> ((Number) m.getOrDefault("turnover", 0.0)).doubleValue()).sum();
        int totalValidOrders = orderMap.values().stream()
                .mapToInt(m -> ((Number) m.getOrDefault("valid_order_count", 0)).intValue()).sum();
        int totalOrders = orderMap.values().stream()
                .mapToInt(m -> ((Number) m.getOrDefault("total_order_count", 0)).intValue()).sum();
        int totalNewUsers = newUserMap.values().stream().mapToInt(Integer::intValue).sum();
        double completionRate = totalOrders > 0 ? (double) totalValidOrders / totalOrders : 0;
        double unitPrice = totalValidOrders > 0 ? totalTurnover / totalValidOrders : 0;


        //通过POI写入Excel表格上
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            XSSFWorkbook sheets = new XSSFWorkbook(inputStream);
            //获取需要编辑对应页的页码名称
            XSSFSheet sheetAt = sheets.getSheetAt(0);
            //获取页码的对应需要编辑的行码
            //编写第二行数据
            XSSFRow row = sheetAt.getRow(1);
            row.getCell(1).setCellValue("时间："+BeginTime+"至"+EndTime);

            //编写第四行数据
            row = sheetAt.getRow(3);
            row.getCell(2).setCellValue(totalTurnover);
            row.getCell(4).setCellValue(completionRate);
            row.getCell(6).setCellValue(totalNewUsers);

            //编写第五行数据
            row = sheetAt.getRow(4);
            row.getCell(2).setCellValue(totalOrders);
            row.getCell(4).setCellValue(unitPrice);

            //开始填充详细数据
            for(int i=0;i<30;i++)
            {
                LocalDate currentTime=BeginTime.plusDays(i);
                Map<String,Object>dayOrder=orderMap.getOrDefault(currentTime,Map.of());
                Integer newUsers=newUserMap.getOrDefault(currentTime,0);
                row = sheetAt.getRow(7+i);
                row.getCell(1).setCellValue(currentTime.toString());
                row.getCell(2).setCellValue(totalTurnover);
                row.getCell(3).setCellValue(totalValidOrders);
                row.getCell(4).setCellValue(completionRate);
                row.getCell(5).setCellValue(unitPrice);
                row.getCell(6).setCellValue(newUsers);
            }

            //开始导出数据
            OutputStream outputStream = response.getOutputStream();
            sheets.write(outputStream);
            //关闭流
            outputStream.close();
            sheets.close();
            inputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}