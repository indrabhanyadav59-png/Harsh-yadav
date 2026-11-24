package com.example.data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class GeneratedMissionSpec(
    val title: String,
    val description: String,
    val category: String,
    val targetCount: Long,
    val rewardCoins: Long,
    val rewardXp: Long
)

class IndraAiRepository {

    private val missionSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Short 2-5 word anime RPG mission title"),
            "description" to Schema.string(description = "Actionable 1-sentence mission briefing set in Indra open world"),
            "category" to Schema.enumeration(
                values = listOf("daily", "driving", "exploration", "pet", "multiplayer", "ai"),
                description = "Mission category"
            ),
            "targetCount" to Schema.integer(description = "Target count between 2 and 8"),
            "rewardCoins" to Schema.integer(description = "Coin reward between 80 and 350"),
            "rewardXp" to Schema.integer(description = "XP reward between 120 and 450")
        )
    )

    private val missionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = missionSchema
            }
        )
    }

    private val npcAdvisorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    suspend fun generateDynamicMission(
        playerName: String,
        level: Long,
        district: String,
        activePet: String
    ): Result<GeneratedMissionSpec> {
        return try {
            val prompt = """
                You are the Game Master AI for INDRA: ANIME LIFE RP, an open-world anime multiplayer action & life simulation RPG.
                Generate ONE fresh, exciting open-world mission for player "$playerName" (Level $level) currently in "$district" with companion pet "$activePet".
                Available world districts: Central Anime City, Hidden Village, Mountain Region, Forest Region, Coastal Region, Residential Region, Commercial Region, and Airport.
                Keep title concise and thrilling, and description under 160 characters.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                missionModel.generateContent(prompt)
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("Gemini AI returned an empty mission response."))

            val json = JSONObject(rawJson)
            val title = json.getString("title").take(75)
            val description = json.getString("description").take(220)
            val category = json.optString("category", "ai").let { cat ->
                if (cat in listOf("daily", "driving", "exploration", "pet", "multiplayer", "ai")) cat else "ai"
            }
            val targetCount = json.optLong("targetCount", 3L).coerceIn(2L, 8L)
            val rewardCoins = json.optLong("rewardCoins", 150L).coerceIn(80L, 350L)
            val rewardXp = json.optLong("rewardXp", 220L).coerceIn(120L, 450L)

            Result.success(
                GeneratedMissionSpec(
                    title = title,
                    description = description,
                    category = category,
                    targetCount = targetCount,
                    rewardCoins = rewardCoins,
                    rewardXp = rewardXp
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI mission generation timed out", e)
            Result.failure(Exception("AI Mission Master timed out after 60s. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI mission generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateNpcOrProxyDialogue(
        speakerRole: String,
        district: String,
        playerActionContext: String
    ): Result<String> {
        return try {
            val prompt = """
                Roleplay as an intelligent anime open-world $speakerRole in "$district" inside INDRA: ANIME LIFE RP.
                Context: $playerActionContext.
                Respond in 1-2 punchy, immersive sentences (max 160 characters). Never break character or mention AI/models.
            """.trimIndent()

            val response = withTimeout(45_000L) {
                npcAdvisorModel.generateContent(prompt)
            }
            val text = response.text?.trim()
                ?: return Result.failure(IllegalStateException("AI NPC returned an empty response."))
            Result.success(text.take(200))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI NPC dialogue timed out", e)
            Result.failure(Exception("NPC AI timed out. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI NPC dialogue failed", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "IndraAiRepo"
    }
}
