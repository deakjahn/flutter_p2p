set PATH=e:\Android\protoc\bin\;e:\Android\flutter\.pub-cache\hosted\pub.dartlang.org\protoc_plugin-20.0.0\bin\;e:\Android\flutter\.pub-cache\bin\;e:\Git\cmd\;%PATH%
rem call flutter pub global deactivate protoc_plugin
rem call flutter pub global activate protoc_plugin
protoc.exe --dart_out=./lib/gen --plugin=e:\Android\flutter\.pub-cache\bin\protoc-gen-dart.bat ./protos/protos.proto
pause
rem protoc.exe --swift_out=./ios/Classes --plugin=e:\Android\flutter\.pub-cache\bin\protoc-gen-dart.bat ./protos/protos.proto
rem call flutter format ./lib/gen
