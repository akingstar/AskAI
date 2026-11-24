package com.akingstar.askai.data
import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Entity(tableName="history") data class HistoryEntry(@PrimaryKey(autoGenerate=true) val id:Long=0,val command:String,val result:String,val createdAt:Long=System.currentTimeMillis())
@Dao interface HistoryDao { @Insert suspend fun add(entry:HistoryEntry); @Query("SELECT * FROM history ORDER BY createdAt DESC LIMIT 100") fun observe():Flow<List<HistoryEntry>> }
@Database(entities=[HistoryEntry::class],version=1,exportSchema=false) abstract class AskDatabase:RoomDatabase(){ abstract fun historyDao():HistoryDao; companion object { @Volatile private var INSTANCE:AskDatabase?=null; fun get(context:Context):AskDatabase=INSTANCE?: synchronized(this){ INSTANCE?:Room.databaseBuilder(context.applicationContext,AskDatabase::class.java,"askai.db").build().also{INSTANCE=it} } } }
