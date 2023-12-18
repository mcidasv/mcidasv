We're using install4j to automatically generate JREs using the JetBrains Runtime, but that doesn't include JavaFX modules (jmods).

install4j allows us to specify platform-specific jmod directories, so we just distribute the required jmods as part of the repo.

Current version of JavaFX is 21.0.1 across all platforms.
