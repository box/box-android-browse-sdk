Box Android Browse SDK
==============
This SDK enables the ability to easily browse through Box storage to pick a folder/file.

Developer Setup
--------------
The SDK can be obtained by adding it as a maven dependency, cloning the source into your project, or by downloading one of the precompiled JARs from the releases page on GitHub.

Gradle: 
```groovy 
compile 'com.box:box-android-browse-sdk:1.1.1'
```
Maven: 
```xml
<dependency>
    <groupId>com.box</groupId>
    <artifactId>box-android-browse-sdk</artifactId>
    <version>1.1.1</version>
</dependency>
```

If not using Gradle or Maven, this SDK has the following dependencies and will need to be included in your project:
* [box-android-sdk](https://github.com/box/box-android-sdk) (maven: `com.box:box-android-sdk:3.0.2`)

Quickstart
--------------
You will need a BoxSession and the BoxItem from the [box-content-sdk](https://github.com/box/box-android-content-sdk). Please refer to the documentation of the box-content-sdk for additional details.
```java
    BoxSession session = new BoxSession(MainActivity.this);
    BoxFolder folder = new BoxApiFolder(session).getInfo("<FOLDER_ID>").send();
```
####File Picker
To launch the activity to browse a given folder and pick a file:
```java
    startActivityForResult(BoxBrowseFileActivity.getLaunchIntent(MainActivity.this, "<FOLDER_ID>", session), "<YOUR_REQUEST_CODE>");
```

To receive the result from the file picker after you picked a file, implement onActivityResult in your activity:
```java
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode ==  "<YOUR_REQUEST_CODE>") {
            if (resultCode == Activity.RESULT_OK) {
                BoxFile boxFile = (BoxFile) data.getSerializableExtra(FilePickerActivity.EXTRA_BOX_FILE);
                // Your own code to handle boxFile goes here.
            } else {
                // Your error handling code.
            }
        }
    }
```

####Folder Picker
To launch the activity to browse a given folder and pick a folder:
```java
    startActivityForResult(BoxBrowseFolderActivity.getLaunchIntent(MainActivity.this, "<FOLDER_ID>", session),  "<YOUR_REQUEST_CODE>");
```

To receive the result from the file picker after you picked a folder, implement onActivityResult in your activity:
```java
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode ==  "<YOUR_REQUEST_CODE>") {
            if (resultCode == Activity.RESULT_OK) {
                BoxFolder boxFolder = (BoxFolder) data.getSerializableExtra(FolderPickerActivity.EXTRA_BOX_FOLDER);
                // Your own code to handle boxFile goes here.
            } else {
                // Your error handling code.
            }
        }
    }
```

Sample App
--------------
A sample app can be found in the [box-share-sample](https://github.com/box/box-android-share-sdk) folder.

Copyright and License
--------------
Copyright 2015 Box, Inc. All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
