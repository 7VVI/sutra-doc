package com.hmoob.doc.service;

import cn.hutool.core.lang.tree.Tree;
import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.doc.domain.bo.KbBusinessTypeBo;
import com.hmoob.doc.domain.vo.KbBusinessTypeVo;

import java.util.List;

/**
 * KB业务类型管理 服务层
 *
 * @author hmoob
 */
public interface IKbBusinessTypeService {

    /**
     * 分页查询业务类型管理数据
     *
     * @param bo        业务类型信息
     * @param pageQuery 分页对象
     * @return 业务类型信息集合
     */
    TableDataInfo<KbBusinessTypeVo> selectPageBusinessTypeList(KbBusinessTypeBo bo, PageQuery pageQuery);

    /**
     * 查询业务类型管理数据
     *
     * @param bo 业务类型信息
     * @return 业务类型信息集合
     */
    List<KbBusinessTypeVo> selectBusinessTypeList(KbBusinessTypeBo bo);

    /**
     * 查询业务类型树结构信息
     *
     * @param bo 业务类型信息
     * @return 业务类型树信息集合
     */
    List<Tree<Long>> selectBusinessTypeTreeList(KbBusinessTypeBo bo);

    /**
     * 根据业务类型ID查询信息
     *
     * @param businessTypeId 业务类型ID
     * @return 业务类型信息
     */
    KbBusinessTypeVo selectBusinessTypeById(Long businessTypeId);

    /**
     * 根据业务类型编码查询信息
     *
     * @param businessTypeCode 业务类型编码
     * @return 业务类型信息
     */
    KbBusinessTypeVo selectBusinessTypeByCode(String businessTypeCode);

    /**
     * 校验业务类型编码是否唯一
     *
     * @param bo 业务类型信息
     * @return 结果
     */
    boolean checkBusinessTypeCodeUnique(KbBusinessTypeBo bo);

    /**
     * 是否存在业务类型子节点
     *
     * @param businessTypeId 业务类型ID
     * @return 结果
     */
    boolean hasChildByBusinessTypeId(Long businessTypeId);

    /**
     * 新增保存业务类型信息
     *
     * @param bo 业务类型信息
     * @return 结果
     */
    int insertBusinessType(KbBusinessTypeBo bo);

    /**
     * 修改保存业务类型信息
     *
     * @param bo 业务类型信息
     * @return 结果
     */
    int updateBusinessType(KbBusinessTypeBo bo);

    /**
     * 删除业务类型管理信息
     *
     * @param businessTypeId 业务类型ID
     * @return 结果
     */
    int deleteBusinessTypeById(Long businessTypeId);

}