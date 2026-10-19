import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { BarChart3, FileText, Image, LayoutDashboard, LogOut, Menu, Package, Percent, RotateCcw, Settings, ShoppingCart, Store, Users, Warehouse, X } from 'lucide-react'
import { CapMark } from '../../components/ui'
import { useAuth } from '../../context/AuthContext'

const NAV = [
  ['/admin', 'Dashboard', LayoutDashboard, true],
  ['/admin/orders', 'Orders', ShoppingCart],
  ['/admin/returns', 'Returns', RotateCcw],
  ['/admin/products', 'Products', Package],
  ['/admin/inventory', 'Inventory', Warehouse],
  ['/admin/customers', 'Customers', Users],
  ['/admin/quotes', 'Quotes', FileText],
  ['/admin/coupons', 'Coupons', Percent],
  ['/admin/banners', 'Banners', Image],
  ['/admin/reports', 'Reports', BarChart3],
  ['/admin/settings', 'Shipping & tax', Settings],
]

export default function AdminLayout() {
  const { user, logout } = useAuth()
  const nav = useNavigate()
  const [open, setOpen] = useState(false)
  const links = (
    <nav className="flex flex-col gap-1 p-3" onClick={() => setOpen(false)}>
      {NAV.map(([to, label, Icon, end]) => (
        <NavLink key={to} to={to} end={end} className={({ isActive }) => `relative flex items-center gap-3 rounded-xl px-3 py-3 text-sm font-bold transition-all ${isActive ? 'bg-white/15 text-white shadow-inner ring-1 ring-white/10 before:absolute before:inset-y-2 before:left-0 before:w-1 before:rounded-full before:bg-thread before:content-[""]' : 'text-white/65 hover:bg-white/[.07] hover:text-white'}`}>
          <Icon size={18} className={to === '/admin' ? 'text-thread' : ''} />{label}
        </NavLink>
      ))}
    </nav>
  )
  return (
    <div className="min-h-screen md:grid md:grid-cols-[250px_1fr]">
      <aside className="relative hidden overflow-hidden bg-gradient-to-b from-[#17233e] via-[#1b2a49] to-[#263958] text-white md:block">
        <div className="pointer-events-none absolute -left-28 top-36 h-64 w-64 rounded-full border border-white/5" />
        <div className="pointer-events-none absolute -left-16 top-48 h-40 w-40 rounded-full border border-thread/10" />
        <Link to="/" className="relative flex items-center gap-3 border-b border-white/10 px-5 py-6">
          <span className="flex h-11 w-11 items-center justify-center rounded-2xl border border-white/15 bg-white/10 shadow-inner"><CapMark className="h-7 w-8" color="#f0d39b" /></span>
          <span><span className="block font-display text-lg font-extrabold tracking-tight">CapStore</span><span className="text-[10px] font-bold uppercase tracking-[.2em] text-white/45">Control room</span></span>
        </Link>
        {links}
      </aside>
      <div className="min-w-0">
        <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-white/70 bg-white/65 px-4 shadow-[0_8px_32px_-24px_rgba(27,42,73,.4)] backdrop-blur-2xl">
          <button className="md:hidden" onClick={() => setOpen(true)} aria-label="Open menu"><Menu /></button>
          <span className="font-display text-sm font-extrabold md:hidden">CapStore <span className="font-sans font-medium text-ink-faint">/ Admin</span></span>
          <div className="ml-auto flex items-center gap-3 text-sm">
            <Link to="/" className="btn-ghost btn-sm flex items-center gap-1"><Store size={15} /> View store</Link>
            <span className="hidden rounded-full border border-white bg-white/50 px-3 py-1.5 text-xs text-ink-faint sm:inline">{user.email}</span>
            <button className="btn-ghost btn-sm" onClick={() => { logout(); nav('/') }}><LogOut size={15} /> Log out</button>
          </div>
        </header>
        {open && (
          <div className="fixed inset-0 z-50 md:hidden">
            <div className="absolute inset-0 bg-ink/50" onClick={() => setOpen(false)} />
            <aside className="absolute inset-y-0 left-0 w-72 bg-gradient-to-b from-[#17233e] to-[#263958] text-white shadow-2xl">
              <button className="absolute right-3 top-3" onClick={() => setOpen(false)} aria-label="Close menu"><X /></button>
              <Link to="/" className="flex items-center gap-3 border-b border-white/10 px-5 py-5">
                <CapMark className="h-8 w-9" color="#f0d39b" /><span className="font-display text-lg font-extrabold">CapStore <span className="text-white/50">Admin</span></span>
              </Link>
              {links}
            </aside>
          </div>
        )}
        <main className="min-h-[calc(100vh-4rem)] bg-white/10 p-4 md:p-7"><Outlet /></main>
      </div>
    </div>
  )
}
