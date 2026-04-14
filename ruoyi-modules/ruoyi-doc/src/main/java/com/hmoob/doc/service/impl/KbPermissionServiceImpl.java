package com.hmoob.doc.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import com.hmoob.common.core.exception.ServiceException;
import com.hmoob.common.core.utils.MapstructUtils;
import com.hmoob.doc.domain.KbDoc;
import com.hmoob.doc.domain.KbDocPermission;
import com.hmoob.doc.domain.KbFolder;
import com.hmoob.doc.domain.KbFolderPermission;
import com.hmoob.doc.domain.bo.KbDocPermissionBo;
import com.hmoob.doc.domain.bo.KbFolderPermissionBo;
import com.hmoob.doc.domain.dto.KbPermissionResult;
import com.hmoob.doc.domain.vo.KbDocPermissionVo;
import com.hmoob.doc.domain.vo.KbDocVo;
import com.hmoob.doc.domain.vo.KbFolderPermissionVo;
import com.hmoob.doc.enums.PermissionTypeEnum;
import com.hmoob.doc.enums.SubjectTypeEnum;
import com.hmoob.doc.mapper.KbDocMapper;
import com.hmoob.doc.mapper.KbDocPermissionMapper;
import com.hmoob.doc.mapper.KbFolderMapper;
import com.hmoob.doc.mapper.KbFolderPermissionMapper;
import com.hmoob.doc.service.IKbPermissionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * KB权限计算 服务实现
 * 实现权限继承计算逻辑
 *
 * @author hmoob
 */
@RequiredArgsConstructor
@Service
public class KbPermissionServiceImpl implements IKbPermissionService {

    private final KbFolderPermissionMapper folderPermissionMapper;
    private final KbDocPermissionMapper docPermissionMapper;
    private final KbFolderMapper folderMapper;
    private final KbDocMapper docMapper;

    /**
     * 计算用户对文档的权限
     * 权限优先级: 文档直接权限 > 目录权限 > 父目录继承权限 > 根目录权限 > 无权限
     *
     * @param docId  文档ID
     * @param userId 用户ID
     * @return 权限计算结果
     */
    @Override
    public KbPermissionResult calculateDocPermission(Long docId, Long userId) {
        return calculateDocPermission(docId, userId, null);
    }

    /**
     * 计算用户对文档的权限(含角色权限合并)
     *
     * @param docId   文档ID
     * @param userId  用户ID
     * @param roleIds 用户角色ID列表
     * @return 权限计算结果
     */
    @Override
    public KbPermissionResult calculateDocPermission(Long docId, Long userId, List<Long> roleIds) {
        // 1. 查询文档信息获取所属目录
        KbDocVo doc = docMapper.selectVoById(docId);
        if (ObjectUtil.isNull(doc)) {
            return KbPermissionResult.noPermission();
        }

        // 2. 检查文档直接权限(用户权限)
        KbPermissionResult docPerm = checkDocDirectPermission(docId, userId, roleIds);
        if (docPerm.hasAnyPermission()) {
            return docPerm;
        }

        // 3. 检查目录权限(含继承)
        Long folderId = doc.getFolderId();
        if (ObjectUtil.isNull(folderId) || folderId <= 0) {
            // 根目录文档,无目录权限控制
            return KbPermissionResult.noPermission();
        }

        return calculateFolderPermission(folderId, userId, roleIds);
    }

    /**
     * 计算用户对目录的权限
     *
     * @param folderId 目录ID
     * @param userId   用户ID
     * @return 权限计算结果
     */
    @Override
    public KbPermissionResult calculateFolderPermission(Long folderId, Long userId) {
        return calculateFolderPermission(folderId, userId, null);
    }

    /**
     * 计算用户对目录的权限(含角色权限合并)
     *
     * @param folderId 目录ID
     * @param userId   用户ID
     * @param roleIds  用户角色ID列表
     * @return 权限计算结果
     */
    @Override
    public KbPermissionResult calculateFolderPermission(Long folderId, Long userId, List<Long> roleIds) {
        // 1. 检查目录直接权限(用户权限)
        KbPermissionResult folderPerm = checkFolderDirectPermission(folderId, userId, roleIds);
        if (folderPerm.hasAnyPermission()) {
            return folderPerm;
        }

        // 2. 递归检查父目录继承权限
        return checkInheritedPermission(folderId, userId, roleIds);
    }

    /**
     * 判断用户是否拥有文档的特定权限
     *
     * @param docId          文档ID
     * @param userId         用户ID
     * @param permissionType 权限类型
     * @return 是否拥有权限
     */
    @Override
    public boolean hasPermission(Long docId, Long userId, PermissionTypeEnum permissionType) {
        return hasPermission(docId, userId, null, permissionType);
    }

    /**
     * 判断用户是否拥有文档的特定权限(含角色权限)
     *
     * @param docId          文档ID
     * @param userId         用户ID
     * @param roleIds        用户角色ID列表
     * @param permissionType 权限类型
     * @return 是否拥有权限
     */
    @Override
    public boolean hasPermission(Long docId, Long userId, List<Long> roleIds, PermissionTypeEnum permissionType) {
        KbPermissionResult result = calculateDocPermission(docId, userId, roleIds);
        return checkPermissionType(result, permissionType);
    }

    /**
     * 判断用户是否拥有目录的特定权限
     *
     * @param folderId       目录ID
     * @param userId         用户ID
     * @param permissionType 权限类型
     * @return 是否拥有权限
     */
    @Override
    public boolean hasFolderPermission(Long folderId, Long userId, PermissionTypeEnum permissionType) {
        return hasFolderPermission(folderId, userId, null, permissionType);
    }

    /**
     * 判断用户是否拥有目录的特定权限(含角色权限)
     *
     * @param folderId       目录ID
     * @param userId         用户ID
     * @param roleIds        用户角色ID列表
     * @param permissionType 权限类型
     * @return 是否拥有权限
     */
    @Override
    public boolean hasFolderPermission(Long folderId, Long userId, List<Long> roleIds, PermissionTypeEnum permissionType) {
        KbPermissionResult result = calculateFolderPermission(folderId, userId, roleIds);
        return checkPermissionType(result, permissionType);
    }

    /**
     * 授予目录权限
     *
     * @param bo 目录权限业务对象
     * @return 影响行数
     */
    @Override
    public int grantFolderPermission(KbFolderPermissionBo bo) {
        // 校验目录是否存在
        KbFolder folder = folderMapper.selectById(bo.getFolderId());
        if (ObjectUtil.isNull(folder)) {
            throw new ServiceException("目录不存在");
        }
        // 校验授权对象类型
        if (SubjectTypeEnum.getByCode(bo.getSubjectType()) == null) {
            throw new ServiceException("授权对象类型无效");
        }
        // 查询是否已存在权限记录
        KbFolderPermissionVo existPerm = folderPermissionMapper.selectByFolderIdAndSubject(
            bo.getFolderId(), bo.getSubjectType(), bo.getSubjectId());
        if (ObjectUtil.isNotNull(existPerm)) {
            // 更新已有权限
            KbFolderPermission update = MapstructUtils.convert(bo, KbFolderPermission.class);
            update.setPermissionId(existPerm.getPermissionId());
            return folderPermissionMapper.updateById(update);
        }
        // 新增权限
        KbFolderPermission permission = MapstructUtils.convert(bo, KbFolderPermission.class);
        // 设置默认值
        if (permission.getInheritFlag() == null) {
            permission.setInheritFlag(1);
        }
        return folderPermissionMapper.insert(permission);
    }

    /**
     * 授予文档权限
     *
     * @param bo 文档权限业务对象
     * @return 影响行数
     */
    @Override
    public int grantDocPermission(KbDocPermissionBo bo) {
        // 校验文档是否存在
        KbDoc doc = docMapper.selectById(bo.getDocId());
        if (ObjectUtil.isNull(doc)) {
            throw new ServiceException("文档不存在");
        }
        // 校验授权对象类型
        if (SubjectTypeEnum.getByCode(bo.getSubjectType()) == null) {
            throw new ServiceException("授权对象类型无效");
        }
        // 查询是否已存在权限记录
        KbDocPermissionVo existPerm = docPermissionMapper.selectByDocIdAndSubject(
            bo.getDocId(), bo.getSubjectType(), bo.getSubjectId());
        if (ObjectUtil.isNotNull(existPerm)) {
            // 更新已有权限
            KbDocPermission update = MapstructUtils.convert(bo, KbDocPermission.class);
            update.setPermissionId(existPerm.getPermissionId());
            return docPermissionMapper.updateById(update);
        }
        // 新增权限
        KbDocPermission permission = MapstructUtils.convert(bo, KbDocPermission.class);
        return docPermissionMapper.insert(permission);
    }

    /**
     * 删除目录权限
     *
     * @param permissionId 权限ID
     * @return 影响行数
     */
    @Override
    public int deleteFolderPermission(Long permissionId) {
        return folderPermissionMapper.deleteById(permissionId);
    }

    /**
     * 删除文档权限
     *
     * @param permissionId 权限ID
     * @return 影响行数
     */
    @Override
    public int deleteDocPermission(Long permissionId) {
        return docPermissionMapper.deleteById(permissionId);
    }

    /**
     * 查询目录的所有权限列表
     *
     * @param folderId 目录ID
     * @return 权限列表
     */
    @Override
    public List<KbFolderPermissionVo> selectFolderPermissions(Long folderId) {
        return folderPermissionMapper.selectByFolderId(folderId);
    }

    /**
     * 获取继承的权限列表
     *
     * @param folderId 目录ID
     * @return 继承权限列表
     */
    @Override
    public List<KbFolderPermissionVo> getInheritedPermissions(Long folderId) {
        List<KbFolderPermissionVo> inheritedPerms = new ArrayList<>();
        // 获取当前目录
        KbFolder folder = folderMapper.selectById(folderId);
        if (ObjectUtil.isNull(folder)) {
            return inheritedPerms;
        }
        // 递归向上查找父目录
        Long parentId = folder.getParentId();
        while (parentId != null && parentId > 0) {
            // 查询父目录权限
            List<KbFolderPermissionVo> parentPerms = folderPermissionMapper.selectByFolderId(parentId);
            // 过滤出可继承的权限(inherit_flag=1)
            for (KbFolderPermissionVo perm : parentPerms) {
                if (perm.getInheritFlag() != null && perm.getInheritFlag() == 1) {
                    inheritedPerms.add(perm);
                }
            }
            // 继续向上查找
            KbFolder parentFolder = folderMapper.selectById(parentId);
            if (ObjectUtil.isNull(parentFolder)) {
                break;
            }
            parentId = parentFolder.getParentId();
        }
        return inheritedPerms;
    }

    // ==================== 私有方法 ====================

    /**
     * 检查文档直接权限
     */
    private KbPermissionResult checkDocDirectPermission(Long docId, Long userId, List<Long> roleIds) {
        // 1. 检查用户直接权限
        KbDocPermissionVo userPerm = docPermissionMapper.selectByDocIdAndSubject(
            docId, SubjectTypeEnum.USER.getCode(), userId);
        if (ObjectUtil.isNotNull(userPerm)) {
            return buildDocPermissionResult(userPerm, "DOC");
        }
        // 2. 检查角色权限(角色列表不为空时)
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                KbDocPermissionVo rolePerm = docPermissionMapper.selectByDocIdAndSubject(
                    docId, SubjectTypeEnum.ROLE.getCode(), roleId);
                if (ObjectUtil.isNotNull(rolePerm)) {
                    return buildDocPermissionResult(rolePerm, "DOC");
                }
            }
        }
        return KbPermissionResult.noPermission();
    }

    /**
     * 检查目录直接权限
     */
    private KbPermissionResult checkFolderDirectPermission(Long folderId, Long userId, List<Long> roleIds) {
        // 1. 检查用户直接权限
        KbFolderPermissionVo userPerm = folderPermissionMapper.selectByFolderIdAndSubject(
            folderId, SubjectTypeEnum.USER.getCode(), userId);
        if (ObjectUtil.isNotNull(userPerm)) {
            return buildFolderPermissionResult(userPerm, "FOLDER");
        }
        // 2. 检查角色权限(角色列表不为空时)
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                KbFolderPermissionVo rolePerm = folderPermissionMapper.selectByFolderIdAndSubject(
                    folderId, SubjectTypeEnum.ROLE.getCode(), roleId);
                if (ObjectUtil.isNotNull(rolePerm)) {
                    return buildFolderPermissionResult(rolePerm, "FOLDER");
                }
            }
        }
        return KbPermissionResult.noPermission();
    }

    /**
     * 递归检查继承权限
     */
    private KbPermissionResult checkInheritedPermission(Long folderId, Long userId, List<Long> roleIds) {
        KbFolder folder = folderMapper.selectById(folderId);
        if (ObjectUtil.isNull(folder)) {
            return KbPermissionResult.noPermission();
        }
        // 递归向上查找父目录的继承权限
        Long parentId = folder.getParentId();
        while (parentId != null && parentId > 0) {
            // 查询父目录权限(只查inherit_flag=1的权限)
            List<KbFolderPermissionVo> parentPerms = folderPermissionMapper.selectByFolderId(parentId);
            for (KbFolderPermissionVo perm : parentPerms) {
                // 只检查可继承的权限
                if (perm.getInheritFlag() == null || perm.getInheritFlag() != 1) {
                    continue;
                }
                // 检查用户权限
                if (perm.getSubjectType().equals(SubjectTypeEnum.USER.getCode())
                    && perm.getSubjectId().equals(userId)) {
                    return buildFolderPermissionResult(perm, "INHERITED");
                }
                // 检查角色权限
                if (perm.getSubjectType().equals(SubjectTypeEnum.ROLE.getCode())
                    && roleIds != null && roleIds.contains(perm.getSubjectId())) {
                    return buildFolderPermissionResult(perm, "INHERITED");
                }
            }
            // 继续向上查找
            KbFolder parentFolder = folderMapper.selectById(parentId);
            if (ObjectUtil.isNull(parentFolder)) {
                break;
            }
            parentId = parentFolder.getParentId();
        }
        return KbPermissionResult.noPermission();
    }

    /**
     * 构建文档权限结果
     */
    private KbPermissionResult buildDocPermissionResult(KbDocPermissionVo perm, String sourceType) {
        KbPermissionResult result = new KbPermissionResult();
        result.setCanView(perm.getPermView() != null && perm.getPermView() == 1);
        result.setCanDownload(perm.getPermDownload() != null && perm.getPermDownload() == 1);
        result.setCanEdit(perm.getPermEdit() != null && perm.getPermEdit() == 1);
        result.setCanDelete(perm.getPermDelete() != null && perm.getPermDelete() == 1);
        // 文档权限无上传和管理权限
        result.setCanUpload(false);
        result.setCanManage(false);
        result.setDataScope(perm.getDataScope());
        result.setSourceId(perm.getPermissionId());
        result.setSourceType(sourceType);
        return result;
    }

    /**
     * 构建目录权限结果
     */
    private KbPermissionResult buildFolderPermissionResult(KbFolderPermissionVo perm, String sourceType) {
        KbPermissionResult result = new KbPermissionResult();
        result.setCanView(perm.getPermView() != null && perm.getPermView() == 1);
        result.setCanDownload(perm.getPermDownload() != null && perm.getPermDownload() == 1);
        result.setCanEdit(perm.getPermEdit() != null && perm.getPermEdit() == 1);
        result.setCanDelete(perm.getPermDelete() != null && perm.getPermDelete() == 1);
        result.setCanUpload(perm.getPermUpload() != null && perm.getPermUpload() == 1);
        result.setCanManage(perm.getPermManage() != null && perm.getPermManage() == 1);
        result.setDataScope(perm.getDataScope());
        result.setSourceId(perm.getPermissionId());
        result.setSourceType(sourceType);
        return result;
    }

    /**
     * 检查权限结果是否包含指定权限类型
     */
    private boolean checkPermissionType(KbPermissionResult result, PermissionTypeEnum permissionType) {
        switch (permissionType) {
            case VIEW:
                return result.isCanView();
            case DOWNLOAD:
                return result.isCanDownload();
            case EDIT:
                return result.isCanEdit();
            case DELETE:
                return result.isCanDelete();
            case UPLOAD:
                return result.isCanUpload();
            case MANAGE:
                return result.isCanManage();
            default:
                return false;
        }
    }

}