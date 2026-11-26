import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { BarChart3, Image, LayoutDashboard, LogOut, Menu, Package, Percent, RotateCcw, Settings, ShoppingCart, Store, Users, Warehouse, X } from 'lucide-react'
import { CapMark } from '../../components/ui'
import { useAuth } from '../../context/AuthContext'

const NAV = [
  ['Overview', [['/admin', 'Dashboard', LayoutDashboard, true], ['/admin/reports', 'Reports', BarChart3]]],
  ['Sales', [['/admin/orders', 'Orders', ShoppingCart], ['/admin/returns', 'Returns', RotateCcw]]],
  ['Catalog', [['/admin/products', 'Products', Package], ['/admin/inventory', 'Inventory', Warehouse]]],
  ['Customers', [['/admin/customers', 'Customers', Users]]],
  ['Marketing', [['/admin/coupons', 'Coupons', Percent], ['/admin/banners', 'Banners', Image]]],
  ['Settings', [['/admin/settings', 'Shipping & tax', Settings]]],
]

function Brand() {
  return (
    <Link to="/admin" className="flex items-center gap-2.5 px-5 py-5">
      <CapMark className="h-7 w-8" color="#ffffff" />
      <span className="font-display text-lg font-semibold uppercase tracking-[-0.02em]">CapStore</span>
      <span className="rounded-full bg-primary px-2 py-0.5 text-[10px] font-medium uppercase tracking-[0.08em]">Admin</span>
    </Link>
  )
}

export default function AdminLayout() {
  const { user, logout } = useAuth()
  const nav = useNavigate()
  const { pathname } = useLocation()
  const [open, setOpen] = useState(false)
  useEffect(() => { setOpen(false) }, [pathname])

  const links = (
    <nav className="flex-1 space-y-6 overflow-y-auto px-3 pb-6" aria-label="Admin">
      {NAV.map(([group, items]) => (
        <div key={group}>
          <p className="mb-1.5 px-3 text-[10px] font-semibold uppercase tracking-[0.12em] text-white/40">{group}</p>
          {items.map(([to, label, Icon, end]) => (
            <NavLink key={to} to={to} end={end} className={({ isActive }) => `relative flex items-center gap-3 rounded-sm px-3 py-2.5 text-sm font-medium transition-colors ${isActive ? 'bg-white/10 text-white before:absolute before:inset-y-1.5 before:left-0 before:w-[3px] before:rounded-full before:bg-primary before:content-[""]' : 'text-white/70 hover:bg-white/5 hover:text-white'}`}>
              <Icon size={17} />{label}
            </NavLink>
          ))}
        </div>
      ))}
    </nav>
  )

  return (
    <div className="min-h-screen bg-twill md:grid md:grid-cols-[248px_1fr]">
      <aside className="sticky top-0 hidden h-screen flex-col bg-secondary text-white md:flex">
        <Brand />
        {links}
      </aside>
      <div className="min-w-0">
        <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-tertiary bg-white px-4 md:px-8">
          <button className="-ml-1 rounded-sm p-1.5 hover:bg-twill md:hidden" onClick={() => setOpen(true)} aria-label="Open menu"><Menu /></button>
          <span className="font-display text-sm font-semibold uppercase md:hidden">CapStore <span className="font-sans font-normal normal-case text-ink-faint">/ Admin</span></span>
          <div className="ml-auto flex items-center gap-1 text-sm">
            <Link to="/" className="btn-ghost btn-sm"><Store size={15} /> <span className="hidden sm:inline">View store</span></Link>
            <span className="mx-2 hidden h-6 w-px bg-tertiary sm:block" />
            <span className="hidden items-center gap-2 sm:flex">
              <span className="flex h-8 w-8 items-center justify-center rounded-full bg-primary-soft text-xs font-semibold uppercase text-primary">{user.name?.[0] || user.email[0]}</span>
              <span className="max-w-[180px] truncate text-xs text-ink-soft">{user.email}</span>
            </span>
            <button className="btn-ghost btn-sm" onClick={() => { logout(); nav('/') }} aria-label="Log out"><LogOut size={15} /></button>
          </div>
        </header>
        {open && (
          <div className="fixed inset-0 z-50 md:hidden" role="dialog" aria-modal="true" aria-label="Admin menu">
            <div className="absolute inset-0 bg-ink/50" onClick={() => setOpen(false)} />
            <aside className="absolute inset-y-0 left-0 flex w-72 animate-fade-up flex-col bg-secondary text-white">
              <div className="flex items-center justify-between pr-3"><Brand /><button onClick={() => setOpen(false)} aria-label="Close menu" className="rounded-sm p-1.5 hover:bg-white/10"><X /></button></div>
              {links}
            </aside>
          </div>
        )}
        <main className="min-h-[calc(100vh-4rem)] p-4 md:p-8"><Outlet /></main>
      </div>
    </div>
  )
}
