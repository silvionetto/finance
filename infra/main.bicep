param environmentName string
param location string = resourceGroup().location
param serviceName string = 'finance'
param tags object = {}

param dbAdminLogin string
@secure()
param dbAdminPassword string

@secure()
param openAiApiKey string
@secure()
param bootstrapAdminPassword string
@secure()
param bootstrapUserPassword string

@secure()
param polygonApiKey string = ''
@secure()
param alpacaApiKeyId string = ''
@secure()
param alpacaApiSecretKey string = ''
@secure()
param fmpApiKey string = ''
@secure()
param brapiApiToken string = ''
@secure()
param euronextAuthKey string = ''

param openAiBaseUrl string = 'https://api.openai.com/v1'
param openAiModel string = 'gpt-4o-mini'
param postgresSkuName string = 'Standard_B1ms'
param postgresTier string = 'Burstable'
param postgresVersion string = '16'
param postgresStorageSizeGb int = 32
param postgresBackupRetentionDays int = 7

var nameToken = toLower(uniqueString(subscription().subscriptionId, resourceGroup().id, environmentName))
var appServicePlanName = 'plan-${nameToken}'
var webAppName = 'finance-${nameToken}-web'
var containerRegistryName = 'fin${nameToken}acr'
var keyVaultName = 'fin${nameToken}kv'
var postgresServerName = 'fin-${nameToken}-psql'
var postgresDatabaseName = 'finance'
var logAnalyticsName = 'log-${nameToken}'
var applicationInsightsName = 'appi-${nameToken}'
var keyVaultSecretsUserRoleDefinitionId = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '4633458b-17de-408a-b874-0445c86b69e6')
var acrPullRoleDefinitionId = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '7f951dda-4ed3-4680-a7ca-43fe172d538d')
var datasourceUrl = 'jdbc:postgresql://${postgresServerName}.postgres.database.azure.com:5432/${postgresDatabaseName}?sslmode=require'
var datasourceUsername = dbAdminLogin

resource logAnalyticsWorkspace 'Microsoft.OperationalInsights/workspaces@2022-10-01' = {
  name: logAnalyticsName
  location: location
  tags: tags
  properties: {
    sku: {
      name: 'PerGB2018'
    }
    retentionInDays: 30
    features: {
      enableLogAccessUsingOnlyResourcePermissions: true
    }
  }
}

resource applicationInsights 'Microsoft.Insights/components@2020-02-02' = {
  name: applicationInsightsName
  location: location
  kind: 'web'
  tags: tags
  properties: {
    Application_Type: 'web'
    WorkspaceResourceId: logAnalyticsWorkspace.id
    IngestionMode: 'LogAnalytics'
  }
}

resource containerRegistry 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: containerRegistryName
  location: location
  tags: tags
  sku: {
    name: 'Basic'
  }
  properties: {
    adminUserEnabled: false
    publicNetworkAccess: 'Enabled'
  }
}

resource keyVault 'Microsoft.KeyVault/vaults@2023-07-01' = {
  name: keyVaultName
  location: location
  tags: tags
  properties: {
    tenantId: subscription().tenantId
    sku: {
      family: 'A'
      name: 'standard'
    }
    enableRbacAuthorization: true
    enabledForDeployment: false
    enabledForDiskEncryption: false
    enabledForTemplateDeployment: false
    enableSoftDelete: true
    enablePurgeProtection: true
    publicNetworkAccess: 'Enabled'
    softDeleteRetentionInDays: 7
  }
}

resource postgresServer 'Microsoft.DBforPostgreSQL/flexibleServers@2022-12-01' = {
  name: postgresServerName
  location: location
  tags: tags
  sku: {
    name: postgresSkuName
    tier: postgresTier
  }
  properties: {
    administratorLogin: dbAdminLogin
    administratorLoginPassword: dbAdminPassword
    version: postgresVersion
    storage: {
      storageSizeGB: postgresStorageSizeGb
    }
    backup: {
      backupRetentionDays: postgresBackupRetentionDays
      geoRedundantBackup: 'Disabled'
    }
    highAvailability: {
      mode: 'Disabled'
    }
  }
}

resource postgresDatabase 'Microsoft.DBforPostgreSQL/flexibleServers/databases@2022-12-01' = {
  parent: postgresServer
  name: postgresDatabaseName
  properties: {
    charset: 'UTF8'
    collation: 'en_US.utf8'
  }
}

resource postgresAllowAzureServices 'Microsoft.DBforPostgreSQL/flexibleServers/firewallRules@2022-12-01' = {
  parent: postgresServer
  name: 'AllowAllAzureIPs'
  properties: {
    startIpAddress: '0.0.0.0'
    endIpAddress: '0.0.0.0'
  }
}

resource datasourceUrlSecret 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'spring-datasource-url'
  properties: {
    value: datasourceUrl
  }
}

resource datasourceUsernameSecret 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'spring-datasource-username'
  properties: {
    value: datasourceUsername
  }
}

resource datasourcePasswordSecret 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'spring-datasource-password'
  properties: {
    value: dbAdminPassword
  }
}

resource openAiApiKeySecret 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'openai-api-key'
  properties: {
    value: openAiApiKey
  }
}

resource bootstrapAdminPasswordSecret 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'finance-bootstrap-admin-password'
  properties: {
    value: bootstrapAdminPassword
  }
}

resource bootstrapUserPasswordSecret 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'finance-bootstrap-user-password'
  properties: {
    value: bootstrapUserPassword
  }
}

resource appServicePlan 'Microsoft.Web/serverfarms@2022-09-01' = {
  name: appServicePlanName
  location: location
  kind: 'linux'
  tags: tags
  sku: {
    name: 'B1'
    tier: 'Basic'
  }
  properties: {
    reserved: true
  }
}

resource webApp 'Microsoft.Web/sites@2022-09-01' = {
  name: webAppName
  location: location
  kind: 'app,linux,container'
  tags: union(tags, {
    'azd-service-name': serviceName
  })
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    serverFarmId: appServicePlan.id
    httpsOnly: true
    siteConfig: {
      alwaysOn: true
      healthCheckPath: '/actuator/health'
      minTlsVersion: '1.2'
      ftpsState: 'Disabled'
      http20Enabled: true
      acrUseManagedIdentityCreds: true
      linuxFxVersion: 'DOCKER|${containerRegistry.properties.loginServer}/${serviceName}:latest'
      appSettings: [
        {
          name: 'APPLICATIONINSIGHTS_CONNECTION_STRING'
          value: applicationInsights.properties.ConnectionString
        }
        {
          name: 'WEBSITES_PORT'
          value: '8080'
        }
        {
          name: 'OPENAI_BASE_URL'
          value: openAiBaseUrl
        }
        {
          name: 'OPENAI_MODEL'
          value: openAiModel
        }
        {
          name: 'SPRING_DATASOURCE_URL'
          value: '@Microsoft.KeyVault(VaultName=${keyVault.name};SecretName=${datasourceUrlSecret.name})'
        }
        {
          name: 'SPRING_DATASOURCE_USERNAME'
          value: '@Microsoft.KeyVault(VaultName=${keyVault.name};SecretName=${datasourceUsernameSecret.name})'
        }
        {
          name: 'SPRING_DATASOURCE_PASSWORD'
          value: '@Microsoft.KeyVault(VaultName=${keyVault.name};SecretName=${datasourcePasswordSecret.name})'
        }
        {
          name: 'OPENAI_API_KEY'
          value: '@Microsoft.KeyVault(VaultName=${keyVault.name};SecretName=${openAiApiKeySecret.name})'
        }
        {
          name: 'FINANCE_BOOTSTRAP_ADMIN_PASSWORD'
          value: '@Microsoft.KeyVault(VaultName=${keyVault.name};SecretName=${bootstrapAdminPasswordSecret.name})'
        }
        {
          name: 'FINANCE_BOOTSTRAP_USER_PASSWORD'
          value: '@Microsoft.KeyVault(VaultName=${keyVault.name};SecretName=${bootstrapUserPasswordSecret.name})'
        }
        {
          name: 'POLYGON_API_KEY'
          value: polygonApiKey
        }
        {
          name: 'ALPACA_API_KEY_ID'
          value: alpacaApiKeyId
        }
        {
          name: 'ALPACA_API_SECRET_KEY'
          value: alpacaApiSecretKey
        }
        {
          name: 'FMP_API_KEY'
          value: fmpApiKey
        }
        {
          name: 'BRAPI_API_TOKEN'
          value: brapiApiToken
        }
        {
          name: 'EURONEXT_AUTH_KEY'
          value: euronextAuthKey
        }
      ]
    }
  }
}

resource keyVaultSecretsUserRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(keyVault.id, webApp.name, 'key-vault-secrets-user')
  scope: keyVault
  properties: {
    principalId: webApp.identity.principalId
    roleDefinitionId: keyVaultSecretsUserRoleDefinitionId
    principalType: 'ServicePrincipal'
  }
}

resource acrPullRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(containerRegistry.id, webApp.name, 'acr-pull')
  scope: containerRegistry
  properties: {
    principalId: webApp.identity.principalId
    roleDefinitionId: acrPullRoleDefinitionId
    principalType: 'ServicePrincipal'
  }
}

output APPLICATION_URL string = 'https://${webApp.properties.defaultHostName}'
output APP_SERVICE_NAME string = webApp.name
output APP_SERVICE_PRINCIPAL_ID string = webApp.identity.principalId
output CONTAINER_REGISTRY_ENDPOINT string = containerRegistry.properties.loginServer
output KEY_VAULT_NAME string = keyVault.name
output POSTGRES_HOST string = '${postgresServer.name}.postgres.database.azure.com'
output POSTGRES_DATABASE_NAME string = postgresDatabase.name
