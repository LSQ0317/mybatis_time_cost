rootProject.name = "mybatis-time-cost-demo"

includeBuild("../mybatis-time-cost-mybatis") {
    dependencySubstitution {
        substitute(module("com.mybatis.timecost:mybatis-time-cost-mybatis"))
            .using(project(":"))
    }
}
