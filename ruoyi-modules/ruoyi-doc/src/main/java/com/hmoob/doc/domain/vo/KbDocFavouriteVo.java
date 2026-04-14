package com.hmoob.doc.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.hmoob.doc.domain.KbDocFavourite;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * KB文档收藏视图对象 kb_doc_favourite
 *
 * @author hmoob
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KbDocFavourite.class)
public class KbDocFavouriteVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 收藏ID
     */
    @ExcelProperty(value = "收藏ID")
    private Long favouriteId;

    /**
     * 文档ID
     */
    @ExcelProperty(value = "文档ID")
    private Long docId;

    /**
     * 用户ID
     */
    @ExcelProperty(value = "用户ID")
    private Long userId;

    /**
     * 收藏文件夹名称
     */
    @ExcelProperty(value = "收藏文件夹名称")
    private String folderName;

    /**
     * 收藏备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

}