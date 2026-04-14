package com.hmoob.demo.mapper;

import com.hmoob.common.mybatis.annotation.DataColumn;
import com.hmoob.common.mybatis.annotation.DataPermission;
import com.hmoob.common.mybatis.core.mapper.BaseMapperPlus;
import com.hmoob.demo.domain.TestTree;
import com.hmoob.demo.domain.vo.TestTreeVo;

/**
 * 测试树表Mapper接口
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@DataPermission({
    @DataColumn(key = "deptName", value = "dept_id"),
    @DataColumn(key = "userName", value = "user_id")
})
public interface TestTreeMapper extends BaseMapperPlus<TestTree, TestTreeVo> {

}
