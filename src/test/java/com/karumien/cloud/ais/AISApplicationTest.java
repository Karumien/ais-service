package com.karumien.cloud.ais;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.karumien.cloud.ais.api.model.UserInfoDTO;
import com.karumien.cloud.ais.api.model.WorkMonthDTO;
import com.karumien.cloud.ais.exception.NoDataFoundException;
import com.karumien.cloud.ais.service.AISService;

@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest
public class AISApplicationTest {

    @Autowired
    private AISService aisService;
    
    @Test
    public void getUserWorkMonth() {
        WorkMonthDTO workMonth = aisService.getWorkDays(2019, 5, "meduna");
        assertNotNull(workMonth);
        assertNotNull(workMonth.getWorkDays());
        assertTrue("Exists works", ! workMonth.getWorkDays().isEmpty());    
        assertEquals(31, workMonth.getWorkDays().size());
    }

    @Test
    public void getUsers() {
        List<UserInfoDTO> users = aisService.getWorkUsers("meduna");
        assertNotNull(users);
        assertTrue("Exists users", ! users.isEmpty());    
        assertTrue("Exists more users", users.size() > 1);    

        users = aisService.getWorkUsers("karkos");
        assertNotNull(users);
        assertTrue("Exists users", ! users.isEmpty());    
        assertTrue("Exists one", users.size() == 1);    
    }

    @Test(expected = NoDataFoundException.class)
    public void getUserNoExisted() {
        List<UserInfoDTO> users = aisService.getWorkUsers("nekdo");
        assertNull(users);
    }
    
}
