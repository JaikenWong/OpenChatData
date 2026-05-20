import api from './request'

export function login(data) {
  return api.post('/auth/login', data)
}

export function getTenants() {
  return api.get('/admin/tenants')
}

export function createTenant(data) {
  return api.post('/admin/tenants', data)
}

export function updateTenant(id, data) {
  return api.put(`/admin/tenants/${id}`, data)
}

export function deleteTenant(id) {
  return api.delete(`/admin/tenants/${id}`)
}

export function connectTenant(id) {
  return api.post(`/admin/tenants/${id}/connect`)
}

export function getDbTypes() {
  return api.get('/admin/tenants/db-types')
}

export function getTables(tenantCode) {
  return api.get(`/schema/${tenantCode}/tables`)
}

export function getTableSchema(tenantCode, tableName) {
  return api.get(`/schema/${tenantCode}/tables/${tableName}`)
}

export function syncSchema(tenantCode, tableNames) {
  return api.post(`/schema/${tenantCode}/sync`, tableNames)
}

export function ask(data) {
  return api.post('/query/ask', data)
}

export function getHistory(tenantCode, params) {
  return api.get(`/history/${tenantCode}`, { params })
}

export function getHistoryStats(tenantCode) {
  return api.get(`/history/${tenantCode}/stats`)
}

export function deleteHistory(tenantCode, id) {
  return api.delete(`/history/${tenantCode}/${id}`)
}

export function getDictionaries(tenantCode, params) {
  return api.get(`/dictionary/${tenantCode}`, { params })
}

export function createDictionary(data) {
  return api.post('/dictionary', data)
}

export function updateDictionary(id, data) {
  return api.put(`/dictionary/${id}`, data)
}

export function deleteDictionary(id) {
  return api.delete(`/dictionary/${id}`)
}

export function getDictionaryTypes() {
  return api.get('/dictionary/types')
}

export function enhanceQuestion(tenantCode, question) {
  return api.post(`/dictionary/${tenantCode}/enhance`, { question })
}

export function getMaskingRules(tenantCode) {
  return api.get(`/masking/${tenantCode}`)
}

export function getMaskingRulesByTable(tenantCode, tableName) {
  return api.get(`/masking/${tenantCode}/${tableName}`)
}

export function createMaskingRule(data) {
  return api.post('/masking', data)
}

export function deleteMaskingRule(id) {
  return api.delete(`/masking/${id}`)
}

export function getMaskTypes() {
  return api.get('/masking/types')
}

export function getAuditLogs(tenantCode, params) {
  return api.get(`/audit/${tenantCode}`, { params })
}

export function getAuditStats(tenantCode) {
  return api.get(`/audit/${tenantCode}/stats`)
}

export function getRoles(params) {
  return api.get('/rbac/roles', { params })
}

export function createRole(data) {
  return api.post('/rbac/roles', data)
}

export function updateRole(id, data) {
  return api.put(`/rbac/roles/${id}`, data)
}

export function deleteRole(id) {
  return api.delete(`/rbac/roles/${id}`)
}

export function getPermissions() {
  return api.get('/rbac/permissions')
}

export function getRolePermissions(roleId) {
  return api.get(`/rbac/roles/${roleId}/permissions`)
}

export function assignPermissions(roleId, permissionIds) {
  return api.post(`/rbac/roles/${roleId}/permissions`, permissionIds)
}

export function getUserRoles(userId) {
  return api.get(`/rbac/users/${userId}/roles`)
}

export function assignRole(userId, data) {
  return api.post(`/rbac/users/${userId}/roles`, data)
}

export function removeRole(userId, roleId) {
  return api.delete(`/rbac/users/${userId}/roles/${roleId}`)
}

export function getUserPermissions(userId) {
  return api.get(`/rbac/users/${userId}/permissions`)
}

export function getLLMProviders() {
  return api.get('/llm/providers')
}

export function getDefaultLLMProvider() {
  return api.get('/llm/providers/default')
}

export function createLLMProvider(data) {
  return api.post('/llm/providers', data)
}

export function updateLLMProvider(id, data) {
  return api.put(`/llm/providers/${id}`, data)
}

export function deleteLLMProvider(id) {
  return api.delete(`/llm/providers/${id}`)
}

export function setDefaultLLMProvider(id) {
  return api.post(`/llm/providers/${id}/set-default`)
}

export function getLLMProviderTypes() {
  return api.get('/llm/providers/types')
}

export function explainSql(data) {
  return api.post('/execution/explain', data)
}

export function createConversation(tenantCode) {
  return api.post(`/context/${tenantCode}/conversations`)
}

export function getConversationHistory(tenantCode, conversationId) {
  return api.get(`/context/${tenantCode}/conversations/${conversationId}`)
}

export function clearConversation(tenantCode, conversationId) {
  return api.delete(`/context/${tenantCode}/conversations/${conversationId}`)
}

export function getDataSourceMonitor() {
  return api.get('/datasource/monitor')
}

export function getDataSourceHealth() {
  return api.get('/datasource/health')
}
