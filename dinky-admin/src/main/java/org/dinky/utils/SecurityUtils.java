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

package org.dinky.utils;

import java.util.Base64;
import java.util.Enumeration;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.alibaba.fastjson.JSONObject;

import cn.chinatelecom.ddaf.security.context.DdafContextHolder;
import cn.chinatelecom.ddaf.systemmanage.client.vo.UserVo;
import cn.chinatelecom.ddaf.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * SecurityUtils
 *
 * @author zhangguohui
 * @date 2025-09-22 17:22:03
 */
@Slf4j
public class SecurityUtils {

    public static final String TENANT_ID = "tenant_id";
    public static final String HEADER_TENANT_ID = "ddaf-tenant-id";
    public static final String AUTHORIZATION = "Authorization";

    public static final String USER_INFO_HEADER = "X-Ddaf-Gateway-Api-Userinfo";
    public static final Long PLATFORM_MANGER_TENANT_ID = 0L;

    public static void initTenantId(HttpServletRequest request) {
        HttpServletRequest httpServletRequest = request;
        String encodedString = httpServletRequest.getHeader(USER_INFO_HEADER);
        UserVo userVo = null;
        Long tenantId = -1L;
        DdafContextHolder.setUserVo(null);
        DdafContextHolder.setUsername(null);
        try {
            String decode = new String(Base64.getDecoder().decode(encodedString));
            String jsonString = JSONObject.parseObject(decode).toJSONString();
            userVo = JSONUtils.parseObject(jsonString, UserVo.class);
        } catch (Exception e) {
            log.debug("Can't decode or extract UserVo from the provided header, please check the encoded string");
        }
        if (userVo != null) {
            extractAndSetUserVoByHeader(userVo);
            // 若可以被解析，则记录下当前的userInfo header到TTL
            DdafContextHolder.setUserInfoHeader(encodedString);
            //log.info("set user info header: {}", encodedString);
            tenantId = userVo.getTenantId();
        }
        // 若无法取到tenantId(没传userInfo 或 客户端模式没有tenantId)，则尝试从header读取
        if (tenantId == null || tenantId < 0L) {
            Object tenantIdObj = httpServletRequest.getHeader(HEADER_TENANT_ID);
            if (tenantIdObj != null) {
                String tenantIdStr = String.valueOf(tenantIdObj);
                tenantId = Long.valueOf(tenantIdStr);
                log.debug("initTenantId Header tenantId:{}", tenantId);
            }
        }

        // 记录当前accessToken
        String authorizationFromHeader = cn.chinatelecom.ddaf.utils.SecurityUtils.getAuthorizationFromHeader();
        if (StringUtils.hasText(authorizationFromHeader)) {
            DdafContextHolder.setAuthorization(authorizationFromHeader);
            log.debug("initTenantId setAuthorization:{}", authorizationFromHeader);
        }
        if (tenantId == null) {
            tenantId = -1L;
        }
        //log.info("initTenantId set tenantId:{}", tenantId);
        DdafContextHolder.setTenantId(tenantId);
    }

    private static void extractAndSetUserVoByHeader(UserVo userVo) {
        // decode to json and put to Context Holder
        DdafContextHolder.setUsername(userVo.getUsername());
        DdafContextHolder.setTenantId(userVo.getTenantId());
        DdafContextHolder.setUserVo(JSONUtils.toJsonString(userVo));
        //log.info("initiated the userVo: {}", userVo);
        //log.info("initTenantId Header tenantId: {}", userVo.getTenantId());
    }

    public static String getAuthorizationFromHeader() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (Objects.isNull(requestAttributes) || !(requestAttributes instanceof ServletRequestAttributes)) {
            return "";
        }
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        Enumeration<String> headerNames = request.getHeaderNames();

        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                Enumeration<String> values = request.getHeaders(name);
                while (values.hasMoreElements()) {
                    String value = values.nextElement();
                    if (AUTHORIZATION.equalsIgnoreCase(name)) {
                        return value;
                    }
                }
            }
        }
        return "";
    }

    public static UserVo getUserVo() {
        String userVoJson = DdafContextHolder.getStr(DdafContextHolder.USER_INFO);
        if (!StringUtils.hasText(userVoJson)) {
            return null;
        }
        UserVo userVo = null;
        try {
            userVo = JSONUtils.parseObject(userVoJson, UserVo.class);
        } catch (Exception e) {
            log.error("getUserVo error:", e);
            return null;
        }
        return userVo;
    }

    public static Long getTenantId() {
        return DdafContextHolder.getTenantId();
    }

    public static void setTenantId(Long tenantId) {
        DdafContextHolder.setTenantId(tenantId);
    }

    public static Long getUserId() {
        UserVo userVo = SecurityUtils.getUserVo();
        if (userVo != null) {
            return userVo.getId();
        }
        return null;
    }

    public static void remove() {
        DdafContextHolder.remove();
    }
}
