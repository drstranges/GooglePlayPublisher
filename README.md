# Google Play Publisher

## Overview

Provides simple way to upload your apk file on Google Play using the Google Play Developer Publishing API.

## Usage
```java -jar gppublisher.jar -n "your_app_name" -p "your_app_package_name" -a "your_service_account" -k "p12_key_file_path" -apk "apk_ile_path" -t "track"```

## Options
The following options are required: -sa, -serviceAccountEmail -n, -appName -T, -tracks -t, -track -k, -jsonKey -p, -packageName -a, -apk, -aab 
- \* `-a`, `-apk`, `-aab`  - The file path to the apk/aab artifact.
- \* `-n`, `-appName`      - The name of your application. Suggested format is "MyCompany-Application/1.0".
- \* `-k`, `-jsonKey`      - The service account key.json file path or file content as text.
- \* `-p`, `-packageName`  - The package name of the app.
- \* `-t`, `-tracks`       - Comma separated track names for assigning artifact.
                             Can be "internal", "alpha", "beta", "production", "rollout", or any custom.
                             If not set - artifact will not be assigned to any tracks.
- `-l`, `-listings`        - The file path to recent changes in format: \[BCP47 Language Code\]:\[recentchanges file path\].
                             Multiple listing thought comma. Sample: `en-US:C:\temp\listing_en.txt`.
- `-fraction`              - The rollout fraction. Acceptable values are 0.05, 0.1, 0.2, and 0.5.
- `-df`, `-deobfuscation`  - The file path to the deobfuscation file.
- `-help`                  - Print usage.

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
