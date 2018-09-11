package com.hhly.paycore.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hhly.skeleton.base.util.ObjectUtil;

@Configuration
public class RedissonClusterConfig {
	protected final Logger logger = LoggerFactory.getLogger(RedissonClusterConfig.class);
	
	@Autowired
	private RedissonProperties redissonProperties;
	
	/**
     * 哨兵模式自动装配
     * @return
     */
    @Bean
    RedissonClient redissonClusterServer() {
        Config config = new Config();
        String nodes[] = redissonProperties.getNodes().split(",");
        String ns[] = new String[nodes.length];
        for(int i=0;i<nodes.length;i++){
        	ns[i] = "redis://"+nodes[i];
        }
        ClusterServersConfig serverConfig = config.useClusterServers().addNodeAddress(ns)
                .setConnectTimeout(redissonProperties.getConnectionTimeout())
                .setReconnectionTimeout(redissonProperties.getSoTimeout())
                .setTimeout(redissonProperties.getSoTimeout());
        
        if(!ObjectUtil.isBlank(redissonProperties.getPassword())) {
            serverConfig.setPassword(redissonProperties.getPassword());
        }
        return Redisson.create(config);
    }
}
