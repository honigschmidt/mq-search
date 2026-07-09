### MQ-Search: Enterprise IBM MQ Console Client

**MQ-Search** is a lightweight, high-performance Java command-line interface (CLI) engineered to seamlessly browse, search, and extract message payloads directly from IBM MQ infrastructures without altering queue states.

### Key Features

+ **Dynamic Queue Selection:** Instantly isolate specific queues directly via interactive runtime prompts or CLI argument wildcards. Streamline repetitive workflows by loading a pre-configured list of target queues directly from local configuration files.
+ **Advanced Contextual Deep-Search:** Locate business-critical data rapidly using multi-layered search parameters. Filter the active message stream by exact string matches, wildcard keywords, or narrow the search window to a precise historical timeline using localized cryptographic timestamps.
+ **Intelligent Payload Pretty-Printing:** Accelerate manual debugging and log analysis with built-in structural parsing. The client automatically detects, formats, and syntax-highlights complex **JSON** and **XML** message payloads, transforming dense, minified production strings into highly readable, structured views.
+ **Native Multi-Environment Support:** Built with environment abstractions to manage complex enterprise staging environments smoothly. Effortlessly hot-swap configurations, connection parameters, and authentication credentials between DEV, TEST, PRE-PROD, and PROD clusters using isolated runtime configuration profiles.

Please refer to the included screen capture for an overview of command-line interface.

### Deployment & Initialization

**1. Infrastructure Mapping**

Define your target environment parameters in src/main/resources/qmconfig.json. (See the included example for schema details).

**2. Topology Discovery**

Execute the CreateQConfig.java utility located under the support package. You may optionally pass search terms as runtime arguments (e.g., CreateQConfig DEV ERR) to apply a wildcard filter to the discovery process; if omitted, all queues are included by default.

The utility will:
+ Authenticate and connect to the configured queue managers.
+ Crawl the system infrastructure to discover available queues.
+ Generate a qconfig.json topology map in your HOME directory.

Move this generated map to the resources directory before launching the application.

**3. Execution**

Launch the engine by running MainEntry.java or executing the standalone Uber-JAR artifact.

### Local Sandbox & Development
For local testing and validation, a pre-configured environment is compatible with the  [**IBM MQ container suite**](https://developer.ibm.com/tutorials/mq-connect-app-queue-manager-containers/).

**System Authorization Setup:**

After starting your container, ensure the following permissions are granted for diagnostic access:
+ setmqaut -m QM1 -t queue -n SYSTEM.ADMIN.COMMAND.QUEUE -g nobody +inq +put
+ setmqaut -m QM1 -t queue -n SYSTEM.DEFAULT.MODEL.QUEUE -g nobody +get +inq +browse

### License

This project is licensed under the **MIT No Attribution License** — see the [LICENSE](LICENSE) file for full text and parameters.