package org.dinky.data.model.rbac;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dinky.mybatis.model.DateBaseEntity;

import java.io.Serializable;

/**
 * DwDinkyTenantUser
 * 中台 dinky 映射
 * @author zhangguohui
 * @date 2025-09-17 17:06:50
 */
@Data
@ApiModel(value = "DwDinkyTenant", description = "DwDinkyTenant Information")
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@TableName("dw_dinky_tenant")
public class DwDinkyTenant extends DateBaseEntity<DwDinkyTenant> implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** DW域-租户ID */
    private Long dwTenantId;
    /** DW域-租户名称 */
    private String dwTenantName;
    /** DS域-租户ID */
    private Integer dinkyTenantId;
    /** DS域-租户CODE */
    private String dinkyTenantCode;
    /** creator */
    @TableField(fill = FieldFill.INSERT)
    @ApiModelProperty(value = "Creator", required = true, dataType = "Integer", example = "Creator")
    private Integer creator;
    /** updater */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @ApiModelProperty(value = "Updater", required = true, dataType = "Integer", example = "updater")
    private Integer updater;

}
