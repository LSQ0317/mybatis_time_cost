package com.mybatis.timecost.demo;

import com.mybatis.timecost.mybatis.TimeCostInterceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    ConfigurationCustomizer mybatisConfigurationCustomizer() {
        return configuration -> configuration.addInterceptor(new TimeCostInterceptor());
    }

    @Bean
    CommandLineRunner demoRunner(UserMapper userMapper) {
        return args -> {
            User user = userMapper.findById(1L);
            System.out.println("Loaded user: " + user);
            // Give the async SQL sender a moment to post the event before the demo exits.
            Thread.sleep(1000L);
        };
    }
}
