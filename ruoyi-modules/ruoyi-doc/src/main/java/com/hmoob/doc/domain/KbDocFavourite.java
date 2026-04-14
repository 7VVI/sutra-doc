package com.hmoob.doc.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * KB文档收藏实体类
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_doc_favourite")
public class KbDocFavourite extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 收藏ID
     */
    @TableId(value = "favourite_id")
    private Long favouriteId;

    /**
     * 文档ID
     */
    private Long docId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 收藏文件夹名称
     */
    private String folderName;

    /**
     * 收藏备注
     */
    private String remark;

    /**
     * 删除标志: 0-正常, 1-已删除
     */
    @TableLogic
    private String delFlag;

}