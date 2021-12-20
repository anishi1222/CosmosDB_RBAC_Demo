# CosmosDB_RBAC_Demo
Interact with RBAC configured Cosmos DB

## How to use

### Functions

- Create Azure function app before deployment.
- Modify pom-ro.xml and pom-rw.xml following your environment.
- Build read-only app and read-write app.

```
 mvn clean package azure-functions:deploy -f pom-ro.xml
 mvn clean package azure-functions:deploy -f pom-rw.xml
```

- Enable system assigned identity in each Function app.

### Cosmos DB

- Create a Cosmos DB account, database and a container.
  - In this sample apps, the database is named "Inventories", and the container is "Tracks".
- Following the document [https://docs.microsoft.com/azure/cosmos-db/how-to-setup-rbac](https://docs.microsoft.com/azure/cosmos-db/how-to-setup-rbac),
  - Create roles.
    ```
    resourceGroupName='<Resource Group>'
    accountName='<Cosmos DB Account>'
    az cosmosdb sql role definition create -a $accountName -g $resourceGroupName -b @role-definition-ro.json
    az cosmosdb sql role definition create -a $accountName -g $resourceGroupName -b @role-definition-rw.json
    ```
  - Retrieve roleDefinitionId using the following command. roleDefinitionId is found in "name" element.
    ```
    az cosmosdb sql role definition list --account-name $accountName -g $resourceGroupName
    
    [
      ...
      {
        "assignableScopes": [
          "/subscriptions/{subscription id}/resourceGroups/{resource group}/providers/Microsoft.DocumentDB/databaseAccounts/{CosmosDB account}"
        ],
        "id": "/subscriptions/{subscription id}/resourceGroups/{resource group}/providers/Microsoft.DocumentDB/databaseAccounts/{CosmosDB account}/sqlRoleDefinitions/{roleDefinitionId}",
        "name": "{roleDefinitionId}",
        "permissions": [
          {
            "dataActions": [
              "Microsoft.DocumentDB/databaseAccounts/readMetadata",
              "Microsoft.DocumentDB/databaseAccounts/sqlDatabases/containers/items/*",
              "Microsoft.DocumentDB/databaseAccounts/sqlDatabases/containers/*"
            ],
            "notDataActions": []
          }
        ],
        "resourceGroup": "{resource group}",
        "roleName": "MyReadWriteRole",
        "type": "Microsoft.DocumentDB/databaseAccounts/sqlRoleDefinitions",
        "typePropertiesType": "1"
      },
      {
        "assignableScopes": [
          "/subscriptions/{subscription id}/resourceGroups/{resource group}/providers/Microsoft.DocumentDB/databaseAccounts/{CosmosDB account}"
        ],
        "id": "/subscriptions/{subscription id}/resourceGroups/{resource group}/providers/Microsoft.DocumentDB/databaseAccounts/{CosmosDB account}/sqlRoleDefinitions/{roleDefinitionId}",
        "name": "{roleDefinitionId}",
        "permissions": [
          {
            "dataActions": [
              "Microsoft.DocumentDB/databaseAccounts/readMetadata",
              "Microsoft.DocumentDB/databaseAccounts/sqlDatabases/containers/items/read",
              "Microsoft.DocumentDB/databaseAccounts/sqlDatabases/containers/executeQuery",
              "Microsoft.DocumentDB/databaseAccounts/sqlDatabases/containers/readChangeFeed"
            ],
            "notDataActions": []
          }
        ],
        "resourceGroup": "{resource group}",
        "roleName": "MyReadOnlyRole",
        "type": "Microsoft.DocumentDB/databaseAccounts/sqlRoleDefinitions",
        "typePropertiesType": "1"
      },
      ...
    ]
    ```
  - Assign managed identity of each app to one of the roles.
    ```
    resourceGroupName='<Resource Group>'
    accountName='<Cosmos DB Account>'
    readOnlyRoleDefinitionId='<roleDefinitionid for MyReadOnlyRole>'
    ROprincipalId='<Principal ID for ReadOnly app>'
    az cosmosdb sql role assignment create -a $accountName -g $resourceGroupName -s "/" -p $ROprincipalId -d $readOnlyRoleDefinitionId

    readWriteRoleDefinitionId='<roleDefinitionId for MyReadWriteRole>'
    RWprincipalId='<Principal ID for ReadWrite app>'
    az cosmosdb sql role assignment create -a $accountName -g $resourceGroupName -s "/" -p $RWprincipalId -d $readWriteRoleDefinitionId
    ```
