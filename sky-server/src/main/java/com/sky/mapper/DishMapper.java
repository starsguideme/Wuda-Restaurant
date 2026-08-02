package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.Annotation.AutoFill;
import com.sky.dto.DishPageQueryDTO;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);
  /**
     * 插入菜品数据
     * @param dish
     */
    @AutoFill(value=OperationType.INSERT)
    void insert(Dish dish);
    //菜品分页查询
    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);

    @Select("select * from dish where id = #{id}")
    Dish getById(Long id);
    /**
     * 根据id查询菜品和口味数据
     * @param ids
     * @return
     */
    @Delete("delete from dish where id in (#{id})")
    void deleteById(Long ids);

    /**
     * 批量删除菜品
     * @param ids
     */
    void deleteByIds(List<Long> ids);

    void update(Dish dish);

    /*
    * 根据分类id查询菜品
     */
  List<Dish> list(Dish dish);

  List<Dish> listByConditon(Dish dish);
  /**
     * 根据销量排序
     * @param begin
     * @param end
     * @return
     */
                      List<GoodsSalesDTO> getTop10(@Param("begin") LocalDateTime begin,
                                                       @Param("end")LocalDateTime  end,
                                                       @Param("status")Integer completed);
  /**
   * 根据条件统计菜品数量
   * @param map
   * @return
   */
  Integer countByMap(Map map);
 void recoverId(@Param("dish_id")Long dish_id,@Param("number")long  number);

    void recoverStock(Long dishId, Integer number);
}
