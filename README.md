# symzip-plugin
Gradle's Zip Task and unzip copyspec functionality doesn't support symbolic links.  :(

This plugin tries to add support for symlinks as best it can given Gradle's internal APIs.

## Usage

```
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'org.paleozogt:symzip-plugin:0.9.4'
    }
}
```

## Zipping

The `SymZip` task is a stand-in for Gradle's `Zip` task and generally works the same way:

```
task zipIt(type:SymZip, dependsOn:makeTestData) {
    from file("/path/to/my/files/with/symlinks/")
    archiveName "myzip.zip"
    into "foobar"
}
```

## Unzipping

Gradle doesn't have a task for unzipping, but instead supports unzipping via ```copy { }``` blocks.
There doesn't appear to be a way for a plugin to inject its behavior there, so we just have a ```SymUnzip``` task.
Note that it doesn't support complicated copyspec-style includes/excludes-- it just unzips everything to a destination while preserving symlinks.


```
task unzipIt(type:SymUnzip, dependsOn:runSymZip) {
    from "myzip.zip"
    into file("$buildDir/myfiles")
}
```
