/*
 * Portions of this file are derived from DetectMagiskHide:
 * https://github.com/darvincisec/DetectMagiskHide
 * Compared upstream source:
 * https://github.com/darvincisec/DetectMagiskHide/blob/dfecbb5ef6c92916bd5e96f22251e07362d47be9/app/src/main/java/com/darvin/security/AppZygotePreload.java
 * Copyright [2020] [Darvin]
 *
 * Modified for POGO Manager: adapted the package name.
 * The exact original import revision is unknown.
 * The following license applies to the upstream-derived portions.
 * See repository-root LICENSES/Apache-2.0.txt and THIRD_PARTY_NOTICES.md.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.lokey0905.location;

import android.app.ZygotePreload;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class AppZygotePreload implements ZygotePreload {
    @Override
    public void doPreload(@NonNull ApplicationInfo appInfo) {
        System.loadLibrary("native-lib");
    }
}
