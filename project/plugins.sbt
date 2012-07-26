resolvers += Resolver.url(
  "sbt-plugin-releases", 
  new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/")
)(Resolver.ivyStylePatterns)

addSbtPlugin("com.github.retronym" % "sbt-onejar" % "0.7")

addSbtPlugin("org.ensime" % "ensime-sbt-cmd" % "0.0.10")

// addSbtPlugin("com.twitter" % "sbt-package-dist" % "1.0.0")
