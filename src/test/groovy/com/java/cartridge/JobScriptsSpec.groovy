package com.java.cartridge

import groovy.io.FileType
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.dsl.MemoryJobManagement
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests that all dsl scripts in the jobs directory will compile.
 */
class JobScriptsSpec extends Specification {

    @Unroll
    void 'test script #file.name should compile'(File file) {
        given:
            MemoryJobManagement jm = new MemoryJobManagement()
            jm.parameters << [
                WORKSPACE_NAME: 'ExampleWorkspace',
                PROJECT_NAME  : 'ExampleProject',
            ]

        when:
            DslScriptLoader.runDslEngine(file.text, jm)

        then:
            noExceptionThrown()

        where:
            file << jobFiles
    }

    static List<File> getJobFiles() {
        List<File> files = []
        new File('jenkins/jobs/dsl').eachFileRecurse(FileType.FILES) {
            if (it =~ /.*?\.groovy/) files << it
        }
        files
    }
}
