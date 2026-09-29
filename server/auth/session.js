import crypto from "node:crypto";

const COOKIE_NAME="freezzz_session";
const DEFAULT_DAYS=30;

function hashToken(token){return crypto.createHash("sha256").update(token).digest("hex");}
export function createSessionCookie(token,days=DEFAULT_DAYS){
  const maxAge=Math.max(1,Math.floor(days*86400));
  return COOKIE_NAME+"="+encodeURIComponent(token)+"; Path=/; HttpOnly; Secure; SameSite=None; Max-Age="+maxAge;
}
export function clearSessionCookie(){return COOKIE_NAME+"=; Path=/; HttpOnly; Secure; SameSite=None; Max-Age=0";}
export function readSessionCookie(req){
  for(const part of String(req.headers.cookie||"").split(";")){
    const i=part.indexOf("=");if(i<0)continue;
    if(part.slice(0,i).trim()===COOKIE_NAME)return decodeURIComponent(part.slice(i+1));
  }
  return "";
}
export async function createSession(pool,telegramId,days=DEFAULT_DAYS){
  const token=crypto.randomBytes(32).toString("base64url");
  const safeDays=Math.max(1,Math.floor(days));
  await pool.query("INSERT INTO sessions(token_hash,telegram_id,expires_at) VALUES($1,$2,NOW()+(($3::text)||' days')::interval)",[hashToken(token),String(telegramId),String(safeDays)]);
  return {token,expiresAt:Date.now()+safeDays*86400000};
}
export async function getSessionPlayer(pool,req){
  const token=readSessionCookie(req);if(!token)return null;
  const q=await pool.query("SELECT p.* FROM sessions s JOIN players p ON p.telegram_id=s.telegram_id WHERE s.token_hash=$1 AND s.expires_at>NOW()",[hashToken(token)]);
  return q.rows[0]||null;
}
export async function revokeSession(pool,req){
  const token=readSessionCookie(req);if(!token)return;
  await pool.query("DELETE FROM sessions WHERE token_hash=$1",[hashToken(token)]);
}
