import http from "node:http";
import {URL} from "node:url";
import {validateTelegramInitData} from "./auth/telegram.js";
import {createSession,createSessionCookie,clearSessionCookie,getSessionPlayer,revokeSession} from "./auth/session.js";
import pg from "pg";
const {Pool}=pg;
const PORT=Number(process.env.PORT||8787);
const DATABASE_URL=process.env.DATABASE_URL;
const BOT_TOKEN=process.env.TELEGRAM_BOT_TOKEN||"";
const ALLOWED_ORIGIN=process.env.ALLOWED_ORIGIN||"";
const SESSION_DAYS=Number(process.env.SESSION_DAYS||30);
if(!DATABASE_URL||!BOT_TOKEN)throw new Error("DATABASE_URL and TELEGRAM_BOT_TOKEN are required");
const pool=new Pool({connectionString:DATABASE_URL,ssl:process.env.PGSSL==="disable"?false:{rejectUnauthorized:false}});
const rooms=new Set(["main","games","relax"]);
const json={"Content-Type":"application/json; charset=utf-8","Cache-Control":"no-store"};
const origin=ALLOWED_ORIGIN||"*";
function out(res,status,data,extra={}){res.writeHead(status,{...json,"Access-Control-Allow-Origin":origin,"Access-Control-Allow-Credentials":"true",...extra});res.end(JSON.stringify(data));}
async function body(req){const a=[];for await(const x of req)a.push(x);const s=Buffer.concat(a).toString("utf8");return s?JSON.parse(s):{};}
async function me(req){return getSessionPlayer(pool,req);}
async function upsert(u){
 await pool.query("INSERT INTO players(telegram_id,username,first_name,last_name,language_code,last_seen) VALUES($1,$2,$3,$4,$5,NOW()) ON CONFLICT(telegram_id) DO UPDATE SET username=EXCLUDED.username,first_name=EXCLUDED.first_name,last_name=EXCLUDED.last_name,language_code=EXCLUDED.language_code,last_seen=NOW()",[String(u.id),String(u.username||"").slice(0,64),String(u.first_name||"").slice(0,64),String(u.last_name||"").slice(0,64),String(u.language_code||"").slice(0,16)]);
 await pool.query("INSERT INTO player_stats(telegram_id) VALUES($1) ON CONFLICT DO NOTHING",[String(u.id)]);
}
function name(p){return p.username?("@"+p.username):([p.first_name,p.last_name].filter(Boolean).join(" ")||"Игрок");}
function pair(a,b){const x=BigInt(a),y=BigInt(b);return x<y?[String(x),String(y)]:[String(y),String(x)];}
const rate=new Map();
function allowed(id,key){const k=id+":"+key,now=Date.now(),a=(rate.get(k)||[]).filter(x=>now-x<60000);if(a.length>=12){rate.set(k,a);return false;}a.push(now);rate.set(k,a);return true;}
async function router(req,res){
 if(req.method==="OPTIONS"){res.writeHead(204,{"Access-Control-Allow-Origin":origin,"Access-Control-Allow-Credentials":"true","Access-Control-Allow-Headers":"Content-Type","Access-Control-Allow-Methods":"GET,POST,OPTIONS"});return res.end();}
 const u=new URL(req.url,"http://localhost");
 if(u.pathname==="/api/health")return out(res,200,{ok:true});
 try{
  if(req.method==="POST"&&u.pathname==="/api/auth/telegram/session"){
   const requestOrigin=String(req.headers.origin||"");
   if(ALLOWED_ORIGIN&&requestOrigin&&requestOrigin!==ALLOWED_ORIGIN)return out(res,403,{error:"Origin not allowed"});
   const b=await body(req);
   const telegramUser=validateTelegramInitData(b.initData,BOT_TOKEN);
   await upsert(telegramUser);
   const session=await createSession(pool,telegramUser.id,SESSION_DAYS);
   return out(res,200,{ok:true,user:{id:String(telegramUser.id),username:telegramUser.username?("@"+telegramUser.username):"",name:name(telegramUser),avatar:telegramUser.photo_url||"👾"}},{"Set-Cookie":createSessionCookie(session.token,SESSION_DAYS)});
  }
  if(req.method==="GET"&&u.pathname==="/api/auth/me"){
   const current=await me(req);
   if(!current)return out(res,401,{error:"Not authenticated"});
   return out(res,200,{ok:true,user:{id:String(current.telegram_id),username:current.username?("@"+current.username):"",name:name(current),avatar:current.avatar||"👾"}});
  }
  if(req.method==="POST"&&u.pathname==="/api/auth/logout"){
   await revokeSession(pool,req);
   return out(res,200,{ok:true},{"Set-Cookie":clearSessionCookie()});
  }
  const user=await me(req);if(!user)return out(res,401,{error:"Telegram authorization required"});
  if(req.method==="GET"&&u.pathname==="/api/chat/messages"){
   const room=u.searchParams.get("room")||"main",limit=Math.min(100,Math.max(1,Number(u.searchParams.get("limit")||50)));if(!rooms.has(room))return out(res,400,{error:"Unknown room"});
   const q=await pool.query("SELECT m.*,p.username,p.first_name,p.last_name,p.avatar FROM room_messages m JOIN players p ON p.telegram_id=m.telegram_id WHERE m.room=$1 ORDER BY m.id DESC LIMIT $2",[room,limit]);
   return out(res,200,{messages:q.rows.reverse().map(m=>({id:String(m.id),room:m.room,playerId:String(m.telegram_id),username:m.username?("@"+m.username):"",name:[m.first_name,m.last_name].filter(Boolean).join(" ")||"Игрок",avatar:m.avatar,text:m.text,createdAt:m.created_at}))});
  }
  if(req.method==="POST"&&u.pathname==="/api/chat/messages"){
   const b=await body(req),room=String(b.room||"main"),text=String(b.text||"").trim().slice(0,500);if(!rooms.has(room)||!text)return out(res,400,{error:"Invalid room or message"});if(!allowed(String(user.telegram_id),"room"))return out(res,429,{error:"Too many messages. Try again later."});
   const q=await pool.query("INSERT INTO room_messages(room,telegram_id,text) VALUES($1,$2,$3) RETURNING id,created_at",[room,String(user.telegram_id),text]);
   await pool.query("UPDATE player_stats SET messages_sent=messages_sent+1,updated_at=NOW() WHERE telegram_id=$1",[String(user.telegram_id)]);
   return out(res,201,{ok:true,id:String(q.rows[0].id),createdAt:q.rows[0].created_at});
  }
  if(req.method==="GET"&&u.pathname==="/api/dm"){
   const q=await pool.query("SELECT x.other_id,p.username,p.first_name,p.last_name,p.avatar,d.text last_message,d.created_at updated_at FROM (SELECT CASE WHEN player_low=$1 THEN player_high ELSE player_low END other_id,MAX(id) last_id FROM dm_messages WHERE player_low=$1 OR player_high=$1 GROUP BY CASE WHEN player_low=$1 THEN player_high ELSE player_low END) x JOIN players p ON p.telegram_id=x.other_id JOIN dm_messages d ON d.id=x.last_id ORDER BY d.id DESC",[String(user.telegram_id)]);
   return out(res,200,{conversations:q.rows.map(x=>({playerId:String(x.other_id),username:x.username?("@"+x.username):"",name:[x.first_name,x.last_name].filter(Boolean).join(" ")||"Игрок",avatar:x.avatar||"👾",lastMessage:x.last_message||"",updatedAt:x.updated_at}))});
  }
  const dm=u.pathname.match(/^\/api\/dm\/([^/]+)\/messages$/);
  if(dm&&req.method==="GET"){
   const target=dm[1];if(target===String(user.telegram_id))return out(res,400,{error:"Cannot message yourself"});const [lo,hi]=pair(user.telegram_id,target),limit=Math.min(100,Math.max(1,Number(u.searchParams.get("limit")||50)));
   const q=await pool.query("SELECT d.*,p.username,p.first_name,p.last_name FROM dm_messages d JOIN players p ON p.telegram_id=d.sender_id WHERE d.player_low=$1 AND d.player_high=$2 ORDER BY d.id DESC LIMIT $3",[lo,hi,limit]);
   return out(res,200,{messages:q.rows.reverse().map(m=>({id:String(m.id),senderId:String(m.sender_id),username:m.username?("@"+m.username):"",name:[m.first_name,m.last_name].filter(Boolean).join(" ")||"Игрок",text:m.text,createdAt:m.created_at}))});
  }
  if(dm&&req.method==="POST"){
   const target=dm[1];if(target===String(user.telegram_id))return out(res,400,{error:"Cannot message yourself"});const b=await body(req),text=String(b.text||"").trim().slice(0,500);if(!text)return out(res,400,{error:"Empty message"});if(!allowed(String(user.telegram_id),"dm"))return out(res,429,{error:"Too many messages. Try again later."});
   const exists=await pool.query("SELECT telegram_id FROM players WHERE telegram_id=$1",[target]);if(!exists.rowCount)return out(res,404,{error:"Player not found"});const [lo,hi]=pair(user.telegram_id,target);
   await pool.query("INSERT INTO dm_conversations(player_low,player_high,updated_at) VALUES($1,$2,NOW()) ON CONFLICT(player_low,player_high) DO UPDATE SET updated_at=NOW()",[lo,hi]);
   const q=await pool.query("INSERT INTO dm_messages(player_low,player_high,sender_id,text) VALUES($1,$2,$3,$4) RETURNING id,created_at",[lo,hi,String(user.telegram_id),text]);
   await pool.query("UPDATE player_stats SET messages_sent=messages_sent+1,updated_at=NOW() WHERE telegram_id=$1",[String(user.telegram_id)]);
   return out(res,201,{ok:true,id:String(q.rows[0].id),createdAt:q.rows[0].created_at});
  }
  if(req.method==="POST"&&u.pathname==="/api/stats"){
   const b=await body(req),active=Array.isArray(b.activeDays)?b.activeDays.slice(-366).map(String):[];
   const v={portalSeconds:Math.max(0,Number(b.portalSeconds||0)),gameSeconds:Math.max(0,Number(b.gameSeconds||0)),chatSeconds:Math.max(0,Number(b.chatSeconds||0)),gameLaunches:Math.max(0,Number(b.gameLaunches||0)),messagesSent:Math.max(0,Number(b.messagesSent||0)),categoryOpens:Math.max(0,Number(b.categoryOpens||0)),gameViews:Math.max(0,Number(b.gameViews||0))};
   await pool.query("UPDATE player_stats SET portal_seconds=$2,game_seconds=$3,chat_seconds=$4,game_launches=$5,messages_sent=$6,category_opens=$7,game_views=$8,active_days=$9,updated_at=NOW() WHERE telegram_id=$1",
     [String(user.telegram_id),v.portalSeconds,v.gameSeconds,v.chatSeconds,Math.floor(v.gameLaunches),Math.floor(v.messagesSent),Math.floor(v.categoryOpens),Math.floor(v.gameViews),active]);
   return out(res,200,{ok:true});
  }
  const profile=u.pathname.match(/^\/api\/profile\/([^/]+)$/);
  if(profile&&req.method==="GET"){
   const target=profile[1],p=await pool.query("SELECT * FROM players WHERE telegram_id=$1",[target]);if(!p.rowCount)return out(res,404,{error:"Player not found"});const s=await pool.query("SELECT * FROM player_stats WHERE telegram_id=$1",[target]);const x=s.rows[0];
   return out(res,200,{id:String(p.rows[0].telegram_id),username:p.rows[0].username?("@"+p.rows[0].username):"",name:name(p.rows[0]),avatar:p.rows[0].avatar||"👾",stats:x?{portalSeconds:x.portal_seconds,gameSeconds:x.game_seconds,chatSeconds:x.chat_seconds,gameLaunches:x.game_launches,messagesSent:x.messages_sent,activeDays:x.active_days||[]}:{}});
  }
  return out(res,404,{error:"Not found"});
 }catch(e){console.error(e);return out(res,500,{error:"Server error"});}
}
http.createServer(router).listen(PORT,()=>{console.log("FREEzzzGames API listening on "+PORT);});
