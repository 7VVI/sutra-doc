package com.hmoob.common.web.config;

import com.hmoob.common.web.config.properties.CaptchaProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 验证码配置
 *
 * @author Lion Li
 */
@AutoConfiguration
@EnableConfigurationProperties(CaptchaProperties.class)
public class CaptchaConfig {

}
