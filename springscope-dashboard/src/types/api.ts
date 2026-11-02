export interface HealthResponse { status: string; version: string }
export interface BeanMetadata { name: string; type: string | null; scope: string; singleton: boolean; prototype: boolean; aopProxy: boolean | null; lazyInit: boolean; role: string; factoryBeanName: string | null; factoryMethodName: string | null; factoryBean: boolean | null }
export interface GraphNode { id: string; name: string }
export interface DependencyEdge { source: string; target: string; type: string }
export interface BeanGraph { nodes: GraphNode[]; edges: DependencyEdge[] }
