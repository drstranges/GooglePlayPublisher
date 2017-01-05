# Google Play Publisher

## Overview

Provides simple way to publish our apk file to Google Play using the Google Play Developer Publishing API.

## Usage
```java -jar -n "your_app_name" -p "your_app_package_name" -a "your_service_account" -k "p12_key_file_path" -apk "apk_ile_path" -t "track"```

## Parameters
- `-n`, `-applicationName` - The name of your application. If the application name is null or blank, the application will log a warning. Suggested format is "MyCompany-Application/1.0".
- `-p`, `-packageName` - The package name of the app.
- `-a`, `-serviceAccountEmail` - The service account email.
- `-k`, `-p12KeyPath` - The service account key.p12 file path.
- `-apk` - The apk file path of the apk to upload.
- `-t`, `-track` - The track for uploading the apk, can be "alpha", "beta", "production" or "rollout".

## Build
`./gradlew shadowJar`

## Used Library:
  - [Google Play Developer API client library](https://developers.google.com/android-publisher/libraries)
  - [JCommander](https://github.com/cbeust/jcommander)

License
=======

    Copyright 2016 Roman Donchenko

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.