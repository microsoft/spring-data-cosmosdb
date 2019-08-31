@echo off
set clientId=%CLIENT_ID%
set clientKey=%CLIENT_KEY%
set tenantId=%TENANT_ID%

if "%clientId%" == "" (
goto :noSetup
)
if "%clientKey%" == "" (
goto :noSetup
)
if "%tenantId%" == "" (
goto :noSetup
)

set resourcegroup=%1
set dbname=%2

if "%resourcegroup" == "" (
goto :noSetup
)
if "%dbname" == "" (
goto :noSetup
)

call az login --service-principal -u %clientId% -p %clientKey% --tenant %tenantId% >> null
call az group create -l westus -n %resourcegroup% >> null
set createcmd='az cosmosdb create --name %dbname% --resource-group %resourcegroup% --kind GlobalDocumentDB --query documentEndpoint'

for /f "tokens=*" %%a in (%createcmd%) do (set cosmosdburi=%%a)

set listcmd='az cosmosdb keys list --name %dbname% --resource-group %resourcegroup% --query primaryMasterKey'
for /f "tokens=*" %%a in (%listcmd%) do (set cosmosdbPrimarykey=%%a)
set cosmosdbPrimarykey=%cosmosdbPrimarykey:"=%

set listSecondaryKeycmd='az cosmosdb keys list --name %dbname% --resource-group %resourcegroup% --query secondaryMasterKey'
for /f "tokens=*" %%a in (%listSecondaryKeycmd%) do (set cosmosdbSecondarykey=%%a)
set cosmosdbSecondarykey=%cosmosdbSecondarykey:"=%

echo %cosmosdbPrimarykey% %cosmosdbSecondarykey%

goto :end

:noSetup
echo not to setup test resources
exit 0

:end