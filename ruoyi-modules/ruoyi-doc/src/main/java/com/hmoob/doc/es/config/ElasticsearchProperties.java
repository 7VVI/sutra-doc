package com.hmoob.doc.es.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ES配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "kb.elasticsearch")
public class ElasticsearchProperties {

    /** ES主机地址 */
    private String host = "localhost";

    /** ES端口 */
    private Integer port = 9200;

    /** 是否启用认证 */
    private Boolean auth = false;

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

    /** 是否启用ES功能 */
    private Boolean enabled = true;
}