package com.hmoob.doc.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmoob.common.mybatis.core.mapper.BaseMapperPlus;
import com.hmoob.doc.domain.KbDocTopicType;
import com.hmoob.doc.domain.vo.KbDocTopicTypeVo;

import java.util.List;

/**
 * KB文档主题关联 Mapper接口
 *
 * @author hmoob
 */
public interface KbDocTopicTypeMapper extends BaseMapperPlus<KbDocTopicType, KbDocTopicTypeVo> {

    /**
     * 根据文档ID查询主题关联列表
     *
     * @param docId 文档ID
     * @return 主题关联列表
     */
    default List<KbDocTopicTypeVo> selectByDocId(Long docId) {
        return this.selectVoList(new LambdaQueryWrapper<KbDocTopicType>()
            .eq(KbDocTopicType::getDocId, docId)
            .orderByAsc(KbDocTopicType::getSortOrder));
    }

    /**
     * 根据主题ID查询文档关联列表
     *
     * @param topicId 主题ID
     * @return 文档关联列表
     */
    default List<KbDocTopicTypeVo> selectByTopicId(Long topicId) {
        return this.selectVoList(new LambdaQueryWrapper<KbDocTopicType>()
            .eq(KbDocTopicType::getTopicId, topicId));
    }

}