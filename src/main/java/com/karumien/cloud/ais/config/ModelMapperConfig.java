/*
 * Copyright (c) 2019-2029 Karumien s.r.o.
 *
 * Karumien s.r.o. is not responsible for defects arising from 
 * unauthorized changes to the source code.
 */
package com.karumien.cloud.ais.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.karumien.cloud.ais.api.entity.ViewPass;
import com.karumien.cloud.ais.api.entity.ViewUserInfo;
import com.karumien.cloud.ais.api.model.PassDTO;
import com.karumien.cloud.ais.api.model.UserInfoDTO;

/**
 * Model Mapping Configuraton.
 *
 * @author <a href="miroslav.svoboda@karumien.com">Miroslav Svoboda</a>
 * @since 1.0, 15. 4. 2019 19:16:12 
 */
@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.createTypeMap(ViewPass.class, PassDTO.class);
        modelMapper.createTypeMap(ViewUserInfo.class, UserInfoDTO.class);
        return modelMapper;        
    }
    
}
