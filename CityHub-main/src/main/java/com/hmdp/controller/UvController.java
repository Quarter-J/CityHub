package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.service.UvStatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/uv")
public class UvController {

    @Resource
    private UvStatisticsService uvStatisticsService;

    @GetMapping("/{scene}/today")
    public Result today(@PathVariable("scene") String scene) {
        return Result.ok(uvStatisticsService.todayCount(scene));
    }
}
