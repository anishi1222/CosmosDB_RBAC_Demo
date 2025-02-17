# CosmosDB RBAC Demo

This sample is used in the following blog entries to elaborate interaction with RBAC configured Cosmos DB.

- Japanese [https://logico-jp.io/2021/12/20/use-not-service-principal-but-managed-identity-to-configure-role-based-access-control-for-cosmos-db/](https://logico-jp.io/2021/12/20/use-not-service-principal-but-managed-identity-to-configure-role-based-access-control-for-cosmos-db/)
- English [https://medium.com/microsoftazure/configure-rbac-for-cosmos-db-with-managed-identity-instead-of-service-principal-18c0f465f9cd](https://medium.com/microsoftazure/configure-rbac-for-cosmos-db-with-managed-identity-instead-of-service-principal-18c0f465f9cd)

## How to use

### Functions

- Create Azure function app before deployment.
- Modify pom-ro.xml and pom-rw.xml following your environment.
- Build read-only app and read-write app.

```bash
mvn clean package azure-functions:deploy -f pom-ro.xml
mvn clean package azure-functions:deploy -f pom-rw.xml
```

- Enable system assigned identity in each Function app.

### Cosmos DB

- Create a Cosmos DB account, database and a container.
  - In these sample apps, the database is named "Inventories", and the container is "Tracks".
  - Following the document [https://learn.microsoft.com/azure/cosmos-db/nosql/security/how-to-grant-data-plane-role-based-access](https://learn.microsoft.com/azure/cosmos-db/nosql/security/how-to-grant-data-plane-role-based-access), assign identities to the specific role.

```bash
ROprincipalId='<Principal ID for ReadOnly app>'
az cosmosdb sql role assignment create -a $accountName -g $resourceGroupName -s "/" -p $ROprincipalId -d $builtInReadOnlyRoleDefinitionId

RWprincipalId='<Principal ID for ReadWrite app>'
az cosmosdb sql role assignment create -a $accountName -g $resourceGroupName -s "/" -p $RWprincipalId -d $builtInReadWriteRoleDefinitionId
```
