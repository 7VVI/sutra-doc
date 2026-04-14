package com.hmoob.doc.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.mybatis.core.domain.BaseEntity;
import com.hmoob.doc.domain.KbDocVisitRecord;

import java.io.Serial;

/**
 * KB文档访问记录业务对象 kb_doc_visit_record
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KbDocVisitRecord.class, reverseConvertGenerate = false)
public class KbDocVisitRecordBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 访问记录ID
     */
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
    @NotNull(message = "访问类型不能为空")
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

}