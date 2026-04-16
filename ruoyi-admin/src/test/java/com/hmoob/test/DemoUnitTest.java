package com.hmoob.test;

import com.alibaba.cloud.ai.parser.tika.TikaDocumentParser;
import com.hmoob.common.web.config.properties.CaptchaProperties;
import org.junit.jupiter.api.*;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 单元测试案例
 *
 * @author Lion Li
 */
@SpringBootTest // 此注解只能在 springboot 主包下使用 需包含 main 方法与 yml 配置文件
@DisplayName("单元测试案例")
public class DemoUnitTest {

    @Autowired
    private CaptchaProperties captchaProperties;

    @DisplayName("测试 @SpringBootTest @Test @DisplayName 注解")
    @Test
    public void testTest() {
        System.out.println(captchaProperties);
    }

    @Disabled
    @DisplayName("测试 @Disabled 注解")
    @Test
    public void testDisabled() {
        System.out.println(captchaProperties);
    }

    @Timeout(value = 2L, unit = TimeUnit.SECONDS)
    @DisplayName("测试 @Timeout 注解")
    @Test
    public void testTimeout() throws InterruptedException {
        Thread.sleep(3000);
        System.out.println(captchaProperties);
    }


    @DisplayName("测试 @RepeatedTest 注解")
    @RepeatedTest(3)
    public void testRepeatedTest() {
        System.out.println(666);
    }

    @BeforeAll
    public static void testBeforeAll() {
        System.out.println("@BeforeAll ==================");
    }

    @BeforeEach
    public void testBeforeEach() {
        System.out.println("@BeforeEach ==================");
    }

    @AfterEach
    public void testAfterEach() {
        System.out.println("@AfterEach ==================");
    }

    @AfterAll
    public static void testAfterAll() {
        System.out.println("@AfterAll ==================");
    }


    @Test
    public void testParseDoc() throws IOException {
        // 创建 Tika Parser
        TikaDocumentParser parser = new TikaDocumentParser();

        // 读取文件输入流

        try (InputStream inputStream = new FileInputStream("C:\\Users\\yanx\\Downloads\\订货平台2.0数据初始化_代理商积分数据_20260410_V0.1.xlsx");) {
            List<Document> parse = parser.parse(inputStream);
            for (Document document : parse) {
                System.out.println(document.getText());
            }
        }

    }

}
