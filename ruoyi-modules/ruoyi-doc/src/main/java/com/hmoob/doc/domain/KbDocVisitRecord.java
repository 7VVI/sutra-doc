package com.hmoob.doc.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * KB文档访问记录实体类
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_doc_visit_record")
public class KbDocVisitRecord extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 访问记录ID
     */
    @TableId(value = "visit_id")
    private Long visitId;

    /**
     * 文档ID(搜索记录可为空)
     */
    private Long docId;

    /**
     * 目录ID(浏览目录时记录)
     */
    private Long folderId;

    /**
     * 访问类型: 1-上传, 2-预览, 3-下载, 4-删除, 5-编辑, 6-分享
     */
    private Integer visitType;

    /**
     * 搜索关键词(搜索时记录)
     */
    private String keywords;

    /**
     * 最大匹配关键词
     */
    private String keywordsMax;

    /**
     * 来源IP地址
     */
    private String sourceIp;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 设备类型: pc, mobile, tablet
     */
    private String deviceType;

    /**
     * 浏览器类型
     */
    private String browser;

    /**
     * 来源页面
     */
    private String referer;

    /**
     * 停留时长,单位:秒
     */
    private Integer durationSeconds;

    /**
     * 删除标志: 0-正常, 1-已删除
     */
    @TableLogic
    private String delFlag;

    /**
     * 更新者
     */
    @TableField(fill = FieldFill.INSERT_UPDATE,exist = false)
    private Long updateBy;

    /**
     * 创建者
     */
    @TableField(fill = FieldFill.INSERT,exist = false)
    private Long createBy;

}
