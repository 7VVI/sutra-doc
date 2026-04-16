package com.hmoob.doc.es.document;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * KB搜索过滤参数
 * 用于结果中检索的二次筛选条件
 *
 * @author hmoob
 */
@Data
public class KbSearchFilterParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 过滤字段名
     */
    private String field;

    /**
     * 过滤操作类型
     * eq: 等于
     * ne: 不等于
     * gt: 大于
     * gte: 大于等于
     * lt: 小于
     * lte: 小于等于
     * like: 模糊匹配
     * in: 包含(多值)
     */
    private String operator;

    /**
     * 过滤值
     */
    private String value;

    /**
     * 多值列表(用于in操作)
     */
    private String[] values;
}