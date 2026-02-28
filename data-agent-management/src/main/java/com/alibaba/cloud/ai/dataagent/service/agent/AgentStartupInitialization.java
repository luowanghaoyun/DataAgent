/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.dataagent.service.agent;

import com.alibaba.cloud.ai.dataagent.entity.Agent;
import com.alibaba.cloud.ai.dataagent.entity.AgentDatasource;
import com.alibaba.cloud.ai.dataagent.service.datasource.AgentDatasourceService;
import com.alibaba.cloud.ai.dataagent.service.datasource.DatasourceService;
import com.alibaba.cloud.ai.dataagent.service.vectorstore.AgentVectorStoreService;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentStartupInitialization implements ApplicationRunner, DisposableBean {

	private final AgentService agentService;

	private final AgentVectorStoreService agentVectorStoreService;

	private final AgentDatasourceService agentDatasourceService;

	private final DatasourceService datasourceService;

	private final ExecutorService executorService;

	@Override
	public void run(ApplicationArguments args) {
		log.info("Starting automatic initialization of published agents...");

		try {
			// 因为异步可以让初始化过程在后台运行，不会阻塞Spring启动主线程，提高启动速度和响应性；即使初始化很耗时也不会影响主程序正常启动。
			CompletableFuture.runAsync(this::initializePublishedAgents, executorService).exceptionally(throwable -> {
				log.error("Error during agent initialization: {}", throwable.getMessage());
				return null;
			});

		}
		catch (Exception e) {
			log.error("Failed to start agent initialization process", e);
		}
	}

	/** 按数据源维度初始化：收集已发布智能体的活跃数据源，对每个数据源全表初始化一次（所有智能体共享） */
	private void initializePublishedAgents() {
		try {
			List<Agent> publishedAgents = agentService.findByStatus("published");

			if (publishedAgents.isEmpty()) {
				log.info("No published agents found, skipping initialization");
				return;
			}

			// 收集所有已发布智能体的活跃数据源 ID（去重）
			Set<Integer> datasourceIds = publishedAgents.stream().map(agent -> {
				try {
					AgentDatasource ad = agentDatasourceService.getCurrentAgentDatasource(agent.getId());
					return ad != null ? ad.getDatasourceId() : null;
				}
				catch (Exception e) {
					return null;
				}
			}).filter(Objects::nonNull).collect(Collectors.toSet());

			if (datasourceIds.isEmpty()) {
				log.info("No active datasources found for published agents, skipping initialization");
				return;
			}

			log.info("Found {} distinct datasource(s) to initialize for published agents", datasourceIds.size());

			int successCount = 0;
			int failureCount = 0;

			for (Integer datasourceId : datasourceIds) {
				try {
					if (isDatasourceAlreadyInitialized(datasourceId)) {
						log.info("Datasource {} already has vector data, skipping", datasourceId);
						successCount++;
						continue;
					}
					boolean result = datasourceService.initializeSchemaForDatasource(datasourceId);
					if (result) {
						successCount++;
						log.info("Successfully initialized schema for datasource: {}", datasourceId);
					}
					else {
						failureCount++;
					}
				}
				catch (Exception e) {
					failureCount++;
					log.error("Error initializing datasource: {}, reason: {}", datasourceId, e.getMessage());
				}

				try {
					Thread.sleep(1000);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}

			log.info("Datasource schema initialization completed. Success: {}, Failed: {}, Total: {}", successCount,
					failureCount, datasourceIds.size());

		}
		catch (Exception e) {
			log.error("Error during published agents datasource initialization", e);
		}
	}

	private boolean isDatasourceAlreadyInitialized(Integer datasourceId) {
		try {
			return agentVectorStoreService.hasDocumentsByDataSourceId(String.valueOf(datasourceId));
		}
		catch (Exception e) {
			log.error("Failed to check initialization status for datasource: {}, assuming not initialized",
					datasourceId, e);
			return false;
		}
	}

	/**
	 * Clean up resources when the application shuts down. Implement the destroy method of
	 * the DisposableBean interface
	 */
	@Override
	public void destroy() {
		if (!executorService.isShutdown()) {
			log.info("Shutting down agent initialization executor service");
			executorService.shutdown();
		}
	}

}
