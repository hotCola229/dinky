/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.dinky.configure;

import com.alibaba.fastjson.JSON;
import org.dinky.context.TenantContextHolder;
import org.dinky.context.UserInfoContextHolder;
import org.dinky.data.dto.AssignRoleDTO;
import org.dinky.data.dto.UserDTO;
import org.dinky.data.enums.UserType;
import org.dinky.data.model.rbac.DwDinkyTenant;
import org.dinky.data.model.rbac.DwDinkyUser;
import org.dinky.data.model.rbac.Tenant;
import org.dinky.data.model.rbac.User;
import org.dinky.data.model.rbac.UserTenant;
import org.dinky.service.DwDinkyTenantService;
import org.dinky.service.DwDinkyUserService;
import org.dinky.service.TenantService;
import org.dinky.service.UserService;
import org.dinky.service.UserTenantService;
import org.dinky.utils.SecurityUtils;

import java.util.Collections;
import java.util.Objects;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import cn.chinatelecom.ddaf.systemmanage.client.vo.UserVo;
import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

    private static final Integer DEFAULT_ROLE_id = 1;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        HttpServletRequest servletRequest = (HttpServletRequest) request;

        SecurityUtils.initTenantId(servletRequest);

        try {
            // 检查数据中台token
            UserVo userVo = SecurityUtils.getUserVo();
            if (Objects.nonNull(userVo)) {
                log.info("进行数据中台用户认证");
                if(StpUtil.isLogin()){
                    log.info("dinky 已登录:{}", StpUtil.getLoginIdAsInt());
                }else {
                    Integer dinkyUserId = getDinkyUserId(userVo);
                    StpUtil.login(dinkyUserId);
                    UserDTO reBuildUserInfo = userService.buildUserInfo(dinkyUserId);
                    // 设置当前租户信息
                    Tenant currentTenant = tenantService.getById(1);
                    reBuildUserInfo.setCurrentTenant(currentTenant);
                    log.info("数据中台用户{}认证成功，映射dinky用户{}", userVo.getUsername(), JSON.toJSONString(reBuildUserInfo));
                    UserInfoContextHolder.set(dinkyUserId, reBuildUserInfo);
                }
            }
            chain.doFilter(request, response);

        } catch (Exception e) {
            log.error("中台token认证发生异常", e);
        } finally {
            SecurityUtils.remove();
        }
    }

    /**
     * 获取数据中台用户映射的dinky用户id
     */
    private Integer getDinkyUserId(UserVo userVo) {
        // 查询dw用户是否存在映射
        DwDinkyUser dbDwDinkyUser = dwDinkyUserService
                .lambdaQuery()
                .eq(DwDinkyUser::getDwUserId, userVo.getId())
                .one();
        if (Objects.nonNull(dbDwDinkyUser)) {
            return dbDwDinkyUser.getDinkyUserId();
        }
        // 检查租户是否存在
        DwDinkyTenant dbDwDinkyTenant = dwDinkyTenantService
                .lambdaQuery()
                .eq(DwDinkyTenant::getDwTenantId, userVo.getTenantId())
                .one();
        if (Objects.isNull(dbDwDinkyTenant)) {
            log.info("数据中台租户{}对应dinky租户不存在，进行创建", userVo.getTenantName());
            // 创建dinky租户
            String dwDinkyTenantCode = String.format(TENANT_CODE_PREFIX, userVo.getTenantId());
            /*
            Tenant tenant = new Tenant();
            tenant.setTenantCode(dwDinkyTenantCode);
            tenantService.saveOrUpdateTenant(tenant);*/
            // todo 注意这里是否返回租户id
            // 创建dw与dinky租户映射
            DwDinkyTenant dwDinkyTenant = new DwDinkyTenant();
            dwDinkyTenant.setDwTenantId(userVo.getTenantId());
            dwDinkyTenant.setDwTenantName(userVo.getTenantName());
//            dwDinkyTenant.setDinkyTenantId((Integer) TenantContextHolder.get());
            dwDinkyTenant.setDinkyTenantId(1);
            dwDinkyTenant.setDinkyTenantCode(dwDinkyTenantCode);
            dwDinkyTenantService.save(dwDinkyTenant);
            dbDwDinkyTenant = dwDinkyTenant;
        }
        log.info("数据中台用户{}对应dinky用户不存在，进行创建", userVo.getUsername());
        // 创建dinky用户
        User user = new User();
        user.setUsername(String.format(USERNAME_PREFIX, userVo.getId()));
        user.setUserType(UserType.LOCAL.getCode());
        user.setPassword(SaSecureUtil.md5(DEFAULT_PASSWORD));
        user.setEnabled(true);
        user.setIsDelete(false);
        user.setSuperAdminFlag(true);
        // todo 根据中台是否为租户管理员
        user.setTenantAdminFlag(false);
        userService.registerUser(user);
        // todo 分配默认角色 目前默认为普通用户
        AssignRoleDTO assignRoleDTO = new AssignRoleDTO();
        assignRoleDTO.setUserId(user.getId());
        assignRoleDTO.setRoleIds(Collections.singletonList(DEFAULT_ROLE_id));
        userService.assignRole(assignRoleDTO);
        // 创建dinky用户与租户关联
        UserTenant userTenant = new UserTenant();
        userTenant.setUserId(user.getId());
        userTenant.setTenantId(dbDwDinkyTenant.getDinkyTenantId());
        userTenantService.save(userTenant);
        // todo 注意此处是否返回用户id
        DwDinkyUser dwDinkyUser = new DwDinkyUser();
        dwDinkyUser.setDwUserId(userVo.getId());
        dwDinkyUser.setDwUserName(userVo.getUsername());
        dwDinkyUser.setDinkyUserId(user.getId());
        dwDinkyUser.setDinkyUserName(user.getUsername());
        dwDinkyUserService.save(dwDinkyUser);
        return dwDinkyUser.getDinkyUserId();
    }
}
