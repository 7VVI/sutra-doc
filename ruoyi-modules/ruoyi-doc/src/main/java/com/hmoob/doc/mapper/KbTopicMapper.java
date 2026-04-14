package com.hmoob.doc.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmoob.common.mybatis.core.mapper.BaseMapperPlus;
import com.hmoob.doc.domain.KbTopic;
import com.hmoob.doc.domain.vo.KbTopicVo;

import java.util.List;

/**
 * KB主题 Mapper接口
 *
 * @author hmoob
 */
public interface KbTopicMapper extends BaseMapperPlus<KbTopic, KbTopicVo> {

    /**
     * 查询主题树结构
     *
     * @return 主题树列表
     */
    default List<KbTopicVo> selectTopicTree() {
        return this.selectVoList(new LambdaQueryWrapper<KbTopic>()
            .eq(KbTopic::getStatus, 1)
            .orderByAsc(KbTopic::getSortOrder));
    }

    /**
     * 根据主题编码查询主题
     *
     * @param topicCode 主题编码
     * @return 主题Vo对象
     */
    default KbTopicVo selectByTopicCode(String topicCode) {
        return this.selectVoOne(new LambdaQueryWrapper<KbTopic>()
            .eq(KbTopic::getTopicCode, topicCode)
            .eq(KbTopic::getStatus, 1));
    }

    /**
     * 根据父主题ID查询子主题列表
     *
     * @param parentId 父主题ID
     * @return 子主题列表
     */
    default List<KbTopic> selectListByParentId(Long parentId) {
        return this.selectList(new LambdaQueryWrapper<KbTopic>()
            .eq(KbTopic::getParentId, parentId)
            .eq(KbTopic::getStatus, 1)
            .orderByAsc(KbTopic::getSortOrder));
    }

    /**
     * 根据主题ID查询主题编码
     *
     * @param topicId 主题ID
     * @return 主题编码
     */
    default String selectTopicCodeById(Long topicId) {
        KbTopic topic = this.selectById(topicId);
        return topic != null ? topic.getTopicCode() : null;
    }

}