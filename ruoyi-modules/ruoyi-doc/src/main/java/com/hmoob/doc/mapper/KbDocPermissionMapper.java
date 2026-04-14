package com.hmoob.doc.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmoob.common.mybatis.core.mapper.BaseMapperPlus;
import com.hmoob.doc.domain.KbDocPermission;
import com.hmoob.doc.domain.vo.KbDocPermissionVo;

import java.util.List;

/**
 * KB文档权限 Mapper接口
 *
 * @author hmoob
 */
public interface KbDocPermissionMapper extends BaseMapperPlus<KbDocPermission, KbDocPermissionVo> {

    /**
     * 根据文档ID和授权对象查询权限
     *
     * @param docId       文档ID
     * @param subjectType 授权对象类型
     * @param subjectId   授权对象ID
     * @return 权限Vo对象
     */
    default KbDocPermissionVo selectByDocIdAndSubject(Long docId, Integer subjectType, Long subjectId) {
        return this.selectVoOne(new LambdaQueryWrapper<KbDocPermission>()
            .eq(KbDocPermission::getDocId, docId)
            .eq(KbDocPermission::getSubjectType, subjectType)
            .eq(KbDocPermission::getSubjectId, subjectId));
    }

    /**
     * 根据文档ID查询所有权限列表
     *
     * @param docId 文档ID
     * @return 权限列表
     */
    default List<KbDocPermissionVo> selectByDocId(Long docId) {
        return this.selectVoList(new LambdaQueryWrapper<KbDocPermission>()
            .eq(KbDocPermission::getDocId, docId));
    }

}