package com.karumien.cloud.ais;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.karumien.cloud.ais.api.model.WorkMonthDTO;
import com.karumien.cloud.ais.service.AISService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AISApplicationTest {

	@Autowired
	private AISService aisService;
	
	@Test
	public void selectUserWorkMonth() {
		WorkMonthDTO workMonth = aisService.getWorkDays(2019, 5, "meduna");
		assertNotNull(workMonth);
		assertNotNull(workMonth.getWorkDays());
		assertTrue("Exists works", ! workMonth.getWorkDays().isEmpty());	
		assertEquals(31, workMonth.getWorkDays().size());
	}

}
