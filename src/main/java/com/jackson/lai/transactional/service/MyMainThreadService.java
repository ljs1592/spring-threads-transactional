package com.jackson.lai.transactional.service;

import com.jackson.lai.transactional.dao.MyTestMapper;
import com.jackson.lai.transactional.util.MyThreadTransactionalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author: aore
 * @date: 2022/01/06 18:35
 **/
@Service
public class MyMainThreadService {

	@Autowired
	MyThreadTransactionalUtil myThreadTransactionalUtil;
	@Autowired
	MySubThreadService1 myService1;
	@Autowired
	MySubThreadService2 myService2;
	@Resource
	MyTestMapper myTestMapper;


	@Transactional
	public void test() throws Exception{
		myTestMapper.saveDate(Thread.currentThread().getName() + " " + getClass().getName());

		ExecutorService executor = Executors.newFixedThreadPool(10);

		myThreadTransactionalUtil.asyncWithTransactional(executor, () -> myService1.doThing(), () -> myService2.doThing());

//		throw new RuntimeException(Thread.currentThread().getName()+" 强行错误");
	}

}
