package com.jackson.lai.transactional.controller;

import com.jackson.lai.transactional.service.MyMainThreadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/myTest/v1.0")
public class MyController {

    @Autowired
    private MyMainThreadService myTest;


    @GetMapping
    public boolean myTest() throws Exception {
        myTest.test();
        return true;
    }
}
