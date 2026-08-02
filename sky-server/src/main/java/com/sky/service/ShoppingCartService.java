package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.entity.ShoppingCartItem;
import com.sky.result.PageResult;
import com.sky.result.Result;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ShoppingCartService {
   void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

   List<ShoppingCart> showShoppingCart();

   void clean();
   PageResult<ShoppingCartItem> getShoppingCart(Long userId, int pageNum, int pageSize);

}
