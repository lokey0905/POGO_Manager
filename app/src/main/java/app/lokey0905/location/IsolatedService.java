/*
 * Portions of this file are derived from DetectMagiskHide:
 * https://github.com/darvincisec/DetectMagiskHide
 * Compared upstream source:
 * https://github.com/darvincisec/DetectMagiskHide/blob/dfecbb5ef6c92916bd5e96f22251e07362d47be9/app/src/main/java/com/darvin/security/IsolatedService.java
 * Copyright [2020] [Darvin]
 *
 * Modified for POGO Manager: adapted the package name and mount-path constants.
 * The constants match an earlier upstream version; the exact import revision is unknown.
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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.system.Os;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class IsolatedService extends Service{

    private static final String[] blackListedMountPaths = { "/sbin/.magisk/", "/sbin/.core/mirror", "/sbin/.core/img", "/sbin/.core/db-0/magisk.db"};
    private static final String TAG = "DetectMagisk-Isolated";
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IIsolatedService.Stub mBinder = new IIsolatedService.Stub(){
        public boolean isMagiskPresent(){
            Log.d(TAG, "Isolated UID:"+ Os.getuid());

            boolean isMagiskPresent = false;

            File file = new File("/proc/self/mounts");

            try {
                FileInputStream fis = new FileInputStream(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
                String str;
                int count =0;
                while((str = reader.readLine()) != null && (count==0)){
                    //Log.d(TAG, "MountPath:"+ str);
                    for(String path:blackListedMountPaths){
                        if(str.contains(path)){
                            Log.d(TAG, "Blacklisted Path found "+ path);
                            count++;
                            break;
                        }
                    }
                }
                reader.close();
                fis.close();
                Log.d(TAG, "Count of detected paths "+ count);
                if(count > 0){
                    Log.d(TAG, "Found magisk in atleast 1 mount path ");
                    isMagiskPresent = true;
                }else {
                    /*Incase the java calls are hooked, there is 1 more level
                    of check in the native to detect if the same blacklisted paths are
                    found in the proc maps along with checks for su files when accessed
                    from native.Native functions can also be hooked.But requires some effort
                    if it is properly obfuscated and syscalls are used in place of libc calls
                     */
                    isMagiskPresent = Native.isMagiskPresentNative();
                    Log.d(TAG, "Found Magisk in Native " + isMagiskPresent);
                }



            } catch (IOException e) {
                e.printStackTrace();
            }
            return isMagiskPresent;
        }
    };
}
