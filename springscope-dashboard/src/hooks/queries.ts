import { useQuery } from '@tanstack/react-query';
import { getBeans, getGraph, getHealth } from '../lib/api/services';
export const useHealth = () => useQuery({ queryKey: ['health'], queryFn: ({ signal }) => getHealth(signal), refetchInterval: 30_000 });
export const useBeans = () => useQuery({ queryKey: ['beans'], queryFn: ({ signal }) => getBeans(signal) });
export const useGraph = () => useQuery({ queryKey: ['graph'], queryFn: ({ signal }) => getGraph(signal) });
