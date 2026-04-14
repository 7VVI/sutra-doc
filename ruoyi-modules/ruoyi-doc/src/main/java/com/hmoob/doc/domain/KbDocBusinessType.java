package com.hmoob.doc.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * KB文档业务类型关联实体类
 *
 * @author hmoob
 */
@Data
@TableName("kb_doc_business_type")
public class KbDocBusinessType implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关联ID
     */
    @TableId(value = "business_type_id")
    private Long businessTypeId;

    /**
     * 文档ID
     */
    private Long docId;

    /**
     * 业务类型编码
     */
    private String businessType;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 创建人ID
     */
    private Long createBy;

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