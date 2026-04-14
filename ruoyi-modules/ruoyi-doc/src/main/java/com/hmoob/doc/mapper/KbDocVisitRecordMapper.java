package com.hmoob.doc.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmoob.common.mybatis.core.mapper.BaseMapperPlus;
import com.hmoob.doc.domain.KbDocVisitRecord;
import com.hmoob.doc.domain.vo.KbDocVisitRecordVo;

import java.util.List;

/**
 * KB文档访问记录 Mapper接口
 *
 * @author hmoob
 */
public interface KbDocVisitRecordMapper extends BaseMapperPlus<KbDocVisitRecord, KbDocVisitRecordVo> {

    /**
     * 根据文档ID查询访问记录列表
     *
     * @param docId 文档ID
     * @return 访问记录列表
     */
    default List<KbDocVisitRecordVo> selectByDocId(Long docId) {
        return this.selectVoList(new LambdaQueryWrapper<KbDocVisitRecord>()
            .eq(KbDocVisitRecord::getDocId, docId)
            .orderByDesc(KbDocVisitRecord::getCreateTime));
    }

    /**
     * 根据用户ID查询访问记录列表
     *
     * @param userId 用户ID
     * @return 访问记录列表
     */
    default List<KbDocVisitRecordVo> selectByUserId(Long userId) {
        return this.selectVoList(new LambdaQueryWrapper<KbDocVisitRecord>()
            .eq(KbDocVisitRecord::getUserId, userId)
            .orderByDesc(KbDocVisitRecord::getCreateTime));
    }

}