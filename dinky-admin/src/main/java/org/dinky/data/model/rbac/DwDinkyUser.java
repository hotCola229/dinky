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

package org.dinky.data.model.rbac;

import org.dinky.mybatis.model.DateBaseEntity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * DwDinkyTenantUser
 * 中台 dinky用户映射
 * @author zhangguohui
 * @date 2025-09-17 17:06:50
 */
@Data
@ApiModel(value = "DwDinkyUser", description = "DwDinkyUser Information")
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@TableName("dw_dinky_user")
public class DwDinkyUser extends DateBaseEntity<DwDinkyUser> implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** DW域-租户ID */
    private Long dwUserId;
    /** DW域-租户名称 */
    private String dwUserName;
    /** DS域-租户ID */
    private Integer dinkyUserId;
    /** DS域-租户CODE */
    private String dinkyUserName;
    /** creator */
    @TableField(fill = FieldFill.INSERT)
    @ApiModelProperty(value = "Creator", required = true, dataType = "Integer", example = "Creator")
    private Integer creator;
    /** updater */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @ApiModelProperty(value = "Updater", required = true, dataType = "Integer", example = "updater")
    private Integer updater;
}
