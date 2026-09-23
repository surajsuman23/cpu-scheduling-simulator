import {schedule} from '../docs/scheduler.mjs';
const reply=(data,status=200)=>new Response(JSON.stringify(data),{status,headers:{'Content-Type':'application/json','Cache-Control':'no-store','X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer'}});
let remaining=120,windowStart=Date.now();
async function readJson(request){
 if(!/^application\/json(?:\s*;|$)/i.test(request.headers.get('content-type')||''))throw Object.assign(Error('Send application/json'),{status:415});
 const reader=request.body?.getReader();let chunks=[],length=0;if(!reader)throw Object.assign(Error('Body required'),{status:400});
 while(true){const {done,value}=await reader.read();if(done)break;length+=value.length;if(length>16384){await reader.cancel();throw Object.assign(Error('Body exceeds 16 KB'),{status:413});}chunks.push(value);}
 const data=new Uint8Array(length);let offset=0;for(const c of chunks){data.set(c,offset);offset+=c.length;}try{return JSON.parse(new TextDecoder().decode(data));}catch{throw Object.assign(Error('Invalid JSON'),{status:400});}
}
export default {async fetch(request,env){const url=new URL(request.url),path=url.pathname;try{
 if(path==='/api/health/ready')return reply({status:'ready',project:'cpu-scheduling-simulator'});
 if(path==='/api/v1/simulations'&&request.method==='POST'){
 const origin=request.headers.get('Origin');if(origin&&origin!==url.origin)return reply({error:{message:'Origin not allowed'}},403);
 if(Date.now()-windowStart>=60000){remaining=120;windowStart=Date.now()}if(remaining--<=0)return reply({error:{message:'Shared local request budget exceeded; retry in one minute'}},429);
 const input=await readJson(request);if(!input||typeof input!=='object'||Array.isArray(input)||Object.keys(input).some(k=>!['processes','quantum'].includes(k)))return reply({error:{message:'Only processes and quantum are accepted'}},422);
 try{return reply({...schedule(input),computedBy:'server'})}catch(e){return reply({error:{message:e.message}},422)}
 }
 if(path.startsWith('/api/'))return reply({error:{message:'Route not found'}},404);
 if(!['GET','HEAD'].includes(request.method))return reply({error:{message:'Method not allowed'}},405);
 return env.ASSETS?env.ASSETS.fetch(request):new Response('Asset binding unavailable',{status:503});
 }catch(e){return reply({error:{message:e.status?e.message:'Unexpected API failure'}},e.status||500)}}};
