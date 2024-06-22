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

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.firebase.Firebase
import com.google.firebase.quickstart.vertexai.feature.chat.ChatViewModel
import com.google.firebase.quickstart.vertexai.feature.functioncalling.FunctionsChatViewModel
import com.google.firebase.quickstart.vertexai.feature.multimodal.PhotoReasoningViewModel
import com.google.firebase.quickstart.vertexai.feature.text.SummarizeViewModel
import com.google.firebase.vertexai.type.Schema
import com.google.firebase.vertexai.type.Tool
import com.google.firebase.vertexai.type.defineFunction
import com.google.firebase.vertexai.type.generationConfig
import com.google.firebase.vertexai.vertexAI
import org.json.JSONObject
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.json.JSONException

val GenerativeViewModelFactory = object : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        viewModelClass: Class<T>,
        extras: CreationExtras
    ): T {

        return with(viewModelClass) {
            remoteConfig = FirebaseRemoteConfig.getInstance();
            val modelNameFlash = remoteConfig.getString("model_name");
            val generationConfigString = remoteConfig.getString("generation_config")
            val config = try {
                val jsonObject = JSONObject(generationConfigString) // Parse the JSON string

                generationConfig {
                    temperature = jsonObject.getDouble("temperature").toFloat()
                    topP = jsonObject.getDouble("topP").toFloat()
                    topK = jsonObject.getInt("topK")
                    maxOutputTokens = jsonObject.getInt("maxOutputTokens")
                    stopSequences = jsonObject.getJSONArray("stopSequences").let { jsonArray ->
                        List(jsonArray.length()) { i -> jsonArray.getString(i) }  // Get list from JSONArray
                    }
                }
            } catch (e: JSONException) {
                Log.e("GenerativeAIModelViewFactory", "Error parsing generationConfig JSON: ${e.message}", e)
                generationConfig {} // Create a default configuration
            }


            Log.d("MainActivity", "got model name in T as $modelNameFlash and config as ${config.stopSequences} ");

            when {

                isAssignableFrom(SummarizeViewModel::class.java) -> {

                    // Initialize a GenerativeModel with the `gemini-flash` AI model
                    // for text generation
                    val generativeModel = Firebase.vertexAI.generativeModel(
                        modelName = modelNameFlash,
                        generationConfig = config
                    )
                    SummarizeViewModel(generativeModel)
                }

                isAssignableFrom(PhotoReasoningViewModel::class.java) -> {
                    // Initialize a GenerativeModel with the `gemini-flash` AI model
                    // for multimodal text generation
                    val generativeModel = Firebase.vertexAI.generativeModel(
                        modelName = modelNameFlash,
                        generationConfig = config
                    )
                    PhotoReasoningViewModel(generativeModel)
                }

                isAssignableFrom(ChatViewModel::class.java) -> {
                    Log.d("MainActivity", "got model name in chat as $modelNameFlash and config as $config ");

                    // Initialize a GenerativeModel with the `gemini-flash` AI model for chat
                    val generativeModel = Firebase.vertexAI.generativeModel(
                        modelName = modelNameFlash,
                        generationConfig = config
                    )
                    ChatViewModel(generativeModel)
                }

                isAssignableFrom(FunctionsChatViewModel::class.java) -> {
                    // Declare the functions you want to make available to the model
                    val tools = listOf(
                        Tool(
                            listOf(
                                defineFunction(
                                    "upperCase",
                                    "Returns the upper case version of the input string",
                                    Schema.str("input", "Text to transform")
                                ) { input ->
                                    JSONObject("{\"response\": \"${input.uppercase()}\"}")
                                }
                            )
                        )
                    )

                    // Initialize a GenerativeModel with the `gemini-pro` AI model for function calling chat
                    val generativeModel = Firebase.vertexAI.generativeModel(
                        modelName = "gemini-1.5-pro-preview-0514",
                        //   generationConfig = config,
                        tools = tools
                    )
                    FunctionsChatViewModel(generativeModel)
                }

                else ->
                    throw IllegalArgumentException("Unknown ViewModel class: ${viewModelClass.name}")
            }
        } as T
    }
}
