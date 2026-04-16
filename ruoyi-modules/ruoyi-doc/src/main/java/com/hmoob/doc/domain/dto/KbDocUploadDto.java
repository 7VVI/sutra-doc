package com.hmoob.doc.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * KB文档上传参数DTO
 *
 * @author hmoob
 */
@Data
public class KbDocUploadDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 目录ID
     */
    private Long folderId;

    /**
     * 文档标题（默认为文件名）
     */
    private String docTitle;

    /**
     * 文档分类
     */
    private String category;

    /**
     * 关键词（逗号分隔）
     */
    private String keywords;

    /**
     * 部门ID
     */
    private Long depId;

    /**
     * 组织编码
     */
    private String orgCode;

    /**
     * 公开备注标志: 0-不公开, 1-公开
     */
    private Integer publicRemark;

    /**
     * 主题ID列表
     */
    private List<Long> topicIds;

    /**
     * 业务类型编码列表
     */
    private List<String> businessTypes;

    /**
     * 备注
     */
    private String remark;

}