package edu.cnan.onecoupon.merchant.admin.task;

import cn.hutool.core.io.FileUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.github.javafaker.Faker;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 百万 Excel 文件生成单元测试
 */
public class ExcelGenerateTests {

    private final Faker faker = new Faker(Locale.CHINA);
    private final int row = 1000000;
    private final String excelPath = Paths.get("").toAbsolutePath().getParent() + "/tmp";


    @Test
    public void generateExcelTest() {
        if (!FileUtil.exist(excelPath)) {
            FileUtil.mkdir(excelPath);
        }
        String fileName = excelPath + "/百万Excel数据.xlsx";
        EasyExcel.write(fileName, ExcelDemoData.class).sheet("用户信息表").doWrite(data());
    }

    public List<ExcelDemoData> data() {
        List<ExcelDemoData> list = new ArrayList<>();

        for (int i = 0; i < row; i++) {
            ExcelDemoData data = ExcelDemoData.builder()
                    .name(faker.name().fullName())
                    .phone(faker.phoneNumber().cellPhone())
                    .email(faker.internet().emailAddress()).build();
            list.add(data);
        }
        return list;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    static class ExcelDemoData {

        @ColumnWidth(30)
        @ExcelProperty("姓名")
        private String name;

        @ColumnWidth(30)
        @ExcelProperty("手机号")
        private String phone;

        @ColumnWidth(30)
        @ExcelProperty("邮箱")
        private String email;
    }
}
