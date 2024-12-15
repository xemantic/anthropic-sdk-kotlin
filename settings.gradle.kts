val groupId = "com.xemantic.anthropic"
val name = "anthropic-sdk-kotlin"

rootProject.name = name
gradle.beforeProject {
  group = groupId
}
