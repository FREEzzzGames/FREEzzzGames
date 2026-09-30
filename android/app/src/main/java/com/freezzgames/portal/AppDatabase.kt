package com.freezzgames.portal

import androidx.room.*
import android.content.Context

@Entity(tableName="games")
data class GameEntity(@PrimaryKey val id:Int,val title:String,val category:String,val url:String)

@Entity(tableName="bots")
data class BotEntity(@PrimaryKey val id:Int,val name:String,val avatar:String,val topic:String)

@Entity(tableName="bot_replies",indices=[Index("botId"),Index("keyword")])
data class BotReplyEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val botId:Int,val keyword:String,val text:String)

@Entity(tableName="chat_messages")
data class ChatMessageEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val room:String,val author:String,val text:String,val timestamp:Long=System.currentTimeMillis(),val bot:Boolean=false)

@Dao interface GameDao { @Query("SELECT * FROM games ORDER BY id") suspend fun all():List<GameEntity> }
@Dao interface BotDao { @Query("SELECT * FROM bots ORDER BY id") suspend fun all():List<BotEntity>; @Query("SELECT * FROM bot_replies WHERE botId=:botId") suspend fun replies(botId:Int):List<BotReplyEntity> }
@Dao interface ChatDao {
 @Query("SELECT * FROM chat_messages WHERE room=:room ORDER BY timestamp") fun messages(room:String):kotlinx.coroutines.flow.Flow<List<ChatMessageEntity>>
 @Insert suspend fun insert(m:ChatMessageEntity)
}

@Database(entities=[GameEntity::class,BotEntity::class,BotReplyEntity::class,ChatMessageEntity::class],version=1,exportSchema=false)
abstract class AppDatabase:RoomDatabase(){
 abstract fun games():GameDao
 abstract fun bots():BotDao
 abstract fun chat():ChatDao
 companion object {
  @Volatile private var instance:AppDatabase?=null
  fun get(c:Context):AppDatabase=instance?:synchronized(this){
   instance?:Room.databaseBuilder(c.applicationContext,AppDatabase::class.java,"freezzgames.db").addCallback(Seed()).build().also{instance=it}
  }
  private class Seed:Callback(){
   override fun onCreate(db:SupportSQLiteDatabase){
    db.execSQL("BEGIN TRANSACTION")
    val cats=listOf("INTERACTIVE WORLDS","CREATIVE / ART","PUZZLES / LOGIC","ARCADE / CLASSIC","SIMULATORS","EXPERIMENTS")
    val titles=listOf(
     "Drive Mad","Subway Surfers","Monkey Mart","Fireboy & Watergirl","Temple Run 2","Stickman Hook","Raft Wars",
     "Quick Draw","AutoDraw","MetaDome Sketch","Chrome Music Lab","Arts & Culture Experiments","Pixilart","Neal Fun",
     "Wordle","Sudoku","Jigsaw Explorer","Chess Computer","Solitr","2048","Little Alchemy 2",
     "Tetris","Pac-Man","Google Pac-Man","Retro Games","Run 3","Drift Hunters","Moto X3M",
     "Hill Climb Racing","Idle Mining Empire","Real Cars in City","Airport Manager","Coffee Shop","Restaurant","Traffic Mania",
     "Fire Truck","Bus Driver","City Car Driving","Chrome Experiments","AI Experiments","Physics Experiments","Pointer Pointer"
    )
    val urls=listOf(
     "https://poki.com/en/g/drive-mad","https://poki.com/en/g/subway-surfers","https://poki.com/en/g/monkey-mart",
     "https://poki.com/en/g/fireboy-and-watergirl-forest-temple","https://poki.com/en/g/temple-run-2","https://poki.com/en/g/stickman-hook","https://poki.com/en/g/raft-wars",
     "https://quickdraw.withgoogle.com/","https://www.autodraw.com/","https://sketch.metademolab.com/","https://musiclab.chromeexperiments.com/","https://artsandculture.google.com/experiment","https://www.pixilart.com/draw","https://neal.fun/",
     "https://www.nytimes.com/games/wordle/index.html","https://sudoku.com/","https://www.jigsawexplorer.com/","https://www.chess.com/play/computer","https://www.solitr.com/","https://2048game.com/","https://littlealchemy2.com/",
     "https://tetris.com/play-tetris","https://plays.org/game/pac-man/","https://www.google.com/logos/2010/pacman/","https://www.retrogames.cc/","https://www.coolmathgames.com/0-run-3","https://poki.com/en/g/drift-hunters","https://poki.com/en/g/moto-x3m",
     "https://poki.com/en/g/hill-climb-racing","https://poki.com/en/g/idle-mining-empire","https://poki.com/en/g/real-cars-in-city","https://poki.com/en/g/airport-manager","https://poki.com/en/g/coffee-shop","https://poki.com/en/g/restaurant","https://poki.com/en/g/traffic-mania",
     "https://poki.com/en/g/fire-truck","https://poki.com/en/g/bus-driver","https://poki.com/en/g/city-car-driving","https://experiments.withgoogle.com/collection/chrome","https://experiments.withgoogle.com/collection/ai","https://experiments.withgoogle.com/collection/physics","https://www.pointerpointer.com/"
    )
    titles.forEachIndexed{i,t->db.execSQL("INSERT INTO games VALUES(?,?,?,?)",arrayOf(i+1,t,cats[i/7],urls[i]))}
    val bots=listOf("NOVA" to "arcade","BYTE" to "tech","PIXEL" to "art","RAVEN" to "games","ECHO" to "community")
    bots.forEachIndexed{i,b->db.execSQL("INSERT INTO bots VALUES(?,?,?,?)",arrayOf(i+1,b.first,b.first.take(1),b.second))}
    val keys=listOf("hello","game","chat","help","live","radio","offline","system","art","tech")
    val stems=listOf("Система зафиксировала активность","Аркадный сектор отвечает","Локальный канал сообщает","Проверка контекста завершена","Внутренний журнал обновлён","Сценарий комнаты готов","Новый сигнал принят","Состояние модуля стабильно","Запрос пользователя понятен","Локальная база доступна","Событие добавлено в очередь","Контекст разговора сохранён","Режим без сети активен","Синхронизация отложена","Игровой сектор на связи")
    val ends=listOf("Можно продолжать.","Данные остаются локальными.","Следующий ход за тобой.","Модуль готов к работе.","Внешний запрос не требуется.","Состояние сохранено.","Комната остаётся активной.","Проверка прошла без ошибок.","Сценарий доступен оффлайн.","Команда принята.")
    var id=1L
    for(b in 1..5) for(n in 0 until 150){
      val text="${stems[(n+b)%stems.size]}: ${ends[(n*3+b)%ends.size]} [${b}-${n+1}]"
      db.execSQL("INSERT INTO bot_replies VALUES(?,?,?,?)",arrayOf(id++,b,keys[(n+b)%keys.size],text))
    }
    db.execSQL("COMMIT")
   }
  }
 }
}
