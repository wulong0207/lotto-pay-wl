package com.hhly.pay;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.hhly.PayApplication;


/**
 * @author Bruce
 *
 * @date 2016年5月27日
 *
 * @desc 
 */
/*@ContextConfiguration(locations={"classpath:applicationContext.xml"}) 
@RunWith(SpringJUnit4ClassRunner.class)
public class BaseTest extends AbstractJUnit4SpringContextTests {
	@Test
	public void test(){
		
	}
}*/

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PayApplication.class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BaseTest {
	// @Autowired
	// private TestRestTemplate testRestTemplate;

	@Test
	public void test() {

	}
}