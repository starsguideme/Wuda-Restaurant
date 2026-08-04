package com.sky.mapper;

import com.sky.entity.ShoppingCartItem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShoppingCartItemMapper {
    List<ShoppingCartItem> selectByUserId(Long userId);
}
