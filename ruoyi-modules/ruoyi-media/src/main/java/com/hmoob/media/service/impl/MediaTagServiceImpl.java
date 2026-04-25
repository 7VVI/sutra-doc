package com.hmoob.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmoob.common.core.utils.MapstructUtils;
import com.hmoob.media.domain.MediaTag;
import com.hmoob.media.domain.bo.MediaTagBo;
import com.hmoob.media.domain.vo.MediaTagVo;
import com.hmoob.media.mapper.MediaTagMapper;
import com.hmoob.media.service.IMediaTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分类标签服务实现
 */
@RequiredArgsConstructor
@Service
public class MediaTagServiceImpl implements IMediaTagService {

    private final MediaTagMapper baseMapper;

    @Override
    public List<MediaTagVo> selectTagList(Integer tagType) {
        LambdaQueryWrapper<MediaTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(tagType != null, MediaTag::getTagType, tagType)
            .eq(MediaTag::getStatus, 1)
            .orderByAsc(MediaTag::getSortOrder);
        return baseMapper.selectVoList(wrapper);
    }

    @Override
    public List<MediaTagVo> selectTagTree(Integer tagType) {
        List<MediaTagVo> allTags = selectTagList(tagType);
        return buildTree(allTags, 0L);
    }

    private List<MediaTagVo> buildTree(List<MediaTagVo> allTags, Long parentId) {
        return allTags.stream()
            .filter(t -> parentId.equals(t.getParentId()))
            .peek(t -> t.setChildren(buildTree(allTags, t.getTagId())))
            .collect(Collectors.toList());
    }

    @Override
    public int insertTag(MediaTagBo bo) {
        MediaTag tag = MapstructUtils.convert(bo, MediaTag.class);
        return baseMapper.insert(tag);
    }

    @Override
    public int updateTag(MediaTagBo bo) {
        MediaTag tag = MapstructUtils.convert(bo, MediaTag.class);
        return baseMapper.updateById(tag);
    }

    @Override
    public int deleteTag(Long tagId) {
        return baseMapper.deleteById(tagId);
    }
}
