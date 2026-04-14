package com.hmoob.doc.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmoob.common.mybatis.core.mapper.BaseMapperPlus;
import com.hmoob.doc.domain.KbDocBusinessType;
import com.hmoob.doc.domain.vo.KbDocBusinessTypeVo;

import java.util.List;

/**
 * KB文档业务类型关联 Mapper接口
 *
 * @author hmoob
 */
public interface KbDocBusinessTypeMapper extends BaseMapperPlus<KbDocBusinessType, KbDocBusinessTypeVo> {

    /**
     * 根据文档ID查询业务类型关联列表
     *
     * @param docId 文档ID
     * @return 业务类型关联列表
     */
    default List<KbDocBusinessTypeVo> selectByDocId(Long docId) {
        return this.selectVoList(new LambdaQueryWrapper<KbDocBusinessType>()
            .eq(KbDocBusinessType::getDocId, docId)
            .orderByAsc(KbDocBusinessType::getSortOrder));
    }

    /**
     * 根据业务类型编码查询文档关联列表
     *
     * @param businessType 业务类型编码
     * @return 文档关联列表
     */
    default List<KbDocBusinessTypeVo> selectByBusinessType(String businessType) {
        return this.selectVoList(new LambdaQueryWrapper<KbDocBusinessType>()
            .eq(KbDocBusinessType::getBusinessType, businessType));
    }

}