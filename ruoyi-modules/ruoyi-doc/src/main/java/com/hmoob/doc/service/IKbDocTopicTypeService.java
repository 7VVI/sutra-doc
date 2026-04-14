package com.hmoob.doc.service;

import com.hmoob.doc.domain.vo.KbDocTopicTypeVo;

import java.util.List;

/**
 * KB文档主题关联 服务层
 *
 * @author hmoob
 */
public interface IKbDocTopicTypeService {

    /**
     * 根据文档ID查询主题关联列表
     *
     * @param docId 文档ID
     * @return 主题关联列表
     */
    List<KbDocTopicTypeVo> selectByDocId(Long docId);

    /**
     * 根据主题ID查询文档关联列表
     *
     * @param topicId 主题ID
     * @return 文档关联列表
     */
    List<KbDocTopicTypeVo> selectByTopicId(Long topicId);

    /**
     * 添加主题关联到文档
     *
     * @param docId   文档ID
     * @param topicId 主题ID
     * @return 影响行数
     */
    int addTopicToDoc(Long docId, Long topicId);

    /**
     * 移除文档的主题关联
     *
     * @param docId   文档ID
     * @param topicId 主题ID
     * @return 影响行数
     */
    int removeTopicFromDoc(Long docId, Long topicId);

    /**
     * 批量添加主题关联到文档
     *
     * @param docId    文档ID
     * @param topicIds 主题ID列表
     * @return 影响行数
     */
    int batchAddTopicsToDoc(Long docId, List<Long> topicIds);

    /**
     * 批量移除文档的主题关联
     *
     * @param docId    文档ID
     * @param topicIds 主题ID列表
     * @return 影响行数
     */
    int batchRemoveTopicsFromDoc(Long docId, List<Long> topicIds);

    /**
     * 更新文档主题关联（先删除旧关联，再添加新关联）
     *
     * @param docId    文档ID
     * @param topicIds 主题ID列表
     * @return 影响行数
     */
    int updateDocTopics(Long docId, List<Long> topicIds);

    /**
     * 根据文档ID获取主题编码列表
     *
     * @param docId 文档ID
     * @return 主题编码列表
     */
    List<String> getTopicCodesByDocId(Long docId);

}