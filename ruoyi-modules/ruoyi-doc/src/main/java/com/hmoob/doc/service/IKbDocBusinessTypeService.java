package com.hmoob.doc.service;

import com.hmoob.doc.domain.vo.KbDocBusinessTypeVo;

import java.util.List;

/**
 * KB文档业务类型关联 服务层
 *
 * @author hmoob
 */
public interface IKbDocBusinessTypeService {

    /**
     * 根据文档ID查询业务类型关联列表
     *
     * @param docId 文档ID
     * @return 业务类型关联列表
     */
    List<KbDocBusinessTypeVo> selectByDocId(Long docId);

    /**
     * 根据业务类型编码查询文档关联列表
     *
     * @param businessType 业务类型编码
     * @return 文档关联列表
     */
    List<KbDocBusinessTypeVo> selectByBusinessType(String businessType);

    /**
     * 添加业务类型关联到文档
     *
     * @param docId        文档ID
     * @param businessType 业务类型编码
     * @return 影响行数
     */
    int addBusinessTypeToDoc(Long docId, String businessType);

    /**
     * 移除文档的业务类型关联
     *
     * @param docId        文档ID
     * @param businessType 业务类型编码
     * @return 影响行数
     */
    int removeBusinessTypeFromDoc(Long docId, String businessType);

    /**
     * 批量添加业务类型关联到文档
     *
     * @param docId         文档ID
     * @param businessTypes 业务类型编码列表
     * @return 影响行数
     */
    int batchAddBusinessTypesToDoc(Long docId, List<String> businessTypes);

    /**
     * 批量移除文档的业务类型关联
     *
     * @param docId         文档ID
     * @param businessTypes 业务类型编码列表
     * @return 影响行数
     */
    int batchRemoveBusinessTypesFromDoc(Long docId, List<String> businessTypes);

    /**
     * 更新文档业务类型关联（先删除旧关联，再添加新关联）
     *
     * @param docId         文档ID
     * @param businessTypes 业务类型编码列表
     * @return 影响行数
     */
    int updateDocBusinessTypes(Long docId, List<String> businessTypes);

    /**
     * 根据文档ID获取业务类型编码列表
     *
     * @param docId 文档ID
     * @return 业务类型编码列表
     */
    List<String> getBusinessTypeCodesByDocId(Long docId);

}