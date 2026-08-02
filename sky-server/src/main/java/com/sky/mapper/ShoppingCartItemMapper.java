package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.ShoppingCartItem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShoppingCartItemMapper extends BaseMapper<ShoppingCartItem> {
    List<ShoppingCartItem> selectByUserId(Long userId);
}
