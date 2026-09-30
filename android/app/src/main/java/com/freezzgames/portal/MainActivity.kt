package com.freezzgames.portal

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

private val Context.store by preferencesDataStore("freezz_settings")
private val BG=Color(0xFF090A0C)
private val PANEL=Color(0xFF111316)
private val PANEL2=Color(0xFF17191D)
private val WHITE=Color(0xFFF1F1EE)
private val MID=Color(0xFFB7B7B0)
private val DIM=Color(0xFF686A67)
private val LINE=Color(0xFF8C8E88)
private val FONT=FontFamily.Monospace

enum class Screen{HOME,CHAT,LIVE,GAME,PLAYER,PROFILE,COLLECTION,MARKET}

data class Copy(val chat:String,val live:String,val game:String,val profile:String,val collection:String,val market:String,val play:String,val stop:String,val back:String,val send:String,val buy:String,val equip:String,val equipped:String)

private fun copy(lang:String)=when(lang){
 "DE"->Copy("CHAT","LIVE","SPIELE","PROFIL","SAMMLUNG","MARKT","RADIO AN","RADIO AUS","ZURÜCK","SENDEN","KAUFEN","AKTIV","AKTIV")
 "EN"->Copy("CHAT","LIVE","GAMES","PROFILE","COLLECTION","MARKET","RADIO ON","RADIO OFF","BACK","SEND","BUY","EQUIP","EQUIPPED")
 else->Copy("ЧАТ","LIVE","ИГРЫ","ПРОФИЛЬ","КОЛЛЕКЦИЯ","РЫНОК","РАДИО ВКЛ","РАДИО ВЫКЛ","НАЗАД","ОТПРАВИТЬ","КУПИТЬ","ЭКИПИРОВАТЬ","АКТИВНО")
}

class RadioController(private val context:Context){
 private var player:MediaPlayer?=null
 private val url="http://stream.radioparadise.com/mp3-128"
 var playing=false
  private set
 fun toggle(callback:(Boolean)->Unit){
  if(playing){stop();callback(false);return}
  try{
   player=MediaPlayer().apply{
    setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build())
    setDataSource(context,Uri.parse(url))
    setOnPreparedListener{playing=true;start();callback(true)}
    setOnErrorListener{_,_,_->playing=false;callback(false);true}
    prepareAsync()
   }
  }catch(_:Throwable){playing=false;callback(false)}
 }
 fun stop(){try{player?.stop()}catch(_:Throwable){};try{player?.release()}catch(_:Throwable){};player=null;playing=false}
}

class MainActivity:ComponentActivity(){
 override fun onCreate(state:Bundle?){super.onCreate(state);setContent{Portal()}}
}

@Composable
fun Portal(){
 val context=LocalContext.current
 val db=remember{AppDatabase.get(context)}
 val radio=remember{RadioController(context)}
 var screen by remember{mutableStateOf(Screen.HOME)}
 var selected by remember{mutableStateOf<GameEntity?>(null)}
 var lang by remember{mutableStateOf("RU")}
 var radioOn by remember{mutableStateOf(false)}
 LaunchedEffect(Unit){lang=context.store.data.first()[stringPreferencesKey("language")]?:"RU"}
 LaunchedEffect(lang){context.store.edit{it[stringPreferencesKey("language")]=lang}}
 DisposableEffect(Unit){onDispose{radio.stop()}}
 BackHandler(screen!=Screen.HOME){
  screen=when(screen){
   Screen.PLAYER->Screen.GAME
   Screen.COLLECTION,Screen.MARKET->Screen.PROFILE
   else->Screen.HOME
  }
 }
 MaterialTheme(colorScheme=darkColorScheme(background=BG,surface=PANEL,primary=WHITE,onBackground=WHITE,onSurface=WHITE)){
  when(screen){
   Screen.HOME->Home(lang,radioOn,{radio.toggle{radioOn=it}},{lang=nextLang(lang)}){screen=it}
   Screen.CHAT->Chat(db,lang,radioOn,{radio.toggle{radioOn=it}},{lang=nextLang(lang)}){screen=Screen.HOME}
   Screen.LIVE->Live(db,lang,radioOn,{radio.toggle{radioOn=it}},{lang=nextLang(lang)}){screen=Screen.HOME}
   Screen.GAME->GameCatalog(db,lang,radioOn,{radio.toggle{radioOn=it}},{lang=nextLang(lang)}){selected=it;screen=Screen.PLAYER}
   Screen.PLAYER->Player(db,selected,lang,radioOn,{radio.toggle{radioOn=it}},{lang=nextLang(lang)}){screen=Screen.GAME}
   Screen.PROFILE->Profile(db,lang,radioOn,{radio.toggle{radioOn=it}},{lang=nextLang(lang)}){screen=it}
   Screen.COLLECTION->Collection(db,lang,radioOn,{radio.toggle{radioOn=it}},{lang=nextLang(lang)}){screen=Screen.PROFILE}
   Screen.MARKET->Market(db,lang,radioOn,{radio.toggle{radioOn=it}},{lang=nextLang(lang)}){screen=Screen.PROFILE}
  }
 }
}

private fun nextLang(v:String)=when(v){"RU"->"DE";"DE"->"EN";else->"RU"}

@Composable
private fun Header(title:String,lang:String,radio:Boolean,onRadio:()->Unit,onLang:()->Unit,onBack:(()->Unit)?=null){
 Row(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
  if(onBack==null)Text("FREEzzzGames",color=WHITE,fontFamily=FONT,style=MaterialTheme.typography.titleMedium)else MetalButton(copy(lang).back){onBack()}
  Spacer(Modifier.width(8.dp))
  Text(title,color=MID,fontFamily=FONT,modifier=Modifier.weight(1f))
  MetalButton(if(radio)copy(lang).stop else copy(lang).play,onRadio)
  Spacer(Modifier.width(4.dp))
  MetalButton(lang,onLang)
 }
}

@Composable
private fun MetalButton(text:String,onClick:()->Unit)=MetalButton(text,true,onClick)

@Composable
private fun MetalButton(text:String,enabled:Boolean,onClick:()->Unit){
 Box(Modifier.clip(CutCornerShape(5.dp)).border(1.dp,if(enabled) LINE else DIM).background(if(enabled)PANEL2 else BG).clickable(enabled=enabled){onClick()}.padding(horizontal=8.dp,vertical=6.dp)){
  Text(text,color=if(enabled)WHITE else DIM,fontFamily=FONT,style=MaterialTheme.typography.labelSmall)
 }
}

@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit){
 Column(modifier.clip(CutCornerShape(6.dp)).border(1.dp,LINE).background(PANEL).padding(11.dp),content=content)
}

@Composable
private fun PortalArt(seed:Int,modifier:Modifier=Modifier){
 CanvasArt(seed,modifier)
}

@Composable
private fun CanvasArt(seed:Int,modifier:Modifier){
 androidx.compose.foundation.Canvas(modifier.background(BG)){
  val cx=size.width/2f
  val cy=size.height/2f
  val r=minOf(size.width,size.height)*.35f
  drawCircle(Color(0xFF333530),r,center=androidx.compose.ui.geometry.Offset(cx,cy),style=Stroke(2f))
  drawCircle(Color(0xFF888A84),r*.68f,center=androidx.compose.ui.geometry.Offset(cx,cy),style=Stroke(1f))
  drawCircle(WHITE,r*.23f,center=androidx.compose.ui.geometry.Offset(cx,cy),style=Stroke(2f))
  for(i in 0..11){
   val x=((i*43+seed*19)%maxOf(1,size.width.toInt())).toFloat()
   val y=((i*67+seed*13)%maxOf(1,size.height.toInt())).toFloat()
   drawCircle(DIM,if(i%3==0)2f else 1f,center=androidx.compose.ui.geometry.Offset(x,y))
  }
 }
}

@Composable
private fun Home(lang:String,radio:Boolean,onRadio:()->Unit,onLang:()->Unit,nav:(Screen)->Unit){
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(BG)){
  Header("SYSTEM / HOME",lang,radio,onRadio,onLang)
  Box(Modifier.fillMaxWidth().height(180.dp).padding(horizontal=12.dp)){PortalArt(4,Modifier.fillMaxSize())}
  Text("FREEzzzGAMES",color=WHITE,fontFamily=FONT,style=MaterialTheme.typography.headlineMedium,modifier=Modifier.padding(horizontal=15.dp))
  Text("LOCAL ARCADE NETWORK / MONOCHROME SYSTEM",color=DIM,fontFamily=FONT,modifier=Modifier.padding(horizontal=15.dp,vertical=3.dp))
  HomeZone("01",copy(lang).chat,"RETRO TERMINAL / FIVE LOCAL AGENTS"){nav(Screen.CHAT)}
  HomeZone("02",copy(lang).live,"CREATOR SIGNAL / DIRECT VIDEO"){nav(Screen.LIVE)}
  HomeZone("03",copy(lang).game,"42 TITLES / EXTERNAL WEB GAMES"){nav(Screen.GAME)}
  HomeZone("04",copy(lang).profile,"XP / COINS / DAILY / ACHIEVEMENTS"){nav(Screen.PROFILE)}
  HomeZone("05",copy(lang).market,"COLLECTIBLES / DIRECT PLAYER SALES"){nav(Screen.MARKET)}
  Text("OFFLINE-FIRST / LOCAL DATABASE / STATE PERSISTENCE",color=DIM,fontFamily=FONT,modifier=Modifier.padding(15.dp))
 }
}

@Composable private fun HomeZone(code:String,title:String,sub:String,onClick:()->Unit){
 Panel(Modifier.fillMaxWidth().padding(horizontal=11.dp,vertical=4.dp).clickable{onClick()}){
  Row(verticalAlignment=Alignment.CenterVertically){
   Text(code,color=DIM,fontFamily=FONT,modifier=Modifier.width(24.dp))
   Column(Modifier.weight(1f)){Text(title,color=WHITE,fontFamily=FONT);Text(sub,color=DIM,fontFamily=FONT,style=MaterialTheme.typography.labelSmall)}
   Text(">",color=WHITE,fontFamily=FONT)
  }
 }
}

@Composable
private fun Chat(db:AppDatabase,lang:String,radio:Boolean,onRadio:()->Unit,onLang:()->Unit,onBack:()->Unit){
 var room by remember{mutableStateOf("MAIN")}
 var input by remember{mutableStateOf("")}
 var lastBot=remember{mutableStateOf(0L)}
 var selectedBot by remember{mutableStateOf<BotEntity?>(null)}
 val messages by db.chat().messages(room).collectAsState(initial=emptyList())
 val bots=remember{mutableStateOf(emptyList<BotEntity>())}
 val scope=rememberCoroutineScope()
 LaunchedEffect(Unit){bots.value=db.bots().all()}
 Column(Modifier.fillMaxSize()){
  Header("CHAT / $room",lang,radio,onRadio,onLang,onBack)
  Row(Modifier.horizontalScroll(rememberScrollState()).padding(7.dp)){listOf("MAIN","GAMES","REST","DM").forEach{r->val label=if(r==room) "["+r+"]" else r;MetalButton(label){room=r}}}
  Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal=7.dp)){bots.value.forEach{b->MetalButton(b.name){selectedBot=if(selectedBot?.id==b.id)null else b}}}
  LazyColumn(Modifier.weight(1f).padding(9.dp)){
   items(messages){m->Row(Modifier.fillMaxWidth().padding(vertical=4.dp)){Text(m.author,color=if(m.bot) WHITE else MID,fontFamily=FONT,modifier=Modifier.width(70.dp));Text(m.text,color=WHITE,fontFamily=FONT,modifier=Modifier.weight(1f))}}
   if(messages.isEmpty())item{Text("LOCAL CHANNEL READY",color=DIM,fontFamily=FONT)}
  }
  Row(Modifier.padding(7.dp),verticalAlignment=Alignment.CenterVertically){
   OutlinedTextField(input,{input=it},Modifier.weight(1f),singleLine=true,label={Text("MESSAGE",fontFamily=FONT)})
   Spacer(Modifier.width(4.dp))
   MetalButton(copy(lang).send){
    val text=input.trim()
    if(text.isNotEmpty()){
     input=""
     scope.launch{
      db.chat().insert(ChatMessageEntity(room=room,author="PLAYER",text=text))
      val p=db.profile().get()?:ProfileEntity()
      db.profile().save(p.copy(chatMessages=p.chatMessages+1,coins=p.coins+1,xp=p.xp+2))
      val now=System.currentTimeMillis()
      if(now-lastBot.value>3500L && Random.nextFloat()<.92f){
       delay(250)
       val bot=selectedBot?:bots.value.randomOrNull()
       if(bot!=null){
        val rs=db.bots().replies(bot.id)
        val key=when{
         Regex("game|игр|spiel",RegexOption.IGNORE_CASE).containsMatchIn(text)->"game"
         Regex("help|помог|hilfe",RegexOption.IGNORE_CASE).containsMatchIn(text)->"help"
         Regex("live|стрим|stream",RegexOption.IGNORE_CASE).containsMatchIn(text)->"live"
         Regex("radio|радио",RegexOption.IGNORE_CASE).containsMatchIn(text)->"radio"
         Regex("art|арт|kunst",RegexOption.IGNORE_CASE).containsMatchIn(text)->"art"
         else->"chat"
        }
        val reply=rs.filter{it.keyword==key}.randomOrNull()?:rs.random()
        db.chat().insert(ChatMessageEntity(room=room,author=bot.name,text=reply.text,bot=true))
        lastBot.value=System.currentTimeMillis()
       }
      }
     }
    }
   }
  }
 }
}

@Composable
private fun Live(db:AppDatabase,lang:String,radio:Boolean,onRadio:()->Unit,onLang:()->Unit,onBack:()->Unit){
 var status by remember{mutableStateOf("READY")}
 LaunchedEffect(Unit){
  val p=db.profile().get()?:ProfileEntity()
  db.profile().save(p.copy(liveVisits=p.liveVisits+1,coins=p.coins+2,xp=p.xp+3))
 }
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
  Header("LIVE / SIGNAL",lang,radio,onRadio,onLang,onBack)
  Text("DIRECT VIDEO / EXTERNAL PROVIDERS",color=DIM,fontFamily=FONT,modifier=Modifier.padding(12.dp))
  LiveFrame("PRIMARY SIGNAL","https://www.youtube.com/embed/live_stream?channel=UC_x5XG1OV2P6uZZ5FSM9Ttw"){status="FALLBACK"}
  LiveFrame("SECONDARY SIGNAL","https://www.youtube.com/embed/live_stream?channel=UCOpNcN46UbXVavZAAC5Eq8Q"){status="FALLBACK"}
  Panel(Modifier.fillMaxWidth().padding(12.dp)){Text("STATUS: "+status,color=WHITE,fontFamily=FONT);Text("Provider failure does not affect HOME, CHAT or GAME.",color=DIM,fontFamily=FONT)}
 }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable private fun LiveFrame(title:String,url:String,onFail:()->Unit){
 var failed by remember{mutableStateOf(false)}
 Column(Modifier.fillMaxWidth().padding(8.dp).border(1.dp,LINE).background(PANEL)){
  Text(title,color=WHITE,fontFamily=FONT,modifier=Modifier.padding(8.dp))
  if(failed){Box(Modifier.fillMaxWidth().height(180.dp),contentAlignment=Alignment.Center){Text("EXTERNAL SIGNAL UNAVAILABLE",color=DIM,fontFamily=FONT)}}else{
   AndroidView(factory={c->WebView(c).apply{
    settings.javaScriptEnabled=true;settings.domStorageEnabled=true;settings.mediaPlaybackRequiresUserGesture=false
    webChromeClient=WebChromeClient()
    webViewClient=object:WebViewClient(){override fun onReceivedError(v:WebView?,r:WebResourceRequest?,e:WebResourceError?){if(r?.isForMainFrame==true){failed=true;onFail()}}}
    loadUrl(url)
   }}, modifier=Modifier.fillMaxWidth().height(205.dp))
  }
 }
}

@Composable
private fun GameCatalog(db:AppDatabase,lang:String,radio:Boolean,onRadio:()->Unit,onLang:()->Unit,onOpen:(GameEntity)->Unit){
 var games by remember{mutableStateOf(emptyList<GameEntity>())}
 var cat by remember{mutableStateOf("ALL")}
 LaunchedEffect(Unit){games=db.games().all()}
 val cats=listOf("ALL","INTERACTIVE WORLDS","CREATIVE / ART","PUZZLES / LOGIC","ARCADE / CLASSIC","SIMULATORS","EXPERIMENTS")
 val shown=if(cat=="ALL")games else games.filter{it.category==cat}
 Column(Modifier.fillMaxSize()){
  Header("GAME / CATALOG",lang,radio,onRadio,onLang)
  Row(Modifier.horizontalScroll(rememberScrollState()).padding(7.dp)){cats.forEach{c->MetalButton(if(c==cat)"["+c+"]" else c){cat=c}}}
  LazyColumn(Modifier.fillMaxSize().padding(horizontal=9.dp)){items(shown){g->
   Panel(Modifier.fillMaxWidth().padding(vertical=4.dp).clickable{onOpen(g)}){
    Row(verticalAlignment=Alignment.CenterVertically){
     PortalArt(g.id,Modifier.size(58.dp))
     Spacer(Modifier.width(9.dp))
     Column(Modifier.weight(1f)){Text(g.title,color=WHITE,fontFamily=FONT);Text(g.category,color=DIM,fontFamily=FONT);Text("WEBVIEW / LOCAL PROGRESS",color=MID,fontFamily=FONT,style=MaterialTheme.typography.labelSmall)}
     Text(">",color=WHITE,fontFamily=FONT)
    }
   }
  }}
 }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun Player(db:AppDatabase,game:GameEntity?,lang:String,radio:Boolean,onRadio:()->Unit,onLang:()->Unit,onBack:()->Unit){
 val context=LocalContext.current
 var failed by remember{mutableStateOf(false)}
 var favorite by remember{mutableStateOf(false)}
 LaunchedEffect(game?.id){
  if(game!=null){
   val p=db.profile().get()?:ProfileEntity()
   val xp=p.xp+5
   db.profile().save(p.copy(gamesOpened=p.gamesOpened+1,coins=p.coins+2,xp=xp,level=xp/100+1))
  }
 }
 Column(Modifier.fillMaxSize()){
  Header(game?.title?:"GAME",lang,radio,onRadio,onLang,onBack)
  if(game==null)Text("NO GAME SELECTED",color=DIM,fontFamily=FONT,modifier=Modifier.padding(18.dp))else{
   Row(Modifier.fillMaxWidth().padding(7.dp)){MetalButton(if(favorite)"FAVORITED" else "FAVORITE"){favorite=!favorite};Spacer(Modifier.width(6.dp));Text(game.category,color=DIM,fontFamily=FONT,modifier=Modifier.padding(6.dp))}
   if(failed){
    Panel(Modifier.fillMaxWidth().padding(10.dp)){
     Text("EMBED BLOCKED OR OFFLINE",color=WHITE,fontFamily=FONT)
     Text(game.url,color=DIM,fontFamily=FONT)
     MetalButton("OPEN EXTERNAL"){try{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(game.url)))}catch(_:Throwable){}}
    }
   }else{
    AndroidView(factory={c->WebView(c).apply{
     settings.javaScriptEnabled=true
     settings.domStorageEnabled=true
     settings.databaseEnabled=true
     settings.mediaPlaybackRequiresUserGesture=false
     settings.allowFileAccess=false
     settings.allowContentAccess=false
     CookieManager.getInstance().setAcceptCookie(true)
     webViewClient=object:WebViewClient(){
      override fun onReceivedError(v:WebView?,r:WebResourceRequest?,e:WebResourceError?){if(r?.isForMainFrame==true)failed=true}
      override fun shouldOverrideUrlLoading(v:WebView?,r:WebResourceRequest?):Boolean{
       val u=r?.url?.toString()?:""
       return !(u.startsWith("https://")||u.startsWith("http://"))
      }
     }
     webChromeClient=WebChromeClient()
     loadUrl(game.url)
    }}, modifier=Modifier.fillMaxSize())
   }
  }
 }
}

@Composable
private fun Profile(db:AppDatabase,lang:String,radio:Boolean,onRadio:()->Unit,onLang:()->Unit,nav:(Screen)->Unit){
 val p by db.profile().profile().collectAsState(initial=null)
 val achievements by db.achievements().all().collectAsState(initial=emptyList())
 val owned=remember(p){mutableStateOf(0)}
 LaunchedEffect(Unit){owned.value=0}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
  Header("PROFILE / SYSTEM",lang,radio,onRadio,onLang)
  Panel(Modifier.fillMaxWidth().padding(10.dp)){
   Text("PLAYER NODE",color=WHITE,fontFamily=FONT,style=MaterialTheme.typography.titleMedium)
   Text("LEVEL "+(p?.level?:1)+" / XP "+(p?.xp?:0),color=MID,fontFamily=FONT)
   Text("COINS "+(p?.coins?:0)+" / STREAK "+(p?.streak?:0),color=MID,fontFamily=FONT)
   Text("GAMES "+(p?.gamesOpened?:0)+" / CHAT "+(p?.chatMessages?:0)+" / LIVE "+(p?.liveVisits?:0),color=DIM,fontFamily=FONT)
  }
  ProfileAction(copy(lang).collection,"OWNED ITEMS / EQUIP / PROGRESSION"){nav(Screen.COLLECTION)}
  ProfileAction(copy(lang).market,"PLAYER MARKET / ZERO COMMISSION"){nav(Screen.MARKET)}
  Text("ACHIEVEMENTS",color=WHITE,fontFamily=FONT,modifier=Modifier.padding(11.dp))
  achievements.forEach{a->Panel(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=3.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text(if(a.unlocked)"OK" else "--",color=WHITE,fontFamily=FONT,modifier=Modifier.width(30.dp));Column{Text(a.title,color=WHITE,fontFamily=FONT);Text(a.description,color=DIM,fontFamily=FONT);Text(a.progress.toString()+"/"+a.requirement,color=MID,fontFamily=FONT)}}}}
 }
}

@Composable private fun ProfileAction(title:String,sub:String,onClick:()->Unit){
 Panel(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=4.dp).clickable{onClick()}){Text(title,color=WHITE,fontFamily=FONT);Text(sub,color=DIM,fontFamily=FONT)}
}

@Composable
private fun Collection(db:AppDatabase,lang:String,radio:Boolean,onRadio:()->Unit,onLang:()->Unit,onBack:()->Unit){
 val items by db.collection().all().collectAsState(initial=emptyList())
 val scope=rememberCoroutineScope()
 Column(Modifier.fillMaxSize()){
  Header("COLLECTION / ARCHIVE",lang,radio,onRadio,onLang,onBack)
  LazyColumn(Modifier.fillMaxSize().padding(9.dp)){items(items){item->
   Panel(Modifier.fillMaxWidth().padding(vertical=4.dp)){
    Row(verticalAlignment=Alignment.CenterVertically){
     PortalArt(item.itemId,Modifier.size(55.dp))
     Spacer(Modifier.width(9.dp))
     Column(Modifier.weight(1f)){Text(item.title,color=WHITE,fontFamily=FONT);Text(item.rarity+" / "+item.category,color=DIM,fontFamily=FONT);Text("VALUE "+item.value,color=MID,fontFamily=FONT)}
     if(item.owned)MetalButton(if (item.equipped) copy(lang).equipped else copy(lang).equip){scope.launch{db.collection().update(item.copy(equipped=!item.equipped))}}else Text("LOCKED",color=DIM,fontFamily=FONT)
    }
   }
  }}
 }
}

@Composable
private fun Market(db:AppDatabase,lang:String,radio:Boolean,onRadio:()->Unit,onLang:()->Unit,onBack:()->Unit){
 val listings by db.market().active().collectAsState(initial=emptyList())
 val p by db.profile().profile().collectAsState(initial=null)
 val scope=rememberCoroutineScope()
 var info by remember{mutableStateOf("")}
 Column(Modifier.fillMaxSize()){
  Header("MARKET / TERMINAL",lang,radio,onRadio,onLang,onBack)
  Panel(Modifier.fillMaxWidth().padding(9.dp)){
   Text("BALANCE "+(p?.coins?:0),color=WHITE,fontFamily=FONT)
   Text("DIRECT PLAYER SALES / ZERO COMMISSION",color=DIM,fontFamily=FONT)
  }
  LazyColumn(Modifier.fillMaxSize().padding(9.dp)){
   items(listings){ l->
    Panel(Modifier.fillMaxWidth().padding(vertical=4.dp)){
     Row(verticalAlignment=Alignment.CenterVertically){
      Column(Modifier.weight(1f)){
       Text(l.title,color=WHITE,fontFamily=FONT)
       Text(l.rarity+" / "+l.seller,color=DIM,fontFamily=FONT)
       Text(l.price.toString()+" COINS",color=MID,fontFamily=FONT)
      }
      MetalButton(copy(lang).buy, enabled=(p?.coins?:0)>=l.price, onClick={
       scope.launch{
        val current=db.profile().get()?:ProfileEntity()
        if(current.coins>=l.price){
         db.market().update(l.copy(sold=true))
         db.profile().save(current.copy(coins=current.coins-l.price,xp=current.xp+10))
         info="ACQUIRED "+l.title
        }else{
         info="INSUFFICIENT BALANCE"
        }
       }
      })
     }
    }
   }
  }
  if(info.isNotEmpty()){
   Text(info,color=WHITE,fontFamily=FONT,modifier=Modifier.padding(10.dp))
  }
 }
}
