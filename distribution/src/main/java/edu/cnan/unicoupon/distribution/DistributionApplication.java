package edu.cnan.unicoupon.distribution;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("edu.cnan.unicoupon.distribution.dao")
public class DistributionApplication {
    public static void main(String[] args) {
        SpringApplication.run(DistributionApplication.class, args);
    }
}
