/*
 * Copyright (c) 2019-2029 Karumien s.r.o.
 *
 * Karumien s.r.o. is not responsible for defects arising from 
 * unauthorized changes to the source code.
 */
package com.karumien.cloud.ais.config;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * Swagger Documentation Configuration.
 *
 * @author <a href="miroslav.svoboda@karumien.com">Miroslav Svoboda</a>
 * @since 1.0, 15. 4. 2019 18:59:56
 */
@Configuration
public class SwaggerDocumentationConfig {

	private ApiInfo apiInfo() {
		return new ApiInfoBuilder().title("Attended Information System").description("REST API for AIS ")
				.license("Apache License 2.0").licenseUrl("https://www.apache.org/licenses/LICENSE-2.0")
				.version("1.0")
				.contact(new Contact("Miroslav Svoboda", "http://www.karumien.com", "miroslav.svoboda@karumien.com"))
				.build();
	}
	
	@Bean
	public Docket api() {
		return new Docket(DocumentationType.SWAGGER_2).groupName("ais-api-1.0").select()
				.apis(RequestHandlerSelectors.basePackage("com.karumien.cloud.ais.api"))
		        .paths(PathSelectors.regex("/api/.*"))                          
				.build()
				.directModelSubstitute(LocalDate.class, java.sql.Date.class)
				.directModelSubstitute(OffsetDateTime.class, java.util.Date.class)
				.apiInfo(apiInfo());
	}

}
