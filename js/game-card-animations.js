(() => {
  "use strict";
  const COLORS={
    sky:"#08152b", night:"#070b18", cyan:"#43e7ff", blue:"#4c7dff", pink:"#ff4fd8",
    lime:"#8cff6a", yellow:"#ffd45a", orange:"#ff8b3d", red:"#ff5364",
    white:"#f7fbff", purple:"#9a6cff", green:"#36d98b", dark:"#101629"
  };
  const scenes={
    roottrees:"detective",password:"password",infinite:"craft",polytrack:"race",remojibus:"rebus",
    hexasort:"hex",puffpilot:"rings",knightle:"knight",buttoncountdown:"button",doodlejump:"jump",
    periodic:"elements",lightsout:"lights",fillsquare:"square",puzzle15:"tiles",horserace:"horse",
    synonymsprint:"words",hitblow:"numbers",addtown:"town",jammy:"music",dailydecision:"decision",
    ricochetdaily:"bounce",easterchilly:"ice",racinggame:"race3d",indiana:"museum",manyme:"clone",
    snake:"snake",2048:"2048",ohh1:"binary",ohn0:"logic",hextris:"hextris"
  };
  const canvases=new Map();
  function px(ctx,x,y,w,h,c){ctx.fillStyle=c;ctx.fillRect(Math.round(x),Math.round(y),Math.max(1,Math.round(w)),Math.max(1,Math.round(h)));}
  function text(ctx,s,x,y,c="#fff",size=7){ctx.fillStyle=c;ctx.font="bold "+size+"px monospace";ctx.fillText(s,x,y);}
  function clear(ctx){ctx.fillStyle=COLORS.night;ctx.fillRect(0,0,160,100);}
  function grid(ctx,c="#ffffff"){ctx.globalAlpha=.08;for(let x=0;x<160;x+=8)px(ctx,x,0,1,100,c);for(let y=0;y<100;y+=8)px(ctx,0,y,160,1,c);ctx.globalAlpha=1;}
  function draw(ctx,scene,t){
    clear(ctx);grid(ctx);
    const pulse=(Math.sin(t*2)+1)/2;
    if(scene==="detective"){
      px(ctx,8,8,144,84,"#14213a"); for(let i=0;i<7;i++){const x=15+i*20;const y=16+(i%3)*20;px(ctx,x,y,13,12,"#d9c7a0");px(ctx,x+3,y+3,7,6,COLORS.blue);}
      for(let i=0;i<6;i++){px(ctx,25+i*20,30+(i%2)*18,10,2,COLORS.red);px(ctx,30+i*20,28+(i%2)*18,2,18,COLORS.red);}
      px(ctx,74,40,12,12,COLORS.yellow);text(ctx,"?",78,49,COLORS.dark,8);text(ctx,"CLUES",9,95,COLORS.cyan,6);
    } else if(scene==="password"){
      px(ctx,15,20,130,60,"#111c32"); for(let i=0;i<6;i++){px(ctx,23+i*20,30,14,5,i<4?COLORS.green:COLORS.red);px(ctx,23+i*20,48,14,5,COLORS.blue);}
      text(ctx,"PASSWORD",22,72,COLORS.white,8);px(ctx,114,68,18,6,COLORS.yellow);
    } else if(scene==="craft"){
      const e=[["💧",35,35],["🔥",78,25],["🌍",120,42],["✨",80,70]];e.forEach((q,i)=>{px(ctx,q[1]-8,q[2]-8,16,16,i%2?COLORS.orange:COLORS.cyan);text(ctx,q[0],q[1]-5,q[2]+3,COLORS.white,8);});
      px(ctx,60,48,40,4,COLORS.pink);px(ctx,76,36,4,28,COLORS.pink);text(ctx,"CRAFT",66,92,COLORS.white,7);
    } else if(scene==="race"||scene==="race3d"){
      for(let y=20;y<100;y+=10){const w=35+y*.55;px(ctx,80-w/2,y,w,5,y%20? "#202d43":"#293b55");}
      px(ctx,72,62,16,8,COLORS.cyan);px(ctx,76,58,8,4,COLORS.white);px(ctx,69,70,5,3,COLORS.red);px(ctx,86,70,5,3,COLORS.red);
      for(let i=0;i<10;i++)px(ctx,10+i*15,15+(i%2)*8,3,3,i%2?COLORS.pink:COLORS.yellow);
      text(ctx,scene==="race3d"?"RACE":"POLY",8,12,COLORS.white,7);
    } else if(scene==="rebus"){
      const arr=["😀","+","🌧️","=","🎬"];arr.forEach((s,i)=>text(ctx,s,18+i*27,50,COLORS.white,10));text(ctx,"GUESS",60,82,COLORS.yellow,7);
    } else if(scene==="hex"){
      for(let r=0;r<5;r++)for(let c=0;c<6;c++){const x=22+c*22+(r%2)*11,y=22+r*13;ctx.fillStyle=[COLORS.cyan,COLORS.pink,COLORS.yellow,COLORS.green][(r+c)%4];ctx.beginPath();for(let k=0;k<6;k++){const a=Math.PI/3*k;ctx.lineTo(x+7*Math.cos(a),y+7*Math.sin(a));}ctx.closePath();ctx.fill();}
    } else if(scene==="rings"){
      for(let i=0;i<5;i++){const x=25+i*28,y=30+Math.sin(t*2+i)*12;ctx.strokeStyle=i%2?COLORS.cyan:COLORS.yellow;ctx.lineWidth=2;ctx.strokeRect(x,y,14,14);}
      px(ctx,70,50,8,8,COLORS.white);text(ctx,"PILOT",61,88,COLORS.cyan,7);
    } else if(scene==="knight"){
      for(let i=0;i<7;i++)for(let j=0;j<5;j++)px(ctx,30+i*14,25+j*10,(i+j)%2?6:5,(i+j)%2?6:5,(i+j)%2?COLORS.blue:"#15223b");
      px(ctx,72+Math.sin(t)*10,45,8,8,COLORS.yellow);px(ctx,120,25,8,8,COLORS.red);text(ctx,"♞",74,52,COLORS.white,8);
    } else if(scene==="button"){
      px(ctx,55,28,50,38,COLORS.dark);px(ctx,63,36,34,22,pulse>.5?COLORS.red:COLORS.cyan);text(ctx,String(3+Math.floor(pulse*7)),76,51,COLORS.white,10);text(ctx,"TAP!",67,78,COLORS.yellow,7);
    } else if(scene==="jump"){
      for(let i=0;i<5;i++)px(ctx,20+i*28,72-(i%2)*15,18,4,COLORS.green);
      const y=55-Math.abs(Math.sin(t*2))*28;px(ctx,75,y,8,8,COLORS.yellow);px(ctx,78,y-5,3,3,COLORS.orange);
    } else if(scene==="elements"){
      const labels=["H","He","C","O","Fe","Au"];labels.forEach((s,i)=>{px(ctx,14+i*24,35+(i%2)*22,18,18,[COLORS.cyan,COLORS.green,COLORS.yellow,COLORS.pink,COLORS.orange,COLORS.purple][i]);text(ctx,s,19+i*24,47+(i%2)*22,COLORS.dark,7);});text(ctx,"118",70,88,COLORS.white,7);
    } else if(scene==="lights"){
      for(let y=0;y<5;y++)for(let x=0;x<5;x++){const on=((x+y+Math.floor(t*2))%3===0);px(ctx,42+x*16,20+y*16,10,10,on?COLORS.yellow:"#24314b");}
    } else if(scene==="square"){
      px(ctx,40,25,80,50,"#16243a");for(let i=0;i<8;i++){px(ctx,48+(i%4)*16,32+Math.floor(i/4)*16,10,10,[COLORS.cyan,COLORS.pink,COLORS.green,COLORS.yellow][i%4]);}px(ctx,72,43,16,16,COLORS.white);
    } else if(scene==="tiles"){
      for(let i=0;i<16;i++){const x=30+(i%4)*25,y=20+Math.floor(i/4)*18;px(ctx,x,y,20,14,(i===5&&pulse>.5)?COLORS.yellow:"#263650");text(ctx,String((i+1)%16),x+7,y+10,COLORS.white,6);}
    } else if(scene==="horse"){
      px(ctx,15,75,130,3,COLORS.green);for(let i=0;i<4;i++){const x=25+i*30+Math.sin(t+i)*8;px(ctx,x,55-i*2,14,8,[COLORS.orange,COLORS.cyan,COLORS.pink,COLORS.yellow][i]);px(ctx,x+2,63-i*2,3,10,COLORS.white);px(ctx,x+9,63-i*2,3,10,COLORS.white);}
      text(ctx,"GO!",130,88,COLORS.yellow,7);
    } else if(scene==="words"){
      const w=["WORD","SYN","NYM","SPRINT"];w.forEach((s,i)=>text(ctx,s,20+i*31,48+(i%2)*16,i===3?COLORS.yellow:COLORS.cyan,7));
    } else if(scene==="numbers"){
      ["1","4","7","9"].forEach((s,i)=>{px(ctx,34+i*25,35,18,18,i<2?COLORS.cyan:COLORS.pink);text(ctx,s,40+i*25,48,COLORS.dark,8);});
      text(ctx,"HIT • BLOW",53,78,COLORS.white,6);
    } else if(scene==="town"){
      px(ctx,10,70,140,3,COLORS.green);for(let i=0;i<6;i++){const x=12+i*24,h=20+(i%3)*8;px(ctx,x,70-h,18,h,[COLORS.cyan,COLORS.yellow,COLORS.pink][i%3]);px(ctx,x+5,70-h-8,8,8,COLORS.orange);px(ctx,x+7,70-h+10,4,7,COLORS.dark);}
    } else if(scene==="music"){
      for(let i=0;i<9;i++){const h=10+Math.abs(Math.sin(t*3+i))*35;px(ctx,18+i*15,75-h,8,h,[COLORS.cyan,COLORS.pink,COLORS.yellow][i%3]);}text(ctx,"♪",73,22,COLORS.white,14);
    } else if(scene==="decision"){
      px(ctx,18,25,50,40,COLORS.blue);px(ctx,92,25,50,40,COLORS.pink);text(ctx,"A",39,49,COLORS.white,10);text(ctx,"B",113,49,COLORS.white,10);px(ctx,74,43,12,5,COLORS.yellow);
    } else if(scene==="bounce"){
      for(let i=0;i<8;i++){const x=15+i*18,y=75-Math.abs(Math.sin(t*3+i))*45;px(ctx,x,y,5,5,i%2?COLORS.cyan:COLORS.pink);}px(ctx,77,20,6,6,COLORS.yellow);
    } else if(scene==="ice"){
      for(let i=0;i<7;i++)px(ctx,12+i*21,65-(i%2)*20,14,20,i%2?COLORS.cyan:"#d8f7ff");px(ctx,70,20,20,12,COLORS.white);text(ctx,"❄",74,29,COLORS.blue,8);
    } else if(scene==="museum"){
      px(ctx,15,15,130,70,"#283047");for(let i=0;i<5;i++){px(ctx,25+i*23,28,16,18,"#8b6f45");px(ctx,29+i*23,32,8,10,[COLORS.cyan,COLORS.yellow,COLORS.pink][i%3]);}text(ctx,"MUSEUM",56,92,COLORS.white,7);
    } else if(scene==="clone"){
      for(let i=0;i<5;i++){const x=25+i*25;px(ctx,x,55-Math.sin(t*2+i)*5,8,14,i%2?COLORS.cyan:COLORS.pink);px(ctx,x-2,50-Math.sin(t*2+i)*5,12,5,COLORS.white);}
      text(ctx,"CLONES",55,88,COLORS.yellow,7);
    } else if(scene==="snake"){
      for(let i=0;i<8;i++)px(ctx,25+i*10,55+Math.sin(i*.8+t)*6,8,8,i===7?COLORS.yellow:COLORS.lime);px(ctx,125,30,7,7,COLORS.red);
    } else if(scene==="2048"){
      for(let i=0;i<8;i++){const x=25+(i%4)*27,y=20+Math.floor(i/4)*27;px(ctx,x,y,22,22,i===7?COLORS.orange:"#263650");text(ctx,String(2**(i%5+1)),x+5,y+14,COLORS.white,6);}
    } else if(scene==="binary"){
      for(let i=0;i<8;i++){px(ctx,35+i*12,30+(i%2)*25,8,8,i%2?COLORS.cyan:COLORS.pink);}text(ctx,"010101",47,82,COLORS.white,8);
    } else if(scene==="logic"){
      for(let i=0;i<6;i++){px(ctx,25+i*20,30,14,14,i%2?COLORS.red:COLORS.blue);text(ctx,String(i+1),30+i*20,40,COLORS.white,6);}px(ctx,55,55,50,4,COLORS.yellow);
    } else if(scene==="hextris"){
      const cx=80,cy=52;for(let i=0;i<6;i++){const a=i*Math.PI/3+t*.15;const x=cx+25*Math.cos(a),y=cy+25*Math.sin(a);px(ctx,x-5,y-5,10,10,[COLORS.cyan,COLORS.pink,COLORS.yellow,COLORS.green][i%4]);}
      px(ctx,76,48,8,8,COLORS.white);
    }
  }
  function makeCanvas(art,id){
    art.innerHTML="";
    const c=document.createElement("canvas");c.className="game-card-animation";c.width=160;c.height=100;c.setAttribute("aria-hidden","true");
    art.appendChild(c);const ctx=c.getContext("2d");ctx.imageSmoothingEnabled=false;
    canvases.set(c,{ctx,scene:scenes[id]||"square",started:performance.now()});
    return c;
  }
  function resizeCanvas(c){
    const rect=c.parentElement.getBoundingClientRect();const scale=Math.max(1,Math.ceil(Math.min(rect.width/160,rect.height/100)));
    c.style.width="100%";c.style.height="100%";
  }
  function enhance(){
    document.querySelectorAll(".game-card").forEach(card=>{
      const id=card.dataset.carouselGame;const art=card.querySelector(".game-card-art");if(!id||!art)return;
      if(art.dataset.animatedId===id)return;
      art.dataset.animatedId=id;makeCanvas(art,id);
    });
  }
  const observer=new MutationObserver(enhance);
  observer.observe(document.getElementById("gamesTrack")||document.body,{childList:true,subtree:true});
  enhance();
  let last=performance.now();
  function loop(now){
    if(now-last>24){
      canvases.forEach((v,c)=>{if(!document.body.contains(c)){canvases.delete(c);return;}v.ctx.imageSmoothingEnabled=false;draw(v.ctx,v.scene,(now-v.started)/1000);resizeCanvas(c);});
      last=now;
    }
    requestAnimationFrame(loop);
  }
  requestAnimationFrame(loop);
  const style=document.createElement("style");
  style.textContent=".game-card-art{padding:0!important;background:#050a16!important;position:relative;overflow:hidden}.game-card-animation{display:block;width:100%;height:100%;image-rendering:pixelated;image-rendering:crisp-edges}.game-card-art::after{background:linear-gradient(180deg,transparent 48%,rgba(0,0,0,.35))!important;z-index:2}.game-card-art:before{content:'';position:absolute;inset:0;z-index:3;pointer-events:none;background:repeating-linear-gradient(0deg,rgba(255,255,255,.025) 0,rgba(255,255,255,.025) 1px,transparent 1px,transparent 4px);mix-blend-mode:screen}";
  document.head.appendChild(style);
})();