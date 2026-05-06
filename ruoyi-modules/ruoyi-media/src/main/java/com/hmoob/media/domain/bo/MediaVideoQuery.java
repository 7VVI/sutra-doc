package com.hmoob.media.domain.bo;

import com.hmoob.common.mybatis.core.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * 视频分页查询对象
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MediaVideoQuery extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    public MediaVideoQuery() {
        super(10, 1);
    }

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 作者名称（模糊搜索）
     */
    private String authorName;

    /**
     * 分类标签ID列表
     */
    private List<Long> categoryIds;

    /**
     * 方向标签ID列表
     */
    private List<Long> directionIds;

    /**
     * 排序字段
     */
    private String sort;

}
