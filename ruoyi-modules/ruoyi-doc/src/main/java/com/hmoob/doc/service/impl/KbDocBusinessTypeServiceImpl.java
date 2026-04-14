package com.hmoob.doc.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import com.hmoob.doc.domain.KbDocBusinessType;
import com.hmoob.doc.domain.vo.KbDocBusinessTypeVo;
import com.hmoob.doc.mapper.KbDocBusinessTypeMapper;
import com.hmoob.doc.service.IKbDocBusinessTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * KB文档业务类型关联 服务实现
 *
 * @author hmoob
 */
@RequiredArgsConstructor
@Service
public class KbDocBusinessTypeServiceImpl implements IKbDocBusinessTypeService {

    private final KbDocBusinessTypeMapper baseMapper;

    /**
     * 根据文档ID查询业务类型关联列表
     *
     * @param docId 文档ID
     * @return 业务类型关联列表
     */
    @Override
    public List<KbDocBusinessTypeVo> selectByDocId(Long docId) {
        return baseMapper.selectByDocId(docId);
    }

    /**
     * 根据业务类型编码查询文档关联列表
     *
     * @param businessType 业务类型编码
     * @return 文档关联列表
     */
    @Override
    public List<KbDocBusinessTypeVo> selectByBusinessType(String businessType) {
        return baseMapper.selectByBusinessType(businessType);
    }

    /**
     * 添加业务类型关联到文档
     *
     * @param docId        文档ID
     * @param businessType 业务类型编码
     * @return 影响行数
     */
    @Override
    public int addBusinessTypeToDoc(Long docId, String businessType) {
        // 检查是否已存在关联
        boolean exists = baseMapper.exists(new LambdaQueryWrapper<KbDocBusinessType>()
            .eq(KbDocBusinessType::getDocId, docId)
            .eq(KbDocBusinessType::getBusinessType, businessType));
        if (exists) {
            return 0; // 已存在，不重复添加
        }
        KbDocBusinessType entity = new KbDocBusinessType();
        entity.setDocId(docId);
        entity.setBusinessType(businessType);
        entity.setSortOrder(0);
        return baseMapper.insert(entity);
    }

    /**
     * 移除文档的业务类型关联
     *
     * @param docId        文档ID
     * @param businessType 业务类型编码
     * @return 影响行数
     */
    @Override
    public int removeBusinessTypeFromDoc(Long docId, String businessType) {
        return baseMapper.delete(new LambdaQueryWrapper<KbDocBusinessType>()
            .eq(KbDocBusinessType::getDocId, docId)
            .eq(KbDocBusinessType::getBusinessType, businessType));
    }

    /**
     * 批量添加业务类型关联到文档
     *
     * @param docId         文档ID
     * @param businessTypes 业务类型编码列表
     * @return 影响行数
     */
    @Override
    public int batchAddBusinessTypesToDoc(Long docId, List<String> businessTypes) {
        if (CollUtil.isEmpty(businessTypes)) {
            return 0;
        }
        List<KbDocBusinessType> entities = new ArrayList<>();
        for (int i = 0; i < businessTypes.size(); i++) {
            String businessType = businessTypes.get(i);
            // 检查是否已存在关联
            boolean exists = baseMapper.exists(new LambdaQueryWrapper<KbDocBusinessType>()
                .eq(KbDocBusinessType::getDocId, docId)
                .eq(KbDocBusinessType::getBusinessType, businessType));
            if (!exists) {
                KbDocBusinessType entity = new KbDocBusinessType();
                entity.setDocId(docId);
                entity.setBusinessType(businessType);
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
     * 批量移除文档的业务类型关联
     *
     * @param docId         文档ID
     * @param businessTypes 业务类型编码列表
     * @return 影响行数
     */
    @Override
    public int batchRemoveBusinessTypesFromDoc(Long docId, List<String> businessTypes) {
        if (CollUtil.isEmpty(businessTypes)) {
            return 0;
        }
        return baseMapper.delete(new LambdaQueryWrapper<KbDocBusinessType>()
            .eq(KbDocBusinessType::getDocId, docId)
            .in(KbDocBusinessType::getBusinessType, businessTypes));
    }

    /**
     * 更新文档业务类型关联（先删除旧关联，再添加新关联）
     *
     * @param docId         文档ID
     * @param businessTypes 业务类型编码列表
     * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateDocBusinessTypes(Long docId, List<String> businessTypes) {
        // 删除文档的所有业务类型关联
        baseMapper.delete(new LambdaQueryWrapper<KbDocBusinessType>()
            .eq(KbDocBusinessType::getDocId, docId));
        // 添加新的业务类型关联
        if (CollUtil.isEmpty(businessTypes)) {
            return 0;
        }
        return batchAddBusinessTypesToDoc(docId, businessTypes);
    }

    /**
     * 根据文档ID获取业务类型编码列表
     *
     * @param docId 文档ID
     * @return 业务类型编码列表
     */
    @Override
    public List<String> getBusinessTypeCodesByDocId(Long docId) {
        List<KbDocBusinessTypeVo> associations = selectByDocId(docId);
        if (CollUtil.isEmpty(associations)) {
            return new ArrayList<>();
        }
        return associations.stream()
            .map(KbDocBusinessTypeVo::getBusinessType)
            .collect(Collectors.toList());
    }

}