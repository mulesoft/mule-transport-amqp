def UPSTREAM_PROJECTS_LIST = [ "Mule-runtime/mule/support/3.9.x" ]

Map pipelineParams = [ "upstreamProjects" : UPSTREAM_PROJECTS_LIST.join(','),
                       "mavenAdditionalArgs" : "-Djarsigner.skip",
                       "projectType" : "Runtime" ]

runtimeBuild(pipelineParams)