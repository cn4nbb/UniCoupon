package edu.cnan.unicoupon.merchant.admin.common.log;

import cn.hutool.core.util.StrUtil;
import com.mzt.logapi.service.IParseFunction;
import edu.cnan.unicoupon.merchant.admin.common.enums.DiscountTargetEnum;
import edu.cnan.unicoupon.merchant.admin.common.enums.DiscountTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 操作日志组件解析枚举值对应描述信息
 */
@Component
public class CommonEnumParseFunction implements IParseFunction {

    private final String DISCOUNT_TARGET_ENUM = DiscountTargetEnum.class.getSimpleName();
    private final String DISCOUNT_TYPE_ENUM = DiscountTypeEnum.class.getSimpleName();

    @Override
    public String functionName() {return "COMMON_ENUM_PARSE";}

    /**
     * 自定义枚举类细节函数
     * @param value 函数参数
     * @return 解析的值
     */
    @Override
    public String apply(Object value) {
        try {
            List<String> values = StrUtil.split(value.toString(), '_');

            if (values.size() != 2) {
                throw new IllegalArgumentException("格式错误，需要 '枚举类_具体值' 的形式。");
            }
            // 获取枚举类型名称
            String enumClassName = values.get(0);
            // 获取枚举值
            Integer enumValue = Integer.parseInt(values.get(1));
            return findEnumValueByName(enumClassName,enumValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("第二个下划线后面的值需要是整数。", e);
        }
    }

    private String findEnumValueByName(String enumClassName,Integer enumValue){
        if (StrUtil.equals(enumClassName,DISCOUNT_TARGET_ENUM)) {
            return DiscountTargetEnum.getValueByType(enumValue);
        }else if (StrUtil.equals(enumClassName,DISCOUNT_TYPE_ENUM)) {
            return DiscountTypeEnum.getValueByType(enumValue);
        }else {
            throw new IllegalArgumentException("未知的枚举类名："+enumClassName);
        }
    }
}
