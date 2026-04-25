package com.hmoob.media.service;

import com.hmoob.media.domain.bo.MediaTagBo;
import com.hmoob.media.domain.vo.MediaTagVo;
import java.util.List;

/**
 * 分类标签服务接口
 */
public interface IMediaTagService {
    /** 查询标签列表 */
    List<MediaTagVo> selectTagList(Integer tagType);
    /** 查询标签树 */
    List<MediaTagVo> selectTagTree(Integer tagType);
    /** 新增标签 */
    int insertTag(MediaTagBo bo);
    /** 修改标签 */
    int updateTag(MediaTagBo bo);
    /** 删除标签 */
    int deleteTag(Long tagId);
}
