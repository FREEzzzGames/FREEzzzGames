package com.freezzgames.portal

import android.content.Context
import androidx.room.Database
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.SupportSQLiteDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val category: String,
    val url: String,
    val description: String = ""
)

@Entity(tableName = "bots")
data class BotEntity(@PrimaryKey val id:Int,val name:String,val avatar:String,val topic:String,val style:String)

@Entity(tableName = "bot_replies", indices=[Index("botId"),Index("keyword")])
data class BotReplyEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val botId:Int,val keyword:String,val text:String)

@Entity(tableName = "chat_messages", indices=[Index("room"),Index("timestamp")])
data class ChatMessageEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val room:String,val author:String,val text:String,val timestamp:Long=System.currentTimeMillis(),val bot:Boolean=false)

@Entity(tableName="profile")
data class ProfileEntity(@PrimaryKey val id:Int=1,val coins:Int=120,val xp:Int=0,val level:Int=1,val streak:Int=0,val lastDaily:Long=0L,val gamesOpened:Int=0,val chatMessages:Int=0,val liveVisits:Int=0)

@Entity(tableName="collection")
data class CollectionEntity(@PrimaryKey val itemId:Int,val title:String,val category:String,val rarity:String,val owned:Boolean=false,val equipped:Boolean=false,val value:Int=0)

@Entity(tableName="achievements")
data class AchievementEntity(@PrimaryKey val id:Int,val title:String,val description:String,val requirement:Int,val progress:Int=0,val unlocked:Boolean=false)

@Entity(tableName="market")
data class MarketListingEntity(@PrimaryKey val id:Int,val title:String,val seller:String,val price:Int,val rarity:String,val sold:Boolean=false)

@Dao interface GameDao{@Query("SELECT * FROM games ORDER BY id") suspend fun all():List<GameEntity>}
@Dao interface BotDao{@Query("SELECT * FROM bots ORDER BY id") suspend fun all():List<BotEntity>;@Query("SELECT * FROM bot_replies WHERE botId=:botId") suspend fun replies(botId:Int):List<BotReplyEntity>}
@Dao interface ChatDao{@Query("SELECT * FROM chat_messages WHERE room=:room ORDER BY timestamp") fun messages(room:String):Flow<List<ChatMessageEntity>>;@Insert suspend fun insert(message:ChatMessageEntity);@Query("SELECT COUNT(*) FROM chat_messages") suspend fun count():Int}
@Dao interface ProfileDao{@Query("SELECT * FROM profile WHERE id=1") fun profile():Flow<ProfileEntity?>;@Query("SELECT * FROM profile WHERE id=1") suspend fun get():ProfileEntity?;@Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun save(profile:ProfileEntity)}
@Dao interface CollectionDao{@Query("SELECT * FROM collection ORDER BY itemId") fun all():Flow<List<CollectionEntity>>;@Update suspend fun update(item:CollectionEntity)}
@Dao interface AchievementDao{@Query("SELECT * FROM achievements ORDER BY id") fun all():Flow<List<AchievementEntity>>;@Update suspend fun update(item:AchievementEntity)}
@Dao interface MarketDao{@Query("SELECT * FROM market WHERE sold=0 ORDER BY id") fun active():Flow<List<MarketListingEntity>>;@Update suspend fun update(item:MarketListingEntity)}

@Database(entities=[GameEntity::class,BotEntity::class,BotReplyEntity::class,ChatMessageEntity::class,ProfileEntity::class,CollectionEntity::class,AchievementEntity::class,MarketListingEntity::class],version=2,exportSchema=false)
abstract class AppDatabase:RoomDatabase(){
 abstract fun games():GameDao
 abstract fun bots():BotDao
 abstract fun chat():ChatDao
 abstract fun profile():ProfileDao
 abstract fun collection():CollectionDao
 abstract fun achievements():AchievementDao
 abstract fun market():MarketDao
 companion object{
  @Volatile private var instance:AppDatabase?=null
  fun get(context:Context):AppDatabase=instance?:synchronized(this){instance?:Room.databaseBuilder(context.applicationContext,AppDatabase::class.java,"freezzgames.db").fallbackToDestructiveMigration().addCallback(Seed()).build().also{instance=it}}
 }
 private class Seed:Callback(){
  override fun onCreate(db:SupportSQLiteDatabase){
   val cats=listOf("INTERACTIVE WORLDS","CREATIVE / ART","PUZZLES / LOGIC","ARCADE / CLASSIC","SIMULATORS","EXPERIMENTS")
   val titles=listOf("Drive Mad","Subway Surfers","Monkey Mart","Fireboy & Watergirl","Temple Run 2","Stickman Hook","Raft Wars","Quick Draw","AutoDraw","MetaDome Sketch","Chrome Music Lab","Arts & Culture Experiments","Pixilart","Neal Fun","Wordle","Sudoku","Jigsaw Explorer","Chess Computer","Solitr","2048","Little Alchemy 2","Tetris","Pac-Man","Google Pac-Man","Retro Games","Run 3","Drift Hunters","Moto X3M","Hill Climb Racing","Idle Mining Empire","Real Cars in City","Airport Manager","Coffee Shop","Restaurant","Traffic Mania","Fire Truck","Bus Driver","City Car Driving","Chrome Experiments","AI Experiments","Physics Experiments","Pointer Pointer")
   val urls=listOf("https://poki.com/en/g/drive-mad","https://poki.com/en/g/subway-surfers","https://poki.com/en/g/monkey-mart","https://poki.com/en/g/fireboy-and-watergirl-forest-temple","https://poki.com/en/g/temple-run-2","https://poki.com/en/g/stickman-hook","https://poki.com/en/g/raft-wars","https://quickdraw.withgoogle.com/","https://www.autodraw.com/","https://sketch.metademolab.com/","https://musiclab.chromeexperiments.com/","https://artsandculture.google.com/experiment","https://www.pixilart.com/draw","https://neal.fun/","https://www.nytimes.com/games/wordle/index.html","https://sudoku.com/","https://www.jigsawexplorer.com/","https://www.chess.com/play/computer","https://www.solitr.com/","https://2048game.com/","https://littlealchemy2.com/","https://tetris.com/play-tetris","https://plays.org/game/pac-man/","https://www.google.com/logos/2010/pacman/","https://www.retrogames.cc/","https://www.coolmathgames.com/0-run-3","https://poki.com/en/g/drift-hunters","https://poki.com/en/g/moto-x3m","https://poki.com/en/g/hill-climb-racing","https://poki.com/en/g/idle-mining-empire","https://poki.com/en/g/real-cars-in-city","https://poki.com/en/g/airport-manager","https://poki.com/en/g/coffee-shop","https://poki.com/en/g/restaurant","https://poki.com/en/g/traffic-mania","https://poki.com/en/g/fire-truck","https://poki.com/en/g/bus-driver","https://poki.com/en/g/city-car-driving","https://experiments.withgoogle.com/collection/chrome","https://experiments.withgoogle.com/collection/ai","https://experiments.withgoogle.com/collection/physics","https://www.pointerpointer.com/")
   db.beginTransaction()
   try{
    titles.forEachIndexed{i,t->db.execSQL("INSERT INTO games(id,title,category,url,description) VALUES(?,?,?,?,?)",arrayOf(i+1,t,cats[i/7],urls[i],"External game. Local progress and rewards are tracked by FREEzzzGames."))}
    val bots=listOf(arrayOf(1,"NOVA","N","arcade","precise"),arrayOf(2,"BYTE","B","tech","analytical"),arrayOf(3,"PIXEL","P","art","observant"),arrayOf(4,"RAVEN","R","games","competitive"),arrayOf(5,"ECHO","E","community","calm"))
    bots.forEach{db.execSQL("INSERT INTO bots(id,name,avatar,topic,style) VALUES(?,?,?,?,?)",it)}
    val keys=listOf("hello","game","chat","help","live","radio","offline","system","art","tech")
    val stems=mapOf(1 to listOf("Аркадный контур проверен","Сектор готов к следующему сеансу","Игровой канал стабилен","Ритм системы выровнен"),2 to listOf("Контекст запроса распознан","Локальный модуль отвечает","Состояние данных проверено","Событие записано в журнал"),3 to listOf("Композиция интерфейса сохранена","Визуальный слой согласован","Контраст системы выдержан","Работа с формой продолжается"),4 to listOf("Игровой маршрут найден","Сложность зафиксирована","Серия испытаний доступна","Следующий уровень определён"),5 to listOf("Комната остаётся открытой","Сообщение принято участниками","Контекст беседы сохранён","Канал сообщества активен"))
    val ends=mapOf(1 to listOf("Можно продолжать.","Сигнал чистый.","Сеанс продолжается.","Следующий шаг доступен."),2 to listOf("Данные локальны.","Проверка завершена.","Состояние сохранено.","Внешний запрос не нужен."),3 to listOf("Баланс выдержан.","Детали на месте.","Слой готов.","Композиция не нарушена."),4 to listOf("Следующий маршрут доступен.","Серия ещё не завершена.","Цель остаётся достижимой.","Результат можно улучшить."),5 to listOf("Я остаюсь на линии.","Канал доступен.","Ответ сохранён.","Разговор продолжается."))
    var rid=1L
    for(b in 1..5)for(n in 0 until 150){
     val text = stems[b]!![n%stems[b]!!.size] + ": " + ends[b]!![(n*3)%ends[b]!!.size] + " " + b + "." + (n+1)
     db.execSQL("INSERT INTO bot_replies(id,botId,keyword,text) VALUES(?,?,?,?)",arrayOf(rid++,b,keys[(n+b)%keys.size],text))
    }
    db.execSQL("INSERT INTO profile(id,coins,xp,level,streak,lastDaily,gamesOpened,chatMessages,liveVisits) VALUES(1,120,0,1,0,0,0,0,0)")
    val collection=listOf(arrayOf(1,"Portal Core","SYSTEM","COMMON",1,1,40),arrayOf(2,"Monochrome Frame","PROFILE","COMMON",1,0,60),arrayOf(3,"Night Protocol","PROFILE","UNCOMMON",0,0,120),arrayOf(4,"Signal Archive","LIVE","UNCOMMON",0,0,160),arrayOf(5,"Black Circuit","GAME","RARE",0,0,240),arrayOf(6,"White Circuit","GAME","RARE",0,0,240),arrayOf(7,"Observer Mark","CHAT","RARE",0,0,300),arrayOf(8,"Deep Scan","SYSTEM","EPIC",0,0,450),arrayOf(9,"Cold Start","SYSTEM","EPIC",0,0,500),arrayOf(10,"Archive Key","SYSTEM","LEGENDARY",0,0,900),arrayOf(11,"Terminal Seal","PROFILE","LEGENDARY",0,0,1000),arrayOf(12,"Zero State","SYSTEM","LEGENDARY",0,0,1500))
    collection.forEach{db.execSQL("INSERT INTO collection(itemId,title,category,rarity,owned,equipped,value) VALUES(?,?,?,?,?,?,?)",it)}
    val achievements=listOf(arrayOf(1,"FIRST SIGNAL","Open the portal for the first time.",1,0,0),arrayOf(2,"EXPLORER","Open 5 external games.",5,0,0),arrayOf(3,"OBSERVER","Visit LIVE 3 times.",3,0,0),arrayOf(4,"LOCAL VOICE","Send 10 chat messages.",10,0,0),arrayOf(5,"STREAK","Reach a 3-day daily streak.",3,0,0),arrayOf(6,"COLLECTOR","Own 5 collection items.",5,0,0),arrayOf(7,"ARCHIVIST","Reach 500 XP.",500,0,0),arrayOf(8,"SYSTEM COMPLETE","Reach level 10.",10,0,0))
    achievements.forEach{db.execSQL("INSERT INTO achievements(id,title,description,requirement,progress,unlocked) VALUES(?,?,?,?,?,?)",it)}
    val listings=listOf(arrayOf(1,"Night Protocol","NOVA",120,"UNCOMMON",0),arrayOf(2,"Signal Archive","ECHO",160,"UNCOMMON",0),arrayOf(3,"Black Circuit","RAVEN",240,"RARE",0),arrayOf(4,"Observer Mark","PIXEL",300,"RARE",0),arrayOf(5,"Deep Scan","BYTE",450,"EPIC",0),arrayOf(6,"Cold Start","NOVA",500,"EPIC",0),arrayOf(7,"Archive Key","ECHO",900,"LEGENDARY",0),arrayOf(8,"Terminal Seal","RAVEN",1000,"LEGENDARY",0))
    listings.forEach{db.execSQL("INSERT INTO market(id,title,seller,price,rarity,sold) VALUES(?,?,?,?,?,?)",it)}
    db.setTransactionSuccessful()
   }finally{db.endTransaction()}
  }
 }
}
