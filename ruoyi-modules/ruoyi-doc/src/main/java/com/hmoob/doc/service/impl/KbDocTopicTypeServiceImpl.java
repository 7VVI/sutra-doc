package com.hmoob.doc.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import com.hmoob.doc.domain.KbDocTopicType;
import com.hmoob.doc.domain.vo.KbDocTopicTypeVo;
import com.hmoob.doc.mapper.KbDocTopicTypeMapper;
import com.hmoob.doc.mapper.KbTopicMapper;
import com.hmoob.doc.service.IKbDocTopicTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * KB文档主题关联 服务实现
 *
 * @author hmoob
 */
@RequiredArgsConstructor
@Service
public class KbDocTopicTypeServiceImpl implements IKbDocTopicTypeService {

    private final KbDocTopicTypeMapper baseMapper;
    private final KbTopicMapper topicMapper;

    /**
     * 根据文档ID查询主题关联列表
     *
     * @param docId 文档ID
     * @return 主题关联列表
     */
    @Override
    public List<KbDocTopicTypeVo> selectByDocId(Long docId) {
        return baseMapper.selectByDocId(docId);
    }

    /**
     * 根据主题ID查询文档关联列表
     *
     * @param topicId 主题ID
     * @return 文档关联列表
     */
    @Override
    public List<KbDocTopicTypeVo> selectByTopicId(Long topicId) {
        return baseMapper.selectByTopicId(topicId);
    }

    /**
     * 添加主题关联到文档
     *
     * @param docId   文档ID
     * @param topicId 主题ID
     * @return 影响行数
     */
    @Override
    public int addTopicToDoc(Long docId, Long topicId) {
        // 检查是否已存在关联
        boolean exists = baseMapper.exists(new LambdaQueryWrapper<KbDocTopicType>()
            .eq(KbDocTopicType::getDocId, docId)
            .eq(KbDocTopicType::getTopicId, topicId));
        if (exists) {
            return 0; // 已存在，不重复添加
        }
        KbDocTopicType entity = new KbDocTopicType();
        entity.setDocId(docId);
        entity.setTopicId(topicId);
        entity.setSortOrder(0);
        return baseMapper.insert(entity);
    }

    /**
     * 移除文档的主题关联
     *
     * @param docId   文档ID
     * @param topicId 主题ID
     * @return 影响行数
     */
    @Override
    public int removeTopicFromDoc(Long docId, Long topicId) {
        return baseMapper.delete(new LambdaQueryWrapper<KbDocTopicType>()
            .eq(KbDocTopicType::getDocId, docId)
            .eq(KbDocTopicType::getTopicId, topicId));
    }

    /**
     * 批量添加主题关联到文档
     *
     * @param docId    文档ID
     * @param topicIds 主题ID列表
     * @return 影响行数
     */
    @Override
    public int batchAddTopicsToDoc(Long docId, List<Long> topicIds) {
        if (CollUtil.isEmpty(topicIds)) {
            return 0;
        }
        List<KbDocTopicType> entities = new ArrayList<>();
        for (int i = 0; i < topicIds.size(); i++) {
            Long topicId = topicIds.get(i);
            // 检查是否已存在关联
            boolean exists = baseMapper.exists(new LambdaQueryWrapper<KbDocTopicType>()
                .eq(KbDocTopicType::getDocId, docId)
                .eq(KbDocTopicType::getTopicId, topicId));
            if (!exists) {
                KbDocTopicType entity = new KbDocTopicType();
                entity.setDocId(docId);
                entity.setTopicId(topicId);
                entity.setSortOrder(i);
                entities.add(entity);
            }
        }
        if (CollUtil.isEmpty(entities)) {
            return 0;
        }
        baseMapper.insertBatch(entities);
        return entities.size();
    }

    /**
     * 批量移除文档的主题关联
     *
     * @param docId    文档ID
     * @param topicIds 主题ID列表
     * @return 影响行数
     */
    @Override
    public int batchRemoveTopicsFromDoc(Long docId, List<Long> topicIds) {
        if (CollUtil.isEmpty(topicIds)) {
            return 0;
        }
        return baseMapper.delete(new LambdaQueryWrapper<KbDocTopicType>()
            .eq(KbDocTopicType::getDocId, docId)
            .in(KbDocTopicType::getTopicId, topicIds));
    }

    /**
     * 更新文档主题关联（先删除旧关联，再添加新关联）
     *
     * @param docId    文档ID
     * @param topicIds 主题ID列表
     * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateDocTopics(Long docId, List<Long> topicIds) {
        // 删除文档的所有主题关联
        baseMapper.delete(new LambdaQueryWrapper<KbDocTopicType>()
            .eq(KbDocTopicType::getDocId, docId));
        // 添加新的主题关联
        if (CollUtil.isEmpty(topicIds)) {
            return 0;
        }
        return batchAddTopicsToDoc(docId, topicIds);
    }

    /**
     * 根据文档ID获取主题编码列表
     *
     * @param docId 文档ID
     * @return 主题编码列表
     */
    @Override
    public List<String> getTopicCodesByDocId(Long docId) {
        List<KbDocTopicTypeVo> associations = selectByDocId(docId);
        if (CollUtil.isEmpty(associations)) {
            return new ArrayList<>();
        }
        List<String> codes = new ArrayList<>();
        for (KbDocTopicTypeVo vo : associations) {
            // 通过topicId查询topicCode
            String topicCode = topicMapper.selectTopicCodeById(vo.getTopicId());
            if (topicCode != null) {
                codes.add(topicCode);
            }
        }
        return codes;
    }

}