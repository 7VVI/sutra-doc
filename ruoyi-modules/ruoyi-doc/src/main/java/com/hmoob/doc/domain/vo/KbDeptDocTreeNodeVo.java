package com.hmoob.doc.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 部门文档目录树节点视图对象
 * 统一表示目录和文档两种节点，支持懒加载
 *
 * @author hmoob
 */
@Data
public class KbDeptDocTreeNodeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 节点类型: folder-目录, doc-文档
     */
    private String type;

    /**
     * 节点ID（目录为folderId，文档为docId）
     */
    private Long id;

    /**
     * 节点名称（目录名称或文档名称）
     */
    private String name;

    /**
     * 父节点ID（根目录下的节点 parentId=0）
     */
    private Long parentId;

    /**
     * 是否有子节点（仅目录节点有效）
     */
    private Boolean hasChildren;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 文件类型（仅文档节点）
     */
    private String fileType;

    /**
     * 文件大小（仅文档节点）
     */
    private Long fileSize;

    /**
     * 文档状态（仅文档节点）
     */
    private Integer status;

    /**
     * 发布标志（仅文档节点）
     */
    private Integer releaseFlag;

    /**
     * 创建时间
     */
    private Date createTime;

}
