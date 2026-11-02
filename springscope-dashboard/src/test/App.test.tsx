import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { App } from '../app/App';
import { getBeans, getGraph, getHealth } from '../lib/api/services';

vi.mock('../lib/api/services', () => ({
  getBeans: vi.fn().mockResolvedValue([{name:'payments',type:'demo.PaymentService',scope:'singleton',singleton:true,prototype:false,aopProxy:false,lazyInit:false,role:'APPLICATION',factoryBeanName:null,factoryMethodName:null,factoryBean:false}]),
  getGraph: vi.fn().mockResolvedValue({nodes:[{id:'payments',name:'payments'}],edges:[]}),
  getHealth: vi.fn().mockRejectedValue(new Error('offline')),
}));
function renderApp(path='/'){const client=new QueryClient({defaultOptions:{queries:{retry:false}}});return render(<QueryClientProvider client={client}><MemoryRouter initialEntries={[path]}><App/></MemoryRouter></QueryClientProvider>);}
describe('dashboard',()=>{
 it('renders navigation and reports disconnected backend',async()=>{renderApp();expect(await screen.findByText('Disconnected')).toBeInTheDocument();expect(screen.getByRole('link',{name:/Bean Explorer/})).toBeInTheDocument();});
 it('searches displayed beans',async()=>{renderApp('/beans');expect(await screen.findByText('payments')).toBeInTheDocument();fireEvent.change(screen.getByPlaceholderText(/Search bean/),{target:{value:'absent'}});expect(await screen.findByText('No beans match these filters.')).toBeInTheDocument();});
 it('renders graph relationships page',async()=>{renderApp('/dependencies');expect(await screen.findByText('1 nodes · 0 relationships')).toBeInTheDocument();});
 it('exposes data fetching through API services',()=>{expect(getBeans).toBeDefined();expect(getGraph).toBeDefined();expect(getHealth).toBeDefined();});
});
