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
  <BaseLayout>
    <div class="datasource-manage-page">
      <main class="main-content">
        <div class="content-header">
          <div class="header-info">
            <h1 class="content-title">数据源管理</h1>
            <p class="content-subtitle">配置和管理数据源，初始化向量后所有智能体共享该数据源的全量表</p>
          </div>
        </div>

        <div class="action-section">
          <el-card>
            <div class="action-content">
              <div class="action-buttons">
                <el-button type="primary" :icon="Plus" @click="showAddDialog" size="large">
                  新增数据源
                </el-button>
                <el-button :icon="Refresh" @click="loadDatasources" size="large">刷新</el-button>
              </div>
            </div>
          </el-card>
        </div>

        <div class="config-table" v-if="!loading">
          <el-card>
            <el-table :data="datasources" style="width: 100%" stripe>
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="name" label="数据源名称" min-width="140" />
              <el-table-column prop="type" label="类型" width="100">
                <template #default="scope">
                  <el-tag size="small">{{ scope.row.type || '-' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="connectionUrl" label="连接地址" min-width="200" show-overflow-tooltip />
              <el-table-column label="连接状态" width="100">
                <template #default="scope">
                  <el-tag :type="scope.row.testStatus === 'success' ? 'success' : 'info'" size="small">
                    {{ scope.row.testStatus === 'success' ? '成功' : '未测' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" width="160" />
              <el-table-column label="操作" width="480" fixed="right">
                <template #default="scope">
                  <div class="action-buttons-cell">
                    <el-button
                      type="primary"
                      size="small"
                      @click="handleTestConnection(scope.row)"
                      :loading="testingId === scope.row.id"
                    >
                      测试连接
                    </el-button>
                    <el-button
                      type="success"
                      size="small"
                      @click="handleInitSchema(scope.row)"
                      :loading="initLoadingId === scope.row.id"
                    >
                      初始化向量
                    </el-button>
                    <el-button
                      type="warning"
                      size="small"
                      @click="openForeignKeyDialog(scope.row)"
                    >
                      <el-icon style="margin-right: 4px"><Link /></el-icon>
                      逻辑外键配置
                    </el-button>
                    <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
                    <el-button type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>

        <div v-if="loading" class="loading-state">
          <el-skeleton :rows="6" animated />
        </div>

        <div v-if="!loading && datasources.length === 0" class="empty-state">
          <el-empty description="暂无数据源">
            <template #image>
              <el-icon size="60"><Connection /></el-icon>
            </template>
            <el-button type="primary" :icon="Plus" @click="showAddDialog">新增数据源</el-button>
          </el-empty>
        </div>
      </main>

      <!-- 新增数据源对话框 -->
      <el-dialog
        v-model="addDialogVisible"
        title="新增数据源"
        width="640px"
        :close-on-click-modal="false"
      >
        <el-form :model="formData" label-width="120px" label-position="left">
          <el-form-item label="数据源名称" required>
            <el-input v-model="formData.name" placeholder="请输入数据源名称" />
          </el-form-item>
          <el-form-item label="数据源类型" required>
            <el-select v-model="formData.type" placeholder="请选择" style="width: 100%">
              <el-option
                v-for="type in datasourceTypes"
                :key="type.typeName"
                :label="type.displayName"
                :value="type.typeName"
              />
            </el-select>
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="主机地址" required>
                <el-input v-model="formData.host" placeholder="localhost 或 IP" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="端口" required>
                <el-input-number v-model="formData.port" :min="1" :max="65535" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="数据库名" required>
            <el-input v-model="formData.databaseName" placeholder="数据库名称" />
          </el-form-item>
          <el-form-item
            v-if="formData.type === 'postgresql' || formData.type === 'oracle'"
            label="Schema"
            required
          >
            <el-input v-model="formData.schemaName" :placeholder="formData.type === 'postgresql' ? '如 public' : '如 SYSTEM'" />
          </el-form-item>
          <el-form-item label="连接地址">
            <el-input v-model="formData.connectionUrl" placeholder="可选，不填则自动生成" />
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="用户名" required>
                <el-input v-model="formData.username" placeholder="数据库用户名" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="密码" required>
                <el-input v-model="formData.password" type="password" show-password placeholder="数据库密码" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="描述">
            <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="可选" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="addDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleAddSubmit" :loading="submitting">创建</el-button>
        </template>
      </el-dialog>

      <!-- 编辑数据源对话框 -->
      <el-dialog
        v-model="editDialogVisible"
        title="编辑数据源"
        width="640px"
        :close-on-click-modal="false"
      >
        <el-form :model="editFormData" label-width="120px" label-position="left">
          <el-form-item label="数据源名称" required>
            <el-input v-model="editFormData.name" placeholder="请输入数据源名称" />
          </el-form-item>
          <el-form-item label="数据源类型" required>
            <el-select v-model="editFormData.type" placeholder="请选择" style="width: 100%">
              <el-option
                v-for="type in datasourceTypes"
                :key="type.typeName"
                :label="type.displayName"
                :value="type.typeName"
              />
            </el-select>
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="主机地址" required>
                <el-input v-model="editFormData.host" placeholder="localhost 或 IP" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="端口" required>
                <el-input-number v-model="editFormData.port" :min="1" :max="65535" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="数据库名" required>
            <el-input v-model="editFormData.databaseName" placeholder="数据库名称" />
          </el-form-item>
          <el-form-item
            v-if="editFormData.type === 'postgresql' || editFormData.type === 'oracle'"
            label="Schema"
            required
          >
            <el-input v-model="editFormData.schemaName" :placeholder="editFormData.type === 'postgresql' ? '如 public' : '如 SYSTEM'" />
          </el-form-item>
          <el-form-item label="连接地址">
            <el-input v-model="editFormData.connectionUrl" placeholder="可选" />
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="用户名" required>
                <el-input v-model="editFormData.username" placeholder="数据库用户名" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="密码" required>
                <el-input v-model="editFormData.password" type="password" show-password placeholder="留空则不修改" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="描述">
            <el-input v-model="editFormData.description" type="textarea" :rows="3" placeholder="可选" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleEditSubmit" :loading="submitting">保存</el-button>
        </template>
      </el-dialog>

      <!-- 逻辑外键配置 Dialog -->
      <el-dialog
        v-model="foreignKeyDialogVisible"
        title="逻辑外键配置"
        width="900px"
        :close-on-click-modal="false"
      >
        <div v-if="currentForeignKeyDatasource">
          <div class="fk-datasource-tip">
            当前配置数据源：<strong>{{ currentForeignKeyDatasource.name }}</strong>
          </div>

          <div class="fk-section">
            <h4 class="fk-section-title">已生效的逻辑外键 (Logical Foreign Keys)</h4>
            <el-table :data="foreignKeyList" border style="width: 100%" size="small">
              <el-table-column prop="sourceTableName" label="主表 (Source)" min-width="100">
                <template #default="scope">
                  <span class="mono primary">{{ scope.row.sourceTableName }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="sourceColumnName" label="字段" min-width="80">
                <template #default="scope">
                  <span class="mono">{{ scope.row.sourceColumnName }}</span>
                </template>
              </el-table-column>
              <el-table-column label="关系类型" min-width="90" align="center">
                <template #default="scope">
                  <el-icon class="link-icon"><Link /></el-icon>
                  <span class="mono">{{ scope.row.relationType || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="targetTableName" label="关联表 (Target)" min-width="100">
                <template #default="scope">
                  <span class="mono success">{{ scope.row.targetTableName }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="targetColumnName" label="字段" min-width="80">
                <template #default="scope">
                  <span class="mono">{{ scope.row.targetColumnName }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="描述" min-width="120" />
              <el-table-column label="操作" width="140" align="right">
                <template #default="scope">
                  <el-button @click="editForeignKey(scope.row)" size="small" type="primary" link>编辑</el-button>
                  <el-button @click="deleteForeignKey(scope.row, scope.$index)" size="small" type="danger" link>删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div v-if="!foreignKeyList.length" class="fk-empty">
              <el-icon size="32"><FolderOpened /></el-icon>
              <div>暂无逻辑外键配置</div>
            </div>
          </div>

          <div class="fk-form-section">
            <h4 class="fk-form-title">
              <el-icon><CirclePlus v-if="!editingForeignKey" /><Edit v-else /></el-icon>
              {{ editingForeignKey ? '编辑关联关系' : '新增关联关系' }}
            </h4>
            <el-row :gutter="10">
              <el-col :span="5">
                <label class="fk-label">主表 (Left Table)</label>
                <el-select
                  v-model="newForeignKey.sourceTableName"
                  placeholder="请选择表..."
                  style="width: 100%"
                  size="large"
                  @change="handleSourceTableChange"
                  clearable
                  filterable
                >
                  <el-option v-for="table in tableList" :key="table" :label="table" :value="table" />
                </el-select>
              </el-col>
              <el-col :span="4">
                <label class="fk-label">字段</label>
                <el-select
                  v-model="newForeignKey.sourceColumnName"
                  placeholder="先选表"
                  style="width: 100%"
                  size="large"
                  :disabled="!newForeignKey.sourceTableName"
                  clearable
                  filterable
                >
                  <el-option v-for="col in sourceColumnList" :key="col" :label="col" :value="col" />
                </el-select>
              </el-col>
              <el-col :span="1" class="fk-arrow"><el-icon :size="20"><Right /></el-icon></el-col>
              <el-col :span="5">
                <label class="fk-label">关联表 (Right Table)</label>
                <el-select
                  v-model="newForeignKey.targetTableName"
                  placeholder="请选择表..."
                  style="width: 100%"
                  size="large"
                  @change="handleTargetTableChange"
                  clearable
                  filterable
                >
                  <el-option v-for="table in tableList" :key="table" :label="table" :value="table" />
                </el-select>
              </el-col>
              <el-col :span="4">
                <label class="fk-label">字段</label>
                <el-select
                  v-model="newForeignKey.targetColumnName"
                  placeholder="先选表"
                  style="width: 100%"
                  size="large"
                  :disabled="!newForeignKey.targetTableName"
                  clearable
                  filterable
                >
                  <el-option v-for="col in targetColumnList" :key="col" :label="col" :value="col" />
                </el-select>
              </el-col>
              <el-col :span="5" class="fk-btn-col">
                <el-button @click="saveOrUpdateForeignKey" type="primary" size="large" style="width: 100%">
                  <el-icon style="margin-right: 4px"><Check /></el-icon>
                  {{ editingForeignKey ? '更新' : '添加' }}
                </el-button>
              </el-col>
            </el-row>
            <el-row style="margin-top: 10px">
              <el-col :span="24">
                <label class="fk-label">关系类型 (Relation Type)</label>
                <el-select v-model="newForeignKey.relationType" placeholder="选择关系类型（可选）" size="large" clearable style="width: 100%">
                  <el-option label="1:1 (一对一)" value="1:1" />
                  <el-option label="1:N (一对多)" value="1:N" />
                  <el-option label="N:1 (多对一)" value="N:1" />
                </el-select>
              </el-col>
            </el-row>
            <el-row style="margin-top: 10px">
              <el-col :span="24">
                <el-input
                  v-model="newForeignKey.description"
                  placeholder="描述（可选）：例如 '订单关联用户'"
                  size="large"
                  clearable
                />
              </el-col>
            </el-row>
          </div>
        </div>
        <template #footer>
          <el-button @click="foreignKeyDialogVisible = false" size="large">取消</el-button>
          <el-button type="primary" @click="saveForeignKeyConfig" size="large" :loading="savingForeignKeys">
            保存全部配置
          </el-button>
        </template>
      </el-dialog>
    </div>
  </BaseLayout>
</template>

<script lang="ts">
  import { defineComponent, ref, reactive, onMounted } from 'vue';
  import { ElMessage, ElMessageBox } from 'element-plus';
  import { Plus, Refresh, Connection, Link, CirclePlus, Check, Right, Edit, FolderOpened } from '@element-plus/icons-vue';
  import BaseLayout from '@/layouts/BaseLayout.vue';
  import datasourceService, { type Datasource, type DatasourceType } from '@/services/datasource';
  import logicalRelationService, { type LogicalRelation } from '@/services/logicalRelation';

  interface FormData {
    name: string;
    type: string;
    host: string;
    port: number;
    databaseName: string;
    schemaName: string;
    connectionUrl: string;
    username: string;
    password: string;
    description: string;
  }

  function toFormData(d: Datasource | null): FormData {
    if (!d) {
      return {
        name: '',
        type: 'mysql',
        host: '',
        port: 3306,
        databaseName: '',
        schemaName: '',
        connectionUrl: '',
        username: '',
        password: '',
        description: '',
      };
    }
    const parts = (d.databaseName || '').split('|');
    return {
      name: d.name || '',
      type: d.type || 'mysql',
      host: d.host || '',
      port: d.port ?? 3306,
      databaseName: parts[0] || '',
      schemaName: parts.length === 2 ? parts[1] : '',
      connectionUrl: d.connectionUrl || '',
      username: d.username || '',
      password: d.password || '',
      description: d.description || '',
    };
  }

  function toDatasource(f: FormData, id?: number): Datasource {
    const dbName =
      (f.type === 'postgresql' || f.type === 'oracle') && f.schemaName
        ? `${f.databaseName}|${f.schemaName}`
        : f.databaseName;
    const ds: Datasource = {
      name: f.name,
      type: f.type,
      host: f.host,
      port: f.port,
      databaseName: dbName,
      connectionUrl: f.connectionUrl || undefined,
      username: f.username,
      password: f.password,
      description: f.description || undefined,
    };
    if (id != null) ds.id = id;
    return ds;
  }

  const emptyNewForeignKey = (): LogicalRelation => ({
    sourceTableName: '',
    sourceColumnName: '',
    targetTableName: '',
    targetColumnName: '',
    relationType: '',
    description: '',
  });

  export default defineComponent({
    name: 'DataSourceManage',
    components: { BaseLayout, Connection, Link, CirclePlus, Check, Right, Edit, FolderOpened },
    setup() {
      const loading = ref(true);
      const datasources = ref<Datasource[]>([]);
      const addDialogVisible = ref(false);
      const editDialogVisible = ref(false);
      const submitting = ref(false);
      const testingId = ref<number | null>(null);
      const initLoadingId = ref<number | null>(null);

      const formData = reactive<FormData>(toFormData(null));
      const editFormData = reactive<FormData>(toFormData(null));
      let editingId: number | null = null;

      // 逻辑外键管理
      const foreignKeyDialogVisible = ref(false);
      const currentForeignKeyDatasource = ref<Datasource | null>(null);
      const foreignKeyList = ref<LogicalRelation[]>([]);
      const editingForeignKey = ref<LogicalRelation | null>(null);
      const newForeignKey = ref<LogicalRelation>(emptyNewForeignKey());
      const tableList = ref<string[]>([]);
      const sourceColumnList = ref<string[]>([]);
      const targetColumnList = ref<string[]>([]);
      const savingForeignKeys = ref(false);

      // 数据源类型列表（含 Hive 等，由后端 /api/datasource/types 返回）
      const datasourceTypes = ref<DatasourceType[]>([]);

      const loadDatasources = async () => {
        loading.value = true;
        try {
          const list = await datasourceService.getAllDatasource();
          datasources.value = list || [];
        } catch (e) {
          ElMessage.error('加载数据源列表失败');
          datasources.value = [];
        } finally {
          loading.value = false;
        }
      };

      const loadDatasourceTypes = async () => {
        try {
          const res = await datasourceService.getDatasourceTypes();
          if (res.success && res.data && res.data.length) {
            datasourceTypes.value = res.data;
          }
        } catch (e) {
          ElMessage.error('加载数据源类型失败');
        }
      };

      const showAddDialog = () => {
        Object.assign(formData, toFormData(null));
        formData.port = 3306;
        loadDatasourceTypes();
        addDialogVisible.value = true;
      };

      const handleAddSubmit = async () => {
        if (!formData.name?.trim() || !formData.type || !formData.host?.trim() || !formData.databaseName?.trim()) {
          ElMessage.warning('请填写名称、类型、主机、数据库名');
          return;
        }
        if ((formData.type === 'postgresql' || formData.type === 'oracle') && !formData.schemaName?.trim()) {
          ElMessage.warning('PostgreSQL/Oracle 请填写 Schema');
          return;
        }
        if (!formData.username?.trim() || !formData.password) {
          ElMessage.warning('请填写用户名和密码');
          return;
        }
        submitting.value = true;
        try {
          await datasourceService.createDatasource(toDatasource(formData));
          ElMessage.success('创建成功');
          addDialogVisible.value = false;
          loadDatasources();
        } catch (e) {
          ElMessage.error('创建失败');
        } finally {
          submitting.value = false;
        }
      };

      const handleEdit = (row: Datasource) => {
        editingId = row.id ?? null;
        Object.assign(editFormData, toFormData(row));
        loadDatasourceTypes();
        editDialogVisible.value = true;
      };

      const handleEditSubmit = async () => {
        if (editingId == null) return;
        if (!editFormData.name?.trim() || !editFormData.type || !editFormData.host?.trim() || !editFormData.databaseName?.trim()) {
          ElMessage.warning('请填写名称、类型、主机、数据库名');
          return;
        }
        if ((editFormData.type === 'postgresql' || editFormData.type === 'oracle') && !editFormData.schemaName?.trim()) {
          ElMessage.warning('PostgreSQL/Oracle 请填写 Schema');
          return;
        }
        if (!editFormData.username?.trim()) {
          ElMessage.warning('请填写用户名');
          return;
        }
        submitting.value = true;
        try {
          await datasourceService.updateDatasource(editingId, toDatasource(editFormData, editingId));
          ElMessage.success('保存成功');
          editDialogVisible.value = false;
          loadDatasources();
        } catch (e) {
          ElMessage.error('保存失败');
        } finally {
          submitting.value = false;
        }
      };

      const handleDelete = async (row: Datasource) => {
        const id = row.id;
        if (id == null) return;
        try {
          await ElMessageBox.confirm('删除后无法恢复，确定要删除该数据源吗？', '确认删除', {
            confirmButtonText: '删除',
            cancelButtonText: '取消',
            type: 'warning',
          });
        } catch {
          return;
        }
        try {
          const res = await datasourceService.deleteDatasource(id);
          if (res.success) {
            ElMessage.success('删除成功');
            loadDatasources();
          } else {
            ElMessage.error(res.message || '删除失败');
          }
        } catch (e) {
          ElMessage.error('删除失败');
        }
      };

      const handleTestConnection = async (row: Datasource) => {
        const id = row.id;
        if (id == null) return;
        testingId.value = id;
        try {
          const res = await datasourceService.testConnection(id);
          if (res.success) {
            ElMessage.success(res.message || '连接成功');
            loadDatasources();
          } else {
            ElMessage.error(res.message || '连接失败');
          }
        } catch (e) {
          ElMessage.error('测试连接失败');
        } finally {
          testingId.value = null;
        }
      };

      const handleInitSchema = async (row: Datasource) => {
        const id = row.id;
        if (id == null) return;
        initLoadingId.value = id;
        try {
          const res = await datasourceService.initSchema(id);
          if (res.success) {
            ElMessage.success('向量初始化成功，该数据源下全部表已向量化，所有智能体共享');
          } else {
            ElMessage.error(res.message || '初始化失败');
          }
        } catch (e) {
          ElMessage.error('初始化失败');
        } finally {
          initLoadingId.value = null;
        }
      };

      // ---------- 逻辑外键 ----------
      const openForeignKeyDialog = async (row: Datasource) => {
        if (!row.id) {
          ElMessage.warning('数据源ID不存在');
          return;
        }
        currentForeignKeyDatasource.value = row;
        foreignKeyDialogVisible.value = true;
        try {
          tableList.value = await datasourceService.getDatasourceTables(row.id);
        } catch (e) {
          ElMessage.error('加载表列表失败');
        }
        try {
          foreignKeyList.value = await logicalRelationService.getLogicalRelations(row.id);
        } catch (e) {
          ElMessage.error('加载逻辑外键列表失败');
        }
        resetForeignKeyForm();
      };

      const handleSourceTableChange = async (tableName: string) => {
        if (!tableName || !currentForeignKeyDatasource.value?.id) {
          sourceColumnList.value = [];
          newForeignKey.value.sourceColumnName = '';
          return;
        }
        try {
          sourceColumnList.value = await logicalRelationService.getTableColumns(
            currentForeignKeyDatasource.value.id,
            tableName,
          );
          newForeignKey.value.sourceColumnName = '';
        } catch (e) {
          ElMessage.error('加载字段列表失败');
        }
      };

      const handleTargetTableChange = async (tableName: string) => {
        if (!tableName || !currentForeignKeyDatasource.value?.id) {
          targetColumnList.value = [];
          newForeignKey.value.targetColumnName = '';
          return;
        }
        try {
          targetColumnList.value = await logicalRelationService.getTableColumns(
            currentForeignKeyDatasource.value.id,
            tableName,
          );
          newForeignKey.value.targetColumnName = '';
        } catch (e) {
          ElMessage.error('加载字段列表失败');
        }
      };

      const editForeignKey = async (fk: LogicalRelation) => {
        editingForeignKey.value = fk;
        newForeignKey.value = {
          id: fk.id,
          datasourceId: fk.datasourceId,
          sourceTableName: fk.sourceTableName,
          sourceColumnName: fk.sourceColumnName,
          targetTableName: fk.targetTableName,
          targetColumnName: fk.targetColumnName,
          relationType: fk.relationType || '',
          description: fk.description || '',
        };
        if (fk.sourceTableName && currentForeignKeyDatasource.value?.id) {
          try {
            sourceColumnList.value = await logicalRelationService.getTableColumns(
              currentForeignKeyDatasource.value.id,
              fk.sourceTableName,
            );
          } catch (_) {}
        }
        if (fk.targetTableName && currentForeignKeyDatasource.value?.id) {
          try {
            targetColumnList.value = await logicalRelationService.getTableColumns(
              currentForeignKeyDatasource.value.id,
              fk.targetTableName,
            );
          } catch (_) {}
        }
        ElMessage.info('正在编辑逻辑外键，修改后点击"更新"按钮');
      };

      const saveOrUpdateForeignKey = () => {
        const n = newForeignKey.value;
        if (!n.sourceTableName || !n.sourceColumnName || !n.targetTableName || !n.targetColumnName) {
          ElMessage.warning('请完整填写主表、字段、关联表和字段');
          return;
        }
        const isDuplicate = foreignKeyList.value.some(
          fk =>
            fk.id !== editingForeignKey.value?.id &&
            fk.sourceTableName === n.sourceTableName &&
            fk.sourceColumnName === n.sourceColumnName &&
            fk.targetTableName === n.targetTableName &&
            fk.targetColumnName === n.targetColumnName,
        );
        if (isDuplicate) {
          ElMessage.warning('该逻辑外键关系已存在');
          return;
        }
        if (editingForeignKey.value?.id) {
          const idx = foreignKeyList.value.findIndex(fk => fk.id === editingForeignKey.value!.id);
          if (idx !== -1) {
            foreignKeyList.value[idx] = {
              ...foreignKeyList.value[idx],
              sourceTableName: n.sourceTableName,
              sourceColumnName: n.sourceColumnName,
              targetTableName: n.targetTableName,
              targetColumnName: n.targetColumnName,
              relationType: n.relationType || '',
              description: n.description || '',
            };
          }
          ElMessage.success('更新成功，请点击"保存全部配置"以保存到数据库');
        } else {
          foreignKeyList.value.push({
            sourceTableName: n.sourceTableName,
            sourceColumnName: n.sourceColumnName,
            targetTableName: n.targetTableName,
            targetColumnName: n.targetColumnName,
            relationType: n.relationType || '',
            description: n.description || '',
          });
          ElMessage.success('添加成功，请点击"保存全部配置"以保存到数据库');
        }
        resetForeignKeyForm();
      };

      const deleteForeignKey = async (fk: LogicalRelation, index: number) => {
        try {
          await ElMessageBox.confirm('确定要删除这条逻辑外键关系吗？', '确认删除', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
          });
          foreignKeyList.value.splice(index, 1);
          ElMessage.success('删除成功，请点击"保存全部配置"以保存到数据库');
        } catch (_) {}
      };

      const saveForeignKeyConfig = async () => {
        if (!currentForeignKeyDatasource.value?.id) {
          ElMessage.error('数据源ID不存在');
          return;
        }
        savingForeignKeys.value = true;
        try {
          const res = await logicalRelationService.saveLogicalRelations(
            currentForeignKeyDatasource.value.id,
            foreignKeyList.value,
          );
          if (res.success) {
            ElMessage.success('保存成功');
            foreignKeyDialogVisible.value = false;
          } else {
            ElMessage.error('保存失败');
          }
        } catch (e) {
          ElMessage.error('保存失败');
        } finally {
          savingForeignKeys.value = false;
        }
      };

      const resetForeignKeyForm = () => {
        editingForeignKey.value = null;
        newForeignKey.value = emptyNewForeignKey();
        sourceColumnList.value = [];
        targetColumnList.value = [];
      };

      onMounted(() => loadDatasources());

      return {
        loading,
        datasources,
        addDialogVisible,
        editDialogVisible,
        formData,
        editFormData,
        submitting,
        testingId,
        initLoadingId,
        loadDatasources,
        showAddDialog,
        handleEdit,
        handleAddSubmit,
        handleEditSubmit,
        handleDelete,
        handleTestConnection,
        handleInitSchema,
        Plus,
        Refresh,
        Link,
        openForeignKeyDialog,
        foreignKeyDialogVisible,
        currentForeignKeyDatasource,
        foreignKeyList,
        newForeignKey,
        tableList,
        sourceColumnList,
        targetColumnList,
        savingForeignKeys,
        editingForeignKey,
        handleSourceTableChange,
        handleTargetTableChange,
        editForeignKey,
        saveOrUpdateForeignKey,
        deleteForeignKey,
        saveForeignKeyConfig,
        datasourceTypes,
      };
    },
  });
</script>

<style scoped>
  .datasource-manage-page {
    min-height: 100vh;
    background: #f8fafc;
  }

  .main-content {
    width: 100%;
    margin: 0 auto;
    padding: 2rem;
  }

  .content-header {
    margin-bottom: 2rem;
  }

  .content-title {
    font-size: 2rem;
    font-weight: 600;
    color: #1f2937;
    margin: 0 0 0.5rem 0;
  }

  .content-subtitle {
    color: #6b7280;
    margin: 0;
    font-size: 1.1rem;
  }

  .action-section {
    margin-bottom: 2rem;
  }

  .action-content {
    padding: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .action-buttons {
    display: flex;
    gap: 1rem;
  }

  .action-buttons-cell {
    display: flex;
    gap: 0.5rem;
    flex-wrap: wrap;
  }

  .config-table {
    margin-bottom: 2rem;
  }

  .loading-state {
    padding: 4rem 2rem;
  }

  .empty-state {
    padding: 4rem 2rem;
  }

  .fk-datasource-tip {
    margin-bottom: 20px;
    padding: 10px;
    background: #f0f9ff;
    border-radius: 4px;
    font-size: 14px;
    color: #666;
  }
  .fk-datasource-tip strong {
    color: #1890ff;
  }
  .fk-section {
    margin-bottom: 30px;
  }
  .fk-section-title {
    font-size: 14px;
    font-weight: 600;
    color: #333;
    margin-bottom: 15px;
    border-left: 4px solid #1890ff;
    padding-left: 10px;
  }
  .fk-empty {
    text-align: center;
    padding: 40px;
    color: #999;
    background: #fafafa;
    border: 1px solid #e8e8e8;
    border-radius: 4px;
  }
  .fk-form-section {
    background: #f0f9ff;
    padding: 20px;
    border-radius: 8px;
    border: 1px solid #bae7ff;
  }
  .fk-form-title {
    font-size: 14px;
    font-weight: 600;
    color: #333;
    margin-bottom: 15px;
  }
  .fk-form-title .el-icon {
    margin-right: 6px;
    vertical-align: middle;
    color: #1890ff;
  }
  .fk-label {
    display: block;
    font-size: 12px;
    font-weight: 600;
    color: #666;
    margin-bottom: 5px;
  }
  .fk-arrow {
    text-align: center;
    line-height: 70px;
    color: #999;
  }
  .fk-btn-col {
    line-height: 70px;
  }
  .mono {
    font-family: monospace;
  }
  .mono.primary {
    color: #1890ff;
  }
  .mono.success {
    color: #52c41a;
  }
  .link-icon {
    margin-right: 4px;
    color: #999;
  }
</style>
