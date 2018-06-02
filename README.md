# digdag-plugin-dfs_wait

Digdag `dfs_wait>` operator plugin to wait for files on DFS.

## dig file configuration example

- Comment

dfs_wait operator set under dfs_setting configuration parameters to Hadoop Configuration object.  
So if you would like to use other Hadoop Configuration parameters, add parameters to under dfs_setting.

##### dfs_wait plugin use common settings.

```yaml
_export:
  plugin:
    repositories:
      - https://jitpack.io
    dependencies:
      - com.github.kimutansk:digdag-plugin-dfs_wait:0.1.0 # Modify latest version.
```

##### local file exist check

```yaml
+step1:
  dfs_wait>: 
  - /home/digdag/exist_check_1
  - /home/digdag/exist_check_2
```

##### HDFS file exist check

```yaml
+step1:
  dfs_wait>:
  - /user/digdag/exist_check_1
  - /user/digdag/exist_check_2
  dfs_setting:
    fs.defaultFS: "hdfs://namenode01.hadoop.kimutansk.github.com:8020"
```

##### High Availability using QJM Hadoop Cluster' HDFS file exist check

```yaml
+step1:
  dfs_wait>:
  - /user/digdag/exist_check_1
  - /user/digdag/exist_check_2
  dfs_setting:
    fs.defaultFS: hdfs://HadoopHACluster
    dfs.nameservices: HadoopHACluster
    dfs.client.failover.proxy.provider.HadoopHACluster: org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider
    dfs.ha.automatic-failover.enabled.HadoopHACluster: true
    ha.zookeeper.quorum: "namenode01.hadoop.kimutansk.github.com:2181,namenode02.hadoop.kimutansk.github.com:2181,namenode03.hadoop.kimutansk.github.com:2181"
    dfs.ha.namenodes.HadoopHACluster: "namenode1,namenode2"
    dfs.namenode.rpc-address.HadoopHACluster.namenode1: "namenode01.hadoop.kimutansk.github.com:8020"
    dfs.namenode.rpc-address.HadoopHACluster.namenode2: "namenode02.hadoop.kimutansk.github.com:8020"
```

##### Kerberized Hadoop Cluster's HDFS file exist check

```yaml
+step1:
  dfs_wait>:
  - /user/digdag/exist_check_1
  - /user/digdag/exist_check_2
  dfs_setting:
    fs.defaultFS: "hdfs://namenode01.hadoop.kimutansk.github.com:8020"
    hadoop.security.authentication: kerberos
    dfs.namenode.kerberos.principal: hdfs/_HOST@HADOOP.KIMUTANSK.GITHUB.COM
    keytab: /home/digdag/conf/digdag.keytab
    principal: digdag@HADOOP.KIMUTANSK.GITHUB.COM
```

##### Kerberized and High Availability using QJM Hadoop Cluster' HDFS file exist check

```yaml
+step1:
  dfs_wait>:
  - /user/digdag/exist_check_1
  - /user/digdag/exist_check_2
  dfs_setting:
    fs.defaultFS: hdfs://HadoopHACluster
    hadoop.security.authentication: kerberos
    dfs.namenode.kerberos.principal: hdfs/_HOST@HADOOP.KIMUTANSK.GITHUB.COM
    keytab: /home/digdag/conf/digdag.keytab
    principal: digdag@HADOOP.KIMUTANSK.GITHUB.COM
    dfs.nameservices: HadoopHACluster
    dfs.client.failover.proxy.provider.HadoopHACluster: org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider
    dfs.ha.automatic-failover.enabled.HadoopHACluster: true
    ha.zookeeper.quorum: "namenode01.hadoop.kimutansk.github.com:2181,namenode02.hadoop.kimutansk.github.com:2181,namenode03.hadoop.kimutansk.github.com:2181"
    dfs.ha.namenodes.HadoopHACluster: "namenode1,namenode2"
    dfs.namenode.rpc-address.HadoopHACluster.namenode1: "namenode01.hadoop.kimutansk.github.com:8020"
    dfs.namenode.rpc-address.HadoopHACluster.namenode2: "namenode02.hadoop.kimutansk.github.com:8020"
```

## Output Parameters

##### dfs_wait.file_status

```json
[
    {
        "owner": "digdag",
        "path": "hdfs://HadoopHACluster/user/digdag/exist_check_1",
        "len": 0,
        "modificationTime": "2018-01-12T17:55:38.47+09:00",
        "permission": "rwxr-xr-x",
        "isDirectory": true,
        "group": "digdag"
    },
    {
        "owner": "digdag",
        "path": "hdfs://HadoopHACluster/user/digdag/exist_check_2",
        "len": 65535,
        "modificationTime": "2018-01-12T17:55:38.371+09:00",
        "permission": "rw-r--r--",
        "isDirectory": false,
        "group": "digdag"
    }
]
```

## Development

### 1) build

```sh
./gradlew publish
```

Artifacts are build on local repos: `./build/repo`.

### 2) run an example

```sh
rm -rf sample/.digdag/plugin 
digdag run -a --project sample plugin.dig -p repos=`pwd`/build/repo
```

### License

- License: Apache License, Version 2.0
