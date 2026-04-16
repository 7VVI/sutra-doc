package com.hmoob.doc.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 访问统计Vo对象
 *
 * @author hmoob
 */
@Data
public class VisitStatVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 访问类型
     */
    private Integer visitType;

    /**
     * 访问次数
     */
    private Long visitCount;

}