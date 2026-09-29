import fs from "node:fs/promises";
import pg from "pg";

const {Pool}=pg;
const url=process.env.DATABASE_URL;
if(!url) throw new Error("DATABASE_URL is required");

const pool=new Pool({
  connectionString:url,
  ssl:process.env.PGSSL==="disable"?false:{rejectUnauthorized:false}
});

try{
  const sql=await fs.readFile(new URL("./schema.sql",import.meta.url),"utf8");
  await pool.query(sql);
  console.log("FREEzzzGames database schema is ready");
}finally{
  await pool.end();
}
