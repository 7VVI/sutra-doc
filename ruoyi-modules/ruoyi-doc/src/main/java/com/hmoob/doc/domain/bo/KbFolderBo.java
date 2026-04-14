package com.hmoob.doc.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.mybatis.core.domain.BaseEntity;
import com.hmoob.doc.domain.KbFolder;

import java.io.Serial;

/**
 * KB目录业务对象 kb_folder
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KbFolder.class, reverseConvertGenerate = false)
public class KbFolderBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 目录ID
     */
    private Long folderId;

    /**
     * 目录名称
     */
    @NotBlank(message = "目录名称不能为空")
    @Size(max = 100, message = "目录名称长度不能超过{max}个字符")
    private String folderName;

    /**
     * 父目录ID(0表示根目录)
     */
    private Long parentId;

    /**
     * 目录层级(1-一级,2-二级...)
     */
    private Integer folderLevel;

    /**
     * 目录完整路径(如:/研发部/技术文档)
     */
    private String folderPath;

    /**
     * 目录编码(业务标识)
     */
    @Size(max = 50, message = "目录编码长度不能超过{max}个字符")
    private String folderCode;

    /**
     * 排序号
     */
    @NotNull(message = "排序号不能为空")
    private Integer sortOrder;

    /**
     * 目录描述
     */
    private String description;

    /**
     * 目录图标
     */
    private String icon;

    /**
     * 文档数量(含子目录)
     */
    private Long docCount;

    /**
     * 状态: 1-启用, 0-禁用
     */
    private Integer status;

}