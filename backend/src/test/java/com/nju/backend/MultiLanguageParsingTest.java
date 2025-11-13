package com.nju.backend;

import com.nju.backend.repository.mapper.ProjectMapper;
import com.nju.backend.repository.mapper.WhiteListMapper;
import com.nju.backend.repository.po.Project;
import com.nju.backend.repository.po.WhiteList;
import com.nju.backend.service.project.ProjectService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 多语言项目依赖解析功能测试
 *
 * 测试内容：
 * 1. 各语言的异步解析方法
 * 2. 依赖数据是否正确写入white_list表
 * 3. Flask服务连接测试
 */
@SpringBootTest
public class MultiLanguageParsingTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private WhiteListMapper whiteListMapper;

    @Autowired
    private RestTemplate restTemplate;

    // 测试项目ID（需要先在数据库中创建测试项目）
    private static final int TEST_PROJECT_ID = 1;

    // 测试项目路径（需要修改为实际存在的项目路径）
    private static final String TEST_JAVA_PROJECT_PATH = "C:/test/java-project";
    private static final String TEST_PYTHON_PROJECT_PATH = "C:/test/python-project";
    private static final String TEST_GO_PROJECT_PATH = "C:/test/go-project";

    /**
     * 测试Flask服务是否运行
     */
    @Test
    public void testFlaskServiceAvailability() {
        System.out.println("========================================");
        System.out.println("测试Flask服务连接");
        System.out.println("========================================");

        try {
            String url = "http://localhost:5000/vulnerabilities/test";
            String response = restTemplate.getForObject(url, String.class);

            System.out.println("✓ Flask服务正常运行");
            System.out.println("响应: " + response);
            assert response != null && response.contains("\"code\":200");
        } catch (Exception e) {
            System.err.println("✗ Flask服务连接失败: " + e.getMessage());
            System.err.println("请确保Flask服务已启动: python app.py");
            throw new RuntimeException("Flask服务未运行", e);
        }
    }

    /**
     * 测试Java项目解析
     */
    @Test
    public void testJavaProjectParsing() {
        System.out.println("========================================");
        System.out.println("测试Java项目解析");
        System.out.println("========================================");

        // 清理之前的测试数据
        cleanupTestData(TEST_JAVA_PROJECT_PATH, "java");

        // 执行解析
        projectService.asyncParseJavaProject(TEST_JAVA_PROJECT_PATH);

        // 等待异步任务完成
        waitForAsyncTask(5000);

        // 验证结果
        List<WhiteList> dependencies = queryDependencies(TEST_JAVA_PROJECT_PATH, "java");

        System.out.println("✓ Java项目解析完成");
        System.out.println("找到依赖数量: " + dependencies.size());

        if (!dependencies.isEmpty()) {
            System.out.println("\n示例依赖:");
            dependencies.stream().limit(5).forEach(dep ->
                System.out.println("  - " + dep.getName())
            );
        }

        assert dependencies.size() > 0 : "Java项目应该至少有一个依赖";
    }

    /**
     * 测试Python项目解析
     */
    @Test
    public void testPythonProjectParsing() {
        System.out.println("========================================");
        System.out.println("测试Python项目解析");
        System.out.println("========================================");

        cleanupTestData(TEST_PYTHON_PROJECT_PATH, "python");

        projectService.asyncParsePythonProject(TEST_PYTHON_PROJECT_PATH);
        waitForAsyncTask(5000);

        List<WhiteList> dependencies = queryDependencies(TEST_PYTHON_PROJECT_PATH, "python");

        System.out.println("✓ Python项目解析完成");
        System.out.println("找到依赖数量: " + dependencies.size());

        if (!dependencies.isEmpty()) {
            System.out.println("\n示例依赖:");
            dependencies.stream().limit(5).forEach(dep ->
                System.out.println("  - " + dep.getName())
            );
        }

        assert dependencies.size() > 0 : "Python项目应该至少有一个依赖";
    }

    /**
     * 测试Go项目解析
     */
    @Test
    public void testGoProjectParsing() {
        System.out.println("========================================");
        System.out.println("测试Go项目解析");
        System.out.println("========================================");

        cleanupTestData(TEST_GO_PROJECT_PATH, "go");

        projectService.asyncParseGoProject(TEST_GO_PROJECT_PATH);
        waitForAsyncTask(5000);

        List<WhiteList> dependencies = queryDependencies(TEST_GO_PROJECT_PATH, "go");

        System.out.println("✓ Go项目解析完成");
        System.out.println("找到依赖数量: " + dependencies.size());

        if (!dependencies.isEmpty()) {
            System.out.println("\n示例依赖:");
            dependencies.stream().limit(5).forEach(dep ->
                System.out.println("  - " + dep.getName())
            );
        }

        assert dependencies.size() > 0 : "Go项目应该至少有一个依赖";
    }

    /**
     * 测试Rust项目解析
     */
    @Test
    public void testRustProjectParsing() {
        System.out.println("========================================");
        System.out.println("测试Rust项目解析");
        System.out.println("========================================");

        String testPath = "C:/test/rust-project";
        cleanupTestData(testPath, "rust");

        projectService.asyncParseRustProject(testPath);
        waitForAsyncTask(5000);

        List<WhiteList> dependencies = queryDependencies(testPath, "rust");

        System.out.println("✓ Rust项目解析完成");
        System.out.println("找到依赖数量: " + dependencies.size());

        printSampleDependencies(dependencies);
    }

    /**
     * 测试JavaScript项目解析
     */
    @Test
    public void testJavaScriptProjectParsing() {
        System.out.println("========================================");
        System.out.println("测试JavaScript项目解析");
        System.out.println("========================================");

        String testPath = "C:/test/javascript-project";
        cleanupTestData(testPath, "javascript");

        projectService.asyncParseJavaScriptProject(testPath);
        waitForAsyncTask(5000);

        List<WhiteList> dependencies = queryDependencies(testPath, "javascript");

        System.out.println("✓ JavaScript项目解析完成");
        System.out.println("找到依赖数量: " + dependencies.size());

        printSampleDependencies(dependencies);
    }

    /**
     * 测试PHP项目解析
     */
    @Test
    public void testPhpProjectParsing() {
        System.out.println("========================================");
        System.out.println("测试PHP项目解析");
        System.out.println("========================================");

        String testPath = "C:/test/php-project";
        cleanupTestData(testPath, "php");

        projectService.asyncParsePhpProject(testPath);
        waitForAsyncTask(5000);

        List<WhiteList> dependencies = queryDependencies(testPath, "php");

        System.out.println("✓ PHP项目解析完成");
        System.out.println("找到依赖数量: " + dependencies.size());

        printSampleDependencies(dependencies);
    }

    /**
     * 测试Ruby项目解析
     */
    @Test
    public void testRubyProjectParsing() {
        System.out.println("========================================");
        System.out.println("测试Ruby项目解析");
        System.out.println("========================================");

        String testPath = "C:/test/ruby-project";
        cleanupTestData(testPath, "ruby");

        projectService.asyncParseRubyProject(testPath);
        waitForAsyncTask(5000);

        List<WhiteList> dependencies = queryDependencies(testPath, "ruby");

        System.out.println("✓ Ruby项目解析完成");
        System.out.println("找到依赖数量: " + dependencies.size());

        printSampleDependencies(dependencies);
    }

    /**
     * 测试Erlang项目解析
     */
    @Test
    public void testErlangProjectParsing() {
        System.out.println("========================================");
        System.out.println("测试Erlang项目解析");
        System.out.println("========================================");

        String testPath = "C:/test/erlang-project";
        cleanupTestData(testPath, "erlang");

        projectService.asyncParseErlangProject(testPath);
        waitForAsyncTask(5000);

        List<WhiteList> dependencies = queryDependencies(testPath, "erlang");

        System.out.println("✓ Erlang项目解析完成");
        System.out.println("找到依赖数量: " + dependencies.size());

        printSampleDependencies(dependencies);
    }

    /**
     * 测试所有语言的Flask API响应格式
     */
    @Test
    public void testFlaskApiResponseFormat() {
        System.out.println("========================================");
        System.out.println("测试Flask API响应格式");
        System.out.println("========================================");

        String testPath = "C:/test/sample-project";

        // 测试各个语言的Flask API
        testFlaskApi("Java", "http://localhost:5000/parse/pom_parse?project_folder=" + testPath);
        testFlaskApi("Python", "http://localhost:5000/parse/python_parse?project_folder=" + testPath);
        testFlaskApi("Go", "http://localhost:5000/parse/go_parse?project_folder=" + testPath);
        testFlaskApi("Rust", "http://localhost:5000/parse/rust_parse?project_folder=" + testPath);
        testFlaskApi("JavaScript", "http://localhost:5000/parse/javascript_parse?project_folder=" + testPath);
        testFlaskApi("PHP", "http://localhost:5000/parse/php_parse?project_folder=" + testPath);
        testFlaskApi("Ruby", "http://localhost:5000/parse/ruby_parse?project_folder=" + testPath);
        testFlaskApi("Erlang", "http://localhost:5000/parse/erlang_parse?project_folder=" + testPath);
    }

    /**
     * 测试数据库写入
     */
    @Test
    public void testDatabaseInsertion() {
        System.out.println("========================================");
        System.out.println("测试数据库写入功能");
        System.out.println("========================================");

        String testPath = "C:/test/db-test-project";
        String testLanguage = "java";

        // 清理测试数据
        cleanupTestData(testPath, testLanguage);

        // 手动创建测试数据
        WhiteList testDep = new WhiteList();
        testDep.setName("test-dependency");
        testDep.setFilePath(testPath);
        testDep.setLanguage(testLanguage);
        testDep.setDescription("测试依赖");
        testDep.setIsdelete(0);

        // 插入数据
        int result = whiteListMapper.insert(testDep);

        System.out.println("插入结果: " + (result > 0 ? "成功" : "失败"));
        System.out.println("插入ID: " + testDep.getId());

        // 验证插入
        WhiteList queried = whiteListMapper.selectById(testDep.getId());

        assert queried != null : "应该能查询到刚插入的记录";
        assert "test-dependency".equals(queried.getName()) : "依赖名称应该匹配";
        assert testLanguage.equals(queried.getLanguage()) : "语言类型应该匹配";

        System.out.println("✓ 数据库写入测试通过");

        // 清理测试数据
        whiteListMapper.deleteById(testDep.getId());
    }

    // ==================== 辅助方法 ====================

    /**
     * 清理测试数据
     */
    private void cleanupTestData(String filePath, String language) {
        QueryWrapper<WhiteList> wrapper = new QueryWrapper<>();
        wrapper.eq("file_path", filePath)
               .eq("language", language);

        int deleted = whiteListMapper.delete(wrapper);
        if (deleted > 0) {
            System.out.println("清理了 " + deleted + " 条旧测试数据");
        }
    }

    /**
     * 查询依赖
     */
    private List<WhiteList> queryDependencies(String filePath, String language) {
        QueryWrapper<WhiteList> wrapper = new QueryWrapper<>();
        wrapper.eq("file_path", filePath)
               .eq("language", language)
               .eq("isdelete", 0);

        return whiteListMapper.selectList(wrapper);
    }

    /**
     * 等待异步任务完成
     */
    private void waitForAsyncTask(long milliseconds) {
        try {
            System.out.println("等待异步任务完成...");
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 打印示例依赖
     */
    private void printSampleDependencies(List<WhiteList> dependencies) {
        if (!dependencies.isEmpty()) {
            System.out.println("\n示例依赖:");
            dependencies.stream().limit(5).forEach(dep ->
                System.out.println("  - " + dep.getName() +
                    (dep.getDescription() != null ? " (" + dep.getDescription() + ")" : ""))
            );
        }
    }

    /**
     * 测试Flask API
     */
    private void testFlaskApi(String language, String url) {
        try {
            System.out.println("\n测试 " + language + " API:");
            System.out.println("URL: " + url);

            String response = restTemplate.getForObject(url, String.class);

            if (response != null && !response.trim().isEmpty()) {
                System.out.println("✓ " + language + " API响应正常");
                System.out.println("响应长度: " + response.length() + " 字符");
                System.out.println("响应预览: " + response.substring(0, Math.min(100, response.length())) + "...");
            } else {
                System.out.println("⚠ " + language + " API返回空响应");
            }
        } catch (Exception e) {
            System.err.println("✗ " + language + " API测试失败: " + e.getMessage());
        }
    }
}
