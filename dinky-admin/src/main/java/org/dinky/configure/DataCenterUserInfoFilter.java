package org.dinky.configure;


import cn.chinatelecom.ddaf.systemmanage.client.vo.UserVo;
import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dinky.context.TenantContextHolder;
import org.dinky.context.UserInfoContextHolder;
import org.dinky.data.dto.UserDTO;
import org.dinky.data.enums.UserType;
import org.dinky.data.model.rbac.*;
import org.dinky.service.*;
import org.dinky.utils.SecurityUtils;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Objects;

/**
 * DdafUserInfoFilter
 *
 * @author zhangguohui
 * @date 2025-09-22 17:21:09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataCenterUserInfoFilter implements Filter {

    private final UserService userService;

    private final TenantService tenantService;

    private final UserTenantService userTenantService;

    private final DwDinkyTenantService dwDinkyTenantService;

    private final DwDinkyUserService dwDinkyUserService;



    private static final String TENANT_CODE_PREFIX = "dw_%s";

    private static final String USERNAME_PREFIX = "dw_user_%s";

    private static final String DEFAULT_PASSWORD = "123456";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest servletRequest = (HttpServletRequest) request;

        SecurityUtils.initTenantId(servletRequest);

        try {
            // todo 检查数据中台token
            UserVo userVo = SecurityUtils.getUserVo();
            if(Objects.nonNull(userVo)){
                log.info("进行数据中台用户认证");
                Integer dinkyUserId = getDinkyUserId(userVo);
                StpUtil.login(dinkyUserId);
                UserDTO reBuildUserInfo = userService.buildUserInfo(dinkyUserId);
                UserInfoContextHolder.set(dinkyUserId, reBuildUserInfo);
            }
            chain.doFilter(request,response);

        } catch (Exception e) {
            log.error(">>> [MidTokenFilter] 发生异常", e);
        } finally {
            SecurityUtils.remove();
        }

    }

    /**
     * 获取数据中台用户映射的dinky用户id
     */
    private Integer getDinkyUserId(UserVo userVo){
        //查询dw用户是否存在映射
        DwDinkyUser dbDwDinkyUser = dwDinkyUserService.lambdaQuery()
                .eq(DwDinkyUser::getDwUserId, userVo.getId())
                .one();
        if(Objects.nonNull(dbDwDinkyUser)){
            return dbDwDinkyUser.getDinkyUserId();
        }
        //检查租户是否存在
        DwDinkyTenant dbDwDinkyTenant = dwDinkyTenantService.lambdaQuery()
                .eq(DwDinkyTenant::getDwTenantId, userVo.getTenantId())
                .one();
        if(Objects.isNull(dbDwDinkyTenant)){
            log.info("数据中台租户{}对应dinky租户不存在，进行创建", userVo.getTenantName());
            //创建dinky租户
            String dwDinkyTenantCode = String.format(TENANT_CODE_PREFIX, userVo.getTenantId());
            Tenant tenant = new Tenant();
            tenant.setTenantCode(dwDinkyTenantCode);
            tenantService.saveOrUpdateTenant(tenant);
            //todo 注意这里是否返回租户id
            //创建dw与dinky租户映射
            DwDinkyTenant dwDinkyTenant = new DwDinkyTenant();
            dwDinkyTenant.setDwTenantId(userVo.getTenantId());
            dwDinkyTenant.setDwTenantName(userVo.getTenantName());
            dwDinkyTenant.setDinkyTenantId((Integer) TenantContextHolder.get());
            dwDinkyTenant.setDinkyTenantCode(tenant.getTenantCode());
            dwDinkyTenantService.save(dwDinkyTenant);
            dbDwDinkyTenant = dwDinkyTenant;
        }
        log.info("数据中台用户{}对应dinky用户不存在，进行创建", userVo.getUsername());
        //创建dinky用户
        User user = new User();
        user.setUsername(String.format(USERNAME_PREFIX, userVo.getId()));
        user.setUserType(UserType.LOCAL.getCode());
        user.setPassword(SaSecureUtil.md5(DEFAULT_PASSWORD));
        user.setEnabled(true);
        user.setIsDelete(false);
        user.setSuperAdminFlag(false);
        //todo 根据中台是否为租户管理员
        user.setTenantAdminFlag(false);
        userService.registerUser(user);
        //创建dinky用户与租户关联
        UserTenant userTenant = new UserTenant();
        userTenant.setUserId(user.getId());
        userTenant.setTenantId(dbDwDinkyTenant.getDinkyTenantId());
        userTenantService.save(userTenant);
        //todo 注意此处是否返回用户id
        DwDinkyUser dwDinkyUser = new DwDinkyUser();
        dwDinkyUser.setDwUserId(userVo.getId());
        dwDinkyUser.setDwUserName(userVo.getUsername());
        dwDinkyUser.setDinkyUserId(user.getId());
        dwDinkyUser.setDinkyUserName(user.getUsername());
        dwDinkyUserService.save(dwDinkyUser);
        return dwDinkyUser.getDinkyUserId();
    }
}
