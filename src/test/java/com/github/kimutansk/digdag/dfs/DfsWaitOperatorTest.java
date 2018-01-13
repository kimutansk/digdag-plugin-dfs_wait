package com.github.kimutansk.digdag.dfs;

import com.google.common.collect.Lists;
import io.digdag.client.config.Config;
import io.digdag.client.config.ConfigFactory;
import io.digdag.spi.*;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static io.digdag.client.DigdagClient.objectMapper;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class DfsWaitOperatorTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private TaskRequest taskRequest;

    @Mock
    private OperatorContext operatorContext;

    private DfsWaitOperatorFactory operatorFactory;
    private Path projectPath;

    @Before
    public void setUp()
            throws Exception {
        this.projectPath = this.temporaryFolder.newFolder().toPath();
        this.operatorFactory = new DfsWaitOperatorFactory();

        when(this.operatorContext.getTaskRequest()).thenReturn(this.taskRequest);
        when(this.operatorContext.getProjectPath()).thenReturn(this.projectPath);
        when(taskRequest.getLastStateParams()).thenReturn(newConfig());
    }

    @Test
    public void testDefaultFileSystem() {
        Config config = newConfig();
        when(taskRequest.getConfig()).thenReturn(config);
        DfsWaitOperator operator = (DfsWaitOperator) this.operatorFactory.newOperator(this.operatorContext);

        FileSystem actual = operator.getFileSystemFromConfig(config);

        assertThat(actual.getClass().getCanonicalName(), is(LocalFileSystem.class.getCanonicalName()));
        assertThat(actual.getScheme(), is("file"));
    }

    @Test
    public void testEmptyTarget() {
        Config config = newConfig();
        when(taskRequest.getConfig()).thenReturn(config);
        Operator operator = this.operatorFactory.newOperator(this.operatorContext);
        TaskResult result = operator.run();

        Config expected = newConfig();
        expected
                .getNestedOrSetEmpty("dfs_wait")
                .set("file_status", Lists.<String>newArrayList());

        assertThat(result.getStoreParams(), is(expected));
    }

    @Test
    public void testExistCheck_SucceedWithNoRetry() {
        Config config = newConfig();
        String tempPath = this.temporaryFolder.getRoot().getAbsolutePath();
        config.set("_command", Lists.newArrayList(tempPath, tempPath));
        Config dfsSetting = config.getNestedOrSetEmpty("dfs_setting");
        dfsSetting.set("fs.defaultFS", "file:///");
        when(taskRequest.getConfig()).thenReturn(config);
        Operator operator = this.operatorFactory.newOperator(this.operatorContext);
        TaskResult result = operator.run();

        List<Config> resultFileStatus = result.getStoreParams().getNestedOrGetEmpty("dfs_wait").getListOrEmpty("file_status", Config.class);

        String tempPathName = this.temporaryFolder.getRoot().getName();
        assertThat(resultFileStatus.size(), is(2));
        assertThat(resultFileStatus.get(0).get("path", String.class).contains(tempPathName), is(true));
        assertThat(resultFileStatus.get(1).get("path", String.class).contains(tempPathName), is(true));
    }

    @Test
    public void testNotExistCheck_ThrowExWithRetry() {
        Config config = newConfig();
        String tempPath = Paths.get(this.temporaryFolder.getRoot().getAbsolutePath(), "not_exist").toAbsolutePath().toString();
        config.set("_command", Lists.newArrayList(tempPath));
        Config dfsSetting = config.getNestedOrSetEmpty("dfs_setting");
        dfsSetting.set("fs.defaultFS", "file:///");
        when(taskRequest.getConfig()).thenReturn(config);
        Operator operator = this.operatorFactory.newOperator(this.operatorContext);

        try {
            operator.run();
            fail(String.format("%s not  occured!", TaskExecutionException.class.getCanonicalName()));
        } catch (TaskExecutionException ex) {
            assertThat(ex.getMessage().contains("Retrying this task after"), is(true));
        }
    }

    // From
    public static final ConfigFactory configFactory = new ConfigFactory(objectMapper());

    public static Config newConfig() {
        return configFactory.create();
    }
}
