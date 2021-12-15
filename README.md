# SputnikN codegen Proto
A SputnikN transport messages generator

### 1 step
Do that once.<br>
Download and unpack Protobuf compiler<br>
We need a `protoc` executable binary, download it here for your platform:<br>
`https://github.com/protocolbuffers/protobuf/releases/` <br>
Then unpack that and put that Path including subdirectory `bin` into system/user variable named `PATH`

### 2 step
Do that once.<br>
Install Dart plugin supporting `Protobuf`, run command:<br>
`dart pub global activate protoc_plugin` <br>
Then add specific plugin directory into system/user variables named `PATH`:<br>
- for Windows: `$HOME/AppData/Local/Pub/Cache/bin` <br>
- for Unix/MacOS systems: `~/.pub-cache/bin`

### 3 step
Do that once.<br>
We need to build project to generate Serializer builder<br>
Run in project directory: `gradle build` or `./gradlew build`

### 4 step
Do that every time `Proto` messages is changed.<br>
To generate Dart/Kotlin classes we should do below<br>
Run in project directory: `gradle :genProto -x test` or `./gradlew :genProto -x test`<br>
After generation the target files will be copied to destination projects<br>
**ATTENTION:**<br>
Destination folders `generated` of the projects is cleanup before generation to keep actual and fresh classes!<br>
Keep in mind to generate database layer objects after `Proto` generation.

### Idea of messaging
As socket send/receive messages in async way we need to have control over what exact response do we receive in specific time.<br>
In that case we implemented `TransportRequest / TransportResponse` message that have `queueId` field and there we store some `sequence id` for specific request, when we receive response we always know what exact `Future` at client should be completed.
