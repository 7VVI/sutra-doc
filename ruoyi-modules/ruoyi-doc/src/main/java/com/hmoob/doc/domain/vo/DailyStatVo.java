package com.hmoob.doc.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 日统计Vo对象
 *
 * @author hmoob
 */
@Data
public class DailyStatVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 统计日期
     */
    private LocalDate statDate;

    /**
     * 访问次数
     */
    private Long visitCount;

}