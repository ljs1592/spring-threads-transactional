package com.jackson.lai.transactional.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author: aore
 * @date: 2022/01/06 18:30
 **/
@Service
public class MyThreadTransactionalUtil {

	@Autowired
	private PlatformTransactionManager transactionManager;

	private static final int THREAD_TIME_OUT = 20;

	/**
	 * 1. 主线程事务提交成功，子线程开始提交事务
	 * 2. 主线程事务回滚，子线程开始回滚事务
	 * 3. 子线程出现异常，主线程抛出异常，异常未被拦截的情况下，主线程回滚事务，然后子线程也开始回滚事务
	 * @param executor		线程池
	 * @param runnables		子线程
	 * @throws Exception 如果子线程有异常或需要回滚，会抛出异常（进而回滚主线程事务）
	 */
	public void asyncWithTransactional(Executor executor, Runnable... runnables) throws Exception {

		CountDownLatch mainThreadLatch = new CountDownLatch(1);
		CountDownLatch subThreadLatch = new CountDownLatch(runnables.length);
		AtomicBoolean rollbackFlag = new AtomicBoolean(false);

		AtomicReference<Exception> exception = new AtomicReference<>();

		for (Runnable runnable1 : runnables) {
			executor.execute(() -> {
				if (rollbackFlag.get()) return; //如果其他线程已经报错 就停止线程
				//设置一个事务
				DefaultTransactionDefinition def = new DefaultTransactionDefinition();
				def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW); // 事物隔离级别，开启新事务，这样会比较安全些。
				TransactionStatus status = transactionManager.getTransaction(def); // 获得事务状态
				try {
					// 执行任务
					runnable1.run();
					// 处理
					subThreadLatch.countDown();// 当前子线程业务执行完成
					mainThreadLatch.await();// 等待主线程，事务回滚或提交 todo 等待时间过长？
					if (rollbackFlag.get()) {
						System.out.println(Thread.currentThread().getName()+" rollback");
						transactionManager.rollback(status);
					} else {
						System.out.println(Thread.currentThread().getName()+" commit");
						transactionManager.commit(status);
					}
				} catch (Exception e) {
					//如果出错了，本线程进行回滚，记录异常信息
					rollbackFlag.set(true);
					subThreadLatch.countDown();
					transactionManager.rollback(status);
					System.out.println(Thread.currentThread().getName()+" rollback for exception");
					exception.set(e);
				}
				System.out.println(Thread.currentThread().getName()+" finish");

			});
		}

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
				@Override
				public void afterCompletion(int status) {
					// 1.主线程业务执行完毕 如果其他线程也执行完毕 正在阻塞状态中 唤醒其他线程 提交/回滚所有的事务
					System.out.println(Thread.currentThread().getName() + " Completion : " + status);
					// 主线程事务未提交（回滚），子线程也回滚
					if (TransactionSynchronization.STATUS_COMMITTED != status) {
						rollbackFlag.set(true);
					}
					mainThreadLatch.countDown();
				}
			});
			waitAllSubTread(subThreadLatch, rollbackFlag); //等待子线程执行业务完毕
		} else {
			subThreadLatch.await(); // 等待子线程执行业务完毕
			mainThreadLatch.countDown(); // 放行子线程进行事务 提交/回滚
		}
		// 若子线程需要回滚，抛出异常，让主线程也回滚事务
		if (rollbackFlag.get()) {
			throw new RuntimeException(exception.get());
		}
		System.out.println(Thread.currentThread() + "end");
	}

	private boolean waitAllSubTread(CountDownLatch subThreadLatch, AtomicBoolean rollbackFlag) throws Exception {
		boolean await = subThreadLatch.await(THREAD_TIME_OUT, TimeUnit.SECONDS);
		if (await) {
			return true;
		} else {
			// 超时 直接回滚
			rollbackFlag.set(true);
			return false;
		}
	}

}
