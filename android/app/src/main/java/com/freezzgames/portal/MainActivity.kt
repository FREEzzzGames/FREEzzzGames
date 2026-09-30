package com.freezzgames.portal

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

private val android.content.Context.store by preferencesDataStore("freezz_settings")
private val BG=Color(0xFF0B0B16)
private val PANEL=Color(0xFF121225)
private val CYAN=Color(0xFF00FFCC)
private val PURPLE=Color(0xFF9B5CFF)
private val TEXT=Color(0xFFE9F7F5)
private val MUTED=Color(0xFF8D93A8)

enum class Screen{HOME,CHAT,LIVE,GAME,PLAYER,PROFILE}

class MainActivity:ComponentActivity(){
 override fun onCreate(b:Bundle?){super.onCreate(b);setContent{Portal()}}
}

@Composable
fun Portal(){
 val ctx=androidx.compose.ui.platform.LocalContext.current
 val db=remember{AppDatabase.get(ctx)}
 var screen by remember{mutableStateOf(Screen.HOME)}
 var selected by remember{mutableStateOf<GameEntity?>(null)}
 var lang by remember{mutableStateOf("RU")}
 var radio by remember{mutableStateOf(false)}
 val scope=rememberCoroutineScope()
 LaunchedEffect(Unit){lang=ctx.store.data.first()[stringPreferencesKey("language")]?:"RU"}
 LaunchedEffect(lang){ctx.store.edit{it[stringPreferencesKey("language")]=lang}}
 BackHandler(screen!=Screen.HOME){
  screen=when(screen){Screen.PLAYER->Screen.GAME;Screen.CHAT,Screen.LIVE,Screen.GAME,Screen.PROFILE->Screen.HOME;else->Screen.HOME}
 }
 MaterialTheme(colorScheme=darkColorScheme(background=BG,surface=PANEL,primary=CYAN,secondary=PURPLE,onBackground=TEXT,onSurface=TEXT)){
  when(screen){
   Screen.HOME->Home({screen=it},lang,radio){radio=!radio;scope.launch{if(radio){} }}
   Screen.CHAT->Chat(db,lang,radio){radio=!radio}
   Screen.LIVE->Live(lang,radio){radio=!radio}
   Screen.GAME->GameCatalog(db,lang,radio,{radio=!radio}){selected=it;screen=Screen.PLAYER}
   Screen.PLAYER->Player(selected,lang,radio){radio=!radio}
   Screen.PROFILE->Profile(lang,radio){radio=!radio}
  }
 }
}

@Composable
private fun Header(title:String,lang:String,radio:Boolean,onRadio:()->Unit){
 Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
  Text(title,color=CYAN,style=MaterialTheme.typography.titleLarge)
  Spacer(Modifier.weight(1f))
  Text(if(radio)"RADIO PLAY" else "RADIO STOP",color=if(radio)CYAN MUTED,modifier=Modifier.border(1.dp,CYAN).clickable{onRadio()}.padding(8.dp))
  Spacer(Modifier.width(6.dp))
  Text(lang,color=TEXT,modifier=Modifier.border(1.dp,PURPLE).padding(8.dp))
 }
}

@Composable
private fun Home(nav:(Screen)->Unit,lang:String,radio:Boolean,onRadio:()->Unit){
 Column(Modifier.fillMaxSize().background(BG)){
  Header("FREEzzzGames",lang,radio,onRadio)
  Text("ARCADE PORTAL",color=MUTED,modifier=Modifier.padding(18.dp))
  Zone("CHAT","RETRO COMPUTER / LOCAL CHAT"){nav(Screen.CHAT)}
  Zone("LIVE","CREATOR STREAMS / VIDEO"){nav(Screen.LIVE)}
  Zone("GAME","CYBERPUNK GAME CATALOG"){nav(Screen.GAME)}
  Zone("PROFILE","LOCAL PROFILE / SETTINGS"){nav(Screen.PROFILE)}
  Spacer(Modifier.weight(1f))
  Text("OFFLINE-FIRST / LOCAL DATA",color=MUTED,modifier=Modifier.padding(18.dp))
 }
}

@Composable private fun Zone(a:String,b:String,on:()->Unit){
 Column(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=6.dp).border(1.dp,CYAN).background(PANEL).clickable{on()}.padding(16.dp)){
  Text(a,color=CYAN);Text(b,color=MUTED)
 }
}

@Composable
private fun Chat(db:AppDatabase,lang:String,radio:Boolean,onRadio:()->Unit){
 var room by remember{mutableStateOf("MAIN")}
 var input by remember{mutableStateOf("")}
 var lastBot by remember{mutableStateOf(0L)}
 val msgs by db.chat().messages(room).collectAsState(initial=emptyList())
 val scope=rememberCoroutineScope()
 Column(Modifier.fillMaxSize()){
  Header("CHAT / $room",lang,radio,onRadio)
  Row(Modifier.horizontalScroll(rememberScrollState()).padding(6.dp)){listOf("MAIN","GAMES","REST","DM").forEach{r->Text(r,color=if(r==room)CYAN MUTED,modifier=Modifier.padding(8.dp).clickable{room=r})}}
  LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(10.dp)){
   items(msgs){m->Text("${m.author}: ${m.text}",color=if(m.bot)CYAN TEXT,modifier=Modifier.padding(vertical=4.dp))}
   if(msgs.isEmpty())item{Text("LOCAL CHAT READY",color=MUTED)}
  }
  Row(Modifier.padding(8.dp),verticalAlignment=Alignment.CenterVertically){
   OutlinedTextField(input,{input=it},Modifier.weight(1f),singleLine=true,label={Text("MESSAGE")})
   Spacer(Modifier.width(6.dp))
   Button(onClick={
    val text=input.trim();if(text.isEmpty())return@Button
    input=""
    scope.launch{
     db.chat().insert(ChatMessageEntity(room=room,author="PLAYER",text=text))
     val now=System.currentTimeMillis()
     if(now-lastBot>5000 && Random.nextFloat()<0.85f){
      delay(350)
      val bots=db.bots().allBots()
      val bot=bots.firstOrNull()
      if(bot!=null){
       val replies=db.bots().replies(bot.id)
       val key=when{Regex("game|игр|spiel",RegexOption.IGNORE_CASE).containsMatchIn(text)->"game";Regex("help|помог|hilfe",RegexOption.IGNORE_CASE).containsMatchIn(text)->"help";Regex("live|стрим|stream",RegexOption.IGNORE_CASE).containsMatchIn(text)->"live";else->"chat"}
       val reply=replies.filter{it.keyword==key}.randomOrNull()?:replies.random()
       db.chat().insert(ChatMessageEntity(room=room,author=bot.name,text=reply.text,bot=true))
       lastBot=System.currentTimeMillis()
      }
     }
    }
   }){Text("SEND")}
  }
 }
}

@Composable
private fun GameCatalog(db:AppDatabase,lang:String,radio:Boolean,onRadio:()->Unit,onOpen:(GameEntity)->Unit){
 var games by remember{mutableStateOf(emptyList<GameEntity>())}
 var cat by remember{mutableStateOf("ALL")}
 LaunchedEffect(Unit){games=db.games().all()}
 val cats=listOf("ALL","INTERACTIVE WORLDS","CREATIVE / ART","PUZZLES / LOGIC","ARCADE / CLASSIC","SIMULATORS","EXPERIMENTS")
 val shown=if(cat=="ALL")games else games.filter{it.category==cat}
 Column(Modifier.fillMaxSize()){
  Header("GAME CATALOG",lang,radio,onRadio)
  Row(Modifier.horizontalScroll(rememberScrollState()).padding(6.dp)){cats.forEach{c->Text(c,color=if(c==cat)CYAN MUTED,modifier=Modifier.padding(7.dp).clickable{cat=c})}}
  LazyColumn(Modifier.fillMaxSize().padding(10.dp)){items(shown){g->Column(Modifier.fillMaxWidth().padding(5.dp).border(1.dp,PURPLE).background(PANEL).clickable{onOpen(g)}.padding(14.dp)){Text(g.title,color=TEXT);Text(g.category,color=MUTED)}}}
 }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun Player(game:GameEntity?,lang:String,radio:Boolean,onRadio:()->Unit){
 Column(Modifier.fillMaxSize()){
  Header(game?.title?:"GAME",lang,radio,onRadio)
  if(game==null) Text("GAME NOT SELECTED",color=MUTED,modifier=Modifier.padding(20.dp))
  else SafeWeb(game.url)
 }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SafeWeb(url:String){
 var failed by remember{mutableStateOf(false)}
 if(failed) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("EMBED BLOCKED OR OFFLINE",color=MUTED,modifier=Modifier.padding(20.dp))}
 else AndroidView(Modifier.fillMaxSize(),factory={c->WebView(c).apply{
  settings.javaScriptEnabled=true;settings.domStorageEnabled=true;settings.databaseEnabled=true;settings.mediaPlaybackRequiresUserGesture=false
  CookieManager.getInstance().setAcceptCookie(true)
  webViewClient=object:WebViewClient(){
   override fun onReceivedError(v:WebView?,r:WebResourceRequest?,e:WebResourceError?){if(r?.isForMainFrame==true)failed=true}
   override fun shouldOverrideUrlLoading(v:WebView?,r:WebResourceRequest?):Boolean{val u=r?.url?.toString()?:"";return !(u.startsWith("https://")||u.startsWith("http://"))}
  }
  webChromeClient=WebChromeClient()
  loadUrl(url)
 }})
}

@Composable
private fun Live(lang:String,radio:Boolean,onRadio:()->Unit){
 Column(Modifier.fillMaxSize()){
  Header("LIVE",lang,radio,onRadio)
  Stream("YOUTUBE LIVE","https://www.youtube.com/embed/live_stream?channel=UC_x5XG1OV2P6uZZ5FSM9Ttw")
  Stream("YOUTUBE GAMING","https://www.youtube.com/embed/live_stream?channel=UCOpNcN46UbXVavZAAC5Eq8Q")
  Text("External providers may refuse embedding. The rest of the portal remains available.",color=MUTED,modifier=Modifier.padding(12.dp))
 }
}
@SuppressLint("SetJavaScriptEnabled")
@Composable private fun Stream(title:String,url:String){
 Column(Modifier.fillMaxWidth().padding(8.dp).border(1.dp,PURPLE)){
  Text(title,color=TEXT,modifier=Modifier.padding(8.dp))
  AndroidView(Modifier.fillMaxWidth().height(210.dp),factory={c->WebView(c).apply{settings.javaScriptEnabled=true;settings.domStorageEnabled=true;settings.mediaPlaybackRequiresUserGesture=false;webChromeClient=WebChromeClient();loadUrl(url)}})
 }
}

@Composable
private fun Profile(lang:String,radio:Boolean,onRadio:()->Unit){
 Column(Modifier.fillMaxSize()){
  Header("PROFILE",lang,radio,onRadio)
  Text("LOCAL PROFILE",color=CYAN,modifier=Modifier.padding(18.dp))
  Text("Language: $lang",color=TEXT,modifier=Modifier.padding(horizontal=18.dp))
  Text("Radio: ${if(radio)"PLAY" else "STOP"}",color=TEXT,modifier=Modifier.padding(horizontal=18.dp))
  Text("Room, bot replies and game catalog are stored locally.",color=MUTED,modifier=Modifier.padding(18.dp))
 }
}
