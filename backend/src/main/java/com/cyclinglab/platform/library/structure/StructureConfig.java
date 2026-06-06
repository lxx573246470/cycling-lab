package com.cyclinglab.platform.library.structure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StructureConfig {

    @Bean
    public StructureValidator structureValidator(ObjectMapper objectMapper) {
        return new StructureValidator(objectMapper);
    }
}
