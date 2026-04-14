package com.hmoob.doc.service;

import com.hmoob.doc.domain.bo.KbDocPermissionBo;
import com.hmoob.doc.domain.bo.KbFolderPermissionBo;
import com.hmoob.doc.domain.dto.KbPermissionResult;
import com.hmoob.doc.domain.vo.KbFolderPermissionVo;
import com.hmoob.doc.enums.PermissionTypeEnum;

import java.util.List;

/**
 * KB权限计算 服务层
 * 实现权限继承计算逻辑
 *
 * @author hmoob
 */
public interface IKbPermissionService {

    /**
     * 计算用户对文档的权限
     * 权限优先级: 文档直接权限 > 目录权限 > 父目录继承权限 > 根目录权限 > 无权限
     *
     * @param docId  文档ID
     * @param userId 用户ID
     * @return 权限计算结果
     */
    KbPermissionResult calculateDocPermission(Long docId, Long userId);

    /**
     * 计算用户对文档的权限(含角色权限合并)
     * 会同时检查用户的角色权限
     *
     * @param docId   文档ID
     * @param userId  用户ID
     * @param roleIds 用户角色ID列表
     * @return 权限计算结果
     */
    KbPermissionResult calculateDocPermission(Long docId, Long userId, List<Long> roleIds);

    /**
     * 计算用户对目录的权限
     * 权限优先级: 目录权限 > 父目录继承权限 > 根目录权限 > 无权限
     *
     * @param folderId 目录ID
     * @param userId   用户ID
     * @return 权限计算结果
     */
    KbPermissionResult calculateFolderPermission(Long folderId, Long userId);

    /**
     * 计算用户对目录的权限(含角色权限合并)
     * 会同时检查用户的角色权限
     *
     * @param folderId 目录ID
     * @param userId   用户ID
     * @param roleIds  用户角色ID列表
     * @return 权限计算结果
     */
    KbPermissionResult calculateFolderPermission(Long folderId, Long userId, List<Long> roleIds);

    /**
     * 判断用户是否拥有文档的特定权限
     *
     * @param docId          文档ID
     * @param userId         用户ID
     * @param permissionType 权限类型
     * @return 是否拥有权限
     */
    boolean hasPermission(Long docId, Long userId, PermissionTypeEnum permissionType);

    /**
     * 判断用户是否拥有文档的特定权限(含角色权限)
     *
     * @param docId          文档ID
     * @param userId         用户ID
     * @param roleIds        用户角色ID列表
     * @param permissionType 权限类型
     * @return 是否拥有权限
     */
    boolean hasPermission(Long docId, Long userId, List<Long> roleIds, PermissionTypeEnum permissionType);

    /**
     * 判断用户是否拥有目录的特定权限
     *
     * @param folderId       目录ID
     * @param userId         用户ID
     * @param permissionType 权限类型
     * @return 是否拥有权限
     */
    boolean hasFolderPermission(Long folderId, Long userId, PermissionTypeEnum permissionType);

    /**
     * 判断用户是否拥有目录的特定权限(含角色权限)
     *
     * @param folderId       目录ID
     * @param userId         用户ID
     * @param roleIds        用户角色ID列表
     * @param permissionType 权限类型
     * @return 是否拥有权限
     */
    boolean hasFolderPermission(Long folderId, Long userId, List<Long> roleIds, PermissionTypeEnum permissionType);

    /**
     * 授予目录权限
     *
     * @param bo 目录权限业务对象
     * @return 影响行数
     */
    int grantFolderPermission(KbFolderPermissionBo bo);

    /**
     * 授予文档权限
     *
     * @param bo 文档权限业务对象
     * @return 影响行数
     */
    int grantDocPermission(KbDocPermissionBo bo);

    /**
     * 删除目录权限
     *
     * @param permissionId 权限ID
     * @return 影响行数
     */
    int deleteFolderPermission(Long permissionId);

    /**
     * 删除文档权限
     *
     * @param permissionId 权限ID
     * @return 影响行数
     */
    int deleteDocPermission(Long permissionId);

    /**
     * 查询目录的所有权限列表
     *
     * @param folderId 目录ID
     * @return 权限列表
     */
    List<KbFolderPermissionVo> selectFolderPermissions(Long folderId);

    /**
     * 获取继承的权限列表
     * 查询父目录链中inherit_flag=1的权限记录
     *
     * @param folderId 目录ID
     * @return 继承权限列表
     */
    List<KbFolderPermissionVo> getInheritedPermissions(Long folderId);

}