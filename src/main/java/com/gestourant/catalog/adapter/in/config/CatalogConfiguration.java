package com.gestourant.catalog.adapter.in.config;

import com.gestourant.catalog.application.CatalogService;
import com.gestourant.catalog.application.port.out.ProductCatalogPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition root for the catalogue module. */
@Configuration
public class CatalogConfiguration {
    @Bean
    CatalogService catalogService(ProductCatalogPort products) {
        return new CatalogService(products);
    }
}
