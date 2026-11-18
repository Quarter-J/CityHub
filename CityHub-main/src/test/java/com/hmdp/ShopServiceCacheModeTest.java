package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopServiceCacheModeTest {

    @Test
    void shopDetailUsesLogicalExpirationCache() {
        CacheClient cacheClient = mock(CacheClient.class);
        ShopServiceImpl shopService = new ShopServiceImpl();
        ReflectionTestUtils.setField(shopService, "clientClient", cacheClient);
        when(cacheClient.queryTwoLevelWithLogicalExpire(
                eq(RedisConstants.CACHE_SHOP_KEY),
                eq(1L),
                eq(Shop.class),
                any(Function.class),
                eq(RedisConstants.CACHE_SHOP_TTL),
                eq(TimeUnit.MINUTES)
        )).thenReturn(null);

        assertFalse(shopService.queryById(1L).getSuccess());
        verify(cacheClient).queryTwoLevelWithLogicalExpire(
                eq(RedisConstants.CACHE_SHOP_KEY),
                eq(1L),
                eq(Shop.class),
                any(Function.class),
                eq(RedisConstants.CACHE_SHOP_TTL),
                eq(TimeUnit.MINUTES)
        );
    }
}
