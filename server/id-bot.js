import crypto from "node:crypto";

const BOT_TOKEN = process.env.ID_BOT_TOKEN || "";
const POLL_MS = 1500;

function esc(v){return String(v||"").replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;");}
async function tg(method,payload={}){
  const r=await fetch("https://api.telegram.org/bot"+BOT_TOKEN+"/"+method,{method:"POST",headers:{"content-type":"application/json"},body:JSON.stringify(payload)});
  const j=await r.json();
  if(!j.ok)throw new Error(j.description||"Telegram API error");
  return j.result;
}
async function reply(chatId,text,threadId){
  const payload={chat_id:chatId,text,parse_mode:"HTML"};
  if(Number.isInteger(threadId)&&threadId>0)payload.message_thread_id=threadId;
  await tg("sendMessage",payload);
}
function topicId(message){return Number(message?.message_thread_id||message?.forum_topic_created?.message_thread_id||0)||null;}
function topicName(message){return String(message?.forum_topic_created?.name||"").trim();}

export async function startIdBot(pool){
  if(!BOT_TOKEN){console.log("ID bot disabled: ID_BOT_TOKEN is not set");return;}
  let offset=0,busy=false;
  const loop=async()=>{
    if(busy)return;
    busy=true;
    try{
      const updates=await tg("getUpdates",{offset,timeout:25,allowed_updates:["message"]});
      for(const update of updates){offset=Number(update.update_id)+1;try{await handleUpdate(pool,update);}catch(e){console.error("ID bot update error",e.message);}}
    }catch(e){console.error("ID bot polling error",e.message);}
    finally{busy=false;setTimeout(loop,POLL_MS);}
  };
  loop();
  console.log("FREEzzzGames ID bot enabled");
}

async function handleUpdate(pool,update){
  const m=update?.message;
  if(!m?.text||!m?.chat?.id)return;
  const text=String(m.text).trim();
  if(!/^\//.test(text))return;
  const command=text.split(/\s+/)[0].split("@")[0].toLowerCase();
  const chatId=String(m.chat.id);
  const threadId=topicId(m);
  const threadName=topicName(m);
  if(command==="/id"||command==="/setup"){
    await reply(m.chat.id,"<b>FREEzzzGames ID Helper</b>\n\n📦 Chat ID: <code>"+esc(chatId)+"</code>\n🧵 Thread ID: <code>"+esc(threadId||"—")+"</code>\n🏷 Topic: <code>"+esc(threadName||"текущее сообщение/общий чат")+"</code>\n\nИспользуй /register в каждой нужной теме.",threadId);
    return;
  }
  if(command==="/register"){
    if(!threadId){await reply(m.chat.id,"⚠️ Команда /register должна быть отправлена внутри Telegram Topic.");return;}
    await pool.query("INSERT INTO telegram_topics(chat_id,thread_id,topic_name,updated_at) VALUES($1,$2,$3,NOW()) ON CONFLICT(chat_id,thread_id) DO UPDATE SET topic_name=EXCLUDED.topic_name,updated_at=NOW()",[chatId,threadId,threadName]);
    await reply(m.chat.id,"✅ Тема зарегистрирована.\n\n📦 Chat ID: <code>"+esc(chatId)+"</code>\n🧵 Thread ID: <code>"+esc(threadId)+"</code>\n🏷 Topic: <code>"+esc(threadName||"без названия")+"</code>",threadId);
    return;
  }
  if(command==="/rooms"){
    const q=await pool.query("SELECT thread_id,topic_name FROM telegram_topics WHERE chat_id=$1 ORDER BY thread_id",[chatId]);
    if(!q.rowCount){await reply(m.chat.id,"Пока нет зарегистрированных тем. Открой каждую тему и отправь /register.",threadId);return;}
    const lines=q.rows.map(x=>"🧵 <b>"+esc(x.topic_name||"Тема")+"</b> — <code>"+esc(x.thread_id)+"</code>");
    await reply(m.chat.id,"<b>FREEzzzGames Topics</b>\n\n📦 Chat ID: <code>"+esc(chatId)+"</code>\n"+lines.join("\n"),threadId);
  }
}
