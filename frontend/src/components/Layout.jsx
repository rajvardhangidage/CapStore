import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { Heart, LogOut, Menu, Package, Search, ShoppingBag, User, X, Shield, FileText } from 'lucide-react'
import { CapMark } from './ui'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { useStoreConfig } from '../lib/hooks'

function Navbar() {
  const { user, logout, isAdmin, isB2B } = useAuth()
  const { cart } = useCart()
  const cartItemCount = cart.items.length
  const cfg = useStoreConfig()
  const nav = useNavigate()
  const [open, setOpen] = useState(false)
  const [menu, setMenu] = useState(false)
  const [q, setQ] = useState('')

  const search = (e) => {
    e.preventDefault()
    nav(`/products${q.trim() ? `?q=${encodeURIComponent(q.trim())}` : ''}`)
    setOpen(false)
  }
  const linkCls = ({ isActive }) => `text-sm font-bold ${isActive ? 'text-ink' : 'text-ink-soft hover:text-ink'}`

  return (
    <header className="sticky top-0 z-40 border-b border-white/70 bg-white/70 shadow-[0_8px_32px_-24px_rgba(27,42,73,.4)] backdrop-blur-2xl">
      <div className="mx-auto flex h-16 max-w-7xl items-center gap-4 px-4">
        <button className="md:hidden" onClick={() => setOpen(!open)} aria-label="Menu" aria-expanded={open}>{open ? <X /> : <Menu />}</button>
        <Link to="/" className="group flex items-center gap-2.5">
          <span className="flex h-10 w-10 items-center justify-center rounded-2xl border border-white bg-white/75 shadow-md shadow-ink/10 transition-transform group-hover:-rotate-3 group-hover:scale-105">
            <CapMark className="h-7 w-8" />
          </span>
          <span className="font-display text-xl font-extrabold tracking-tight text-luxe">{cfg?.storeName || 'CapStore'}</span>
        </Link>
        <nav className="ml-4 hidden items-center gap-5 md:flex">
          <NavLink to="/products" className={linkCls}>Shop</NavLink>
          {isB2B ? <NavLink to="/quotes" className={linkCls}>Quotes</NavLink> : !user && <NavLink to="/register?type=business" className={linkCls}>For business</NavLink>}
        </nav>
        <form onSubmit={search} className="ml-auto hidden max-w-xs flex-1 md:block">
          <div className="relative">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-faint" />
            <input className="input pl-9" placeholder="Search caps" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search caps" />
          </div>
        </form>
        <div className="ml-auto flex items-center gap-1 md:ml-0">
          <Link to="/wishlist" className="rounded p-2 hover:bg-twill" aria-label="Wishlist"><Heart size={20} /></Link>
          <Link to="/cart" className="relative rounded p-2 hover:bg-twill" aria-label={`Cart, ${cartItemCount} ${cartItemCount === 1 ? 'item' : 'items'}`}>
            <ShoppingBag size={20} />
            {cartItemCount > 0 && <span className="absolute -right-0.5 -top-0.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-thread px-1 text-[11px] font-extrabold">{cartItemCount}</span>}
          </Link>
          {user ? (
            <div className="relative">
              <button onClick={() => setMenu(!menu)} className="flex items-center gap-2 rounded p-2 hover:bg-twill" aria-expanded={menu} aria-label="Account menu">
                <User size={20} /><span className="hidden max-w-[110px] truncate text-sm font-bold lg:inline">{user.name.split(' ')[0]}</span>
              </button>
              {menu && (
                <>
                  <div className="fixed inset-0 z-40" onClick={() => setMenu(false)} />
                  <div className="absolute right-0 z-50 mt-2 w-52 rounded-lg border border-twill-dark bg-white py-1 shadow-lg" onClick={() => setMenu(false)}>
                    <Link className="flex items-center gap-2 px-4 py-2 text-sm hover:bg-twill" to="/orders"><Package size={16} />My orders</Link>
                    <Link className="flex items-center gap-2 px-4 py-2 text-sm hover:bg-twill" to="/account"><User size={16} />Account & addresses</Link>
                    {isB2B && <Link className="flex items-center gap-2 px-4 py-2 text-sm hover:bg-twill" to="/quotes"><FileText size={16} />Quote requests</Link>}
                    {isAdmin && <Link className="flex items-center gap-2 px-4 py-2 text-sm font-bold hover:bg-twill" to="/admin"><Shield size={16} />Admin panel</Link>}
                    <button className="flex w-full items-center gap-2 px-4 py-2 text-left text-sm hover:bg-twill" onClick={() => { logout(); nav('/') }}><LogOut size={16} />Log out</button>
                  </div>
                </>
              )}
            </div>
          ) : (
            <Link to="/login" className="btn-primary btn-sm ml-1">Log in</Link>
          )}
        </div>
      </div>
      {open && (
        <div className="border-t border-twill-dark bg-white p-4 md:hidden">
          <form onSubmit={search} className="mb-3"><input className="input" placeholder="Search caps" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search caps" /></form>
          <div className="flex flex-col gap-3" onClick={() => setOpen(false)}>
            <Link to="/products" className="font-bold">Shop all caps</Link>
            {isB2B ? <Link to="/quotes" className="font-bold">Quote requests</Link> : !user && <Link to="/register?type=business" className="font-bold">For business</Link>}
            {user && <Link to="/orders" className="font-bold">My orders</Link>}
          </div>
        </div>
      )}
    </header>
  )
}

function Footer() {
  const cfg = useStoreConfig()
  return (
    <footer className="relative mt-20 overflow-hidden bg-gradient-to-br from-[#17233e] via-ink to-[#283b60] text-white">
      <div className="pointer-events-none absolute -right-24 -top-32 h-80 w-80 rounded-full border border-white/10" />
      <div className="pointer-events-none absolute -right-10 -top-16 h-52 w-52 rounded-full border border-thread/20" />
      <div className="mx-auto grid max-w-7xl gap-8 px-4 py-10 sm:grid-cols-3">
        <div>
          <div className="flex items-center gap-2"><CapMark className="h-8 w-9" color="#f0b429" /><span className="font-display text-xl font-extrabold">{cfg?.storeName || 'CapStore'}</span></div>
          <p className="mt-3 text-sm text-white/70">Caps made in our own workshop. One piece for you, or a thousand for your team.</p>
        </div>
        <div className="text-sm">
          <h4 className="mb-2 font-bold">Shop</h4>
          <ul className="space-y-1 text-white/70"><li><Link to="/products">All caps</Link></li><li><Link to="/orders">Track an order</Link></li><li><Link to="/wishlist">Wishlist</Link></li></ul>
        </div>
        <div className="text-sm">
          <h4 className="mb-2 font-bold">For business</h4>
          <ul className="space-y-1 text-white/70"><li><Link to="/register?type=business">Open a business account</Link></li><li>Bulk pricing and GST invoices</li><li>Custom logo quotes</li></ul>
        </div>
      </div>
      <div className="seam-line mx-auto max-w-7xl border-white/20" />
      <p className="px-4 py-4 text-center text-xs text-white/50">© {new Date().getFullYear()} {cfg?.storeName || 'CapStore'}. All rights reserved.</p>
    </footer>
  )
}

export default function Layout() {
  return (
    <div className="flex min-h-screen flex-col">
      <a href="#main" className="sr-only focus:not-sr-only focus:absolute focus:z-50 focus:bg-white focus:p-2">Skip to content</a>
      <Navbar />
      <main id="main" className="flex-1"><Outlet /></main>
      <Footer />
    </div>
  )
}
