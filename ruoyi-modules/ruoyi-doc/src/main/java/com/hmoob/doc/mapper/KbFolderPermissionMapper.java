package com.hmoob.doc.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmoob.common.mybatis.annotation.DataColumn;
import com.hmoob.common.mybatis.annotation.DataPermission;
import com.hmoob.common.mybatis.core.mapper.BaseMapperPlus;
import com.hmoob.doc.domain.KbFolderPermission;
import com.hmoob.doc.domain.vo.KbFolderPermissionVo;

import java.util.List;

/**
 * KB目录权限 Mapper接口
 *
 * @author hmoob
 */
public interface KbFolderPermissionMapper extends BaseMapperPlus<KbFolderPermission, KbFolderPermissionVo> {

    /**
     * 根据目录ID和授权对象查询权限
     *
     * @param folderId    目录ID
     * @param subjectType 授权对象类型
     * @param subjectId   授权对象ID
     * @return 权限Vo对象
     */
    default KbFolderPermissionVo selectByFolderIdAndSubject(Long folderId, Integer subjectType, Long subjectId) {
        return this.selectVoOne(new LambdaQueryWrapper<KbFolderPermission>()
            .eq(KbFolderPermission::getFolderId, folderId)
            .eq(KbFolderPermission::getSubjectType, subjectType)
            .eq(KbFolderPermission::getSubjectId, subjectId));
    }

    /**
     * 根据目录ID查询所有权限列表
     *
     * @param folderId 目录ID
     * @return 权限列表
     */
    default List<KbFolderPermissionVo> selectByFolderId(Long folderId) {
        return this.selectVoList(new LambdaQueryWrapper<KbFolderPermission>()
            .eq(KbFolderPermission::getFolderId, folderId));
    }

}