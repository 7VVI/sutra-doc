package com.hmoob.doc.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * KB搜索记录实体类
 * 注意: 搜索记录表不需要完整的审计字段,仅保留必要的时间字段
 *
 * @author hmoob
 */
@Data
@TableName("kb_search_record")
public class KbSearchRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 搜索记录ID
     */
    @TableId(value = "search_id")
    private Long searchId;

    /**
     * 搜索关键词
     */
    private String keywords;

    /**
     * 最大匹配关键词
     */
    private String keywordsMax;

    /**
     * 搜索结果标题
     */
    private String searchTitle;

    /**
     * 搜索内容片段
     */
    private String searchContent;

    /**
     * 主题编码(逗号分隔)
     */
    private String topicCodes;

    /**
     * 业务类型(逗号分隔)
     */
    private String businessTypes;

    /**
     * 目录ID列表(逗号分隔)
     */
    private String folderIds;

    /**
     * 来源IP地址
     */
    private String sourceIp;

    /**
     * 搜索结果数量
     */
    private Integer resultCount;

    /**
     * 搜索耗时,单位:毫秒
     */
    private Integer searchDurationMs;

    /**
     * 搜索类型: 1-关键词搜索, 2-高级搜索, 3-全文检索
     */
    private Integer searchType;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 搜索时间
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
