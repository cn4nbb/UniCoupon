package edu.cnan.onecoupon.merchant.admin.service.basics.log;

import com.mzt.logapi.beans.Operator;
import com.mzt.logapi.service.IOperatorGetService;
import edu.cnan.onecoupon.merchant.admin.common.context.UserContext;
import org.springframework.stereotype.Service;

/**
 * 操作日志自动从用户上下文中获取操作人
 */
@Service
public class DefaultOperatorGetServiceImpl implements IOperatorGetService {
    @Override
    public Operator getUser() {
        Operator operator = new Operator();
        operator.setOperatorId(UserContext.getUserId());
        return operator;
    }
}
