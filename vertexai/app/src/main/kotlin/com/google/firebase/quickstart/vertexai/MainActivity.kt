/*
 * Copyright 2023 Google LLC
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

package com.google.firebase.quickstart.vertexai

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.quickstart.vertexai.feature.chat.ChatRoute
import com.google.firebase.quickstart.vertexai.feature.functioncalling.FunctionsChatRoute
import com.google.firebase.quickstart.vertexai.feature.multimodal.PhotoReasoningRoute
import com.google.firebase.quickstart.vertexai.feature.text.SummarizeRoute
import com.google.firebase.quickstart.vertexai.ui.theme.GenerativeAISample
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.Firebase
import com.google.firebase.initialize
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfigSettings

var remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

class MainActivity : ComponentActivity() {
    private fun init() {
        // [START appcheck_initialize]
        Firebase.initialize(context = this)
        Firebase.appCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance(),
        )
        // [END appcheck_initialize]
    }

    private fun initDebug() {
        // [START appcheck_initialize_debug]
        Firebase.initialize(context = this)
        Firebase.appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance(),
        )
        // [END appcheck_initialize_debug]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        init();
        initDebug();
        // Add Remote Config fetch logic to onCreate
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        // Set default values.
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)


        // Fetch and activate Remote Config values
        remoteConfig.fetchAndActivate()
            .addOnSuccessListener {
                Log.d("MainActivity", "Remote Config values fetched and activated from MainActivity")
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error fetching Remote Config from MainActivity", e)
            }
        // Add a real-time Remote Config listener
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate : ConfigUpdate) {
                Log.d(ContentValues.TAG, "Updated keys: " + configUpdate.updatedKeys);
                if (configUpdate.updatedKeys.contains("model_name")) {
                    remoteConfig.activate().addOnCompleteListener {
                    }
                }
            }

            override fun onError(error : FirebaseRemoteConfigException) {
                Log.w(ContentValues.TAG, "Config update error with code: " + error.code, error)
            }
        })
        super.onCreate(savedInstanceState)

        setContent {
            GenerativeAISample {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "menu") {
                        composable("menu") {
                            MenuScreen(onItemClicked = { routeId ->
                                navController.navigate(routeId)
                            })
                        }
                        composable("summarize") {
                            SummarizeRoute()
                        }
                        composable("photo_reasoning") {
                            PhotoReasoningRoute()
                        }
                        composable("chat") {
                            ChatRoute()
                        }
                        composable("functions_chat") {
                            FunctionsChatRoute()
                        }
                    }
                }
            }
        }
    }
}
