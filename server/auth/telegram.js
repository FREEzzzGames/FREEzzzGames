import crypto from "node:crypto";

const MAX_AGE_SECONDS=10*60;

export function validateTelegramInitData(raw,botToken){
  const params=new URLSearchParams(String(raw||""));
  const hash=params.get("hash");
  const authDate=Number(params.get("auth_date")||0);
  if(!hash||!authDate)throw new Error("Invalid Telegram authorization data");
  if(Math.abs(Math.floor(Date.now()/1000)-authDate)>MAX_AGE_SECONDS)throw new Error("Telegram authorization data expired");
  const pairs=[];
  for(const [key,value] of params.entries())if(key!=="hash")pairs.push([key,value]);
  pairs.sort((a,b)=>a[0].localeCompare(b[0]));
  const checkString=pairs.map(([key,value])=>key+"="+value).join("\n");
  const secretKey=crypto.createHmac("sha256","WebAppData").update(botToken).digest();
  const expected=crypto.createHmac("sha256",secretKey).update(checkString).digest("hex");
  const received=String(hash).toLowerCase();
  if(received.length!==expected.length||!crypto.timingSafeEqual(Buffer.from(expected,"hex"),Buffer.from(received,"hex")))throw new Error("Invalid Telegram signature");
  let user;
  try{user=JSON.parse(params.get("user")||"null");}catch(e){throw new Error("Invalid Telegram user data");}
  if(!user?.id)throw new Error("Telegram user missing");
  return {
    id:String(user.id),
    username:String(user.username||"").slice(0,64),
    first_name:String(user.first_name||"").slice(0,64),
    last_name:String(user.last_name||"").slice(0,64),
    language_code:String(user.language_code||"").slice(0,16),
    photo_url:String(user.photo_url||"").slice(0,2048)
  };
}
