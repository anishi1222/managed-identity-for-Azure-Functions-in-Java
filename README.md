# Managed identity for Azure Functions (Java)

Sample Java codes to authenticate and authorize managed identity of Azure functions.

## func-j01

- Caller application (HTTP trigger) to call func-j02.
- audienceId (application Id for func-j02) and targetUrl (API endpoint for func-j02) is specified.

## func-j02

- Callee application (HTTP trigger) to be called by func-j01.

## Usage

- Create function apps in Azure, and deployed func-j01 and func-j02 to Azure.
- System assigned managed identity is enabled on func-j01 function app.
- Authentication is enabled on func-j02 function app.

