package com.java.cartridge

import javaposse.jobdsl.dsl.GeneratedItems
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.dsl.MemoryJobManagement

public class DslHelper {

    public MemoryJobManagement jm = getMemoryJobManagement()
    public GeneratedItems items

    public static final String workspaceName = 'ExampleWorkspace'
    public static final String projectName = "$workspaceName/ExampleProject"

    public DslHelper(String filePath) {
        File file = new File(filePath)
        this.items = DslScriptLoader.runDslEngine(file.text, jm)
    }

    private static def MemoryJobManagement getMemoryJobManagement() {
        MemoryJobManagement jm = new MemoryJobManagement()
        jm.parameters << [
            WORKSPACE_NAME: workspaceName,
            PROJECT_NAME  : projectName,
        ]
        return jm
    }
}
