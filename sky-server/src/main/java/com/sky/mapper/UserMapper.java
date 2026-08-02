package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    void insert(User user);

    User getById(Long userId);

    //根据动态条件来查询用户数量
      List<Map<String, Object>> getDailyUserStats(Map<String, Object> params);
      List<Map<String, Object>> getDailyNumber(@Param("beginTime")LocalDateTime beginTime,
                                               @Param("endTime")LocalDateTime endTime);

    Integer countByMap(Map map);
}
