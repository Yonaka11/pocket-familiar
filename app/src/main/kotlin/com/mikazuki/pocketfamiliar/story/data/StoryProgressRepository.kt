package com.mikazuki.pocketfamiliar.story.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mikazuki.pocketfamiliar.story.model.StoryProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.storyDataStore: DataStore<Preferences> by preferencesDataStore(name = "story_progress")

class StoryProgressRepository(private val context: Context) {

    private object Keys {
        val COMPLETED_EPISODES = stringSetPreferencesKey("completed_episode_ids")
        val MEMORY_FRAGMENTS = stringSetPreferencesKey("memory_fragment_ids")
    }

    val progressFlow: Flow<StoryProgress> = context.storyDataStore.data.map { prefs ->
        StoryProgress(
            completedEpisodeIds = prefs[Keys.COMPLETED_EPISODES]?.toSet() ?: emptySet(),
            memoryFragmentIds = prefs[Keys.MEMORY_FRAGMENTS]?.toSet() ?: emptySet(),
        )
    }

    /** Returns true only the first time this episode is completed. */
    suspend fun completeEpisode(episodeId: String, memoryFragmentId: String): Boolean {
        val snapshot = context.storyDataStore.data.first()
        val alreadyCompleted = episodeId in (snapshot[Keys.COMPLETED_EPISODES] ?: emptySet())

        context.storyDataStore.edit { prefs ->
            val episodes = prefs[Keys.COMPLETED_EPISODES]?.toMutableSet() ?: mutableSetOf()
            val memories = prefs[Keys.MEMORY_FRAGMENTS]?.toMutableSet() ?: mutableSetOf()
            episodes += episodeId
            memories += memoryFragmentId
            prefs[Keys.COMPLETED_EPISODES] = episodes
            prefs[Keys.MEMORY_FRAGMENTS] = memories
        }

        return !alreadyCompleted
    }
}
