<!--
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
-->

<template>
  <div style="padding: 20px">
    <div style="margin-bottom: 20px">
      <h2>数据源配置</h2>
    </div>
    <el-divider />

    <div style="margin-bottom: 30px">
      <el-row style="display: flex; justify-content: space-between; align-items: center">
        <el-col :span="12">
          <h3>已绑定数据源</h3>
        </el-col>
        <el-col :span="12" style="text-align: right">
          <el-button @click="dialogVisible = true" size="large" type="primary" round :icon="Plus">
            绑定数据源
          </el-button>
        </el-col>
      </el-row>
    </div>

    <el-table :data="datasource" style="width: 100%" border @expand-change="handleExpandChange">
      <el-table-column type="expand" width="100" label="选择数据表">
        <template #default="scope">
          <div
              v-if="scope.row.status === 'active' && scope.row.testStatus === 'success'"
              style="padding: 20px; background: #f8f9fa; border-radius: 8px"
          >
            <div
                style="
                margin-bottom: 15px;
                display: flex;
                justify-content: space-between;
                align-items: center;
              "
            >
              <h4 style="margin: 0">数据表管理</h4>
              <el-button
                  @click="loadDatasourceTables(scope.row)"
                  size="small"
                  type="primary"
                  :loading="tableLoadingStates[scope.row.id]"
                  round
              >
                刷新表列表
              </el-button>
            </div>

            <div v-if="tableLists[scope.row.id] && tableLists[scope.row.id].length > 0">
              <el-checkbox-group v-model="selectedTables[scope.row.id]">
                <el-row :gutter="10">
                  <el-col
                      v-for="table in tableLists[scope.row.id]"
                      :key="table"
                      :span="6"
                      style="margin-bottom: 10px"
                  >
                    <el-checkbox :label="table" size="large">
                      {{ table }}
                    </el-checkbox>
                  </el-col>
                </el-row>
              </el-checkbox-group>

              <div style="margin-top: 20px; text-align: right">
                <el-button
                    @click="updateDatasourceTables(scope.row)"
                    size="small"
                    type="success"
                    :loading="updateLoadingStates[scope.row.id]"
                    round
                >
                  更新数据表
                </el-button>
                <el-button
                    @click="selectAllTables(scope.row)"
                    size="small"
                    type="primary"
                    round
                    plain
                >
                  全选
                </el-button>
                <el-button @click="clearAllTables(scope.row)" size="small" type="info" round plain>
                  清空
                </el-button>
              </div>
            </div>
            <div
                v-else-if="tableLoadingStates[scope.row.id]"
                style="text-align: center; padding: 20px"
            >
              <el-icon class="is-loading" style="font-size: 24px"><Loading /></el-icon>
              <div style="margin-top: 10px; color: #666">正在加载表列表...</div>
            </div>
            <div v-else style="text-align: center; padding: 20px; color: #999">
              <el-icon style="font-size: 24px"><FolderOpened /></el-icon>
              <div style="margin-top: 10px">暂无表数据，请点击刷新表列表</div>
            </div>
          </div>
          <div
              v-else-if="scope.row.status === 'active' && scope.row.testStatus !== 'success'"
              style="padding: 20px; text-align: center; color: #999">
            <el-icon style="font-size: 24px"><Lock /></el-icon>
            <div style="margin-top: 10px">连接未测试或失败，请前往顶部「数据源管理」测试连接后再管理表</div>
          </div>
          <div v-else style="padding: 20px; text-align: center; color: #999">
            <el-icon style="font-size: 24px"><Lock /></el-icon>
            <div style="margin-top: 10px">请先启用数据源以管理表</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="数据源名称" min-width="120px" />
      <el-table-column prop="type" label="数据源类型" min-width="100px" />
      <el-table-column prop="connectionUrl" label="连接地址" min-width="200px">
        <template #default="scope">
          <el-tooltip
              :content="scope.row.connectionUrl"
              placement="top"
              :disabled="!scope.row.connectionUrl || scope.row.connectionUrl.length <= 50"
          >
            <span class="connection-url-text">
              {{ scope.row.connectionUrl ? truncateText(scope.row.connectionUrl, 50) : '-' }}
            </span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="连接状态" min-width="50px">
        <template #default="scope">
          <el-tag :type="scope.row.testStatus === 'success' ? 'success' : 'danger'" round>
            {{ scope.row.testStatus === 'success' ? '连接成功' : '连接失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" min-width="40px">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'active' ? 'success' : 'info'" round>
            {{ scope.row.status === 'active' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="100px" />
      <el-table-column label="操作" min-width="120px">
        <template #default="scope">
          <el-button
              v-if="scope.row.status === 'active'"
              @click="changeDatasource(scope.row, false)"
              size="small"
              type="warning"
              round
              plain
          >
            禁用
          </el-button>
          <el-button
              v-else
              @click="changeDatasource(scope.row, true)"
              size="small"
              type="success"
              round
              plain
          >
            启用
          </el-button>
          <el-button
              @click="removeAgentDatasource(scope.row)"
              size="small"
              type="danger"
              round
              plain
          >
            移除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <!-- 绑定数据源：从已有数据源中选择并绑定到当前智能体 -->
  <el-dialog v-model="dialogVisible" title="绑定数据源" width="900px">
    <p class="dialog-tip">请从下方列表选择要绑定到当前智能体的数据源，数据源的新增/编辑/删除请在顶部「数据源管理」中操作。</p>
    <el-table
        @current-change="handleSelectDatasourceChange"
        :data="allDatasource"
        highlight-current-row
        style="width: 100%"
    >
      <el-table-column property="name" label="数据源名称" width="150" />
      <el-table-column property="type" label="数据源类型" width="100" />
      <el-table-column property="host" label="Host" width="120" />
      <el-table-column property="port" label="Port" width="80" />
      <el-table-column property="description" label="描述" min-width="200" show-overflow-tooltip />
    </el-table>
    <template #footer>
      <div style="text-align: right">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addSelectDatasource">绑定选中数据源</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted, Ref, watch } from 'vue';
import {
  Plus,
  Loading,
  FolderOpened,
  Lock,
  WarningFilled,
} from '@element-plus/icons-vue';
import datasourceService from '@/services/datasource';
import { Datasource, AgentDatasource } from '@/services/datasource';
import { ApiResponse } from '@/services/common';
import { ElMessage, ElMessageBox } from 'element-plus';
import agentDatasourceService from '@/services/agentDatasource';

export default defineComponent({
  name: 'AgentDataSourceConfig',
  props: {
    agentId: {
      type: Number,
      required: true,
    },
  },
  setup(props) {
    // 当前智能体已绑定的数据源列表
    const datasource: Ref<Datasource[]> = ref([]);
    const dialogVisible: Ref<boolean> = ref(false);
    const allDatasource: Ref<Datasource[]> = ref([]);
    const selectedDatasourceId: Ref<number | null> = ref(null);

    // 数据表管理相关状态
    const tableLists: Ref<Record<number, string[]>> = ref({});
    const selectedTables: Ref<Record<number, string[]>> = ref({});
    const tableLoadingStates: Ref<Record<number, boolean>> = ref({});
    const updateLoadingStates: Ref<Record<number, boolean>> = ref({});
    const agentDatasourceList: Ref<AgentDatasource[]> = ref([]);

    watch(dialogVisible, newValue => {
      if (newValue) loadAllDatasource();
    });

    // 初始化Agent数据源列表
    const loadAgentDatasource = async () => {
      selectedDatasourceId.value = null;
      try {
        const response = await agentDatasourceService.getAgentDatasource(props.agentId);
        agentDatasourceList.value = response || [];
        const agentDatasource: AgentDatasource[] = response || [];
        datasource.value = agentDatasource.map(item => {
          const datasourceItem = { ...item.datasource };
          datasourceItem.status = item.isActive === 1 ? 'active' : 'inactive';

          // 初始化已选择的表
          if (item.selectTables && item.datasource?.id) {
            selectedTables.value[item.datasource.id] = [...item.selectTables];
          }

          return datasourceItem;
        });
      } catch (error) {
        ElMessage.error('加载当前智能体的数据源列表失败');
        console.error('Failed to load datasource:', error);
      }
    };

    const handleSelectDatasourceChange = (value: Datasource) => {
      if (value === null || value === undefined) {
        selectedDatasourceId.value = null;
      } else {
        selectedDatasourceId.value = value.id;
      }
    };

    const loadAllDatasource = async () => {
      try {
        const response = await datasourceService.getAllDatasource();
        allDatasource.value = response || [];
      } catch (error) {
        ElMessage.error('加载数据源列表失败');
        console.error('Failed to load all datasource:', error);
      }
    };

    // 启用/禁用当前智能体的某个数据源
    const changeDatasource = async (row: Datasource, active: boolean) => {
      const datasourceId = row.id;
      try {
        const response: ApiResponse = await agentDatasourceService.toggleDatasourceForAgent(
            props.agentId,
            { datasourceId, isActive: active },
        );
        if (response.success) {
          ElMessage.success('操作成功！');
          row.status = active ? 'active' : 'inactive';
        } else {
          ElMessage.error(response.message || '操作失败！');
          console.error('Failed to change datasource:', response);
        }
      } catch (error: unknown) {
        const msg =
            (error as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message ||
            (error as Error)?.message ||
            '操作失败！';
        ElMessage.error(msg);
        console.error('Failed to change datasource:', error);
      }
    };

    // 解除数据源与当前智能体的绑定
    const removeAgentDatasource = async (row: Datasource) => {
      const datasourceId = row.id;
      try {
        await ElMessageBox.confirm('确定要解除该数据源与当前智能体的绑定吗？', '解除绑定', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        });
      } catch {
        return;
      }

      try {
        const response: ApiResponse = await agentDatasourceService.removeDatasourceFromAgent(
            props.agentId,
            datasourceId,
        );
        if (response.success) {
          ElMessage.success('移除成功！');
          datasource.value = datasource.value.filter(item => item.id !== datasourceId);
        } else {
          ElMessage.error('移除失败！');
          console.error('Failed to remove datasource:', response);
        }
      } catch (error) {
        ElMessage.error('移除失败！');
        console.error('Failed to remove datasource:', error);
      }
    };

    const addDatasourceToAgent = async (datasourceId: number) => {
      try {
        await agentDatasourceService.addDatasourceToAgent(props.agentId, datasourceId);
        await loadAgentDatasource();
        ElMessage.success('添加数据源成功');
        dialogVisible.value = false;
      } catch (error) {
        ElMessage.error('添加数据源失败');
        console.error('Failed to add datasource:', error);
      }
    };

    const addSelectDatasource = async () => {
      const datasourceId = selectedDatasourceId.value;
      if (datasourceId === null || datasourceId === undefined) {
        ElMessage.warning('请选择一个数据源');
        return;
      }
      await addDatasourceToAgent(datasourceId);
    };

    // 加载数据源的表列表
    const loadDatasourceTables = async (datasource: Datasource) => {
      if (!datasource.id) return;

      tableLoadingStates.value[datasource.id] = true;
      try {
        const tables = await datasourceService.getDatasourceTables(datasource.id);
        tableLists.value[datasource.id] = tables;

        // 如果没有初始化已选择的表，则使用当前已选择的表
        if (!selectedTables.value[datasource.id]) {
          const agentDatasource = agentDatasourceList.value.find(
              item => item.datasource?.id === datasource.id,
          );
          selectedTables.value[datasource.id] = agentDatasource?.selectTables || [];
        }

        ElMessage.success(`成功加载 ${tables.length} 个表`);
      } catch (error) {
        ElMessage.error('加载表列表失败');
        console.error('Failed to load datasource tables:', error);
      } finally {
        tableLoadingStates.value[datasource.id] = false;
      }
    };

    // 更新数据源的表列表
    const updateDatasourceTables = async (datasource: Datasource) => {
      if (!datasource.id) return;

      updateLoadingStates.value[datasource.id] = true;
      try {
        const response = await agentDatasourceService.updateDatasourceTables(
            String(props.agentId),
            {
              datasourceId: datasource.id,
              tables: selectedTables.value[datasource.id] || [],
            },
        );

        if (response.success) {
          ElMessage.success('数据表更新成功');
          // 更新本地存储的已选择表
          const agentDatasource = agentDatasourceList.value.find(
              item => item.datasource?.id === datasource.id,
          );
          if (agentDatasource) {
            agentDatasource.selectTables = [...(selectedTables.value[datasource.id] || [])];
          }
        } else {
          ElMessage.error('数据表更新失败');
        }
      } catch (error) {
        ElMessage.error('数据表更新失败');
        console.error('Failed to update datasource tables:', error);
      } finally {
        updateLoadingStates.value[datasource.id] = false;
      }
    };

    // 全选表
    const selectAllTables = (datasource: Datasource) => {
      if (!datasource.id || !tableLists.value[datasource.id]) return;
      selectedTables.value[datasource.id] = [...tableLists.value[datasource.id]];
    };

    // 清空选择的表
    const clearAllTables = (datasource: Datasource) => {
      if (!datasource.id) return;
      selectedTables.value[datasource.id] = [];
    };

    // 文本截断函数
    const truncateText = (text: string, maxLength: number): string => {
      if (!text || text.length <= maxLength) {
        return text;
      }
      return text.substring(0, maxLength) + '...';
    };

    // 处理表格展开事件：仅当已启用且连接成功时自动加载表列表
    const handleExpandChange = (row: Datasource, expandedRows: Datasource[]) => {
      if (
          expandedRows.includes(row) &&
          row.status === 'active' &&
          row.testStatus === 'success' &&
          row.id
      ) {
        loadDatasourceTables(row);
      }
    };

    onMounted(() => {
      loadAgentDatasource();
    });

    return {
      props,
      Plus,
      Loading,
      FolderOpened,
      Lock,
      WarningFilled,
      datasource,
      dialogVisible,
      allDatasource,
      tableLists,
      selectedTables,
      tableLoadingStates,
      updateLoadingStates,
      changeDatasource,
      removeAgentDatasource,
      loadAllDatasource,
      addSelectDatasource,
      handleSelectDatasourceChange,
      loadDatasourceTables,
      updateDatasourceTables,
      selectAllTables,
      clearAllTables,
      truncateText,
      handleExpandChange,
    };
  },
});
</script>

<style scoped>
.dialog-tip {
  margin: 0 0 16px 0;
  font-size: 13px;
  color: #666;
  line-height: 1.5;
}
.connection-url-text {
  font-family: monospace;
  font-size: 12px;
}
</style>
