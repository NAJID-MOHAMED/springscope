import { api } from './client';
import type { BeanGraph, BeanMetadata, HealthResponse } from '../../types/api';
export const getHealth = async (signal?: AbortSignal) => (await api.get<HealthResponse>('/health', { signal })).data;
export const getBeans = async (signal?: AbortSignal) => (await api.get<BeanMetadata[]>('/beans', { signal })).data;
export const getBean = async (name: string, signal?: AbortSignal) => (await api.get<BeanMetadata>(`/beans/${encodeURIComponent(name)}`, { signal })).data;
export const getGraph = async (signal?: AbortSignal) => (await api.get<BeanGraph>('/graph', { signal })).data;
