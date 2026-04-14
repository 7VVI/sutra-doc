package com.hmoob.doc.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.mybatis.core.domain.BaseEntity;
import com.hmoob.doc.domain.KbDocFavourite;

import java.io.Serial;

/**
 * KB文档收藏业务对象 kb_doc_favourite
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KbDocFavourite.class, reverseConvertGenerate = false)
public class KbDocFavouriteBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 收藏ID
     */
    private Long favouriteId;

    /**
     * 文档ID
     */
    @NotNull(message = "文档ID不能为空")
    private Long docId;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 收藏文件夹名称
     */
    private String folderName;

    /**
     * 收藏备注
     */
    private String remark;

}