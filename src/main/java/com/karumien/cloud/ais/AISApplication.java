/*
 * Copyright (c) 2019-2029 Karumien s.r.o.
 *
 * Karumien s.r.o. is not responsible for defects arising from 
 * unauthorized changes to the source code.
 */
package com.karumien.cloud.ais;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

//import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * SpringBoot application AIS.
 *
 * @author <a href="miroslav.svoboda@karumien.com">Miroslav Svoboda</a>
 * @since 1.0, 15. 4. 2019 17:20:36
 */
@SpringBootApplication
//@EnableDiscoveryClient
@EnableSwagger2
@ComponentScan(basePackages = { "com.karumien"})
public class AISApplication {
	
	/**
	 * Run app with DB password -Dspring-boot.run.arguments=--spring.datasource.password=xxxx<br/>
	 * ln -s /tmp/ais-1.0.0-SNAPSHOT.jar /etc/init.d/aditus-reader
	 * 
	 * @param args arguments for start
	 */
    public static void main(String[] args) {
        SpringApplication.run(AISApplication.class, args);
    }

}
