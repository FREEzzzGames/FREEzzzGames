(function(){
  "use strict";
  const webApp=window.Telegram&&window.Telegram.WebApp?window.Telegram.WebApp:null;
  const API_TIMEOUT_MS=10000;
  function ready(){try{webApp?.ready?.();}catch(e){}}
  function expand(){try{webApp?.expand?.();}catch(e){}}
  function openLink(url){
    if(!url)return;
    try{if(webApp?.openLink)return webApp.openLink(url);}catch(e){}
    window.open(url,"_blank","noopener,noreferrer");
  }
  async function authenticate(apiBase){
    const initData=String(webApp?.initData||"");
    if(!initData)throw new Error("Telegram Mini App authorization data is missing");
    const controller=new AbortController();
    const timer=setTimeout(()=>controller.abort(),API_TIMEOUT_MS);
    try{
      const response=await fetch(String(apiBase).replace(/\/$/,"")+"/auth/telegram/session",{
        method:"POST",credentials:"include",headers:{"Content-Type":"application/json"},
        body:JSON.stringify({initData}),signal:controller.signal
      });
      let payload=null;try{payload=await response.json();}catch(e){}
      if(!response.ok)throw new Error(payload?.error||("HTTP "+response.status));
      return payload;
    }finally{clearTimeout(timer);}
  }
  window.FREEZZTelegramAuth=Object.freeze({
    isMiniApp:!!webApp,get initData(){return String(webApp?.initData||"");},
    ready,expand,openLink,authenticate
  });
})();