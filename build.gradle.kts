plugins {
    alias(libs.plugins.publishdata)
}

group = "net.onelitefeather.titan"
version = "2.0.0"

publishData {
    addBuildData()
    useGitlabReposForProject("106", "https://gitlab.onelitefeather.dev/")
    publishTask("shadowJar")
}


