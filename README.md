# Kabomu Library for Java

This is a port of the Kabomu library originally written in C#.NET to the JDK 8 platform.

In a nutshell, Kabomu enables building quasi web applications that can connect endpoints within localhost and even within an OS process, through IPC mechanisms other than TCP.

See the [repository for the .NET version](https://github.com/aaronicsubstances/cskabomu) for more details.

## Install
```
implementation "com.aaronicsubstances:kabomu:${INSERT_LATEST_VERSION}"
```


## Usage

The entry classes of the libary are [StandardQuasiHttpClient](https://github.com/aaronicsubstances/kabomu-java/blob/master/kabomu/src/main/java/com/aaronicsubstances/kabomu/StandardQuasiHttpClient.java) and [StandardQuasiHttpServer](https://github.com/aaronicsubstances/kabomu-java/blob/master/kabomu/src/main/java/com/aaronicsubstances/kabomu/StandardQuasiHttpServer.java).

See [Examples](https://github.com/aaronicsubstances/kabomu-java/tree/master/examples) folder for sample file serving programs. Each of those programs demonstrates an IPC mechanism as represented by main files named with "-client" or "-server" suffix. E.g. to run the TCP client example, run

```
gradlew tcp-client:run
```

The sample programs come in pairs: a client program and corresponding server program. The server program must be started first. By default a client program uploads all files from a *logs/client* folder in the current directory, to a folder created in a *logs/server* folder of the server program's current directory.

The application-example.xml files in the example directories indicate how to change the default client and server endpoints (TCP ports or paths), as well as the directories of upload and saving.