package com.github.kimutansk.digdag.dfs;

import static org.apache.hadoop.security.UserGroupInformation.AuthenticationMethod.KERBEROS;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.digdag.client.config.Config;
import io.digdag.client.config.ConfigException;
import io.digdag.client.config.ConfigKey;
import io.digdag.spi.OperatorContext;
import io.digdag.spi.TaskResult;
import io.digdag.standards.operator.DurationInterval;
import io.digdag.standards.operator.state.PollingRetryExecutor;
import io.digdag.standards.operator.state.TaskState;
import io.digdag.util.BaseOperator;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.SecurityUtil;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class DfsWaitOperator extends BaseOperator {
    private static final DurationInterval POLL_INTERVAL = DurationInterval.of(Duration.ofSeconds(5), Duration.ofMinutes(5));

    private final TaskState state;

    DfsWaitOperator(OperatorContext context) {
        super(context);
        this.state = TaskState.of(super.request);
    }

    @Override
    public TaskResult runTask() {
        ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            // This operator uses dependency package's "META-INF/services/org.apache.hadoop.fs.FileSystem" for service loading.
            // Set DfsWaitPlugin.class's classloader() to current thread temporally.
            Thread.currentThread().setContextClassLoader(DfsWaitPlugin.class.getClassLoader());
            return runTaskIn();
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }

    private TaskResult runTaskIn() {
        Config params = super.request.getConfig();

        List<String> checkTargets = params.getListOrEmpty("_command", String.class);
        FileSystem fileSystem = getFileSystemFromConfig(params);

        List<FileStatus> result = PollingRetryExecutor.pollingRetryExecutor(this.state, "EXISTS")
                .withRetryInterval(POLL_INTERVAL)
                .withErrorMessage("Target object in '%s' does not yet exist.", fileSystem.getUri())
                .retryIf(ex -> ex instanceof FileNotFoundException)
                .run(pollState -> {
                    List<FileStatus> checkResult = Lists.newArrayList();
                    // Lambda clause force exception check, so without lambda clause.
                    for (String target : checkTargets) {
                        checkResult.add(fileSystem.getFileStatus(new Path(target)));
                    }
                    return checkResult;
                });

        return TaskResult.defaultBuilder(super.request)
                .resetStoreParams(ImmutableList.of(ConfigKey.of("dfs_wait", "last_objects")))
                .storeParams(createResult(result)).build();
    }

    private Config createResult(List<FileStatus> results) {
        Config params = super.request.getConfig().getFactory().create();
        Config object = params.getNestedOrSetEmpty("dfs_wait");
        object.set("file_status", results.stream().map(DfsWaitOperator::createFileStatusResult).collect(Collectors.toList()));
        return params;
    }

    private static Map<String, Object> createFileStatusResult(FileStatus fileStatus) {
        Map<String, Object> result = Maps.newHashMap();
        result.put("path", fileStatus.getPath().toString());
        result.put("isDirectory", fileStatus.isDirectory());
        result.put("len", fileStatus.getLen());
        result.put("modificationTime", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(fileStatus.getModificationTime()), ZoneId.systemDefault())));
        result.put("owner", fileStatus.getOwner());
        result.put("group", fileStatus.getGroup());
        result.put("permission", fileStatus.getPermission().toString());
        return result;
    }

    @VisibleForTesting
    FileSystem getFileSystemFromConfig(Config digdagConf) {
        Configuration hadoopConf = new Configuration();
        Config dfsSetting = digdagConf.getNestedOrSetEmpty("dfs_setting");
        dfsSetting.getKeys().forEach(key -> hadoopConf.set(key, dfsSetting.get(key, String.class)));

        UserGroupInformation.AuthenticationMethod authMethod;

        try {
            authMethod = SecurityUtil.getAuthenticationMethod(hadoopConf);
        } catch (IllegalArgumentException ex) {
            throw new ConfigException(ex);
        }

        Optional<String> keytab = dfsSetting.getOptional("keytab", String.class);
        Optional<String> principal = dfsSetting.getOptional("principal", String.class);

        if (authMethod == KERBEROS && (!keytab.isPresent() || !principal.isPresent())) {
            throw new ConfigException("The dfs_wait operator both and 'dfs_setting : keytab' and 'dfs_setting : principal' parameters must be set with kerberos authentication.");
        }

        if (authMethod == KERBEROS) {
            UserGroupInformation.setConfiguration(hadoopConf);
            try {
                SecurityUtil.login(hadoopConf, "keytab", "principal");
            } catch (IOException ex) {
                throw new ConfigException(ex);
            }
        }

        FileSystem fs;

        try {
            fs = FileSystem.get(hadoopConf);
        } catch (IOException ex) {
            throw new ConfigException(ex);
        }

        return fs;
    }
}
