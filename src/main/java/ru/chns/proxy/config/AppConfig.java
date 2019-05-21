package ru.chns.proxy.config;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * Spring configuration bean
 * setting configuration file
 */
@Configuration
public class AppConfig {
    /**
     * Configuration file name
     */
    private static final String CONFIGURATION_FILE = "config.properties";

    @Bean
    public static PropertyPlaceholderConfigurer getPropertyPlaceholderConfigurer()
    {
        PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
        ppc.setLocation(new ClassPathResource(CONFIGURATION_FILE));
        ppc.setIgnoreUnresolvablePlaceholders(true);
        return ppc;
    }
}
