package com.hmoob.media.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 学习统计视图对象
 *
 * @author hmoob
 */
@Data
public class MediaLearningStatisticsVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 总数
     */
    private Long totalCount;

    /**
     * 已完成数
     */
    private Long completedCount;

    /**
     * 学习中数
     */
    private Long learningCount;

}
