package com.hmoob.doc.service.impl;

import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import com.hmoob.common.core.constant.SystemConstants;
import com.hmoob.common.core.exception.ServiceException;
import com.hmoob.common.core.utils.MapstructUtils;
import com.hmoob.common.core.utils.StringUtils;
import com.hmoob.common.core.utils.TreeBuildUtils;
import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.doc.domain.KbFolder;
import com.hmoob.doc.domain.bo.KbFolderBo;
import com.hmoob.doc.domain.vo.KbFolderVo;
import com.hmoob.doc.mapper.KbFolderMapper;
import com.hmoob.doc.mapper.KbDocMapper;
import com.hmoob.doc.service.IKbFolderService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * KB目录管理 服务实现
 *
 * @author hmoob
 */
@RequiredArgsConstructor
@Service
public class KbFolderServiceImpl implements IKbFolderService {

    private final KbFolderMapper baseMapper;
    private final KbDocMapper docMapper;

    /**
     * 分页查询目录管理数据
     *
     * @param bo        目录信息
     * @param pageQuery 分页对象
     * @return 目录信息集合
     */
    @Override
    public TableDataInfo<KbFolderVo> selectPageFolderList(KbFolderBo bo, PageQuery pageQuery) {
        Page<KbFolderVo> page = baseMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return TableDataInfo.build(page);
    }

    /**
     * 查询目录管理数据
     *
     * @param bo 目录信息
     * @return 目录信息集合
     */
    @Override
    public List<KbFolderVo> selectFolderList(KbFolderBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    /**
     * 查询目录树结构信息
     *
     * @param bo 目录信息
     * @return 目录树信息集合
     */
    @Override
    public List<Tree<Long>> selectFolderTreeList(KbFolderBo bo) {
        List<KbFolderVo> folders = selectFolderList(bo);
        return TreeBuildUtils.buildMultiRoot(
            folders,
            KbFolderVo::getFolderId,
            KbFolderVo::getParentId,
            (node, treeNode) -> treeNode
                .setId(node.getFolderId())
                .setParentId(node.getParentId())
                .setName(node.getFolderName())
                .setWeight(node.getSortOrder())
        );
    }

    /**
     * 构建查询条件
     *
     * @param bo 目录信息
     * @return 查询条件
     */
    private LambdaQueryWrapper<KbFolder> buildQueryWrapper(KbFolderBo bo) {
        LambdaQueryWrapper<KbFolder> lqw = Wrappers.lambdaQuery();
        lqw.eq(KbFolder::getDelFlag, SystemConstants.NORMAL);
        lqw.eq(ObjectUtil.isNotNull(bo.getFolderId()), KbFolder::getFolderId, bo.getFolderId());
        lqw.eq(ObjectUtil.isNotNull(bo.getParentId()), KbFolder::getParentId, bo.getParentId());
        lqw.like(StringUtils.isNotBlank(bo.getFolderName()), KbFolder::getFolderName, bo.getFolderName());
        lqw.eq(StringUtils.isNotBlank(bo.getFolderCode()), KbFolder::getFolderCode, bo.getFolderCode());
        lqw.eq(ObjectUtil.isNotNull(bo.getStatus()), KbFolder::getStatus, bo.getStatus());
        lqw.orderByAsc(KbFolder::getSortOrder);
        return lqw;
    }

    /**
     * 根据目录ID查询信息
     *
     * @param folderId 目录ID
     * @return 目录信息
     */
    @Override
    public KbFolderVo selectFolderById(Long folderId) {
        return baseMapper.selectVoById(folderId);
    }

    /**
     * 校验目录名称是否唯一
     *
     * @param bo 目录信息
     * @return 结果
     */
    @Override
    public boolean checkFolderNameUnique(KbFolderBo bo) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<KbFolder>()
            .eq(KbFolder::getFolderName, bo.getFolderName())
            .eq(KbFolder::getParentId, bo.getParentId())
            .ne(ObjectUtil.isNotNull(bo.getFolderId()), KbFolder::getFolderId, bo.getFolderId()));
        return !exist;
    }

    /**
     * 是否存在目录子节点
     *
     * @param folderId 目录ID
     * @return 结果
     */
    @Override
    public boolean hasChildByFolderId(Long folderId) {
        return baseMapper.exists(new LambdaQueryWrapper<KbFolder>()
            .eq(KbFolder::getParentId, folderId));
    }

    /**
     * 查询目录是否存在文档
     *
     * @param folderId 目录ID
     * @return 结果 true 存在 false 不存在
     */
    @Override
    public boolean checkFolderExistDoc(Long folderId) {
        return docMapper.exists(new LambdaQueryWrapper<com.hmoob.doc.domain.KbDoc>()
            .eq(com.hmoob.doc.domain.KbDoc::getFolderId, folderId));
    }

    /**
     * 新增保存目录信息
     *
     * @param bo 目录信息
     * @return 结果
     */
    @Override
    public int insertFolder(KbFolderBo bo) {
        KbFolder folder = MapstructUtils.convert(bo, KbFolder.class);
        // 设置初始值
        if (folder.getStatus() == null) {
            folder.setStatus(1);
        }
        if (folder.getSortOrder() == null) {
            folder.setSortOrder(0);
        }
        if (folder.getDocCount() == null) {
            folder.setDocCount(0L);
        }
        // 设置层级和路径
        if (bo.getParentId() != null && bo.getParentId() > 0) {
            KbFolder parentFolder = baseMapper.selectById(bo.getParentId());
            if (ObjectUtil.isNull(parentFolder)) {
                throw new ServiceException("父目录不存在");
            }
            folder.setFolderLevel(parentFolder.getFolderLevel() + 1);
            folder.setFolderPath(parentFolder.getFolderPath() + "/" + bo.getFolderName());
        } else {
            folder.setParentId(0L);
            folder.setFolderLevel(1);
            folder.setFolderPath("/" + bo.getFolderName());
        }
        return baseMapper.insert(folder);
    }

    /**
     * 修改保存目录信息
     *
     * @param bo 目录信息
     * @return 结果
     */
    @Override
    public int updateFolder(KbFolderBo bo) {
        KbFolder folder = MapstructUtils.convert(bo, KbFolder.class);
        KbFolder oldFolder = baseMapper.selectById(folder.getFolderId());
        if (ObjectUtil.isNull(oldFolder)) {
            throw new ServiceException("目录不存在，无法修改");
        }
        // 如果父目录发生变化，需要更新层级和路径
        if (!oldFolder.getParentId().equals(folder.getParentId())) {
            if (folder.getParentId() != null && folder.getParentId() > 0) {
                KbFolder parentFolder = baseMapper.selectById(folder.getParentId());
                if (ObjectUtil.isNull(parentFolder)) {
                    throw new ServiceException("父目录不存在");
                }
                folder.setFolderLevel(parentFolder.getFolderLevel() + 1);
                folder.setFolderPath(parentFolder.getFolderPath() + "/" + folder.getFolderName());
            } else {
                folder.setParentId(0L);
                folder.setFolderLevel(1);
                folder.setFolderPath("/" + folder.getFolderName());
            }
        }
        return baseMapper.updateById(folder);
    }

    /**
     * 删除目录管理信息
     *
     * @param folderId 目录ID
     * @return 结果
     */
    @Override
    public int deleteFolderById(Long folderId) {
        // 校验是否有子目录
        if (hasChildByFolderId(folderId)) {
            throw new ServiceException("存在子目录，不允许删除");
        }
        // 校验是否有文档
        if (checkFolderExistDoc(folderId)) {
            throw new ServiceException("目录下存在文档，不允许删除");
        }
        return baseMapper.deleteById(folderId);
    }

}