package com.sky.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.sky.RedisConstant.Constant;
import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.entity.ShoppingCartItem;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartItemMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.result.PageResult;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.sky.RedisConstant.Constant.DISH;
import static com.sky.RedisConstant.Constant.SETMEAL;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    
    // 新增注入
    @Autowired
    private ShoppingCartItemMapper cartItemMapper;
    
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    
    // TODO: 【购物车缓存】使用 Redis Hash 优化购物车性能
    // 场景：购物车频繁读写，使用 Redis 提升性能
    // 练习目标：
    // 1. 使用 Redis Hash 存储购物车数据
    // 2. key 格式：cart:{userId}
    // 3. field 格式：{dishId}_{flavor} 或 {setmealId}
    // 4. value 格式：JSON 序列化的购物车对象
    // 5. 设置 7 天过期时间

    public void addShoppingCart(@Valid ShoppingCartDTO shoppingCartDTO) {
        log.info("添加购物车：{}", shoppingCartDTO);
        Long userId= BaseContext.getCurrentId();
       try {
           String key = Constant.SHOPPING_CART + userId;
           String fieldKey = shoppingCartDTO.getDishId() != null ?
                   DISH + shoppingCartDTO.getDishId() + "_" + shoppingCartDTO.getDishFlavor()
                   : SETMEAL+ shoppingCartDTO.getSetmealId();
           ShoppingCart cart = (ShoppingCart) redisTemplate.opsForHash().get(key, fieldKey);
           if (cart != null) {
               cart.setNumber(cart.getNumber() + 1);
               redisTemplate.opsForHash().put(key, fieldKey, cart);
           } else {
               cart = new ShoppingCart();
               BeanUtils.copyProperties(shoppingCartDTO, cart);
               if (shoppingCartDTO.getDishId() != null) {
                   Dish dish = dishMapper.getById(shoppingCartDTO.getDishId());
                   cart.setName(dish.getName());
                   cart.setImage(dish.getImage());
                   cart.setAmount(dish.getPrice());
               } else {
                   Setmeal setmeal = setmealMapper.getById(shoppingCartDTO.getSetmealId());
                   cart.setName(setmeal.getName());
                   cart.setImage(setmeal.getImage());
                   cart.setAmount(setmeal.getPrice());
               }
               cart.setNumber(1);
               cart.setUserId(userId);
               cart.setCreateTime(LocalDateTime.now());
           }
           redisTemplate.opsForHash().put(key, fieldKey, cart);
           redisTemplate.expire(key, 3, TimeUnit.HOURS);
       }
       catch (Exception e)
       {
           log.error("购物车数据添加失败", e);
       }
    }
    // TODO: 【购物车方法】添加商品到 Redis 购物车
    // private void addToRedisCart(Long userId, ShoppingCart cart) {
    //     String key = "cart:" + userId;
    //     String field = cart.getDishId() != null 
    //         ? cart.getDishId() + "_" + cart.getDishFlavor()
    //         : cart.getSetmealId().toString();
    //     redisTemplate.opsForHash().put(key, field, cart);
    //     redisTemplate.expire(key, 7, TimeUnit.DAYS);
    // }
    
    // TODO: 【购物车方法】从 Redis 查询购物车
    // private List<ShoppingCart> getFromRedisCart(Long userId) {
    //     String key = "cart:" + userId;
    //     return redisTemplate.opsForHash().values(key);
    // }
    
    // TODO: 【购物车方法】更新 Redis 购物车数量
    // private void updateRedisCartNumber(Long userId, Long dishId, String flavor, Integer increment) {
    //     String key = "cart:" + userId;
    //     String field = dishId + "_" + flavor;
    //     redisTemplate.opsForHash().increment(key, field, increment);
    // }
    
    // TODO: 【购物车方法】清空 Redis 购物车
    // private void clearRedisCart(Long userId) {
    //     String key = "cart:" + userId;
    //     redisTemplate.delete(key);
    // }
    
//    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
//        log.info("添加购物车：{}", shoppingCartDTO);
//        ShoppingCart shoppingCart = new ShoppingCart();
//        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
//        Long userId = BaseContext.getCurrentId();
//        shoppingCart.setUserId(userId);
//        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
//        if (list != null && list.size() > 0) {
//            ShoppingCart cart = list.get(0);
//            cart.setNumber(cart.getNumber() + 1);
//            shoppingCartMapper.updateNumberById(cart);
//        } else {
//            Long dishId = shoppingCartDTO.getDishId();
//            if (dishId != null) {
//                Dish dish = dishMapper.getById(dishId);
//                shoppingCart.setName(dish.getName());
//                shoppingCart.setImage(dish.getImage());
//                shoppingCart.setAmount(dish.getPrice());
//            } else {
//                Long setmealId = shoppingCartDTO.getSetmealId();
//                Setmeal setmeal = setmealMapper.getById(setmealId);
//                shoppingCart.setName(setmeal.getName());
//                shoppingCart.setImage(setmeal.getImage());
//                shoppingCart.setAmount(setmeal.getPrice());
//            }
//            shoppingCart.setNumber(1);
//            shoppingCart.setCreateTime(LocalDateTime.now());
//            shoppingCartMapper.insert(shoppingCart);
//        }
//    }



    public void clean() {
        log.info("清空购物车");
        Long userId = BaseContext.getCurrentId();
        //由于进行了双写，必须得删除Redis中的数据
        String key=Constant.SHOPPING_CART+userId;
        redisTemplate.delete(key);
        shoppingCartMapper.deleteByUserId(userId);
    }

    @Override
    @Transactional
    public PageResult<ShoppingCartItem> getShoppingCart(Long userId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        // 假设你的 Mapper 返回的是 List<ShoppingCartItem>
        List<ShoppingCartItem> items = cartItemMapper.selectByUserId(userId);

        // 直接返回，PageHelper 会自动统计 total 并填充
        PageInfo<ShoppingCartItem> pageInfo = new PageInfo<>(items);

        // 此时 new PageResult<>(total, records) 会自动匹配泛型
        return new PageResult<>(pageInfo.getTotal(), pageInfo.getList());
    }
    @Override
    public List<ShoppingCart>showShoppingCart()
    {
        Long userId=BaseContext.getCurrentId();
        String key=Constant.SHOPPING_CART+userId;
        try{
            List<ShoppingCart> list=redisTemplate.opsForHash().values(key);
            if(list!=null&&!list.isEmpty())
            {
                return list;
            }
        }
        catch (Exception e)
            {
            log.error("购物车数据查询失败",e);
        }
       ShoppingCart shoppingCart=ShoppingCart.builder().userId(userId).build();
        return shoppingCartMapper.list(shoppingCart);
    }
}

