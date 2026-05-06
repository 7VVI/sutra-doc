package com.hmoob.doc.service;

import cn.hutool.core.lang.tree.Tree;
import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.doc.domain.bo.KbFolderBo;
import com.hmoob.doc.domain.vo.KbDeptDocTreeNodeVo;
import com.hmoob.doc.domain.vo.KbFolderVo;

import java.util.List;

/**
 * KB目录管理 服务层
 *
 * @author hmoob
 */
public interface IKbFolderService {

    /**
     * 分页查询目录管理数据
     *
     * @param bo        目录信息
     * @param pageQuery 分页对象
     * @return 目录信息集合
     */
    TableDataInfo<KbFolderVo> selectPageFolderList(KbFolderBo bo, PageQuery pageQuery);

    /**
     * 查询目录管理数据
     *
     * @param bo 目录信息
     * @return 目录信息集合
     */
    List<KbFolderVo> selectFolderList(KbFolderBo bo);

    /**
     * 查询目录树结构信息
     *
     * @param bo 目录信息
     * @return 目录树信息集合
     */
    List<Tree<Long>> selectFolderTreeList(KbFolderBo bo);

    /**
     * 根据目录ID查询信息
     *
     * @param folderId 目录ID
     * @return 目录信息
     */
    KbFolderVo selectFolderById(Long folderId);

    /**
     * 校验目录名称是否唯一
     *
     * @param bo 目录信息
     * @return 结果
     */
    boolean checkFolderNameUnique(KbFolderBo bo);

    /**
     * 是否存在目录子节点
     *
     * @param folderId 目录ID
     * @return 结果
     */
    boolean hasChildByFolderId(Long folderId);

    /**
     * 查询目录是否存在文档
     *
     * @param folderId 目录ID
     * @return 结果 true 存在 false 不存在
     */
    boolean checkFolderExistDoc(Long folderId);

    /**
     * 新增保存目录信息
     *
     * @param bo 目录信息
     * @return 结果
     */
    int insertFolder(KbFolderBo bo);

    /**
     * 修改保存目录信息
     *
     * @param bo 目录信息
     * @return 结果
     */
    int updateFolder(KbFolderBo bo);

    /**
     * 删除目录管理信息
     *
     * @param folderId 目录ID
     * @return 结果
     */
    int deleteFolderById(Long folderId);

    /**
     * 查询部门下的文档目录结构（懒加载，每次返回一层）
     *
     * @param deptId   部门ID
     * @param parentId 父目录ID（0表示根目录）
     * @return 当前层级的目录和文档节点列表
     */
    List<KbDeptDocTreeNodeVo> selectDeptDocTree(Long deptId, Long parentId);

    /**
     * 懒加载查询子目录列表（每次返回一层）
     *
     * @param parentId 父目录ID（0表示根目录）
     * @return 当前层级的目录列表（含 hasChildren）
     */
    List<KbFolderVo> selectFolderLazyList(Long parentId);

}