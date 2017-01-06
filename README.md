# DX Module Manager
DX module that provides enterprise level module management functionality
- API base url: http://{dx.host}:{dx.host}/{dx.tomcat.contextPath}/modules/api/bundles
- user should have the `adminTemplates` permission in DX to be able to use this API
- The `target` parameter is optional, the value of the `target` group of cluster nodes could be specified as `null`, meaning the default group is concerned, which includes all cluster nodes.

**Install bundle**
----
  Install the specified bundle, optionally starting it right after and return the operation result.

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

   `bundle=[file]`: the bundle to deploy file input stream

* **Success Response:**

  * **Code:** 200 <br />
    **Content:** `{"bundleInfos":[{"groupId":"org.jahia.modules","symbolicName":"article","version":"2.0.3.SNAPSHOT","key":"org.jahia.modules/article/2.0.3.SNAPSHOT"}],"message":"Operation successful"}`

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

**Start bundle**
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

**Stop bundle**
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

**Uninstall bundle**
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

**Get local state for one bundle**
----
  Get the current local state of a single bundle.

* **URL**

  /:bundleKey/_localState

* **Method:**

  `GET`

*  **Params**

   **Required:**

   `bundleKey=[string]`: the bundle key

* **Success Response:**

  * **Code:** 200 <br />
    **Content:** `"STATE"` <br />
    **Possible values:**: `UNINSTALLED`, `INSTALLED`, `RESOLVED`, `STARTING`, `STOPPING`, `ACTIVE`

* **Error Response:**

  * **Code:** 400 BAD REQUEST <br />
    **Content:** `{"status":400,"reasonPhrase":"Bad Request","message":"Invalid module key: org.jahia.modules/article/2.0.3.SPSHOT"}`

  OR

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `user /users/hj/di/ac/bill is not allowed to access Module Manager HTTP API`

* **Sample Call:**

  ```sh
  curl -s --user jon:password --request GET http://localhost:8080/modules/api/bundles/org.jahia.modules/article/2.0.3.SNAPSHOT/_localState
  ```

**Get local states for multiple bundles**
----
  Get the current local states of multiple bundles.

* **URL**

  /[:bundleKeys]/_localState

* **Method:**

  `GET`

*  **Params**

   **Required:**

   `bundleKeys=[string]`: comma separated list of bundle keys

* **Success Response:**

  * **Code:** 200 <br />
    **Content:** `{"org.jahia.modules/article/2.0.3.SNAPSHOT":"ACTIVE","org.jahia.modules/news/2.0.3":"ACTIVE"}`

* **Error Response:**

  * **Code:** 400 BAD REQUEST <br />
    **Content:** `{"status":400,"reasonPhrase":"Bad Request","message":"Invalid module key: org.jahia.modules/article/2.0.3.SPSHOT"}`

  OR

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `user /users/hj/di/ac/bill is not allowed to access Module Manager HTTP API`

* **Sample Call:**

  ```sh
  curl -g -s --user jon:password --request GET http://localhost:8090/modules/api/bundles/\[org.jahia.modules/article/2.0.3.SNAPSHOT,org.jahia.modules/news/2.0.3\]/_localState
  ```