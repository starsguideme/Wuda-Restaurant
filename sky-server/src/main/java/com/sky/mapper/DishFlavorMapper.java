package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DishFlavorMapper {
    //批量插入菜品与口味
    void insertBatch(@Param("dishFlavors") List<DishFlavor> dishFlavors);
@Delete("delete from dish_flavor where dish_id = #{id}")
    void deleteByDishId(Long dishId);

    void deleteByDishIds(List<Long> ids);

    List<DishFlavor> getByDishId(Long id);
}
