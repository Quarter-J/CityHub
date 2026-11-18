package com.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.JwtTokenService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RefreshTokenInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtTokenService jwtTokenService;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate,
                                   JwtTokenService jwtTokenService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UserHolder.removeUser();
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            return true;
        }
        Long jwtUserId = jwtTokenService.parseUserId(token);
        if (jwtUserId == null) {
            return true;
        }
        String userKey = RedisConstants.LOGIN_USER_KEY + token;
        Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(userKey);
        if (map.isEmpty()) {
            return true;
        }
        UserDTO userDTO = BeanUtil.fillBeanWithMap(map, new UserDTO(), false);
        if (!jwtUserId.equals(userDTO.getId())) {
            return true;
        }
        UserHolder.saveUser(userDTO);
        stringRedisTemplate.expire(userKey, jwtTokenService.getTtlMinutes(), TimeUnit.MINUTES);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserHolder.removeUser();
    }
}
