package com.hmoob.doc.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmoob.common.mybatis.core.mapper.BaseMapperPlus;
import com.hmoob.doc.domain.KbBusinessType;
import com.hmoob.doc.domain.vo.KbBusinessTypeVo;

import java.util.List;

/**
 * KB业务类型 Mapper接口
 *
 * @author hmoob
 */
public interface KbBusinessTypeMapper extends BaseMapperPlus<KbBusinessType, KbBusinessTypeVo> {

    /**
     * 查询业务类型树结构
     *
     * @return 业务类型树列表
     */
    default List<KbBusinessTypeVo> selectBusinessTypeTree() {
        return this.selectVoList(new LambdaQueryWrapper<KbBusinessType>()
            .eq(KbBusinessType::getStatus, 1)
            .orderByAsc(KbBusinessType::getSortOrder));
    }

    /**
     * 根据业务类型编码查询业务类型
     *
     * @param businessTypeCode 业务类型编码
     * @return 业务类型Vo对象
     */
    default KbBusinessTypeVo selectByCode(String businessTypeCode) {
        return this.selectVoOne(new LambdaQueryWrapper<KbBusinessType>()
            .eq(KbBusinessType::getBusinessTypeCode, businessTypeCode)
            .eq(KbBusinessType::getStatus, 1));
    }

    /**
     * 根据父业务类型ID查询子业务类型列表
     *
     * @param parentId 父业务类型ID
     * @return 子业务类型列表
     */
    default List<KbBusinessType> selectListByParentId(Long parentId) {
        return this.selectList(new LambdaQueryWrapper<KbBusinessType>()
            .eq(KbBusinessType::getParentId, parentId)
            .eq(KbBusinessType::getStatus, 1)
            .orderByAsc(KbBusinessType::getSortOrder));
    }

}