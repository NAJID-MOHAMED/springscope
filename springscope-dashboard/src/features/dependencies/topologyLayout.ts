import type { BeanGraph } from '../../types/api';
import type { Edge, Node } from '@xyflow/react';

export function topologyLayout(graph: BeanGraph, selected: string | null): { nodes: Node[]; edges: Edge[] } {
  const width = 920, height = 700;
  const positions = new Map(graph.nodes.map((node, index) => {
    const angle = index * 2.399963229728653;
    const radius = 45 + Math.sqrt(index + 1) * 38;
    return [node.id, { x: width / 2 + Math.cos(angle) * radius, y: height / 2 + Math.sin(angle) * radius }];
  }));
  // Deterministic force relaxation: links pull together, nodes repel, preventing fabricated graph links.
  for (let pass = 0; pass < 90; pass++) {
    const forces = new Map(graph.nodes.map(n => [n.id, { x: 0, y: 0 }]));
    for (let i = 0; i < graph.nodes.length; i++) for (let j = i + 1; j < graph.nodes.length; j++) {
      const a = graph.nodes[i], b = graph.nodes[j], pa = positions.get(a.id)!, pb = positions.get(b.id)!;
      let dx = pa.x-pb.x, dy = pa.y-pb.y, d2 = Math.max(dx*dx+dy*dy, 100), force = 1800/d2;
      forces.get(a.id)!.x += dx*force; forces.get(a.id)!.y += dy*force;
      forces.get(b.id)!.x -= dx*force; forces.get(b.id)!.y -= dy*force;
    }
    for (const edge of graph.edges) { const a=positions.get(edge.source), b=positions.get(edge.target), fa=forces.get(edge.source), fb=forces.get(edge.target); if(!a||!b||!fa||!fb)continue;const dx=b.x-a.x,dy=b.y-a.y,d=Math.max(Math.hypot(dx,dy),1),f=(d-125)*.006;fa.x+=dx*f;fa.y+=dy*f;fb.x-=dx*f;fb.y-=dy*f; }
    const cooling = .65 * (1-pass/100);
    for(const node of graph.nodes){const p=positions.get(node.id)!,f=forces.get(node.id)!;p.x=Math.max(35,Math.min(width-35,p.x+Math.max(-8,Math.min(8,f.x))*cooling));p.y=Math.max(35,Math.min(height-35,p.y+Math.max(-8,Math.min(8,f.y))*cooling));}
  }
  const related=new Set(graph.edges.filter(e=>e.source===selected||e.target===selected).flatMap(e=>[e.source,e.target]));
  const nodes:Node[]=graph.nodes.map(bean=>({id:bean.id,position:positions.get(bean.id)!,data:{label:bean.name},type:'beanCircle',selected:bean.id===selected,style:{opacity:selected&&bean.id!==selected&&!related.has(bean.id)?.24:1}}));
  const edges:Edge[]=graph.edges.map((e,i)=>{const active=!!selected&&(e.source===selected||e.target===selected);return{id:`${e.source}-${e.target}-${i}`,source:e.source,target:e.target,type:'topology',data:{active},style:{opacity:selected&&!active?.14:1}};});
  return {nodes,edges};
}
