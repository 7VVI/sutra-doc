package com.hmoob.doc.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * KB文档收藏实体类
 *
 * @author hmoob
 */
@Data
@TableName("kb_doc_favourite")
public class KbDocFavourite implements Serializable {

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
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 删除标志: 0-正常, 1-已删除
     */
    @TableLogic
    private String delFlag;

}