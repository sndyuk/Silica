package com.silica.job;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.silica.Silica;

public class JobExecutorTest {

	private static final Logger log = LoggerFactory
			.getLogger(JobExecutorTest.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		System.setProperty("SILICA_CONF",
				"C:/Space/WorkSpace/dev/silica/trunk/example.properties");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		
		try {
			Silica.main(new String[]{"exit"});
			Thread.sleep(3600);
		} catch (Exception e) {
			log.error(e.toString(), e);
			throw e;
		}
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCall() throws Exception {

		for (int i = 0; i < 3; i++) {
			call();
		}
	}
	
	private void call() throws Exception {

		JobExecutor<String> executor = new JobExecutor<String>(new TestJob());

		ExecutorService service = Executors.newSingleThreadExecutor();
		Future<String> future = service.submit(executor);

		try {

			String result = future.get();
			assertEquals("success", result);

		} catch (Exception e) {
			log.error(e.toString(), e);
			throw e;
		}
	}
}
