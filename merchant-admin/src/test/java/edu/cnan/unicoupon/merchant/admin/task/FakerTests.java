package edu.cnan.unicoupon.merchant.admin.task;

import com.github.javafaker.Address;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.Test;

import java.util.Locale;

public class FakerTests {

    @Test
    public void fakerTest() {
        // 指定随机数据所属位置
        Faker faker = new Faker(Locale.CHINA);

        // 生成中文名
        String chineseName = faker.name().fullName();
        System.out.println(chineseName);

        // 生成手机号
        String mobileNumber = faker.phoneNumber().cellPhone();
        System.out.println(mobileNumber);

        // 生成邮箱
        String emailAddress = faker.internet().emailAddress();
        System.out.println(emailAddress);

        // 生成省市区县地址
        Address address = faker.address();
        System.out.println(address.country()+address.state()+address.city()+address.streetAddress());
    }
}
