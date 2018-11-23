# DX Module Manager
DX module that provides enterprise level module management functionality
- API base url: http://{dx.host}:{dx.port}/{dx.tomcat.contextPath}/modules/api/bundles
- User should have the `adminTemplates` permission in DX to be able to use this API
- The `target` parameter is optional, the value of the `target` group of cluster nodes could be specified as `null`, meaning the default group is concerned, which includes all cluster nodes.
- Available actions:
 - [Install one or multiple bundle(s)](#install)
 - [Start a bundle](#start)
 - [Stop a bundle](#stop)
 - [Uninstall a bundle](#uninstall)
 - [Get cluster wide info about bundle(s) (since DX 7.2.0.2)](#getInfo)
 - [Get local info about bundle(s) (since DX 7.2.0.2)](#getLocalInfo)
 - [Get local state of bundle(s)](#getLocalState)


<a name="install"></a>**Install one or multiple bundle(s)**
----
  Install the specified bundle(s), optionally starting it/them right after and return the operation result(s).


  In case you need to deploy multiple bundles at the same time, you can do it in one call to avoid multiple "refresh" of dependencies,
  for that you just need one "bundle" parameter for each bundle you want to deploy.

* **URL**

  /

* **Method:**

  `POST`

* **Consume:**

  `multipart/form-data`

*  **Params**

   `start=[boolean]`: `true`, if the bundle should be started right after installation

   `target=[string]`: The group of cluster nodes targeted by the operation

   **Required:**

   `bundle=[file]`: a bundle to be deploy, this parameter can be repeat for each bundle that need to be deploy, at least one bundle is required

* **Success Response:**

  * **Code:** 200 <br />
    **Content:** `{"bundleInfos":[{"groupId":"org.jahia.modules","symbolicName":"article","version":"2.0.3.SNAPSHOT","key":"org.jahia.modules/article/2.0.3.SNAPSHOT"}],"message":"Operation successful"}`

  * **Code:** 200 <br />
    **Content:** `{"bundleInfos":[{"groupId":"org.jahia.modules","symbolicName":"article","version":"3.0.0.SNAPSHOT","key":"org.jahia.modules/article/3.0.0.SNAPSHOT"},{"groupId":"org.jahia.modules","symbolicName":"news","version":"2.0.2.SNAPSHOT","key":"org.jahia.modules/news/2.0.2.SNAPSHOT"}],"message":"Operation successful"}`

* **Error Response:**

  * **Code:** 400 BAD REQUEST <br />
    **Content:** `{"status":400,"reasonPhrase":"Bad Request","message":"Unable to install module. Cause: Submitted bundle is either not a valid OSGi bundle or has no required manifest headers Bundle-SymbolicName and Implementation-Version/Bundle-Version"}`

  OR

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `user /users/hj/di/ac/bill is not allowed to access Module Manager HTTP API`

* **Sample Call:**

  ```sh
  curl -s --user jon:password --form bundle=@/Users/jon/Projects/article/target/article-2.0.3-SNAPSHOT.jar --form start=true http://localhost:8080/modules/api/bundles
  ```

  ```sh
  curl -s --user jon:password --form bundle=@/Users/jon/Projects/article/target/article-3.0.0-SNAPSHOT.jar --form bundle=@/Users/jon/Projects/news/target/news-2.0.2-SNAPSHOT.jar --form start=true http://localhost:8080/modules/api/bundles
  ```

<a name="start"></a>**Start bundle**
----
  Starts the specified bundle and return the operation result.

* **URL**

  /:bundleKey/_start

* **Method:**

  `POST`

*  **Params**

   `target=[string]`: The group of cluster nodes targeted by the operation

   **Required:**

   `bundleKey=[string]`: the bundle key

* **Success Response:**

  * **Code:** 200 <br />
    **Content:** `{"bundleInfos":[{"groupId":"org.jahia.modules","symbolicName":"article","version":"2.0.3.SNAPSHOT","key":"org.jahia.modules/article/2.0.3.SNAPSHOT"}],"message":"Operation successful"}`

* **Error Response:**

  * **Code:** 404 NOT FOUND <br />
    **Content:** `{"status":404,"reasonPhrase":"Not Found","message":"Unable to find a module bundle corresponding to the key: org.jahia.modules/article/2.0.3.SPSHOT"}`

  OR

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `user /users/hj/di/ac/bill is not allowed to access Module Manager HTTP API`

* **Sample Call:**

  ```sh
  curl -s --user jon:root1234 --data --request POST http://localhost:8080/modules/api/bundles/org.jahia.modules/article/2.0.3.SNAPSHOT/_start
  ```

<a name="stop"></a>**Stop bundle**
----
  Stops the specified bundle and return the operation result.

* **URL**

  /:bundleKey/_stop

* **Method:**

  `POST`

*  **Params**

   `target=[string]`: The group of cluster nodes targeted by the operation

   **Required:**

   `bundleKey=[string]`: the bundle key

* **Success Response:**

  * **Code:** 200 <br />
    **Content:** `{"bundleInfos":[{"groupId":"org.jahia.modules","symbolicName":"article","version":"2.0.3.SNAPSHOT","key":"org.jahia.modules/article/2.0.3.SNAPSHOT"}],"message":"Operation successful"}`

* **Error Response:**

  * **Code:** 404 NOT FOUND <br />
    **Content:** `{"status":404,"reasonPhrase":"Not Found","message":"Unable to find a module bundle corresponding to the key: org.jahia.modules/article/2.0.3.SPSHOT"}`

  OR

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `user /users/hj/di/ac/bill is not allowed to access Module Manager HTTP API`

* **Sample Call:**

  ```sh
  curl -s --user jon:root1234 --data --request POST http://localhost:8080/modules/api/bundles/org.jahia.modules/article/2.0.3.SNAPSHOT/_stop
  ```

<a name="uninstall"></a>**Uninstall bundle**
----
  Uninstalls the specified bundle and return the operation result.

* **URL**

  /:bundleKey/_uninstall

* **Method:**

  `POST`

*  **Params**

   `target=[string]`: The group of cluster nodes targeted by the operation

   **Required:**

   `bundleKey=[string]`: the bundle key

* **Success Response:**

  * **Code:** 200 <br />
    **Content:** `{"bundleInfos":[{"groupId":"org.jahia.modules","symbolicName":"article","version":"2.0.3.SNAPSHOT","key":"org.jahia.modules/article/2.0.3.SNAPSHOT"}],"message":"Operation successful"}`

* **Error Response:**

  * **Code:** 404 NOT FOUND <br />
    **Content:** `{"status":404,"reasonPhrase":"Not Found","message":"Unable to find a module bundle corresponding to the key: org.jahia.modules/article/2.0.3.SPSHOT"}`

  OR

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `user /users/hj/di/ac/bill is not allowed to access Module Manager HTTP API`

* **Sample Call:**

  ```sh
  curl -s --user jon:root1234 --data --request POST http://localhost:8080/modules/api/bundles/org.jahia.modules/article/2.0.3.SNAPSHOT/_uninstall
  ```

<a name="getInfo"></a>**Get cluster wide info about bundle(s) (since DX 7.2.0.2)**
----
  Get cluster wide info about one or more installed bundles.

* **URL**

  /:bundleSelector/_info

* **Method:**

  `GET`

*  **Params**

   `target=[string]`: The group of cluster nodes targeted by the operation

   **Required:**

   `bundleSelector=[string]`: one of
   - a bundle key; the version value in the bundle key may be replaced with an asterisk symbol which stands for all installed versions of the bundle (since DX 7.2.3.3/7.3.0.1)
   - a comma separated list of bundle keys wrapped with square brackets
   - an asterisk symbol which stands for all installed bundles (since DX 7.2.3.3/7.3.0.1)

* **Success Response:**

  * **Code:** 200 <br />
    **Content:** `{"jahiaNode1":{"type":"MODULE","osgiState":"ACTIVE","moduleState":"STARTED"},"jahiaNode2":{"type":"MODULE","osgiState":"ACTIVE","moduleState":"STARTED"}`

  OR

  * **Code:** 200 <br />
    **Content:** `{"jahiaNode1":{"article/2.0.2":{"type":"MODULE","osgiState":"ACTIVE","moduleState":"STARTED"},"news/2.0.3":{"type":"type":"MODULE","osgiState":"ACTIVE","moduleState":"STARTED"}},"jahiaNode2":{"article/2.0.2":{"type":"MODULE","osgiState":"ACTIVE","moduleState":"STARTED"},"news/2.0.3":{"type":"MODULE","osgiState":"ACTIVE","moduleState":"STARTED"}}}`



* **Error Response:**

  * **Code:** 200 <br />
    **Content:** `{"jahiaNode1":{"message":"Error retrieving bundle info from cluster node jahiaNode1","cause":"org.jahia.services.modulemanager.ModuleNotFoundException: Unable to find a module bundle corresponding to the key: article/2.0.3.SPSHOT"},"jahiaNode2":{"message":"Error retrieving bundle info from cluster node jahiaNode2","cause":"org.jahia.services.modulemanager.ModuleNotFoundException: Unable to find a module bundle corresponding to the key: article/2.0.3.SPSHOT"}`

  OR

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `user /users/hj/di/ac/bill is not allowed to access Module Manager HTTP API`

* **Sample Calls:**

  ```sh
  curl -s --user jon:password --request GET http://localhost:8080/modules/api/bundles/article/2.0.3.SNAPSHOT/_info
  curl -g -s --user jon:password --request GET http://localhost:8090/modules/api/bundles/\[article/2.0.3.SNAPSHOT,news/2.0.3\]/_info
  curl -s --user jon:password --request GET http://localhost:8080/modules/api/bundles/article/*/_info
  curl -s --user jon:password --request GET http://localhost:8080/modules/api/bundles/org.jahia.modules/\*/\*/_info
  curl -s --user jon:password --request GET http://localhost:8080/modules/api/bundles/*/_info
  ```

<a name="getLocalInfo"></a>**Get local info about bundle(s) (since DX 7.2.0.2)**
----
  Get local info about one or more installed bundles.

* **URL**

  /:bundleSelector/_localInfo

* **Method:**

  `GET`

*  **Params**

   **Required:**

   `bundleSelector=[string]`: one of
   - a bundle key; the version value in the bundle key may be replaced with an asterisk symbol which stands for all installed versions of the bundle (since DX 7.2.3.3/7.3.0.1)
   - a comma separated list of bundle keys wrapped with square brackets
   - an asterisk symbol which stands for all installed bundles (since DX 7.2.3.3/7.3.0.1)

* **Success Response:**

  * **Code:** 200 <br />
    **Content:** `{"type":"MODULE","osgiState":"ACTIVE","moduleState":"STARTED"}`

  OR

  * **Code:** 200 <br />
    **Content:** `{"article/2.0.3.SNAPSHOT":{"type":"MODULE","osgiState":"ACTIVE","moduleState":"STARTED"},"news/2.0.3":{"type":"MODULE","osgiState":"ACTIVE","moduleState":"STARTED"}}`

* **Error Response:**

  * **Code:** 404 NOT FOUND <br />
    **Content:** `{"status":404,"reasonPhrase":"Not Found","message":"Unable to find a module bundle corresponding to the key: article/2.0.3.SPSHOT"}`

  OR

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `user /users/hj/di/ac/bill is not allowed to access Module Manager HTTP API`

* **Sample Calls:**

  ```sh
  curl -s --user jon:password --request GET http://localhost:8080/modules/api/bundles/article/2.0.3.SNAPSHOT/_localInfo
  curl -g -s --user jon:password --request GET http://localhost:8090/modules/api/bundles/\[article/2.0.3.SNAPSHOT,news/2.0.3\]/_localInfo
  curl -s --user jon:password --request GET http://localhost:8080/modules/api/bundles/article/*/_localInfo
  curl -s --user jon:password --request GET http://localhost:8080/modules/api/bundles/org.jahia.modules/\*/\*/_localInfo
  curl -s --user jon:password --request GET http://localhost:8080/modules/api/bundles/*/_localInfo
  ```

<a name="getLocalState"></a>**Get local state of bundle(s)**
----
  Get current local state of one or more bundles.

* **URL**

  /:bundleSelector/_localState

* **Method:**

  `GET`

*  **Params**

   **Required:**

   `bundleSelector=[string]`: one of
   - a bundle key
   - a comma separated list of bundle keys wrapped with square brackets

* **Success Response:**

  * **Code:** 200 <br />
    **Content:** `"STATE"` <br />
    **Possible values:** `UNINSTALLED`, `INSTALLED`, `RESOLVED`, `STARTING`, `STOPPING`, `ACTIVE`

  OR

  * **Code:** 200 <br />
    **Content:** `{"article/2.0.3.SNAPSHOT":"ACTIVE","news/2.0.3":"ACTIVE"}`

* **Error Response:**

  * **Code:** 404 NOT FOUND <br />
    **Content:** `{"status":404,"reasonPhrase":"Not Found","message":"Unable to find a module bundle corresponding to the key: article/2.0.3.SPSHOT"}`

  OR

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `user /users/hj/di/ac/bill is not allowed to access Module Manager HTTP API`

* **Sample Calls:**

  ```sh
  curl -s --user jon:password --request GET http://localhost:8080/modules/api/bundles/article/2.0.3.SNAPSHOT/_localState
  curl -g -s --user jon:password --request GET http://localhost:8090/modules/api/bundles/\[article/2.0.3.SNAPSHOT,news/2.0.3\]/_localState
  ```
