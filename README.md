# Maven BSP

[![Style](https://github.com/bsp-capstone/maven-bsp/actions/workflows/style.yaml/badge.svg)](https://github.com/bsp-capstone/maven-bsp/actions/workflows/style.yaml)

## Importing a project
Create following `JSON` file in `.bsp` folder within your project, e.g.:

```json
{
"name": "Maven BSP",
"version": "1.0.0",
"bspVersion": "1.0.0",
"languages": ["Java"],
"argv": ["bash", "connection script"]
}
```

In `argv` specify path to connection establishing script and set correct path to Maven BSP:

```bash
#!/usr/bin/env bash

bsp_path=

cd $bsp_path
mvn clean install

cd server
mvn exec:java -Dexec.mainClass="org.jetbrains.maven.server.ServerProxy"
```

After installing Scala plugin for IDEA, choose File -> New -> Project From Existing Sources
and BSP while importing.