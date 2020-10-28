# data-share-service
This repository contains the source code and design documents for MOSIP Data Share module.

## Design

[Design - Data-Share](https://mosip.atlassian.net/wiki/spaces/DD/pages/80904528/Data+Share)

## Default Context Path and Port
```
server.port=8097
server.servlet.path=/v1/datashare

## Configurable Properties from Config Server
mosip.data.share.service.id=mosip.data.share
mosip.data.share.service.version=1.0

mosip.data.share.urlshortner=false