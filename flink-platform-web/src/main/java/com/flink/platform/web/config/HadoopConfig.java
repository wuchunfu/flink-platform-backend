package com.flink.platform.web.config;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.FileSystem;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static com.flink.platform.common.constants.Constant.SLASH;

/** create hdfs instance. */
@Slf4j
@Setter
@Configuration
@ConfigurationProperties(prefix = "hadoop")
public class HadoopConfig {

    private String username;

    private String localDirName;

    private Map<String, String> properties;

    @Bean("hdfsFileSystem")
    public FileSystem createHdfsFileSystem() throws Exception {
        System.setProperty("HADOOP_USER_NAME", username);
        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
        properties.forEach(conf::set);
        log.info("=============== [hadoop configuration info start.] ===============");
        log.info("[hadoop conf]: size:{}, {}", conf.size(), conf);
        log.info("[fs.defaultFS]: {}", conf.get("fs.defaultFS"));
        log.info("[fs.hdfs.impl]: {}", conf.get("fs.hdfs.impl"));
        FileSystem fs = FileSystem.newInstance(conf);
        log.info("[fileSystem scheme]: {}", fs.getScheme());
        log.info("=============== [hadoop configuration info end.] ===============");
        return fs;
    }

    @Bean("localDataDir")
    public Path createDataDir() {
        String rootDir = System.getProperty("user.dir");
        String dataDir = rootDir + SLASH + localDirName;
        Path path = Paths.get(dataDir);
        File file = path.toFile();
        if (!file.exists()) {
            if (file.mkdir()) {
                log.info("data dir: {} created successfully.", dataDir);
            } else {
                throw new RuntimeException("create local data dir failed.");
            }
        } else {
            log.info("data dir: {} already exists", dataDir);
        }
        return path;
    }
}
