package com.toters.twilio_chat_module.localCache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.toters.twilio_chat_module.localCache.dao.ConversationsDao
import com.toters.twilio_chat_module.localCache.dao.MessagesDao
import com.toters.twilio_chat_module.localCache.dao.ParticipantsDao
import com.toters.twilio_chat_module.localCache.entity.ConversationDataItem
import com.toters.twilio_chat_module.localCache.entity.MessageDataItem
import com.toters.twilio_chat_module.localCache.entity.ParticipantDataItem

@Database(entities = [ConversationDataItem::class, MessageDataItem::class, ParticipantDataItem::class], version = 1, exportSchema = false)
abstract class LocalCacheProvider : RoomDatabase() {

    abstract fun conversationsDao(): ConversationsDao

    abstract fun messagesDao(): MessagesDao

    abstract fun participantsDao(): ParticipantsDao

    companion object {
        val INSTANCE get() = _instance ?: error("call LocalCacheProvider.createInstance() first")

        private var _instance: LocalCacheProvider? = null

        fun createInstance(context: Context) {
            check(_instance == null) { "LocalCacheProvider singleton instance has been already created" }
            _instance = Room.databaseBuilder(
                context.applicationContext,
                LocalCacheProvider::class.java,
                "Chat.db"
            ).addMigrations().build()
        }
    }
}
