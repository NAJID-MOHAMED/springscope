import type { Edge, Node } from '@xyflow/react';
import type { BeanGraph } from '../../types/api';

export function topologyLayout(graph: BeanGraph, selected: string | null): { nodes: Node[]; edges: Edge[] } {
  const center={x:520,y:390}, positions=new Map<string,{x:number;y:number}>();
  const adjacent=new Set(graph.edges.filter(e=>e.source===selected||e.target===selected).flatMap(e=>[e.source,e.target]));
  if(selected&&graph.nodes.some(n=>n.id===selected)) positions.set(selected,center);
  const first=graph.nodes.filter(n=>n.id!==selected&&adjacent.has(n.id));
  first.forEach((n,i)=>{const a=2*Math.PI*i/Math.max(first.length,1)-Math.PI/2;positions.set(n.id,{x:center.x+Math.cos(a)*190,y:center.y+Math.sin(a)*190});});
  const outer=graph.nodes.filter(n=>!positions.has(n.id));
  outer.forEach((n,i)=>{const a=i*2.399963229728653;const r=310+Math.sqrt(i+1)*34;positions.set(n.id,{x:center.x+Math.cos(a)*r,y:center.y+Math.sin(a)*r});});
  for(let pass=0;pass<55;pass++){const forces=new Map(graph.nodes.map(n=>[n.id,{x:0,y:0}]));for(let i=0;i<graph.nodes.length;i++)for(let j=i+1;j<graph.nodes.length;j++){const a=graph.nodes[i],b=graph.nodes[j],pa=positions.get(a.id)!,pb=positions.get(b.id)!;const dx=pa.x-pb.x,dy=pa.y-pb.y,d2=Math.max(dx*dx+dy*dy,2500),f=2500/d2;forces.get(a.id)!.x+=dx*f;forces.get(a.id)!.y+=dy*f;forces.get(b.id)!.x-=dx*f;forces.get(b.id)!.y-=dy*f;}for(const e of graph.edges){const a=positions.get(e.source),b=positions.get(e.target),fa=forces.get(e.source),fb=forces.get(e.target);if(!a||!b||!fa||!fb)continue;const dx=b.x-a.x,dy=b.y-a.y,d=Math.max(Math.hypot(dx,dy),1),f=(d-(adjacent.has(e.source)&&adjacent.has(e.target)?190:125))*.002;fa.x+=dx*f;fa.y+=dy*f;fb.x-=dx*f;fb.y-=dy*f;}for(const n of graph.nodes){if(n.id===selected||adjacent.has(n.id))continue;const p=positions.get(n.id)!,f=forces.get(n.id)!;p.x+=Math.max(-5,Math.min(5,f.x));p.y+=Math.max(-5,Math.min(5,f.y));}}
  const nodes:Node[]=graph.nodes.map(bean=>({id:bean.id,position:positions.get(bean.id)??center,data:{label:bean.name},type:'beanCircle',selected:bean.id===selected,style:{opacity:selected&&bean.id!==selected&&!adjacent.has(bean.id)?.18:1}}));
  const edges:Edge[]=graph.edges.map((e,i)=>{const active=!!selected&&(e.source===selected||e.target===selected);return{id:`${e.source}-${e.target}-${i}`,source:e.source,target:e.target,type:'topology',data:{active},markerEnd:active?{type:'arrowclosed',color:'#6f80f7'}:undefined,style:{opacity:selected&&!active?.13:1}};});return{nodes,edges};
}
