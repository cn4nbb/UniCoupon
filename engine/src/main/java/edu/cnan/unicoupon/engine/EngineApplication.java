package edu.cnan.unicoupon.engine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("edu.cnan.unicoupon.engine.dao.mapper")
public class EngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(EngineApplication.class,args);
    }
}
