# How to configure Keycloak for IGSN Metadata Registry
Please refer to the [Official Documentation](https://www.keycloak.org/docs/10.0/getting_started/index.html) for Keycloak Installation and general configuration

## Requirements
* Keycloak Server 10.0.1 installed and configured
* Keycloak Server is accessible from the internet (for IGSN Metadata Portal and IGSN Metadta Editor usage)
* Credentials to mint IGSN is obtained and authenticated against the global IGSN resolver

## Keycloak Client Setup
It's recommended to create 2 separate [Keycloak clients](https://www.keycloak.org/docs/10.0/server_admin/#_clients) for use with the IGSN Service, an Authorization Enabled client for use with IGSN Metadata Registry, and a public client for use with IGSN Metadata Editor and IGSN Metadata Portal.

### Roles and Groups Setup
Create the follow set of Realm Role:
* IGSN_USER
* IGSN_ADMIN

Create one or more Realm Groups, for eg. `/AuScope` to group user membership with.

User(s) should have the `IGSN_USER` role and be associated with the allocated Group if they need to access the IGSN Metadata Registry. `IGSN_ADMIN` role is for administrative operations

### Authorization Client Setup

> An example Keycloak client export can be found at [example igsn client](example-igsn-client.json) and an example authorization export can be found at [example authz config](example-igsn-client-authz-config.json)

Each IGSN Prefix will be modelled as a Protected Resource in Keycloak and will contain all necessary information for IGSN Metadata Registry to mint/update with the global IGSN resolver. The permission will be handled by User's Realm Role and User's Group membership. This is achieved by the following steps
1. On the Keycloak ADMIN UI of the Client, go to Authorzation > Authorization Scopes and create the following scopes: `create`, `import` and `update`
2. On the Keycloak Admin UI of the Client, go to Authorization > Resources > Create to create a new Protected Resource
   1. The type of the resource should be `urn:ardc:igsn:allocation`
   2. The scope associated with the resource should be `create`, `import` and `update`
   3. The attributes must contain the following
      1. `namespace` : the *namespace* portion of the IGSN, e.g. `XXAA`
      2. `password` : the global IGSN registrar password
      3. `prefix` the *prefix* portion of the IGSN, e.g. `10273`
      4. `server_url` the *url* of the global IGSN registrar, e.g. [https://doidb.wdc-terra.org/igsn/](https://doidb.wdc-terra.org/igsn/)
      5. `status` the state of the Protected Resources, accepted values are: `prod` and `test`.
      6. `username`: the global IGSN registrar username
      7. `groups`: the full path to the group associated, e.g. `/AuScope`
3. On the Keycloak Admin UI of the Client, configure the Policies by going to Authorization > Policies and add a Policy for
   1. *is an IGSN_USER* that is a *role* type policy that checks if the Realm Role `IGSN_USER` is available
   2. *belongs to Group* is a *group* type policy that checks if the Group membership is associated with the user.
4. Configure the Permission that make use of the previous set of Policy to grant access to the IGSN Allocation, do this by going to Authorization > Permissions
   1. The *Resources* will point to the IGSN Allocation created above
   2. The *Policies* will need to have both the IGSN_USER policy and the is in Group policy
   3. The *Decision Strategy* will need to be Unanimous
5. You can Evaluate the Policy for a given user by testing it out on the Authorization > Evaluate tab
