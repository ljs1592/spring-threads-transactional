package com.jackson.lai.transactional.service;

import com.jackson.lai.transactional.dao.MyTestMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author: aore
 * @date: 2022/01/06 18:37
 **/
@Service
public class MySubThreadService2 {

	@Resource
	MyTestMapper myTestMapper;

	@Transactional
	public String doThing() {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		myTestMapper.saveDate(Thread.currentThread().getName() + " " + getClass().getName());
		System.err.println(Thread.currentThread().getName() + " " + getClass().getName());
		int i = 1/0; // 模拟异常
		return "MySubThreadService2";
	}

}
